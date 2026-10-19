package com.jasminesoftwaresolutions.witchwood.client;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import com.jasminesoftwaresolutions.witchwood.proto.v1.*;
import com.jasminesoftwaresolutions.witchwood.persistence.ResourcePathSerde;
import io.grpc.CallCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.netty.NettyChannelBuilder;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * High-level async client for the Witchwood gRPC API.
 *
 * <pre>{@code
 * var client = new WitchwoodClient("localhost", 50051, () -> jwtToken);
 * client.createPrincipal(List.of(), List.of())
 *       .thenAccept(id -> System.out.println("Created: " + id));
 * }</pre>
 */
public class WitchwoodClient implements AutoCloseable {
    private static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private final ManagedChannel channel;
    private final WitchwoodGrpc.WitchwoodFutureStub stub;

    /**
     * @param host          the gRPC server hostname
     * @param port          the gRPC server port
     * @param tokenSupplier supplies a fresh JWT Bearer token for each call
     */
    public WitchwoodClient(String host, int port, Supplier<String> tokenSupplier) {
        // Use an explicit gRPC DNS target rather than pre-resolving into an
        // InetSocketAddress: the server can move (Spot replacement) and the
        // channel must follow it instead of pinning the first IP forever.
        // Without the dns:/// scheme gRPC can select the unix resolver.
        this(NettyChannelBuilder.forTarget("dns:///" + host + ":" + port)
                .usePlaintext()
                .build(), tokenSupplier);
    }

    /**
     * @param channel       a pre-configured managed channel (e.g. with TLS)
     * @param tokenSupplier supplies a fresh JWT Bearer token for each call
     */
    public WitchwoodClient(ManagedChannel channel, Supplier<String> tokenSupplier) {
        this.channel = Objects.requireNonNull(channel);
        this.stub = WitchwoodGrpc.newFutureStub(channel)
                .withCallCredentials(new TokenCallCredentials(tokenSupplier));
    }

    // -----------------------------------------------------------------------
    // Principal
    // -----------------------------------------------------------------------

    public CompletableFuture<UUID> createPrincipal(UUID id,
                                                    List<RoleAssignment> roleAssignments,
                                                    List<UUID> policyIds) {
        var builder = ProtoCreatePrincipalRequest.newBuilder();
        if (id != null) builder.setId(id.toString());
        for (var ra : roleAssignments) {
            builder.addRoles(ProtoPrincipalRoleAssignment.newBuilder()
                    .setRoleId(ra.roleId.toString())
                    .setWindow(toTimestampRange(ra.effective, ra.expiration)));
        }

        for (var pid : policyIds) {
            builder.addPolicies(ProtoPrincipalPolicyAssignment.newBuilder()
                    .setPolicyId(pid.toString()));
        }

        return toFuture(stub.createPrincipal(builder.build()))
                .thenApply(r -> UUID.fromString(r.getId()));
    }

    public CompletableFuture<PrincipalInfo> getPrincipal(UUID id) {
        return toFuture(stub.getPrincipal(ProtoGetPrincipalRequest.newBuilder()
                .setId(id.toString()).build()))
                .thenApply(r -> toPrincipalInfo(r.getPrincipal()));
    }

    public CompletableFuture<Void> updatePrincipal(UUID id,
                                                    List<RoleAssignment> roleAssignments,
                                                    List<UUID> policyIds) {
        var builder = ProtoUpdatePrincipalRequest.newBuilder().setId(id.toString());
        for (var ra : roleAssignments) {
            builder.addRoles(ProtoPrincipalRoleAssignment.newBuilder()
                    .setRoleId(ra.roleId.toString())
                    .setWindow(toTimestampRange(ra.effective, ra.expiration)));
        }
        for (var pid : policyIds) {
            builder.addPolicies(ProtoPrincipalPolicyAssignment.newBuilder()
                    .setPolicyId(pid.toString()));
        }
        return toFuture(stub.updatePrincipal(builder.build()))
                .thenApply(r -> null);
    }

    public CompletableFuture<Void> deletePrincipal(UUID id) {
        return toFuture(stub.deletePrincipal(ProtoDeletePrincipalRequest.newBuilder()
                .setId(id.toString()).build()))
                .thenApply(r -> null);
    }

    // -----------------------------------------------------------------------
    // Policy
    // -----------------------------------------------------------------------

