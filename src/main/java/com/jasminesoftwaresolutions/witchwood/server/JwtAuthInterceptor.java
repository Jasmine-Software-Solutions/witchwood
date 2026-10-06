package com.jasminesoftwaresolutions.witchwood.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.grpc.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * gRPC interceptor that authenticates requests using a JWT Bearer token
 * and places the caller's {@code sub} and ID-service flag into the
 * gRPC {@link Context}.
 *
 * <p>Authorization (permission checking) is performed by
 * {@link WitchwoodGrpcService} on a per-method basis.
 */
public class JwtAuthInterceptor implements ServerInterceptor {

    private final String issuer;
    private final String audience;
    private final String jwksUrl;
    private final long jwksTtlMillis;
    private final String[] jwtTrustedSubjects;

    private static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
    private static final String BEARER_PREFIX = "Bearer ";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final Map<String, PublicKey> jwksCache = new ConcurrentHashMap<>();
    private volatile long jwksLastRefresh = 0;

    public JwtAuthInterceptor(String issuer,
                              String audience,
                              String jwksUrl,
                              long jwksTtlSeconds,
                              String[] jwtTrustedSubjects) {
        this.issuer = Objects.requireNonNull(issuer);
        this.audience = Objects.requireNonNull(audience);
        this.jwksUrl = Objects.requireNonNull(jwksUrl);
        this.jwksTtlMillis = TimeUnit.SECONDS.toMillis(jwksTtlSeconds);
        this.jwtTrustedSubjects = Objects.requireNonNull(jwtTrustedSubjects);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        System.out.println("[ww-server-jwt] interceptCall for " + call.getMethodDescriptor().getFullMethodName());

        String authHeader = headers.get(AUTHORIZATION_KEY);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            System.out.println("[ww-server-jwt] missing/invalid auth header");
            call.close(Status.UNAUTHENTICATED
                    .withDescription("Missing or invalid Authorization header"), headers);
            return new ServerCall.Listener<>() {};
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        Claims claims;
        try {
            System.out.println("[ww-server-jwt] parsing and validating token...");
            claims = parseAndValidate(token);
            System.out.println("[ww-server-jwt] token valid, sub=" + claims.getSubject());
        } catch (JwtException e) {
            System.out.println("[ww-server-jwt] invalid JWT: " + e.getMessage());
            call.close(Status.UNAUTHENTICATED
                    .withDescription("Invalid JWT: " + e.getMessage()), headers);
            return new ServerCall.Listener<>() {};
        } catch (Exception e) {
            System.out.println("[ww-server-jwt] JWT validation error: " + e.getMessage());
            call.close(Status.UNAUTHENTICATED
                    .withDescription("JWT validation failed: " + e.getMessage()), headers);
            return new ServerCall.Listener<>() {};
        }

        Set<String> aud = claims.getAudience();
        if (!aud.contains(audience)) {
            call.close(Status.UNAUTHENTICATED
                    .withDescription("Invalid JWT: Missing 'aud' claim with [" + audience + "]"), headers);
            return new ServerCall.Listener<>() {};
        }

        String sub = claims.getSubject();
        if (sub == null || sub.isBlank()) {
            call.close(Status.UNAUTHENTICATED
                    .withDescription("JWT missing sub claim"), headers);
            return new ServerCall.Listener<>() {};
        }

        // iss == aud means the ID service is calling home: trusted unconditionally
        String iss = claims.getIssuer();
        boolean isIdService = iss != null && iss.equals(sub);
        boolean isTrustedSubject = false;
        for (String jwtTrustedSubject : jwtTrustedSubjects) {
            if (jwtTrustedSubject.equals(sub))
                isTrustedSubject = true;
        }

        sub = sub.substring("https://".length(), sub.length() - 1);

        Context ctx = Context.current()
                .withValue(ServerConstants.CALLER_SUB, sub)
                .withValue(ServerConstants.IS_TRUSTED_SUB, isIdService || isTrustedSubject);
        return Contexts.interceptCall(ctx, call, headers, next);
    }

    private Claims parseAndValidate(String token) {
        String kid;
        try {
            var parts = token.split("\\.");
            if (parts.length < 2) throw new JwtException("Invalid JWT structure");
            var headerJson = new String(java.util.Base64.getUrlDecoder().decode(parts[0]));
            var header = JsonParser.parseString(headerJson).getAsJsonObject();
            JsonElement kidEl = header.get("kid");
            kid = kidEl != null && !kidEl.isJsonNull() ? kidEl.getAsString() : null;
        } catch (Exception e) {
            throw new JwtException("Unable to parse JWT header", e);
        }

        PublicKey key = resolveSigningKey(kid);
        if (key == null) {
            throw new JwtException("No matching key found for kid: " + kid);
        }

        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private PublicKey resolveSigningKey(String kid) {
        refreshJwksIfNeeded();
        if (kid != null) {
            PublicKey key = jwksCache.get(kid);
            if (key != null) return key;
        }
        refreshJwks();
        if (kid != null) return jwksCache.get(kid);
        if (!jwksCache.isEmpty()) return jwksCache.values().iterator().next();
        return null;
    }

    private void refreshJwksIfNeeded() {
        if (System.currentTimeMillis() - jwksLastRefresh > jwksTtlMillis) {
            refreshJwks();
        }
    }

    private synchronized void refreshJwks() {
        if (System.currentTimeMillis() - jwksLastRefresh <= jwksTtlMillis && !jwksCache.isEmpty()) {
            return;
        }

        try {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(jwksUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            var resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new RuntimeException("JWKS endpoint returned " + resp.statusCode());
            }
            jwksCache.putAll(parseJwks(resp.body()));
            jwksLastRefresh = System.currentTimeMillis();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch JWKS: " + e.getMessage(), e);
        }
    }

    private static Map<String, PublicKey> parseJwks(String json) throws Exception {
        Map<String, PublicKey> keys = new HashMap<>();
        var root = JsonParser.parseString(json).getAsJsonObject();
        var jwksKeys = root.getAsJsonArray("keys");
        if (jwksKeys == null) return keys;

        KeyFactory kf = KeyFactory.getInstance("RSA");
        for (var k : jwksKeys) {
            var keyObj = k.getAsJsonObject();
            String kid = keyObj.has("kid") ? keyObj.get("kid").getAsString() : null;
            String n = keyObj.get("n").getAsString();
            String e = keyObj.get("e").getAsString();

            byte[] nBytes = java.util.Base64.getUrlDecoder().decode(n);
            byte[] eBytes = java.util.Base64.getUrlDecoder().decode(e);

            var modulus = new java.math.BigInteger(1, nBytes);
            var exponent = new java.math.BigInteger(1, eBytes);

            var spec = new RSAPublicKeySpec(modulus, exponent);
            var pubKey = kf.generatePublic(spec);

            keys.put(kid != null ? kid : "__default__", pubKey);
        }
        return keys;
    }
}
