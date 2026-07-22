package com.jasminesoftwaresolutions.witchwood.persistence;

import com.jasminesoftwaresolutions.witchwood.entities.*;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import com.jasminesoftwaresolutions.witchwood.models.*;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.ast.JsonLogicParser;
import org.jooq.DSLContext;

import java.time.Instant;
import java.util.*;

import static org.jooq.impl.DSL.*;

/**
 * A {@link PrincipalSource} that streams principals from a PostgreSQL database,
 * assembling the full permission graph for each principal and handing it to
 * the compiler one at a time.
 *
 * <p>The shared subgraph (policies, roles, and their relationships) is loaded
 * once at construction and reused across all {@link #accept(PrincipalConsumer)}
 * calls. Principal and assignment data is streamed via lazy cursors with
 * merge-join — no large lists are materialized in memory.</p>
 */
public class PersistedPrincipalSource implements PrincipalSource {

    private final DSLContext db;
    private final Instant asOf;
    private final Map<UUID, Policy> policies;
    private final Map<UUID, Role> roles;

    public PersistedPrincipalSource(DSLContext db, Instant asOf) {
        this.db = Objects.requireNonNull(db);
        this.asOf = Objects.requireNonNull(asOf);
        this.policies = loadAllPolicies();
        this.roles = loadAllRoles();
    }

    @Override
    public void accept(PrincipalConsumer consumer) {
        try (
                var principalCursor = db.select(field("id", UUID.class))
                        .from(table("principals"))
                        .orderBy(field("id"))
                        .fetchLazy();
                var raCursor = db.select(
                                field("principal_id", UUID.class),
                                field("role_id", UUID.class),
                                field("effective", Instant.class),
                                field("expiration", Instant.class))
                        .from(table("principal_role_assignments"))
                        .orderBy(field("principal_id"))
                        .fetchLazy();
                var paCursor = db.select(
                                field("principal_id", UUID.class),
                                field("policy_id", UUID.class))
                        .from(table("principal_policy_assignments"))
                        .orderBy(field("principal_id"))
                        .fetchLazy()
        ) {
            var raIter = raCursor.iterator();
            var paIter = paCursor.iterator();
            org.jooq.Record nextRa = raIter.hasNext() ? raIter.next() : null;
            org.jooq.Record nextPa = paIter.hasNext() ? paIter.next() : null;

            for (var principalRecord : principalCursor) {
                UUID principalId = principalRecord.get(0, UUID.class);
                var principal = new Principal(principalId);

                while (nextRa != null) {
                    UUID raPid = nextRa.get(0, UUID.class);
                    int cmp = raPid.compareTo(principalId);
                    if (cmp < 0) { nextRa = advance(raIter); continue; }
                    if (cmp > 0) break;
                    attachRole(principal, nextRa);
                    nextRa = advance(raIter);
                }

                while (nextPa != null) {
                    UUID paPid = nextPa.get(0, UUID.class);
                    int cmp = paPid.compareTo(principalId);
                    if (cmp < 0) { nextPa = advance(paIter); continue; }
                    if (cmp > 0) break;
                    attachDirectPolicy(principal, nextPa);
                    nextPa = advance(paIter);
                }

                consumer.accept(principal);
            }
        }
    }

    private static <R extends org.jooq.Record> R advance(Iterator<R> iter) {
        return iter.hasNext() ? iter.next() : null;
    }

    private void attachRole(Principal principal, org.jooq.Record row) {
        UUID roleId = row.get(1, UUID.class);
        Instant effective = row.get(2, Instant.class);
        Instant expiration = row.get(3, Instant.class);

        if (effective == null) effective = Instant.EPOCH;
        if (expiration == null) expiration = Instant.MAX;

        if (expiration.isBefore(asOf) || effective.isAfter(asOf))
            return;

        Role role = roles.get(roleId);
        if (role != null)
            principal.addRole(new PrincipalRoleAssignment(
                    principal, role, effective, expiration));
    }

