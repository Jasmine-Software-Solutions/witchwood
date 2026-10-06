package com.jasminesoftwaresolutions.witchwood.server;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;

import java.util.UUID;

/**
 * Helpers for constructing Witchwood {@link CompiledResourcePath} instances
 * for authorization checks.
 *
 * <p>The service name for all Witchwood-internal resources is
 * {@value #WITCHWOOD_FQDN}.
 */
public final class WitchwoodResources {
    private WitchwoodResources() {}

    /** The Witchwood service's fully-qualified domain name. */
    public static final String WITCHWOOD_FQDN = "witchwood.jasmine.software";

    /** The service-level resource (null root — for create operations). */
    public static final CompiledResourcePath SERVICE_ROOT =
            new CompiledResourcePath(WITCHWOOD_FQDN, null);

    public static CompiledResourcePath principal(UUID id) {
        return CompiledResourcePath.builder()
                .service(WITCHWOOD_FQDN)
                .one("principal", id)
                .build();
    }

    public static CompiledResourcePath policy(UUID id) {
        return CompiledResourcePath.builder()
                .service(WITCHWOOD_FQDN)
                .one("policy", id)
                .build();
    }

    public static CompiledResourcePath role(UUID id) {
        return CompiledResourcePath.builder()
                .service(WITCHWOOD_FQDN)
                .one("role", id)
                .build();
    }

    /**
     * Build the evaluate-permission resource for checking whether a caller
     * may evaluate a specific (service, action) pair.
     *
     * @param serviceName the caller's service FQDN (e.g. "onboarding.jasmine.software")
     * @param actionName  the action key (e.g. "CreateCandidate")
     * @return a path like {@code witchwood.jasmine.software://service:<F(serviceName)>/action:<F(actionName)>}
     */
    public static CompiledResourcePath evaluatePermission(String serviceName, String actionName) {
        UUID serviceId = UUID.nameUUIDFromBytes(serviceName.getBytes());
        UUID actionId = UUID.nameUUIDFromBytes(actionName.getBytes());

        return CompiledResourcePath.builder()
                .service(WITCHWOOD_FQDN)
                .one("service", serviceId)
                .one("action", actionId)
                .build();
    }
}
