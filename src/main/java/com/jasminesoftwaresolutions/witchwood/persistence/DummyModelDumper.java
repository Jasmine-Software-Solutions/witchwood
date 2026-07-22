package com.jasminesoftwaresolutions.witchwood.persistence;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers.DummyModelGenerator;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import com.jasminesoftwaresolutions.witchwood.models.*;
import io.github.jamsesso.jsonlogic.ast.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

public class DummyModelDumper {
    private static final int BATCH_SIZE = 1000;

    private final java.sql.Connection conn;

    public DummyModelDumper(java.sql.Connection conn) {
        this.conn = conn;
    }

    /**
     * Generate a full dummy model and dump everything to the database.
     */
    public void dump(DummyModelGenerator generator) {
        var principals = generator.principalObjects();

        dumpPolicies(principals);
        dumpRoles(principals);
        dumpPrincipals(principals);
    }

    private void dumpPolicies(List<IPrincipal> principals) {
        Set<UUID> seen = new LinkedHashSet<>();
        List<IPolicy> policies = new ArrayList<>();
        for (var p : principals) {
            for (var ra : p.roles())
                for (var rpa : ra.role().policies())
                    if (seen.add(rpa.policy().id())) policies.add(rpa.policy());
            for (var pa : p.policies())
                if (seen.add(pa.policy().id())) policies.add(pa.policy());
        }

        try (var psPolicy = conn.prepareStatement(
                "INSERT INTO policies (id) VALUES (?)");
             var psStmt = conn.prepareStatement(
                     "INSERT INTO policy_statements (policy_id, statement_id, effect, condition) VALUES (?, ?, ?, ?::jsonb)");
             var psAction = conn.prepareStatement(
                     "INSERT INTO statement_actions (policy_id, statement_id, action) VALUES (?, ?, ?)");
             var psResource = conn.prepareStatement(
                     "INSERT INTO statement_resources (policy_id, statement_id, resource) VALUES (?, ?, ?)")) {

            int count = 0;
            for (var policy : policies) {
                UUID pid = policy.id();
                psPolicy.setObject(1, pid);
                psPolicy.addBatch();

                for (var stmt : policy.statements()) {
                    psStmt.setObject(1, pid);
                    psStmt.setInt(2, stmt.id());
                    psStmt.setString(3, stmt.effect() == PolicyEffect.ALLOW ? "ALLOW" : "DENY");
                    psStmt.setString(4, conditionToJson(stmt.condition()));
                    psStmt.addBatch();

                    for (String action : stmt.actions()) {
                        psAction.setObject(1, pid);
                        psAction.setInt(2, stmt.id());
                        psAction.setString(3, action);
                        psAction.addBatch();
                    }

                    for (CompiledResourcePath res : stmt.resources()) {
                        psResource.setObject(1, pid);
                        psResource.setInt(2, stmt.id());
                        psResource.setBytes(3, ResourcePathSerde.toProto(res).toByteArray());
                        psResource.addBatch();
                    }
                }

                if (++count % BATCH_SIZE == 0) {
                    psPolicy.executeBatch();
                    psStmt.executeBatch();
                    psAction.executeBatch();
                    psResource.executeBatch();
                }
            }

            psPolicy.executeBatch();
            psStmt.executeBatch();
            psAction.executeBatch();
            psResource.executeBatch();
        } catch (Exception e) {
            throw new RuntimeException("Failed to dump policies", e);
        }
    }

    private void dumpRoles(List<IPrincipal> principals) {
        Set<UUID> seen = new LinkedHashSet<>();
        Map<UUID, List<UUID>> rolePolicyMap = new LinkedHashMap<>();

        for (var p : principals) {
            for (var ra : p.roles()) {
                UUID rid = ra.role().id();
                if (seen.add(rid)) {
                    List<UUID> pids = new ArrayList<>();
                    for (var rpa : ra.role().policies())
                        pids.add(rpa.policy().id());
                    rolePolicyMap.put(rid, pids);
                }
            }
        }

        try (var psRole = conn.prepareStatement(
                "INSERT INTO roles (id) VALUES (?)");
             var psAssign = conn.prepareStatement(
                     "INSERT INTO role_policy_assignments (role_id, policy_id) VALUES (?, ?)")) {

            int count = 0;
            for (var entry : rolePolicyMap.entrySet()) {
                UUID rid = entry.getKey();
                psRole.setObject(1, rid);
                psRole.addBatch();

                for (UUID pid : entry.getValue()) {
                    psAssign.setObject(1, rid);
                    psAssign.setObject(2, pid);
                    psAssign.addBatch();
                }

                if (++count % BATCH_SIZE == 0) {
                    psRole.executeBatch();
                    psAssign.executeBatch();
                }
            }

            psRole.executeBatch();
            psAssign.executeBatch();
        } catch (Exception e) {
            throw new RuntimeException("Failed to dump roles", e);
        }
    }

