package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers;

import com.jasminesoftwaresolutions.witchwood.entities.*;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.*;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.ServiceDirectory;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import com.jasminesoftwaresolutions.witchwood.models.*;
import com.jasminesoftwaresolutions.witchwood.util.ArrayView;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNumber;
import io.github.jamsesso.jsonlogic.ast.JsonLogicOperation;
import io.github.jamsesso.jsonlogic.ast.JsonLogicArray;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ModelSnapshotCompilerTest {

    // -----------------------------------------------------------------------
    // Factory helpers
    // -----------------------------------------------------------------------

    /** Create a simple resource path of the form {@code service://type:id} */
    private static CompiledResourcePath resource(String service, String type, UUID id) {
        return new CompiledResourcePath(
                service,
                new CompiledResourcePath.ResourceNode(type, new CompiledResourcePath.DefiniteId(id), null)
        );
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    void emptyPrincipalsProducesEmptySnapshot() {
        ModelSnapshotCompiler compiler = new ModelSnapshotCompiler(
                ServiceDirectory.from(Map.of("svc", Set.of("act1", "act2"))),
                List.of()
        );

        compiler.compile();
        EvaluationSnapshot snapshot = compiler.snapshot();

        assertNotNull(snapshot);
        // Actions registered but no principals
        assertNotEquals(-1, snapshot.serviceDirectory().actionId("svc:act1"));
        assertNotEquals(-1, snapshot.serviceDirectory().actionId("svc:act2"));
    }

    @Test
    void principalWithNoPoliciesProducesNoPermissionSets() {
        UUID principalId = UUID.randomUUID();
        Principal principal = new Principal(principalId);

        ModelSnapshotCompiler compiler = new ModelSnapshotCompiler(
                ServiceDirectory.from(Map.of("svc", Set.of("act"))),
                List.of(principal)
        );

        compiler.compile();
        EvaluationSnapshot snapshot = compiler.snapshot();

        int pid = snapshot.principalDirectory().principalId(principalId);
        assertNotEquals(-1, pid);

        ArrayView<PermissionSetSnapshot> sets = snapshot.permissionsTable().permissions(pid);
        assertEquals(0, sets.end() - sets.start());
    }

    @Test
    void singleAllowStatementProducesExpectedPermissions() {
        UUID principalId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();

        Principal principal = new Principal(principalId);
        Role role = new Role(roleId);
        Policy policy = new Policy(policyId);

        String service = "iam";
        String actionKey = "CreateUser";

        PolicyStatement statement = new PolicyStatement(
                policy, 1, PolicyEffect.ALLOW,
                Set.of(service + ":" + actionKey),
                Set.of(resource(service, "account", UUID.randomUUID())),
                null
        );

        policy.addStatement(statement);
        role.addPolicy(new RolePolicyAssignment(role, policy));
        principal.addRole(new PrincipalRoleAssignment(principal, role));

        ModelSnapshotCompiler compiler = new ModelSnapshotCompiler(
                ServiceDirectory.from(Map.of(service, Set.of(actionKey))),
                List.of(principal)
        );
        compiler.compile();
        EvaluationSnapshot snapshot = compiler.snapshot();

        int pid = snapshot.principalDirectory().principalId(principalId);
        int actionId = snapshot.serviceDirectory().actionId(service + ":" + actionKey);
        assertNotEquals(-1, actionId);

        ArrayView<PermissionSetSnapshot> sets = snapshot.permissionsTable().permissions(pid);
        assertEquals(1, sets.end() - sets.start());

        PermissionSnapshot perm = sets.array()[sets.start()].permission(actionId);
        assertNotNull(perm);
        assertEquals(1, perm.allowStatements().length);
        assertEquals(0, perm.denyStatements().length);
    }

    @Test
    void denyStatementsEvaluatedBeforeAllow() {
        UUID principalId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();

        Principal principal = new Principal(principalId);
        Role role = new Role(roleId);
        Policy policy = new Policy(policyId);

        String service = "iam";
        String actionKey = "DeleteUser";

        CompiledResourcePath res = resource(service, "account", UUID.randomUUID());

        PolicyStatement denyStatement = new PolicyStatement(
                policy, 1, PolicyEffect.DENY,
                Set.of(service + ":" + actionKey),
                Set.of(res), null
        );
        PolicyStatement allowStatement = new PolicyStatement(
                policy, 2, PolicyEffect.ALLOW,
                Set.of(service + ":" + actionKey),
                Set.of(res), null
        );

        policy.addStatement(denyStatement);
        policy.addStatement(allowStatement);
        role.addPolicy(new RolePolicyAssignment(role, policy));
        principal.addRole(new PrincipalRoleAssignment(principal, role));

        ModelSnapshotCompiler compiler = new ModelSnapshotCompiler(
                ServiceDirectory.from(Map.of(service, Set.of(actionKey))),
                List.of(principal)
        );
        compiler.compile();
        EvaluationSnapshot snapshot = compiler.snapshot();

        int pid = snapshot.principalDirectory().principalId(principalId);
        int actionId = snapshot.serviceDirectory().actionId(service + ":" + actionKey);

        ArrayView<PermissionSetSnapshot> sets = snapshot.permissionsTable().permissions(pid);
        PermissionSnapshot perm = sets.array()[sets.start()].permission(actionId);

        assertEquals(1, perm.allowStatements().length);
        assertEquals(1, perm.denyStatements().length);
    }

    @Test
    void wildcardStarMatchesAllActions() {
        UUID principalId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();

        Principal principal = new Principal(principalId);
        Role role = new Role(roleId);
        Policy policy = new Policy(policyId);

        String service1 = "svc1";
        String service2 = "svc2";

        PolicyStatement statement = new PolicyStatement(
                policy, 1, PolicyEffect.ALLOW,
                Set.of("*"),
                Set.of(resource(service1, "res", UUID.randomUUID())),
                null
        );

        policy.addStatement(statement);
        role.addPolicy(new RolePolicyAssignment(role, policy));
        principal.addRole(new PrincipalRoleAssignment(principal, role));

        ModelSnapshotCompiler compiler = new ModelSnapshotCompiler(
                ServiceDirectory.from(Map.of(service1, Set.of("a", "b"), service2, Set.of("c"))),
                List.of(principal)
        );
        compiler.compile();
        EvaluationSnapshot snapshot = compiler.snapshot();

        int pid = snapshot.principalDirectory().principalId(principalId);

        // The statement should be expanded into all three actions
        int aId = snapshot.serviceDirectory().actionId(service1 + ":a");
        int bId = snapshot.serviceDirectory().actionId(service1 + ":b");
        int cId = snapshot.serviceDirectory().actionId(service2 + ":c");

        ArrayView<PermissionSetSnapshot> sets = snapshot.permissionsTable().permissions(pid);
        assertEquals(1, sets.end() - sets.start());

        PermissionSnapshot permA = sets.array()[sets.start()].permission(aId);
        assertNotNull(permA);
        assertEquals(1, permA.allowStatements().length);

        PermissionSnapshot permB = sets.array()[sets.start()].permission(bId);
        assertNotNull(permB);
        assertEquals(1, permB.allowStatements().length);

        PermissionSnapshot permC = sets.array()[sets.start()].permission(cId);
        assertNotNull(permC);
        assertEquals(1, permC.allowStatements().length);
    }

    @Test
    void wildcardServiceStarMatchesAllActionsOfService() {
        UUID principalId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();

        Principal principal = new Principal(principalId);
        Role role = new Role(roleId);
        Policy policy = new Policy(policyId);

        String service1 = "svc1";
        String service2 = "svc2";

        PolicyStatement statement = new PolicyStatement(
                policy, 1, PolicyEffect.ALLOW,
                Set.of(service1 + ":*"),
                Set.of(resource(service1, "res", UUID.randomUUID())),
                null
        );

        policy.addStatement(statement);
        role.addPolicy(new RolePolicyAssignment(role, policy));
        principal.addRole(new PrincipalRoleAssignment(principal, role));

        ModelSnapshotCompiler compiler = new ModelSnapshotCompiler(
                ServiceDirectory.from(Map.of(service1, Set.of("a", "b"), service2, Set.of("c"))),
                List.of(principal)
        );
        compiler.compile();
        EvaluationSnapshot snapshot = compiler.snapshot();

        int pid = snapshot.principalDirectory().principalId(principalId);
        int aId = snapshot.serviceDirectory().actionId(service1 + ":a");
        int bId = snapshot.serviceDirectory().actionId(service1 + ":b");
        int cId = snapshot.serviceDirectory().actionId(service2 + ":c");

        ArrayView<PermissionSetSnapshot> sets = snapshot.permissionsTable().permissions(pid);
        assertEquals(1, sets.end() - sets.start());

        // svc1 actions have the statement
        assertNotNull(sets.array()[sets.start()].permission(aId));
        assertEquals(1, sets.array()[sets.start()].permission(aId).allowStatements().length);
        assertNotNull(sets.array()[sets.start()].permission(bId));
        assertEquals(1, sets.array()[sets.start()].permission(bId).allowStatements().length);

        // svc2 action should NOT have the statement
        assertNull(sets.array()[sets.start()].permission(cId));
    }

    @Test
    void wildcardPrefixExpandsToMatchingActions() {
        UUID principalId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();

        Principal principal = new Principal(principalId);
        Role role = new Role(roleId);
        Policy policy = new Policy(policyId);

        String service = "svc";

        PolicyStatement statement = new PolicyStatement(
                policy, 1, PolicyEffect.ALLOW,
                Set.of(service + ":Create*"),
                Set.of(resource(service, "res", UUID.randomUUID())),
                null
        );

        policy.addStatement(statement);
        role.addPolicy(new RolePolicyAssignment(role, policy));
        principal.addRole(new PrincipalRoleAssignment(principal, role));

        ModelSnapshotCompiler compiler = new ModelSnapshotCompiler(
                ServiceDirectory.from(Map.of(service, Set.of("CreateUser", "CreateRole", "DeleteUser"))),
                List.of(principal)
        );
        compiler.compile();
        EvaluationSnapshot snapshot = compiler.snapshot();

        int pid = snapshot.principalDirectory().principalId(principalId);
        int createUser = snapshot.serviceDirectory().actionId(service + ":CreateUser");
        int createRole = snapshot.serviceDirectory().actionId(service + ":CreateRole");
        int deleteUser = snapshot.serviceDirectory().actionId(service + ":DeleteUser");

        ArrayView<PermissionSetSnapshot> sets = snapshot.permissionsTable().permissions(pid);

        assertNotNull(sets.array()[sets.start()].permission(createUser));
        assertNotNull(sets.array()[sets.start()].permission(createRole));
        assertNull(sets.array()[sets.start()].permission(deleteUser));
    }

    @Test
    void wildcardSuffixExpandsToMatchingActions() {
        UUID principalId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();

        Principal principal = new Principal(principalId);
        Role role = new Role(roleId);
        Policy policy = new Policy(policyId);

        String service = "svc";

        PolicyStatement statement = new PolicyStatement(
                policy, 1, PolicyEffect.ALLOW,
                Set.of(service + ":*User"),
                Set.of(resource(service, "res", UUID.randomUUID())),
                null
        );

        policy.addStatement(statement);
        role.addPolicy(new RolePolicyAssignment(role, policy));
        principal.addRole(new PrincipalRoleAssignment(principal, role));

        ModelSnapshotCompiler compiler = new ModelSnapshotCompiler(
                ServiceDirectory.from(Map.of(service, Set.of("CreateUser", "DeleteUser", "CreateRole"))),
                List.of(principal)
        );
        compiler.compile();
        EvaluationSnapshot snapshot = compiler.snapshot();

        int pid = snapshot.principalDirectory().principalId(principalId);
        int createUser = snapshot.serviceDirectory().actionId(service + ":CreateUser");
        int deleteUser = snapshot.serviceDirectory().actionId(service + ":DeleteUser");
        int createRole = snapshot.serviceDirectory().actionId(service + ":CreateRole");

        ArrayView<PermissionSetSnapshot> sets = snapshot.permissionsTable().permissions(pid);

        assertNotNull(sets.array()[sets.start()].permission(createUser));
        assertNotNull(sets.array()[sets.start()].permission(deleteUser));
        assertNull(sets.array()[sets.start()].permission(createRole));
    }

    @Test
    void duplicateStatementsAreDeduplicated() {
        UUID principalId = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();

        Principal principal = new Principal(principalId);
        Policy policy = new Policy(policyId);

        String service = "svc";
        CompiledResourcePath res = resource(service, "r", UUID.randomUUID());
        String action = service + ":a";

        // Two identical statements
        PolicyStatement s1 = new PolicyStatement(
                policy, 1, PolicyEffect.ALLOW,
                Set.of(action), Set.of(res), null
        );
        PolicyStatement s2 = new PolicyStatement(
                policy, 2, PolicyEffect.ALLOW,
                Set.of(action), Set.of(res), null
        );

        policy.addStatement(s1);
        policy.addStatement(s2);
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));

        ModelSnapshotCompiler compiler = new ModelSnapshotCompiler(
                ServiceDirectory.from(Map.of(service, Set.of("a"))),
                List.of(principal)
        );
        compiler.compile();
        EvaluationSnapshot snapshot = compiler.snapshot();

        int pid = snapshot.principalDirectory().principalId(principalId);
        int actionId = snapshot.serviceDirectory().actionId(action);

        ArrayView<PermissionSetSnapshot> sets = snapshot.permissionsTable().permissions(pid);
        PermissionSnapshot perm = sets.array()[sets.start()].permission(actionId);

        // Should have only 1 allow statement (deduplicated)
        assertEquals(1, perm.allowStatements().length);
    }

    @Test
    void directPolicyCreatesOnePermissionSet() {
        UUID principalId = UUID.randomUUID();
        UUID policy1Id = UUID.randomUUID();
        UUID policy2Id = UUID.randomUUID();

        Principal principal = new Principal(principalId);
        Policy policy1 = new Policy(policy1Id);
        Policy policy2 = new Policy(policy2Id);

        String service = "svc";

        PolicyStatement s1 = new PolicyStatement(
                policy1, 1, PolicyEffect.ALLOW,
                Set.of(service + ":a"),
                Set.of(resource(service, "r", UUID.randomUUID())),
                null
        );
        PolicyStatement s2 = new PolicyStatement(
                policy2, 2, PolicyEffect.ALLOW,
                Set.of(service + ":b"),
                Set.of(resource(service, "r", UUID.randomUUID())),
                null
        );

        policy1.addStatement(s1);
        policy2.addStatement(s2);
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy1));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy2));

        ModelSnapshotCompiler compiler = new ModelSnapshotCompiler(
                ServiceDirectory.from(Map.of(service, Set.of("a", "b"))),
                List.of(principal)
        );
        compiler.compile();
        EvaluationSnapshot snapshot = compiler.snapshot();

        int pid = snapshot.principalDirectory().principalId(principalId);
        ArrayView<PermissionSetSnapshot> sets = snapshot.permissionsTable().permissions(pid);

        // Should be exactly 1 permission set (merged direct policies)
        assertEquals(1, sets.end() - sets.start());

        int aId = snapshot.serviceDirectory().actionId(service + ":a");
        int bId = snapshot.serviceDirectory().actionId(service + ":b");

        PermissionSetSnapshot ps = sets.array()[sets.start()];
        assertNotNull(ps.permission(aId));
        assertNotNull(ps.permission(bId));
    }

    @Test
    void rolesAndDirectPoliciesProduceSeparatePermissionSets() {
        UUID principalId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID rolePolicyId = UUID.randomUUID();
        UUID directPolicyId = UUID.randomUUID();

        Principal principal = new Principal(principalId);
        Role role = new Role(roleId);
        Policy rolePolicy = new Policy(rolePolicyId);
        Policy directPolicy = new Policy(directPolicyId);

        String service = "svc";

        PolicyStatement roleStatement = new PolicyStatement(
                rolePolicy, 1, PolicyEffect.ALLOW,
                Set.of(service + ":a"),
                Set.of(resource(service, "r", UUID.randomUUID())),
                null
        );
        PolicyStatement directStatement = new PolicyStatement(
                directPolicy, 2, PolicyEffect.ALLOW,
                Set.of(service + ":b"),
                Set.of(resource(service, "r", UUID.randomUUID())),
                null
        );

        rolePolicy.addStatement(roleStatement);
        directPolicy.addStatement(directStatement);

        role.addPolicy(new RolePolicyAssignment(role, rolePolicy));
        principal.addRole(new PrincipalRoleAssignment(principal, role));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, directPolicy));

        ModelSnapshotCompiler compiler = new ModelSnapshotCompiler(
                ServiceDirectory.from(Map.of(service, Set.of("a", "b"))),
                List.of(principal)
        );
        compiler.compile();
        EvaluationSnapshot snapshot = compiler.snapshot();

        int pid = snapshot.principalDirectory().principalId(principalId);
        ArrayView<PermissionSetSnapshot> sets = snapshot.permissionsTable().permissions(pid);

        // Should have 2 permission sets: 1 for the role, 1 for direct
        assertEquals(2, sets.end() - sets.start());
    }

    @Test
    void statementWithConditionIsPreserved() {
        UUID principalId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();

        Principal principal = new Principal(principalId);
        Role role = new Role(roleId);
        Policy policy = new Policy(policyId);

        String service = "svc";
        String actionKey = "a";

        JsonLogicNode condition = new JsonLogicOperation("==",
                new JsonLogicArray(List.of(new JsonLogicNumber(1), new JsonLogicNumber(1))));

        PolicyStatement statement = new PolicyStatement(
                policy, 1, PolicyEffect.ALLOW,
                Set.of(service + ":" + actionKey),
                Set.of(resource(service, "r", UUID.randomUUID())),
                condition
        );

        policy.addStatement(statement);
        role.addPolicy(new RolePolicyAssignment(role, policy));
        principal.addRole(new PrincipalRoleAssignment(principal, role));

        ModelSnapshotCompiler compiler = new ModelSnapshotCompiler(
                ServiceDirectory.from(Map.of(service, Set.of(actionKey))),
                List.of(principal)
        );
        compiler.compile();
        EvaluationSnapshot snapshot = compiler.snapshot();

        int pid = snapshot.principalDirectory().principalId(principalId);
        int actionId = snapshot.serviceDirectory().actionId(service + ":" + actionKey);

        ArrayView<PermissionSetSnapshot> sets = snapshot.permissionsTable().permissions(pid);
        PermissionSnapshot perm = sets.array()[sets.start()].permission(actionId);

        assertEquals(1, perm.allowStatements().length);
        assertNotNull(perm.allowStatements()[0].condition());
    }

    @Test
    void multiplePrincipalsWithDistinctPermissions() {
        UUID principal1Id = UUID.randomUUID();
        UUID principal2Id = UUID.randomUUID();
        UUID policy1Id = UUID.randomUUID();
        UUID policy2Id = UUID.randomUUID();

        Principal principal1 = new Principal(principal1Id);
        Principal principal2 = new Principal(principal2Id);
        Policy policy1 = new Policy(policy1Id);
        Policy policy2 = new Policy(policy2Id);

        String service = "svc";

        PolicyStatement s1 = new PolicyStatement(
                policy1, 1, PolicyEffect.ALLOW,
                Set.of(service + ":a"),
                Set.of(resource(service, "r", UUID.randomUUID())),
                null
        );
        PolicyStatement s2 = new PolicyStatement(
                policy2, 2, PolicyEffect.ALLOW,
                Set.of(service + ":b"),
                Set.of(resource(service, "r", UUID.randomUUID())),
                null
        );

        policy1.addStatement(s1);
        policy2.addStatement(s2);
        principal1.addPolicy(new PrincipalPolicyAssignment(principal1, policy1));
        principal2.addPolicy(new PrincipalPolicyAssignment(principal2, policy2));

        ModelSnapshotCompiler compiler = new ModelSnapshotCompiler(
                ServiceDirectory.from(Map.of(service, Set.of("a", "b"))),
                List.of(principal1, principal2)
        );
        compiler.compile();
        EvaluationSnapshot snapshot = compiler.snapshot();

        int pid1 = snapshot.principalDirectory().principalId(principal1Id);
        int pid2 = snapshot.principalDirectory().principalId(principal2Id);
        int aId = snapshot.serviceDirectory().actionId(service + ":a");
        int bId = snapshot.serviceDirectory().actionId(service + ":b");

        // Principal 1 has action "a" but not "b"
        ArrayView<PermissionSetSnapshot> sets1 = snapshot.permissionsTable().permissions(pid1);
        assertEquals(1, sets1.end() - sets1.start());
        assertNotNull(sets1.array()[sets1.start()].permission(aId));
        assertNull(sets1.array()[sets1.start()].permission(bId));

        // Principal 2 has action "b" but not "a"
        ArrayView<PermissionSetSnapshot> sets2 = snapshot.permissionsTable().permissions(pid2);
        assertEquals(1, sets2.end() - sets2.start());
        assertNull(sets2.array()[sets2.start()].permission(aId));
        assertNotNull(sets2.array()[sets2.start()].permission(bId));
    }

    @Test
    void serviceDirectoryHasCorrectActionIds() {
        Map<String, Set<String>> actions = Map.of(
                "iam", Set.of("CreateUser", "DeleteRole"),
                "storage", Set.of("PutObject", "GetObject")
        );

        ModelSnapshotCompiler compiler = new ModelSnapshotCompiler(ServiceDirectory.from(actions), List.of());
        compiler.compile();
        EvaluationSnapshot snapshot = compiler.snapshot();

        // All actions should have valid IDs
        assertNotEquals(-1, snapshot.serviceDirectory().actionId("iam:CreateUser"));
        assertNotEquals(-1, snapshot.serviceDirectory().actionId("iam:DeleteRole"));
        assertNotEquals(-1, snapshot.serviceDirectory().actionId("storage:PutObject"));
        assertNotEquals(-1, snapshot.serviceDirectory().actionId("storage:GetObject"));

        // Unknown actions should return -1
        assertEquals(-1, snapshot.serviceDirectory().actionId("iam:UnknownAction"));
    }

    @Test
    void serviceDirectoryHasServicePrincipals() {
        ModelSnapshotCompiler compiler = new ModelSnapshotCompiler(
                ServiceDirectory.from(Map.of("iam", Set.of("a"))),
                List.of()
        );
        compiler.compile();
        EvaluationSnapshot snapshot = compiler.snapshot();

        // Service principal should be resolvable
        assertNotNull(snapshot.serviceDirectory().principalId("iam"));
        assertNull(snapshot.serviceDirectory().principalId("unknown-service"));
    }

    @Test
    void statementWithMultipleActionsExpandsCorrectly() {
        UUID principalId = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();

        Principal principal = new Principal(principalId);
        Policy policy = new Policy(policyId);

        String service = "svc";
        CompiledResourcePath res = resource(service, "r", UUID.randomUUID());

        PolicyStatement statement = new PolicyStatement(
                policy, 1, PolicyEffect.ALLOW,
                Set.of(service + ":a", service + ":b"),
                Set.of(res),
                null
        );

        policy.addStatement(statement);
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));

        ModelSnapshotCompiler compiler = new ModelSnapshotCompiler(
                ServiceDirectory.from(Map.of(service, Set.of("a", "b", "c"))),
                List.of(principal)
        );
        compiler.compile();
        EvaluationSnapshot snapshot = compiler.snapshot();

        int pid = snapshot.principalDirectory().principalId(principalId);
        int aId = snapshot.serviceDirectory().actionId(service + ":a");
        int bId = snapshot.serviceDirectory().actionId(service + ":b");
        int cId = snapshot.serviceDirectory().actionId(service + ":c");

        ArrayView<PermissionSetSnapshot> sets = snapshot.permissionsTable().permissions(pid);
        PermissionSetSnapshot ps = sets.array()[sets.start()];

        // Actions a and b should have the statement; c should not
        assertNotNull(ps.permission(aId));
        assertEquals(1, ps.permission(aId).allowStatements().length);
        assertNotNull(ps.permission(bId));
        assertEquals(1, ps.permission(bId).allowStatements().length);
        assertNull(ps.permission(cId));
    }

    @Test
    void compileCanBeCalledMultipleTimes() {
        UUID principalId = UUID.randomUUID();
        Principal principal = new Principal(principalId);
        Policy policy = new Policy(UUID.randomUUID());
        policy.addStatement(new PolicyStatement(
                policy, 1, PolicyEffect.ALLOW,
                Set.of("svc:a"),
                Set.of(resource("svc", "r", UUID.randomUUID())),
                null
        ));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));

        ModelSnapshotCompiler compiler = new ModelSnapshotCompiler(
                ServiceDirectory.from(Map.of("svc", Set.of("a"))),
                List.of(principal)
        );

        // First compile
        compiler.compile();
        EvaluationSnapshot first = compiler.snapshot();
        assertNotNull(first);

        // Second compile (with different data possible)
        compiler.compile();
        EvaluationSnapshot second = compiler.snapshot();
        assertNotNull(second);

        // Both should produce valid results
        int pid = second.principalDirectory().principalId(principalId);
        ArrayView<PermissionSetSnapshot> sets = second.permissionsTable().permissions(pid);
        assertEquals(1, sets.end() - sets.start());
    }
}
