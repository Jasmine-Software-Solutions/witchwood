package com.jasminesoftwaresolutions.witchwood.repository;

import org.jooq.DSLContext;

import java.util.UUID;

import static org.jooq.impl.DSL.*;

/**
 * CRUD operations for service and action registrations.
 *
 * <p>Service IDs are always derived via
 * {@link UUID#nameUUIDFromBytes(byte[])} from the service FQDN;
 * callers never supply an explicit ID.
 */
public class ServiceRepository {
    private final DSLContext db;

    public ServiceRepository(DSLContext db) {
        this.db = db;
    }

    /**
     * Registers a service. The ID is derived from the FQDN.
     *
     * @param fqdn the fully-qualified domain name (e.g. "onboarding.jasmine.software")
     * @return the derived UUID
     * @throws org.jooq.exception.DataAccessException if the service already exists
     */
    public UUID registerService(String fqdn) {
        UUID id = UUID.nameUUIDFromBytes(fqdn.getBytes());
        db.insertInto(table("services"))
                .columns(field("id"), field("name"))
                .values(id, fqdn)
                .onConflict(field("id"))
                .doNothing()
                .execute();
        return id;
    }

    /** Returns true if the service is registered. */
    public boolean serviceExists(String fqdn) {
        UUID id = UUID.nameUUIDFromBytes(fqdn.getBytes());
        return db.fetchExists(
                db.selectOne()
                        .from(table("services"))
                        .where(field("id").eq(id)));
    }

    /**
     * Deregisters a service and all its actions.
     *
     * @param fqdn the FQDN of the service to remove
     */
    public void deregisterService(String fqdn) {
        UUID id = UUID.nameUUIDFromBytes(fqdn.getBytes());
        db.deleteFrom(table("service_actions"))
                .where(field("service_id").eq(id))
                .execute();
        db.deleteFrom(table("services"))
                .where(field("id").eq(id))
                .execute();
    }

    /**
     * Overwrites the entire set of actions for a service.
     * All existing actions are deleted and replaced with the given list.
     *
     * @param fqdn    the service FQDN
     * @param actions the complete set of action keys (e.g. ["CreateCandidate", "DeleteCandidate"])
     */
    public void putActions(String fqdn, java.util.List<String> actions) {
        UUID serviceId = UUID.nameUUIDFromBytes(fqdn.getBytes());
        db.transaction(configuration -> {
            var ctx = configuration.dsl();
            ctx.deleteFrom(table("service_actions"))
                    .where(field("service_id").eq(serviceId))
                    .execute();
            if (!actions.isEmpty()) {
                var insert = ctx.insertInto(table("service_actions"),
                        field("service_id"), field("action"));
                for (String action : actions) {
                    insert = insert.values(serviceId, action);
                }
                insert.execute();
            }
        });
    }

    /** Derives the service UUID from an FQDN without touching the database. */
    public static UUID serviceId(String fqdn) {
        return UUID.nameUUIDFromBytes(fqdn.getBytes());
    }
}
