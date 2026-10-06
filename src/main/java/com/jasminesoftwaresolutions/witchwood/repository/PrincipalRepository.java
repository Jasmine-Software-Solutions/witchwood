package com.jasminesoftwaresolutions.witchwood.repository;

import org.jooq.DSLContext;

import java.time.Instant;
import java.util.UUID;

import static org.jooq.impl.DSL.*;

/**
 * CRUD operations for principals and their role/policy assignments.
 */
public class PrincipalRepository {
    private final DSLContext db;

    public PrincipalRepository(DSLContext db) {
        this.db = db;
    }

    public UUID createPrincipal(UUID id) {
        db.insertInto(table("principals"))
                .columns(field("id"))
                .values(id)
                .execute();
        return id;
    }

    public boolean principalExists(UUID id) {
        return db.fetchExists(
                db.selectOne()
                        .from(table("principals"))
                        .where(field("id").eq(id)));
    }

    public void deletePrincipal(UUID id) {
        db.deleteFrom(table("principal_role_assignments"))
                .where(field("principal_id").eq(id))
                .execute();
        db.deleteFrom(table("principal_policy_assignments"))
                .where(field("principal_id").eq(id))
                .execute();
        db.deleteFrom(table("principals"))
                .where(field("id").eq(id))
                .execute();
    }

    public void replaceRoleAssignments(UUID principalId,
                                       java.util.List<RoleAssignment> assignments) {
        db.deleteFrom(table("principal_role_assignments"))
                .where(field("principal_id").eq(principalId))
                .execute();

        for (var a : assignments) {
            db.insertInto(table("principal_role_assignments"))
                    .columns(
                            field("principal_id"),
                            field("role_id"),
                            field("effective"),
                            field("expiration"))
                    .values(principalId, a.roleId, a.effective, a.expiration)
                    .execute();
        }
    }

    public void replacePolicyAssignments(UUID principalId,
                                         java.util.List<UUID> policyIds) {
        db.deleteFrom(table("principal_policy_assignments"))
                .where(field("principal_id").eq(principalId))
                .execute();

        for (UUID policyId : policyIds) {
            db.insertInto(table("principal_policy_assignments"))
                    .columns(field("principal_id"), field("policy_id"))
                    .values(principalId, policyId)
                    .execute();
        }
    }

    public void addRoleAssignment(UUID principalId, UUID roleId, Instant effective, Instant expiration) {
        db.insertInto(table("principal_role_assignments"))
                .columns(field("principal_id"), field("role_id"), field("effective"), field("expiration"))
                .values(principalId, roleId, effective, expiration)
                .onDuplicateKeyIgnore()
                .execute();
    }

    public void removeRoleAssignment(UUID principalId, UUID roleId) {
        db.deleteFrom(table("principal_role_assignments"))
                .where(field("principal_id").eq(principalId))
                .and(field("role_id").eq(roleId))
                .execute();
    }

    public void addPolicyAssignment(UUID principalId, UUID policyId) {
        db.insertInto(table("principal_policy_assignments"))
                .columns(field("principal_id"), field("policy_id"))
                .values(principalId, policyId)
                .onDuplicateKeyIgnore()
                .execute();
    }

    public void removePolicyAssignment(UUID principalId, UUID policyId) {
        db.deleteFrom(table("principal_policy_assignments"))
                .where(field("principal_id").eq(principalId))
                .and(field("policy_id").eq(policyId))
                .execute();
    }

    public record RoleAssignment(UUID roleId, Instant effective, Instant expiration) {
    }
}
