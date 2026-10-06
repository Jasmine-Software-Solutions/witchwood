package com.jasminesoftwaresolutions.witchwood.repository;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import org.jooq.DSLContext;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.jooq.impl.DSL.*;

/**
 * CRUD operations for policies and their statements, actions, and resources.
 */
public class PolicyRepository {
    private final DSLContext db;

    public PolicyRepository(DSLContext db) {
        this.db = db;
    }

    public UUID createPolicy(UUID id) {
        db.insertInto(table("policies"))
                .columns(field("id"))
                .values(id)
                .execute();
        return id;
    }

    public boolean policyExists(UUID id) {
        return db.fetchExists(
                db.selectOne()
                        .from(table("policies"))
                        .where(field("id").eq(id)));
    }

    public void deletePolicy(UUID id) {
        db.deleteFrom(table("statement_resources"))
                .where(field("policy_id").eq(id))
                .execute();
        db.deleteFrom(table("statement_actions"))
                .where(field("policy_id").eq(id))
                .execute();
        db.deleteFrom(table("policy_statements"))
                .where(field("policy_id").eq(id))
                .execute();
        db.deleteFrom(table("role_policy_assignments"))
                .where(field("policy_id").eq(id))
                .execute();
        db.deleteFrom(table("principal_policy_assignments"))
                .where(field("policy_id").eq(id))
                .execute();
        db.deleteFrom(table("policies"))
                .where(field("id").eq(id))
                .execute();
    }

    public void replaceStatements(UUID policyId, java.util.List<StatementData> statements) {
        db.deleteFrom(table("statement_resources"))
                .where(field("policy_id").eq(policyId))
                .execute();
        db.deleteFrom(table("statement_actions"))
                .where(field("policy_id").eq(policyId))
                .execute();
        db.deleteFrom(table("policy_statements"))
                .where(field("policy_id").eq(policyId))
                .execute();

        for (var stmt : statements) {
            var insert = db.insertInto(table("policy_statements"))
                    .columns(
                            field("policy_id"),
                            field("statement_id"),
                            field("effect"),
                            field("condition"));
            if (stmt.condition == null) {
                insert.values(policyId, stmt.statementId, stmt.effect, (Object) null);
            } else {
                insert.values(policyId, stmt.statementId, stmt.effect,
                        org.jooq.impl.DSL.cast(stmt.condition, org.jooq.impl.SQLDataType.JSONB));
            }
            insert.execute();

            for (String action : stmt.actions) {
                db.insertInto(table("statement_actions"))
                        .columns(field("policy_id"), field("statement_id"), field("action"))
                        .values(policyId, stmt.statementId, action)
                        .execute();
            }

            for (var resource : stmt.resources) {
                db.insertInto(table("statement_resources"))
                        .columns(field("policy_id"), field("statement_id"), field("resource"))
                        .values(policyId, stmt.statementId,
                                com.jasminesoftwaresolutions.witchwood.persistence.ResourcePathSerde.toProto(resource).toByteArray())
                        .execute();
            }
        }
    }

    public record StatementData(
            int statementId,
            String effect,
            Set<String> actions,
            Set<CompiledResourcePath> resources,
            String condition  // JSON string, nullable
    ) {
        public StatementData {
            if (actions == null) actions = Set.of();
            if (resources == null) resources = Set.of();
        }
    }
}