    private void attachDirectPolicy(Principal principal, org.jooq.Record row) {
        Policy policy = policies.get(row.get(1, UUID.class));
        if (policy != null)
            principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));
    }

    private Map<UUID, Policy> loadAllPolicies() {
        Map<UUID, Policy> policies = new LinkedHashMap<>();
        var ids = new LinkedHashSet<UUID>();

        for (var r : db.select(field("id", UUID.class)).from(table("policies")).fetch()) {
            UUID id = r.get(0, UUID.class);
            ids.add(id);
            policies.put(id, new Policy(id));
        }
        if (ids.isEmpty()) return policies;

        record Key(UUID pid, int sid) {}

        // Statements
        Map<Key, Object[]> stmts = new LinkedHashMap<>();
        for (var r : db.select(
                field("policy_id", UUID.class), field("statement_id", Integer.class),
                field("effect", String.class), field("condition", String.class))
                .from(table("policy_statements")).where(field("policy_id", UUID.class).in(ids))
                .orderBy(field("policy_id"), field("statement_id")).fetch()) {
            stmts.put(new Key(r.get(0, UUID.class), r.get(1, Integer.class)),
                    new Object[]{r.get(2, String.class), r.get(3, String.class)});
        }

        // Actions
        Map<Key, Set<String>> actions = new LinkedHashMap<>();
        for (var r : db.select(
                field("policy_id", UUID.class), field("statement_id", Integer.class),
                field("action", String.class))
                .from(table("statement_actions")).where(field("policy_id", UUID.class).in(ids))
                .orderBy(field("policy_id"), field("statement_id")).fetch()) {
            actions.computeIfAbsent(new Key(r.get(0, UUID.class), r.get(1, Integer.class)),
                    k -> new LinkedHashSet<>()).add(r.get(2, String.class));
        }

        // Resources
        Map<Key, Set<CompiledResourcePath>> resources = new LinkedHashMap<>();
        for (var r : db.select(
                field("policy_id", UUID.class), field("statement_id", Integer.class),
                field("resource", byte[].class))
                .from(table("statement_resources")).where(field("policy_id", UUID.class).in(ids))
                .orderBy(field("policy_id"), field("statement_id")).fetch()) {
            resources.computeIfAbsent(new Key(r.get(0, UUID.class), r.get(1, Integer.class)),
                    k -> new LinkedHashSet<>())
                    .add(ResourcePathSerde.fromProtoBytes(r.get(2, byte[].class)));
        }

        // Assemble
        for (var e : stmts.entrySet()) {
            var k = e.getKey();
            var v = e.getValue();
            PolicyEffect eff = "ALLOW".equals(v[0]) ? PolicyEffect.ALLOW : PolicyEffect.DENY;
            JsonLogicNode cond = null;
            String cj = (String) v[1];
            if (cj != null && !cj.isBlank()) {
                try { cond = JsonLogicParser.parse(cj); }
                catch (io.github.jamsesso.jsonlogic.ast.JsonLogicParseException ex) {
                    throw new RuntimeException("Bad condition policy=" + k.pid + " stmt=" + k.sid, ex);
                }
            }
            policies.get(k.pid).addStatement(new PolicyStatement(
                    policies.get(k.pid), k.sid, eff,
                    actions.getOrDefault(k, Set.of()),
                    resources.getOrDefault(k, Set.of()), cond));
        }

        return policies;
    }

    private Map<UUID, Role> loadAllRoles() {
        Map<UUID, Role> roles = new LinkedHashMap<>();
        var ids = new LinkedHashSet<UUID>();
        for (var r : db.select(field("id", UUID.class)).from(table("roles")).fetch()) {
            UUID id = r.get(0, UUID.class);
            ids.add(id);
            roles.put(id, new Role(id));
        }
        if (ids.isEmpty()) return roles;

        for (var r : db.select(field("role_id", UUID.class), field("policy_id", UUID.class))
                .from(table("role_policy_assignments")).where(field("role_id", UUID.class).in(ids))
                .fetch()) {
            Role role = roles.get(r.get(0, UUID.class));
            Policy policy = policies.get(r.get(1, UUID.class));
            if (role != null && policy != null)
                role.addPolicy(new RolePolicyAssignment(role, policy));
        }
        return roles;
    }
}
