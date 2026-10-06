package com.jasminesoftwaresolutions.witchwood.application;

import java.util.List;

public final class Env {
    private Env() {}

    public static int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "50051"));

    public static String DATABASE_URL = System.getenv("DATABASE_URL");
    public static String DATABASE_USERNAME = System.getenv("DATABASE_USERNAME");
    public static String DATABASE_PASSWORD = System.getenv("DATABASE_PASSWORD");

    public static String JWT_ISSUER = System.getenv("JWT_ISSUER");
    public static String JWT_AUDIENCE = System.getenv("JWT_AUDIENCE");
    public static String JWT_JWKS_URL =  System.getenv("JWT_JWKS_URL");

    public static String[] JWT_TRUSTED_SUBJECTS = System.getenv().getOrDefault("JWT_TRUSTED_SUBJECTS", "")
            .split(",");

    public static int JWT_JWKS_TTL = Integer.parseInt(System.getenv().getOrDefault("JWT_JWKS_TTL", "3600"));
    public static int SNAPSHOT_REFRESH_INTERVAL = Integer.parseInt(System.getenv().getOrDefault("SNAPSHOT_REFRESH_INTERVAL", "5"));
}