    public CompletableFuture<UUID> createPolicy(UUID id, List<PolicyStatement> statements) {
        var builder = ProtoCreatePolicyRequest.newBuilder();
        if (id != null) builder.setId(id.toString());
        for (var s : statements) builder.addStatements(toProto(s));
        return toFuture(stub.createPolicy(builder.build()))
                .thenApply(r -> UUID.fromString(r.getId()));
    }

    public CompletableFuture<PolicyInfo> getPolicy(UUID id) {
        return toFuture(stub.getPolicy(ProtoGetPolicyRequest.newBuilder()
                .setId(id.toString()).build()))
                .thenApply(r -> toPolicyInfo(r.getPolicy()));
    }

    public CompletableFuture<Void> updatePolicy(UUID id, List<PolicyStatement> statements) {
        var builder = ProtoUpdatePolicyRequest.newBuilder().setId(id.toString());
        for (var s : statements) builder.addStatements(toProto(s));
        return toFuture(stub.updatePolicy(builder.build()))
                .thenApply(r -> null);
    }

    public CompletableFuture<Void> deletePolicy(UUID id) {
        return toFuture(stub.deletePolicy(ProtoDeletePolicyRequest.newBuilder()
                .setId(id.toString()).build()))
                .thenApply(r -> null);
    }

    // -----------------------------------------------------------------------
    // Role
    // -----------------------------------------------------------------------

    public CompletableFuture<UUID> createRole(UUID id, Set<UUID> policyIds) {
        var builder = ProtoCreateRoleRequest.newBuilder();
        if (id != null) builder.setId(id.toString());
        for (var pid : policyIds) builder.addPolicyIds(pid.toString());
        return toFuture(stub.createRole(builder.build()))
                .thenApply(r -> UUID.fromString(r.getId()));
    }

    public CompletableFuture<RoleInfo> getRole(UUID id) {
        return toFuture(stub.getRole(ProtoGetRoleRequest.newBuilder()
                .setId(id.toString()).build()))
                .thenApply(r -> toRoleInfo(r.getRole()));
    }

    public CompletableFuture<Void> updateRole(UUID id, Set<UUID> policyIds) {
        var builder = ProtoUpdateRoleRequest.newBuilder().setId(id.toString());
        for (var pid : policyIds) builder.addPolicyIds(pid.toString());
        return toFuture(stub.updateRole(builder.build()))
                .thenApply(r -> null);
    }

    public CompletableFuture<Void> deleteRole(UUID id) {
        return toFuture(stub.deleteRole(ProtoDeleteRoleRequest.newBuilder()
                .setId(id.toString()).build()))
                .thenApply(r -> null);
    }

    // -- Incremental role-policy assignments --

    public CompletableFuture<Void> assignPolicyToRole(UUID roleId, UUID policyId) {
        return toFuture(stub.assignPolicyToRole(ProtoAssignPolicyToRoleRequest.newBuilder()
                .setRoleId(roleId.toString())
                .setPolicyId(policyId.toString())
                .build()))
                .thenApply(r -> null);
    }

    public CompletableFuture<Void> unassignPolicyFromRole(UUID roleId, UUID policyId) {
        return toFuture(stub.unassignPolicyFromRole(ProtoUnassignPolicyFromRoleRequest.newBuilder()
                .setRoleId(roleId.toString())
                .setPolicyId(policyId.toString())
                .build()))
                .thenApply(r -> null);
    }

    // -- Incremental principal-role assignments --

    public CompletableFuture<Void> assignRoleToPrincipal(UUID principalId, UUID roleId) {
        return toFuture(stub.assignRoleToPrincipal(ProtoAssignRoleToPrincipalRequest.newBuilder()
                .setPrincipalId(principalId.toString())
                .setRoleId(roleId.toString())
                .build()))
                .thenApply(r -> null);
    }

    public CompletableFuture<Void> unassignRoleFromPrincipal(UUID principalId, UUID roleId) {
        return toFuture(stub.unassignRoleFromPrincipal(ProtoUnassignRoleFromPrincipalRequest.newBuilder()
                .setPrincipalId(principalId.toString())
                .setRoleId(roleId.toString())
                .build()))
                .thenApply(r -> null);
    }

    // -- Incremental principal-policy assignments --

