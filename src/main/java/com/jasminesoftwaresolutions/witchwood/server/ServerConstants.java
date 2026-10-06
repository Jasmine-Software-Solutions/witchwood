package com.jasminesoftwaresolutions.witchwood.server;

import io.grpc.Context;

/**
 * gRPC context keys used by the Witchwood server.
 */
public final class ServerConstants {
    private ServerConstants() {}

    /** The authenticated caller's {@code sub} claim. */
    public static final Context.Key<String> CALLER_SUB =
            Context.key("caller-sub");

    /** The JWT {@code iss} claim (whether the caller is the ID service). */
    public static final Context.Key<Boolean> IS_TRUSTED_SUB =
            Context.key("is-trusted-sub");
}