    private void dumpPrincipals(List<IPrincipal> principals) {
        try (var psPrincipal = conn.prepareStatement(
                "INSERT INTO principals (id) VALUES (?)");
             var psRoleAssign = conn.prepareStatement(
                     "INSERT INTO principal_role_assignments (principal_id, role_id, effective, expiration) VALUES (?, ?, ?, ?)");
             var psPolicyAssign = conn.prepareStatement(
                     "INSERT INTO principal_policy_assignments (principal_id, policy_id) VALUES (?, ?)")) {

            int count = 0;
            for (var p : principals) {
                UUID pid = p.id();
                psPrincipal.setObject(1, pid);
                psPrincipal.addBatch();

                for (var ra : p.roles()) {
                    psRoleAssign.setObject(1, pid);
                    psRoleAssign.setObject(2, ra.role().id());
                    psRoleAssign.setTimestamp(3, toSqlTimestamp(ra.effective()));
                    psRoleAssign.setTimestamp(4, toSqlTimestamp(ra.expiration()));
                    psRoleAssign.addBatch();
                }

                for (var pa : p.policies()) {
                    psPolicyAssign.setObject(1, pid);
                    psPolicyAssign.setObject(2, pa.policy().id());
                    psPolicyAssign.addBatch();
                }

                if (++count % BATCH_SIZE == 0) {
                    psPrincipal.executeBatch();
                    psRoleAssign.executeBatch();
                    psPolicyAssign.executeBatch();
                }
            }

            psPrincipal.executeBatch();
            psRoleAssign.executeBatch();
            psPolicyAssign.executeBatch();
        } catch (Exception e) {
            throw new RuntimeException("Failed to dump principals", e);
        }
    }

    private static Timestamp toSqlTimestamp(Instant instant) {
        if (instant == Instant.MAX)
            return Timestamp.valueOf("9999-12-31 23:59:59");
        return Timestamp.from(instant);
    }

    static String conditionToJson(JsonLogicNode node) {
        if (node == null) return null;
        return GSON.toJson(toJsonElement(node));
    }

    private static final Gson GSON = new Gson();

    private static JsonElement toJsonElement(JsonLogicNode node) {
        return switch (node.getType()) {
            case PRIMITIVE -> primitiveToJson((JsonLogicPrimitive<?>) node);
            case VARIABLE -> variableToJson((JsonLogicVariable) node);
            case ARRAY -> arrayToJson((JsonLogicArray) node);
            case OPERATION -> operationToJson((JsonLogicOperation) node);
        };
    }

    private static JsonElement primitiveToJson(JsonLogicPrimitive<?> p) {
        return switch (p.getPrimitiveType()) {
            case STRING -> new JsonPrimitive(((JsonLogicString) p).getValue());
            case NUMBER -> new JsonPrimitive(((JsonLogicNumber) p).getValue());
            case BOOLEAN -> new JsonPrimitive(((JsonLogicBoolean) p).getValue());
            case NULL -> JsonNull.INSTANCE;
        };
    }

    private static JsonElement variableToJson(JsonLogicVariable v) {
        JsonArray args = new JsonArray();
        args.add(toJsonElement(v.getKey()));
        if (!(v.getDefaultValue() instanceof JsonLogicNull))
            args.add(toJsonElement(v.getDefaultValue()));
        JsonObject obj = new JsonObject();
        obj.add("var", args);
        return obj;
    }

    private static JsonElement arrayToJson(JsonLogicArray arr) {
        JsonArray json = new JsonArray();
        for (JsonLogicNode el : arr)
            json.add(toJsonElement(el));
        return json;
    }

    private static JsonElement operationToJson(JsonLogicOperation op) {
        JsonObject obj = new JsonObject();
        obj.add(op.getOperator(), toJsonElement(op.getArguments()));
        return obj;
    }
}