    public CompletableFuture<Void> assignPolicyToPrincipal(UUID principalId, UUID policyId) {
        return toFuture(stub.assignPolicyToPrincipal(ProtoAssignPolicyToPrincipalRequest.newBuilder()
                .setPrincipalId(principalId.toString())
                .setPolicyId(policyId.toString())
                .build()))
                .thenApply(r -> null);
    }

    public CompletableFuture<Void> unassignPolicyFromPrincipal(UUID principalId, UUID policyId) {
        return toFuture(stub.unassignPolicyFromPrincipal(ProtoUnassignPolicyFromPrincipalRequest.newBuilder()
                .setPrincipalId(principalId.toString())
                .setPolicyId(policyId.toString())
                .build()))
                .thenApply(r -> null);
    }

    // -----------------------------------------------------------------------
    // Evaluate
    // -----------------------------------------------------------------------

    public CompletableFuture<Boolean> evaluate(UUID principal,
                                                String action,
                                                CompiledResourcePath resource,
                                                Map<String, String> context) {
        var builder = ProtoEvaluateRequest.newBuilder()
                .setPrincipal(principal.toString())
                .setAction(action)
                .setResource(ResourcePathSerde.toProto(resource));
        if (context != null) builder.putAllContext(context);
        return toFuture(stub.evaluate(builder.build()))
                .thenApply(ProtoEvaluateResponse::getAllowed);
    }

    public CompletableFuture<Boolean> evaluate(String service,
                                                String action,
                                                CompiledResourcePath resource,
                                                Map<String, String> context) {
        UUID principalId = UUID.nameUUIDFromBytes(service.getBytes());
        return evaluate(principalId, action, resource, context);
    }

    // -----------------------------------------------------------------------
    // Query
    // -----------------------------------------------------------------------

