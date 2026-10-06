package com.jasminesoftwaresolutions.witchwood.evaluation;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.*;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.ServiceDirectory;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers.ModelSnapshotCompiler;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import com.jasminesoftwaresolutions.witchwood.entities.*;
import com.jasminesoftwaresolutions.witchwood.models.*;
import com.jasminesoftwaresolutions.witchwood.util.JsonLogicEvaluators;
import io.github.jamsesso.jsonlogic.ast.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class EvaluationEngineTest {
    private static final String SERVICE = "svc";
    private static final String ACTION_A = SERVICE + ":a";
    private static final String ACTION_B = SERVICE + ":b";

    private ServiceDirectory serviceDirectory;
    private EvaluationSnapshot snapshot;
    private EvaluationEngine engine;

    @BeforeEach
    void setUp() {
        serviceDirectory = ServiceDirectory.from(Map.of(SERVICE, Set.of("a", "b")));
    }

    // ---- helpers ----------------------------------------------------------

    private void compile(List<IPrincipal> principals) {
        snapshot = new ModelSnapshotCompiler(serviceDirectory, principals)
                .compile()
                .snapshot();
        engine = new EvaluationEngine(snapshot, JsonLogicEvaluators.withAllExpressions());
    }

    private CompiledResourcePath resource(String type, UUID id) {
        return new CompiledResourcePath(SERVICE,
                new CompiledResourcePath.ResourceNode(type, new CompiledResourcePath.DefiniteId(id), null));
    }

    private CompiledResourcePath resource(String type, UUID id, String type2, UUID id2) {
        return new CompiledResourcePath(SERVICE,
                new CompiledResourcePath.ResourceNode(type, new CompiledResourcePath.DefiniteId(id),
                        new CompiledResourcePath.ResourceNode(type2, new CompiledResourcePath.DefiniteId(id2), null)));
    }

    // ---- basic evaluation -------------------------------------------------

    @Test
    void allowStatementGrantsAccess() {
        UUID principalId = UUID.randomUUID();
        var r = resource("res", UUID.randomUUID());

        var principal = new Principal(principalId);
        var policy = new Policy(UUID.randomUUID());
        policy.addStatement(new PolicyStatement(policy, 1, PolicyEffect.ALLOW,
                Set.of(ACTION_A), Set.of(r), null));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));

        compile(List.of(principal));

        assertTrue(engine.evaluate(principalId, ACTION_A, r, new HashMap<>()));
    }

    @Test
    void noMatchingActionDeniesAccess() {
        UUID principalId = UUID.randomUUID();
        var r = resource("res", UUID.randomUUID());

        var principal = new Principal(principalId);
        var policy = new Policy(UUID.randomUUID());
        policy.addStatement(new PolicyStatement(policy, 1, PolicyEffect.ALLOW,
                Set.of(ACTION_A), Set.of(r), null));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));

        compile(List.of(principal));

        assertFalse(engine.evaluate(principalId, ACTION_B, r, new HashMap<>()));
    }

    @Test
    void noMatchingResourceDeniesAccess() {
        UUID principalId = UUID.randomUUID();
        var r1 = resource("res", UUID.randomUUID());
        var r2 = resource("other", UUID.randomUUID());

        var principal = new Principal(principalId);
        var policy = new Policy(UUID.randomUUID());
        policy.addStatement(new PolicyStatement(policy, 1, PolicyEffect.ALLOW,
                Set.of(ACTION_A), Set.of(r1), null));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));

        compile(List.of(principal));

        assertFalse(engine.evaluate(principalId, ACTION_A, r2, new HashMap<>()));
    }

    @Test
    void unknownPrincipalDeniesAccess() {
        UUID principalId = UUID.randomUUID();
        var r = resource("res", UUID.randomUUID());

        compile(List.of());

        assertFalse(engine.evaluate(principalId, ACTION_A, r, new HashMap<>()));
    }

    // ---- deny before allow -------------------------------------------------

    @Test
    void denyOverridesAllow() {
        UUID principalId = UUID.randomUUID();
        var r = resource("res", UUID.randomUUID());

        var principal = new Principal(principalId);
        var policy = new Policy(UUID.randomUUID());
        policy.addStatement(new PolicyStatement(policy, 1, PolicyEffect.DENY,
                Set.of(ACTION_A), Set.of(r), null));
        policy.addStatement(new PolicyStatement(policy, 2, PolicyEffect.ALLOW,
                Set.of(ACTION_A), Set.of(r), null));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));

        compile(List.of(principal));

        assertFalse(engine.evaluate(principalId, ACTION_A, r, new HashMap<>()));
    }

    @Test
    void denyInOneSetBlocksAllowInAnother() {
        UUID principalId = UUID.randomUUID();
        var r = resource("res", UUID.randomUUID());

        var principal = new Principal(principalId);

        // Role grants ALLOW on ACTION_A
        var role = new Role(UUID.randomUUID());
        var rolePolicy = new Policy(UUID.randomUUID());
        rolePolicy.addStatement(new PolicyStatement(rolePolicy, 1, PolicyEffect.ALLOW,
                Set.of(ACTION_A), Set.of(r), null));
        role.addPolicy(new RolePolicyAssignment(role, rolePolicy));
        principal.addRole(new PrincipalRoleAssignment(principal, role));

        // Direct policy denies ACTION_A — deny across all sets blocks access
        var directPolicy = new Policy(UUID.randomUUID());
        directPolicy.addStatement(new PolicyStatement(directPolicy, 2, PolicyEffect.DENY,
                Set.of(ACTION_A), Set.of(r), null));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, directPolicy));

        compile(List.of(principal));

        // Deny in direct set overrides allow in role set (deny evaluated first across all sets)
        assertFalse(engine.evaluate(principalId, ACTION_A, r, new HashMap<>()));
    }

    // ---- conditions --------------------------------------------------------

    @Test
    void conditionMustBeTrueToApply() {
        UUID principalId = UUID.randomUUID();
        var r = resource("res", UUID.randomUUID());

        JsonLogicNode condition = new JsonLogicOperation("==",
                new JsonLogicArray(List.of(
                        new JsonLogicVariable(new JsonLogicString("x"), new JsonLogicNumber(0)),
                        new JsonLogicNumber(42)
                )));

        var principal = new Principal(principalId);
        var policy = new Policy(UUID.randomUUID());
        policy.addStatement(new PolicyStatement(policy, 1, PolicyEffect.ALLOW,
                Set.of(ACTION_A), Set.of(r), condition));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));

        compile(List.of(principal));

        // Condition true — access granted
        assertTrue(engine.evaluate(principalId, ACTION_A, r, new HashMap<>() {{
            put("x", 42);
        }}));

        // Condition false — access denied
        assertFalse(engine.evaluate(principalId, ACTION_A, r, new HashMap<>() {{
            put("x", 99);
        }}));
    }

    @Test
    void conditionEvaluationErrorDefaultsToFalse() {
        UUID principalId = UUID.randomUUID();
        var r = resource("res", UUID.randomUUID());

        // Condition references missing variable without default — will throw
        JsonLogicNode condition = new JsonLogicOperation(">",
                new JsonLogicArray(List.of(
                        new JsonLogicVariable(new JsonLogicString("x"), JsonLogicNull.NULL),
                        new JsonLogicNumber(0)
                )));

        var principal = new Principal(principalId);
        var policy = new Policy(UUID.randomUUID());
        policy.addStatement(new PolicyStatement(policy, 1, PolicyEffect.ALLOW,
                Set.of(ACTION_A), Set.of(r), condition));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));

        compile(List.of(principal));

        // Null > 0 throws — statement does not apply, access denied
        assertFalse(engine.evaluate(principalId, ACTION_A, r, new HashMap<>()));
    }

    @Test
    void statementWithoutConditionAlwaysApplies() {
        UUID principalId = UUID.randomUUID();
        var r = resource("res", UUID.randomUUID());

        var principal = new Principal(principalId);
        var policy = new Policy(UUID.randomUUID());
        policy.addStatement(new PolicyStatement(policy, 1, PolicyEffect.ALLOW,
                Set.of(ACTION_A), Set.of(r), null));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));

        compile(List.of(principal));

        // No context needed
        assertTrue(engine.evaluate(principalId, ACTION_A, r, new HashMap<>()));
    }

    // ---- service principals ------------------------------------------------

    @Test
    void servicePrincipalEvaluation() {
        var r = resource("res", UUID.randomUUID());

        UUID servicePrincipalId = serviceDirectory.servicePrincipals().stream()
                .map(serviceDirectory::principalId)
                .findFirst()
                .orElseThrow();

        // Register the service principal explicitly
        ServiceDirectory.Builder sdBuilder = new ServiceDirectory.Builder();
        sdBuilder.addServicePrincipal(SERVICE);
        sdBuilder.addAction(ACTION_A);
        sdBuilder.addAction(ACTION_B);
        var sd = sdBuilder.build();

        // Create a principal entry in the directory for the service principal
        var principal = new Principal(servicePrincipalId);
        var policy = new Policy(UUID.randomUUID());
        policy.addStatement(new PolicyStatement(policy, 1, PolicyEffect.ALLOW,
                Set.of(ACTION_A), Set.of(r), null));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));

        snapshot = new ModelSnapshotCompiler(sd, List.of(principal))
                .compile()
                .snapshot();
        engine = new EvaluationEngine(snapshot, JsonLogicEvaluators.withAllExpressions());

        assertTrue(engine.evaluate(UUID.nameUUIDFromBytes(SERVICE.getBytes(StandardCharsets.UTF_8)), ACTION_A, r, new HashMap<>()));
    }

    // ---- resource matching ------------------------------------------------

    @Test
    void wildcardNodeMatchesAnyResource() {
        UUID principalId = UUID.randomUUID();
        var pattern = new CompiledResourcePath(SERVICE,
                new CompiledResourcePath.WildcardNode(null, false));
        var r = resource("res", UUID.randomUUID());

        var principal = new Principal(principalId);
        var policy = new Policy(UUID.randomUUID());
        policy.addStatement(new PolicyStatement(policy, 1, PolicyEffect.ALLOW,
                Set.of(ACTION_A), Set.of(pattern), null));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));

        compile(List.of(principal));

        // Wildcard pattern matches any single-node resource
        assertTrue(engine.evaluate(principalId, ACTION_A, r, new HashMap<>()));
    }

    @Test
    void greedyWildcardMatchesMultipleNodes() {
        UUID principalId = UUID.randomUUID();
        var pattern = new CompiledResourcePath(SERVICE,
                new CompiledResourcePath.WildcardNode(null, true));
        var r = resource("res", UUID.randomUUID(), "sub", UUID.randomUUID());

        var principal = new Principal(principalId);
        var policy = new Policy(UUID.randomUUID());
        policy.addStatement(new PolicyStatement(policy, 1, PolicyEffect.ALLOW,
                Set.of(ACTION_A), Set.of(pattern), null));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));

        compile(List.of(principal));

        assertTrue(engine.evaluate(principalId, ACTION_A, r, new HashMap<>()));
    }

    @Test
    void wildcardIdMatchesAnyResourceId() {
        UUID principalId = UUID.randomUUID();
        var pattern = new CompiledResourcePath(SERVICE,
                new CompiledResourcePath.ResourceNode("res", new CompiledResourcePath.WildcardId(), null));
        var r = resource("res", UUID.randomUUID());

        var principal = new Principal(principalId);
        var policy = new Policy(UUID.randomUUID());
        policy.addStatement(new PolicyStatement(policy, 1, PolicyEffect.ALLOW,
                Set.of(ACTION_A), Set.of(pattern), null));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));

        compile(List.of(principal));

        assertTrue(engine.evaluate(principalId, ACTION_A, r, new HashMap<>()));
    }

    @Test
    void differentServiceDeniesAccess() {
        UUID principalId = UUID.randomUUID();
        var r = resource("res", UUID.randomUUID());

        var principal = new Principal(principalId);
        var policy = new Policy(UUID.randomUUID());
        policy.addStatement(new PolicyStatement(policy, 1, PolicyEffect.ALLOW,
                Set.of(ACTION_A), Set.of(r), null));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));

        compile(List.of(principal));

        // Same path but different service
        var otherServiceResource = new CompiledResourcePath("other",
                r.root());
        assertFalse(engine.evaluate(principalId, ACTION_A, otherServiceResource, new HashMap<>()));
    }

    // ---- multiple actions ---------------------------------------------------

    @Test
    void statementWithMultipleActions() {
        UUID principalId = UUID.randomUUID();
        var r = resource("res", UUID.randomUUID());

        var principal = new Principal(principalId);
        var policy = new Policy(UUID.randomUUID());
        policy.addStatement(new PolicyStatement(policy, 1, PolicyEffect.ALLOW,
                Set.of(ACTION_A, ACTION_B), Set.of(r), null));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));

        compile(List.of(principal));

        assertTrue(engine.evaluate(principalId, ACTION_A, r, new HashMap<>()));
        assertTrue(engine.evaluate(principalId, ACTION_B, r, new HashMap<>()));
    }

    // ---- snapshot swap -----------------------------------------------------

    @Test
    void snapshotSwapTakesEffect() {
        UUID principalId = UUID.randomUUID();
        var r = resource("res", UUID.randomUUID());

        // Initial snapshot: no permissions
        compile(List.of());
        assertFalse(engine.evaluate(principalId, ACTION_A, r, new HashMap<>()));

        // New snapshot: grant permission
        var principal = new Principal(principalId);
        var policy = new Policy(UUID.randomUUID());
        policy.addStatement(new PolicyStatement(policy, 1, PolicyEffect.ALLOW,
                Set.of(ACTION_A), Set.of(r), null));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));

        var newSnapshot = new ModelSnapshotCompiler(serviceDirectory, List.of(principal))
                .compile()
                .snapshot();

        engine.snapshot(newSnapshot);
        assertTrue(engine.evaluate(principalId, ACTION_A, r, new HashMap<>()));
    }

    // ---- string resource parsing -------------------------------------------

    @Test
    void stringResourceIsCompiledAndEvaluated() {
        UUID principalId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        var r = resource("res", resourceId);

        var principal = new Principal(principalId);
        var policy = new Policy(UUID.randomUUID());
        policy.addStatement(new PolicyStatement(policy, 1, PolicyEffect.ALLOW,
                Set.of(ACTION_A), Set.of(r), null));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));

        compile(List.of(principal));

        String resourcePath = SERVICE + "://res:" + resourceId;
        assertTrue(engine.evaluate(principalId, ACTION_A, resourcePath, new HashMap<>()));
    }

    @Test
    void sameResourceStringEvaluatedMultipleTimes() {
        UUID principalId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        var r = resource("res", resourceId);

        var principal = new Principal(principalId);
        var policy = new Policy(UUID.randomUUID());
        policy.addStatement(new PolicyStatement(policy, 1, PolicyEffect.ALLOW,
                Set.of(ACTION_A), Set.of(r), null));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));

        compile(List.of(principal));

        String resourcePath = SERVICE + "://res:" + resourceId;

        // Repeated evaluations with the same string resource path produce
        // consistent results (internally the compiled path is cached, but
        // that is an implementation detail not asserted here).
        assertTrue(engine.evaluate(principalId, ACTION_A, resourcePath, new HashMap<>()));
        assertTrue(engine.evaluate(principalId, ACTION_A, resourcePath, new HashMap<>()));
        assertTrue(engine.evaluate(principalId, ACTION_A, resourcePath, new HashMap<>()));
    }
}
