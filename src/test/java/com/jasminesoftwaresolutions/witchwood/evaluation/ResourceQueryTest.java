package com.jasminesoftwaresolutions.witchwood.evaluation;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.*;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers.ModelSnapshotCompiler;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import com.jasminesoftwaresolutions.witchwood.entities.*;
import com.jasminesoftwaresolutions.witchwood.models.*;
import com.jasminesoftwaresolutions.witchwood.util.JsonLogicEvaluators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ResourceQueryTest {
    private static final String SERVICE = "id.jasmine.software";
    private static final String ACTION_LIST_ORGS = SERVICE + ":ListOrganizations";
    private static final String ACTION_CREATE_ORG = SERVICE + ":CreateOrganization";
    private static final UUID TENANT_ID = UUID.fromString("a69db823-ce61-4bac-a53a-3b883d013cee");
    private static final UUID TENANT_ID_2 = UUID.fromString("b71ec934-df72-5cba-b64b-4c994e124dff");

    private ServiceDirectory serviceDirectory;
    private EvaluationEngine engine;

    @BeforeEach
    void setUp() {
        serviceDirectory = ServiceDirectory.from(Map.of(
                SERVICE, Set.of("ListOrganizations", "CreateOrganization")));
    }

    private void compile(List<IPrincipal> principals) {
        var snapshot = new ModelSnapshotCompiler(serviceDirectory, principals)
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

    private Policy allowPolicy(String action, CompiledResourcePath... resources) {
        Policy policy = new Policy(UUID.randomUUID());
        var stmt = new PolicyStatement(policy, 1, PolicyEffect.ALLOW,
                Set.of(action), new LinkedHashSet<>(Arrays.asList(resources)), null);
        policy.addStatement(stmt);
        return policy;
    }

    private Policy denyPolicy(String action, CompiledResourcePath... resources) {
        Policy policy = new Policy(UUID.randomUUID());
        var stmt = new PolicyStatement(policy, 1, PolicyEffect.DENY,
                Set.of(action), new LinkedHashSet<>(Arrays.asList(resources)), null);
        policy.addStatement(stmt);
        return policy;
    }

    private Principal principalWithPolicy(Policy policy) {
        Principal principal = new Principal(UUID.randomUUID());
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));
        return principal;
    }

    private ResourceQuery query() {
        return new ResourceQuery(engine);
    }

    @Test
    void singleAllowStatementYieldsAllResources() {
        UUID orgId = UUID.randomUUID();
        CompiledResourcePath orgResource = resource("organization", orgId);
        Policy policy = allowPolicy(ACTION_LIST_ORGS, orgResource);
        Principal principal = principalWithPolicy(policy);
        compile(List.of(principal));

        Set<CompiledResourcePath> result = query().query(principal.id(), ACTION_LIST_ORGS);

        assertEquals(1, result.size());
        assertTrue(result.contains(orgResource));
    }

    @Test
    void multipleResourcesFromSameStatement() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        CompiledResourcePath r1 = resource("organization", id1);
        CompiledResourcePath r2 = resource("organization", id2);
        Policy policy = allowPolicy(ACTION_LIST_ORGS, r1, r2);
        Principal principal = principalWithPolicy(policy);
        compile(List.of(principal));

        Set<CompiledResourcePath> result = query().query(principal.id(), ACTION_LIST_ORGS);

        assertEquals(2, result.size());
        assertTrue(result.contains(r1));
        assertTrue(result.contains(r2));
    }

    @Test
    void wildcardPatternsAreReturnedAsIs() {
        CompiledResourcePath tenantPattern = CompiledResourcePath.builder()
                .one("tenant", TENANT_ID).build();
        CompiledResourcePath greedyPattern = CompiledResourcePath.builder()
                .one("tenant", TENANT_ID).any(true).build();
        Policy policy = allowPolicy(ACTION_LIST_ORGS, tenantPattern, greedyPattern);
        Principal principal = principalWithPolicy(policy);
        compile(List.of(principal));

        Set<CompiledResourcePath> result = query().query(principal.id(), ACTION_LIST_ORGS);

        assertEquals(2, result.size());
        assertTrue(result.contains(tenantPattern));
        assertTrue(result.contains(greedyPattern));
    }

    @Test
    void unknownPrincipalReturnsEmpty() {
        compile(List.of());
        Set<CompiledResourcePath> result = query().query(UUID.randomUUID(), ACTION_LIST_ORGS);
        assertTrue(result.isEmpty());
    }

    @Test
    void unknownActionReturnsEmpty() {
        CompiledResourcePath r = resource("organization", UUID.randomUUID());
        Policy policy = allowPolicy(ACTION_LIST_ORGS, r);
        Principal principal = principalWithPolicy(policy);
        compile(List.of(principal));

        Set<CompiledResourcePath> result = query().query(principal.id(), "unknown.service:UnknownAction");
        assertTrue(result.isEmpty());
    }

    @Test
    void differentActionReturnsEmpty() {
        CompiledResourcePath r = resource("organization", UUID.randomUUID());
        Policy policy = allowPolicy(ACTION_LIST_ORGS, r);
        Principal principal = principalWithPolicy(policy);
        compile(List.of(principal));

        Set<CompiledResourcePath> result = query().query(principal.id(), ACTION_CREATE_ORG);
        assertTrue(result.isEmpty());
    }

    @Test
    void denyShadowsAllowOnSamePattern() {
        CompiledResourcePath orgResource = resource("organization", UUID.randomUUID());
        Policy allowPol = allowPolicy(ACTION_LIST_ORGS, orgResource);
        Policy denyPol = denyPolicy(ACTION_LIST_ORGS, orgResource);
        Principal principal = new Principal(UUID.randomUUID());
        principal.addPolicy(new PrincipalPolicyAssignment(principal, allowPol));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, denyPol));
        compile(List.of(principal));

        Set<CompiledResourcePath> result = query().query(principal.id(), ACTION_LIST_ORGS);
        assertTrue(result.isEmpty());
    }

    @Test
    void denyOnBroaderPatternShadowsAllow() {
        // Allow: *://tenant:TENANT_ID/organization:ID
        CompiledResourcePath allowPattern = resource("tenant", TENANT_ID, "organization", UUID.randomUUID());
        // Deny: * (single wildcard) at the same depth shadows the concrete allow
        CompiledResourcePath denyPattern = CompiledResourcePath.builder()
                .one("tenant", TENANT_ID).any().build();
        Policy allowPol = allowPolicy(ACTION_LIST_ORGS, allowPattern);
        Policy denyPol = denyPolicy(ACTION_LIST_ORGS, denyPattern);
        Principal principal = new Principal(UUID.randomUUID());
        principal.addPolicy(new PrincipalPolicyAssignment(principal, allowPol));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, denyPol));
        compile(List.of(principal));

        Set<CompiledResourcePath> result = query().query(principal.id(), ACTION_LIST_ORGS);
        // The DENY * pattern matches the concrete allow pattern
        assertTrue(result.isEmpty());
    }

    @Test
    void denyOnNarrowerPatternDoesNotShadowBroaderAllow() {
        CompiledResourcePath allowPattern = CompiledResourcePath.builder()
                .one("tenant", TENANT_ID).any(true).build();
        CompiledResourcePath denyPattern = CompiledResourcePath.builder()
                .one("tenant", TENANT_ID).build();
        Policy allowPol = allowPolicy(ACTION_LIST_ORGS, allowPattern);
        Policy denyPol = denyPolicy(ACTION_LIST_ORGS, denyPattern);
        Principal principal = new Principal(UUID.randomUUID());
        principal.addPolicy(new PrincipalPolicyAssignment(principal, allowPol));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, denyPol));
        compile(List.of(principal));

        Set<CompiledResourcePath> result = query().query(principal.id(), ACTION_LIST_ORGS);
        assertEquals(1, result.size());
        assertTrue(result.contains(allowPattern));
    }

    @Test
    void denyOnDifferentResourceDoesNotAffectAllow() {
        CompiledResourcePath allowRes = resource("organization", UUID.randomUUID());
        CompiledResourcePath denyRes = resource("organization", UUID.randomUUID());
        Policy allowPol = allowPolicy(ACTION_LIST_ORGS, allowRes);
        Policy denyPol = denyPolicy(ACTION_LIST_ORGS, denyRes);
        Principal principal = new Principal(UUID.randomUUID());
        principal.addPolicy(new PrincipalPolicyAssignment(principal, allowPol));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, denyPol));
        compile(List.of(principal));

        Set<CompiledResourcePath> result = query().query(principal.id(), ACTION_LIST_ORGS);
        assertEquals(1, result.size());
        assertTrue(result.contains(allowRes));
    }

    @Test
    void principalWithNoPoliciesReturnsEmpty() {
        Principal principal = new Principal(UUID.randomUUID());
        compile(List.of(principal));

        Set<CompiledResourcePath> result = query().query(principal.id(), ACTION_LIST_ORGS);
        assertTrue(result.isEmpty());
    }

    @Test
    void deduplicatesIdenticalResourcePatterns() {
        CompiledResourcePath r = resource("organization", UUID.randomUUID());
        Policy policy1 = allowPolicy(ACTION_LIST_ORGS, r);
        Policy policy2 = allowPolicy(ACTION_LIST_ORGS, r);
        Principal principal = new Principal(UUID.randomUUID());
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy1));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy2));
        compile(List.of(principal));

        Set<CompiledResourcePath> result = query().query(principal.id(), ACTION_LIST_ORGS);
        assertEquals(1, result.size());
    }

    @Test
    void policiesGrantedViaRoleAreIncluded() {
        CompiledResourcePath orgResource = resource("organization", UUID.randomUUID());
        Policy policy = allowPolicy(ACTION_LIST_ORGS, orgResource);
        Role role = new Role(UUID.randomUUID());
        role.addPolicy(new RolePolicyAssignment(role, policy));
        Principal principal = new Principal(UUID.randomUUID());
        principal.addRole(new PrincipalRoleAssignment(principal, role, null, null));
        compile(List.of(principal));

        Set<CompiledResourcePath> result = query().query(principal.id(), ACTION_LIST_ORGS);
        assertEquals(1, result.size());
        assertTrue(result.contains(orgResource));
    }

    @Test
    void denyViaRoleShadowsAllowViaDirectPolicy() {
        CompiledResourcePath orgResource = resource("organization", UUID.randomUUID());
        Policy allowPol = allowPolicy(ACTION_LIST_ORGS, orgResource);
        Policy denyPol = denyPolicy(ACTION_LIST_ORGS, orgResource);
        Role role = new Role(UUID.randomUUID());
        role.addPolicy(new RolePolicyAssignment(role, denyPol));
        Principal principal = new Principal(UUID.randomUUID());
        principal.addPolicy(new PrincipalPolicyAssignment(principal, allowPol));
        principal.addRole(new PrincipalRoleAssignment(principal, role, null, null));
        compile(List.of(principal));

        Set<CompiledResourcePath> result = query().query(principal.id(), ACTION_LIST_ORGS);
        assertTrue(result.isEmpty());
    }
}
