package com.jasminesoftwaresolutions.witchwood.repository;

import org.jooq.DSLContext;

import java.util.List;
import java.util.UUID;

import static org.jooq.impl.DSL.*;

/**
 * CRUD operations for roles and their policy assignments.
 */
public class RoleRepository {
    private final DSLContext db;

    public RoleRepository(DSLContext db) {
        this.db = db;
    }

    public UUID createRole(UUID id) {
        db.insertInto(table("roles"))
                .columns(field("id"))
                .values(id)
                .execute();
        return id;
    }

    public boolean roleExists(UUID id) {
        return db.fetchExists(
                db.selectOne()
                        .from(table("roles"))
                        .where(field("id").eq(id)));
    }

    public void deleteRole(UUID id) {
        db.deleteFrom(table("role_policy_assignments"))
                .where(field("role_id").eq(id))
                .execute();
        db.deleteFrom(table("principal_role_assignments"))
                .where(field("role_id").eq(id))
                .execute();
        db.deleteFrom(table("roles"))
                .where(field("id").eq(id))
                .execute();
    }

    public void replacePolicyAssignments(UUID roleId, List<UUID> policyIds) {
        db.deleteFrom(table("role_policy_assignments"))
                .where(field("role_id").eq(roleId))
                .execute();

        for (UUID policyId : policyIds) {
            db.insertInto(table("role_policy_assignments"))
                    .columns(field("role_id"), field("policy_id"))
                    .values(roleId, policyId)
                    .execute();
        }
    }

    public void addPolicyAssignment(UUID roleId, UUID policyId) {
        db.insertInto(table("role_policy_assignments"))
                .columns(field("role_id"), field("policy_id"))
                .values(roleId, policyId)
                .onDuplicateKeyIgnore()
                .execute();
    }

    public void removePolicyAssignment(UUID roleId, UUID policyId) {
        db.deleteFrom(table("role_policy_assignments"))
                .where(field("role_id").eq(roleId))
                .and(field("policy_id").eq(policyId))
                .execute();
    }
}