    /**
     * Query all resource patterns that a principal can access for a given action.
     *
     * @param principal the principal UUID
     * @param action    the full action string (e.g. {@code "id.jasmine.software:ListOrganizations"})
     * @return a future that completes with the set of resource patterns
     */
    public CompletableFuture<Set<CompiledResourcePath>> queryResources(UUID principal, String action) {
        return toFuture(stub.queryResources(ProtoQueryResourcesRequest.newBuilder()
                .setPrincipal(principal.toString())
                .setAction(action)
                .build()))
                .thenApply(r -> r.getResourcesList().stream()
                        .map(ResourcePathSerde::fromProto)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    // -----------------------------------------------------------------------
    // Service & Action Registration
    // -----------------------------------------------------------------------

    public CompletableFuture<UUID> registerService(String fqdn) {
        return toFuture(stub.registerService(ProtoRegisterServiceRequest.newBuilder()
                .setName(fqdn).build()))
                .thenApply(r -> UUID.fromString(r.getId()));
    }

    public CompletableFuture<Void> deregisterService(String fqdn) {
        return toFuture(stub.deregisterService(ProtoDeregisterServiceRequest.newBuilder()
                .setName(fqdn).build()))
                .thenApply(r -> null);
    }

    public CompletableFuture<Void> putActions(String fqdn, List<String> actions) {
        return toFuture(stub.putActions(ProtoPutActionsRequest.newBuilder()
                .setService(fqdn).addAllActions(actions).build()))
                .thenApply(r -> null);
    }

    @Override
    public void close() {
        channel.shutdown();
        try {
            if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                channel.shutdownNow();
            }
        } catch (InterruptedException e) {
            channel.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private static <T> CompletableFuture<T> toFuture(
            com.google.common.util.concurrent.ListenableFuture<T> listenable) {
        var future = new CompletableFuture<T>();
        listenable.addListener(() -> {
            try {
                future.complete(listenable.get());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }, Runnable::run);
        return future;
    }

    private static ProtoPolicyStatement toProto(PolicyStatement s) {
        var builder = ProtoPolicyStatement.newBuilder()
                .setId(s.id)
                .setEffect(s.effect);
        for (var a : s.actions) builder.addActions(a);
        for (var r : s.resources) builder.addResources(ResourcePathSerde.toProto(r));
        if (s.condition != null && !s.condition.isBlank()) {
            builder.setCondition(ProtoStatementCondition.newBuilder().setJson(s.condition));
        }
        return builder.build();
    }

    private static ProtoTimestampRange toTimestampRange(Instant effective, Instant expiration) {
        return ProtoTimestampRange.newBuilder()
                .setEffective(effective != null ? effective.toEpochMilli() : 0)
                .setExpiration(expiration != null ? expiration.toEpochMilli() : Long.MAX_VALUE)
                .build();
    }

    private static PrincipalInfo toPrincipalInfo(
            ProtoPrincipal r) {
        List<RoleAssignment> roles = new ArrayList<>();
        for (var ra : r.getRolesList()) {
            roles.add(new RoleAssignment(
                    UUID.fromString(ra.getRoleId()),
                    toInstant(ra.getWindow().getEffective()),
                    toInstant(ra.getWindow().getExpiration())));
        }
        List<UUID> policies = new ArrayList<>();
        for (var pa : r.getPoliciesList()) {
            policies.add(UUID.fromString(pa.getPolicyId()));
        }
        return new PrincipalInfo(UUID.fromString(r.getId()), roles, policies);
    }

    private static PolicyInfo toPolicyInfo(
            ProtoPolicy r) {
        List<PolicyStatement> stmts = new ArrayList<>();
        for (var s : r.getStatementsList()) {
            Set<String> actions = new LinkedHashSet<>(s.getActionsList());
            Set<CompiledResourcePath> resources = s.getResourcesList().stream()
                    .map(ResourcePathSerde::fromProto)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            String cond = s.hasCondition() ? s.getCondition().getJson() : null;
            stmts.add(new PolicyStatement(s.getId(), s.getEffect(), actions, resources, cond));
        }
        return new PolicyInfo(UUID.fromString(r.getId()), stmts);
    }

    private static RoleInfo toRoleInfo(ProtoRole r) {
        Set<UUID> policyIds = new LinkedHashSet<>();
        for (var pid : r.getPolicyIdsList()) {
            policyIds.add(UUID.fromString(pid));
        }
        return new RoleInfo(UUID.fromString(r.getId()), policyIds);
    }

    private static Instant toInstant(long epochMillis) {
        if (epochMillis == 0) return Instant.EPOCH;
        if (epochMillis == Long.MAX_VALUE) return Instant.MAX;
        return Instant.ofEpochMilli(epochMillis);
    }

    // -----------------------------------------------------------------------
    // CallCredentials
    // -----------------------------------------------------------------------

    private static class TokenCallCredentials extends CallCredentials {
        private final Supplier<String> tokenSupplier;

        TokenCallCredentials(Supplier<String> tokenSupplier) {
            this.tokenSupplier = Objects.requireNonNull(tokenSupplier);
        }

        @Override
        public void applyRequestMetadata(RequestInfo requestInfo,
                                         Executor appExecutor,
                                         MetadataApplier applier) {
            appExecutor.execute(() -> {
                try {
                    String token = tokenSupplier.get();
                    if (token != null && !token.isBlank()) {
                        Metadata headers = new Metadata();
                        headers.put(AUTHORIZATION_KEY, "Bearer " + token);
                        applier.apply(headers);
                    } else {
                        applier.apply(new Metadata());
                    }
                } catch (Exception e) {
                    applier.fail(io.grpc.Status.UNAUTHENTICATED
                            .withDescription("Failed to obtain token")
                            .withCause(e));
                }
            });
        }
    }

    // -----------------------------------------------------------------------
    // Value types
    // -----------------------------------------------------------------------

    /** A role assignment on a principal, with an optional validity window. */
    public record RoleAssignment(UUID roleId, Instant effective, Instant expiration) {
        public RoleAssignment {
            Objects.requireNonNull(roleId);
        }
        public RoleAssignment(UUID roleId) {
            this(roleId, null, null);
        }
    }

    /** A single policy statement. */
    public record PolicyStatement(
            int id,
            String effect,
            Set<String> actions,
            Set<CompiledResourcePath> resources,
            String condition) {
        public PolicyStatement {
            Objects.requireNonNull(effect);
            Objects.requireNonNull(actions);
            Objects.requireNonNull(resources);
        }
    }

    public record PrincipalInfo(UUID id, List<RoleAssignment> roles, List<UUID> policies) {}
    public record PolicyInfo(UUID id, List<PolicyStatement> statements) {}
    public record RoleInfo(UUID id, Set<UUID> policyIds) {}
}
