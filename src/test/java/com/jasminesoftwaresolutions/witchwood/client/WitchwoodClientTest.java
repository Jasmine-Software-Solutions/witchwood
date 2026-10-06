package com.jasminesoftwaresolutions.witchwood.client;

import com.jasminesoftwaresolutions.witchwood.entities.Policy;
import com.jasminesoftwaresolutions.witchwood.entities.PolicyStatement;
import com.jasminesoftwaresolutions.witchwood.entities.Principal;
import com.jasminesoftwaresolutions.witchwood.entities.PrincipalPolicyAssignment;
import com.jasminesoftwaresolutions.witchwood.evaluation.EvaluationEngine;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.ServiceDirectory;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers.ModelSnapshotCompiler;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import com.jasminesoftwaresolutions.witchwood.models.PolicyEffect;
import com.jasminesoftwaresolutions.witchwood.repository.PolicyRepository;
import com.jasminesoftwaresolutions.witchwood.repository.PrincipalRepository;
import com.jasminesoftwaresolutions.witchwood.repository.RoleRepository;
import com.jasminesoftwaresolutions.witchwood.server.ServerConstants;
import com.jasminesoftwaresolutions.witchwood.server.WitchwoodGrpcService;
import com.jasminesoftwaresolutions.witchwood.server.WitchwoodResources;
import com.jasminesoftwaresolutions.witchwood.util.JsonLogicEvaluators;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ServerInterceptor;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WitchwoodClientTest {

    private static final String SERVICE = "onboarding.jasmine.software";
    private static final String ACTION = SERVICE + ":CreateCandidate";

    @Mock private PrincipalRepository principalRepo;
    @Mock private PolicyRepository policyRepo;
    @Mock private RoleRepository roleRepo;

    private GrpcCleanupRule grpcCleanup;
    private WitchwoodClient client;

    private final CompiledResourcePath testResource = new CompiledResourcePath(SERVICE,
            new CompiledResourcePath.ResourceNode("candidate",
                    new CompiledResourcePath.DefiniteId(UUID.randomUUID()), null));

    @BeforeEach
    void setUp() throws Exception {
        grpcCleanup = new GrpcCleanupRule();

        var serviceDirectory = ServiceDirectory.from(Map.of(
                SERVICE, Set.of("CreateCandidate"),
                "witchwood.jasmine.software", Set.of("CreatePrincipal", "GetPrincipal", "UpdatePrincipal",
                        "DeletePrincipal", "CreatePolicy", "GetPolicy", "UpdatePolicy",
                        "DeletePolicy", "CreateRole", "GetRole", "UpdateRole",
                        "DeleteRole", "Evaluate")));
        UUID servicePrincipalId = serviceDirectory.principalId(SERVICE);

        var principal = new Principal(servicePrincipalId);
        var policy = new Policy(UUID.randomUUID());
        policy.addStatement(new PolicyStatement(policy, 1, PolicyEffect.ALLOW,
                Set.of(ACTION), Set.of(testResource), null));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, policy));

        var anyWitchwoodResource = CompiledResourcePath.builder()
                .service(WitchwoodResources.WITCHWOOD_FQDN)
                .any(true)
                .build();

        var witchwoodPolicy = new Policy(UUID.randomUUID());
        witchwoodPolicy.addStatement(new PolicyStatement(witchwoodPolicy, 1, PolicyEffect.ALLOW,
                Set.of("witchwood.jasmine.software:CreatePrincipal", "witchwood.jasmine.software:GetPrincipal",
                        "witchwood.jasmine.software:UpdatePrincipal", "witchwood.jasmine.software:DeletePrincipal",
                        "witchwood.jasmine.software:CreatePolicy", "witchwood.jasmine.software:GetPolicy",
                        "witchwood.jasmine.software:UpdatePolicy", "witchwood.jasmine.software:DeletePolicy",
                        "witchwood.jasmine.software:CreateRole", "witchwood.jasmine.software:GetRole",
                        "witchwood.jasmine.software:UpdateRole", "witchwood.jasmine.software:DeleteRole",
                        "witchwood.jasmine.software:Evaluate"),
                Set.of(WitchwoodResources.SERVICE_ROOT, anyWitchwoodResource), null));
        principal.addPolicy(new PrincipalPolicyAssignment(principal, witchwoodPolicy));

        var snapshot = new ModelSnapshotCompiler(serviceDirectory, List.of(principal))
                .compile()
                .snapshot();
        EvaluationEngine engine = new EvaluationEngine(snapshot, JsonLogicEvaluators.withAllExpressions());

        var service = new WitchwoodGrpcService(principalRepo, policyRepo, roleRepo,
                org.mockito.Mockito.mock(com.jasminesoftwaresolutions.witchwood.repository.ServiceRepository.class),
                engine);

        String serverName = InProcessServerBuilder.generateName();
        grpcCleanup.register(InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(service)
                .intercept(new BypassAuthInterceptor())
                .build()
                .start());

        ManagedChannel channel = grpcCleanup.register(InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build());

        client = new WitchwoodClient(channel, () -> "test-token");
    }

    // -------------------------------------------------------------------
    // Principal operations
    // -------------------------------------------------------------------

    @Test
    void createAndGetPrincipal() {
        UUID id = UUID.randomUUID();
        when(principalRepo.createPrincipal(id)).thenReturn(id);
        when(principalRepo.principalExists(id)).thenReturn(true);

        UUID created = client.createPrincipal(id, List.of(), List.of()).join();
        assertEquals(id, created);

        var info = client.getPrincipal(id).join();
        assertEquals(id, info.id());
    }

    @Test
    void createPrincipalAutoGeneratesId() {
        when(principalRepo.createPrincipal(any())).thenReturn(null);
        UUID id = client.createPrincipal(null, List.of(), List.of()).join();
        assertNotNull(id);
    }

    @Test
    void createPrincipalWithRoleAssignments() {
        UUID id = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        when(principalRepo.createPrincipal(id)).thenReturn(id);

        UUID created = client.createPrincipal(id,
                List.of(new WitchwoodClient.RoleAssignment(roleId)),
                List.of()).join();
        assertEquals(id, created);
        verify(principalRepo).replaceRoleAssignments(eq(id), any());
    }

    @Test
    void updateAndDeletePrincipal() {
        UUID id = UUID.randomUUID();
        when(principalRepo.principalExists(id)).thenReturn(true);

        client.updatePrincipal(id, List.of(), List.of()).join();
        verify(principalRepo).replaceRoleAssignments(id, List.of());
        verify(principalRepo).replacePolicyAssignments(id, List.of());

        client.deletePrincipal(id).join();
        verify(principalRepo).deletePrincipal(id);
    }

    @Test
    void getPrincipalNotFoundThrows() {
        UUID id = UUID.randomUUID();
        when(principalRepo.principalExists(id)).thenReturn(false);

        assertThrows(Exception.class, () -> client.getPrincipal(id).join());
    }

    // -------------------------------------------------------------------
    // Policy operations
    // -------------------------------------------------------------------

    @Test
    void createPolicyWithStatements() {
        UUID id = UUID.randomUUID();
        when(policyRepo.createPolicy(id)).thenReturn(id);

        var stmt = new WitchwoodClient.PolicyStatement(1, "ALLOW",
                Set.of(ACTION), Set.of(testResource), null);

        UUID created = client.createPolicy(id, List.of(stmt)).join();
        assertEquals(id, created);
        verify(policyRepo).replaceStatements(eq(id), any());
    }

    @Test
    void updateAndDeletePolicy() {
        UUID id = UUID.randomUUID();
        when(policyRepo.policyExists(id)).thenReturn(true);

        client.updatePolicy(id, List.of()).join();
        verify(policyRepo).replaceStatements(eq(id), any());

        client.deletePolicy(id).join();
        verify(policyRepo).deletePolicy(id);
    }

    // -------------------------------------------------------------------
    // Role operations
    // -------------------------------------------------------------------

    @Test
    void createRoleWithPolicyIds() {
        UUID id = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();
        when(roleRepo.createRole(id)).thenReturn(id);

        UUID created = client.createRole(id, Set.of(policyId)).join();
        assertEquals(id, created);
        verify(roleRepo).replacePolicyAssignments(eq(id), any());
    }

    @Test
    void updateAndDeleteRole() {
        UUID id = UUID.randomUUID();
        when(roleRepo.roleExists(id)).thenReturn(true);

        client.updateRole(id, Set.of()).join();
        verify(roleRepo).replacePolicyAssignments(eq(id), any());

        client.deleteRole(id).join();
        verify(roleRepo).deleteRole(id);
    }

    // -------------------------------------------------------------------
    // Evaluate
    // -------------------------------------------------------------------

    @Test
    void evaluateAllowedReturnsTrue() {
        boolean allowed = client.evaluate(SERVICE, ACTION, testResource, Map.of()).join();
        assertTrue(allowed);
    }

    @Test
    void evaluateDeniedReturnsFalse() {
        var unknownResource = new CompiledResourcePath("other-service",
                new CompiledResourcePath.ResourceNode("thing",
                        new CompiledResourcePath.DefiniteId(UUID.randomUUID()), null));

        boolean allowed = client.evaluate("unknown.service", ACTION, unknownResource, Map.of()).join();
        assertFalse(allowed);
    }

    @Test
    void evaluateWithContext() {
        boolean allowed = client.evaluate(SERVICE, ACTION, testResource,
                Map.of("region", "us-east-1")).join();
        assertTrue(allowed);
    }

    // -------------------------------------------------------------------
    // CompiledResourcePath builder (client-side)
    // -------------------------------------------------------------------

    @Test
    void builderServiceRoot() {
        var path = CompiledResourcePath.builder()
                .service("onboarding.jasmine.software")
                .build();
        assertEquals("onboarding.jasmine.software", path.service());
        assertNull(path.root());
    }

    @Test
    void builderSingleResource() {
        UUID id = UUID.randomUUID();
        var path = CompiledResourcePath.builder()
                .service("onboarding.jasmine.software")
                .one("candidate", id)
                .build();

        assertEquals("onboarding.jasmine.software", path.service());
        var node = (CompiledResourcePath.ResourceNode) path.root();
        assertEquals("candidate", node.type());
        assertEquals(id, ((CompiledResourcePath.DefiniteId) node.resourceId()).id());
        assertNull(node.child());
    }

    @Test
    void builderNestedResource() {
        UUID outerId = UUID.randomUUID();
        UUID innerId = UUID.randomUUID();

        var path = CompiledResourcePath.builder()
                .service("onboarding.jasmine.software")
                .one("candidate", outerId)
                .one("profile", innerId)
                .build();

        var outer = (CompiledResourcePath.ResourceNode) path.root();
        assertEquals("candidate", outer.type());

        var inner = (CompiledResourcePath.ResourceNode) outer.child();
        assertNotNull(inner);
        assertEquals("profile", inner.type());
        assertEquals(innerId, ((CompiledResourcePath.DefiniteId) inner.resourceId()).id());
    }

    @Test
    void builderWildcardId() {
        var path = CompiledResourcePath.builder()
                .service("onboarding.jasmine.software")
                .any("candidate")
                .build();
        var node = (CompiledResourcePath.ResourceNode) path.root();
        assertInstanceOf(CompiledResourcePath.WildcardId.class, node.resourceId());
    }

    @Test
    void builderNonGreedyWildcard() {
        var path = CompiledResourcePath.builder()
                .service("onboarding.jasmine.software")
                .any()
                .build();
        assertInstanceOf(CompiledResourcePath.WildcardNode.class, path.root());
        assertFalse(((CompiledResourcePath.WildcardNode) path.root()).greedy());
    }

    @Test
    void builderGreedyWildcard() {
        var path = CompiledResourcePath.builder()
                .service("onboarding.jasmine.software")
                .any(true)
                .build();
        assertInstanceOf(CompiledResourcePath.WildcardNode.class, path.root());
        assertTrue(((CompiledResourcePath.WildcardNode) path.root()).greedy());
    }

    @Test
    void roleAssignmentConvenienceCtor() {
        UUID roleId = UUID.randomUUID();
        var ra = new WitchwoodClient.RoleAssignment(roleId);
        assertEquals(roleId, ra.roleId());
        assertNull(ra.effective());
        assertNull(ra.expiration());
    }

    // -------------------------------------------------------------------
    // Test interceptor
    // -------------------------------------------------------------------

    private static class BypassAuthInterceptor implements ServerInterceptor {
        @Override
        public <ReqT, RespT> io.grpc.ServerCall.Listener<ReqT> interceptCall(
                io.grpc.ServerCall<ReqT, RespT> call,
                io.grpc.Metadata headers,
                io.grpc.ServerCallHandler<ReqT, RespT> next) {
            Context ctx = Context.current()
                    .withValue(ServerConstants.CALLER_SUB, "test.service")
                    .withValue(ServerConstants.IS_TRUSTED_SUB, true);
            return io.grpc.Contexts.interceptCall(ctx, call, headers, next);
        }
    }
}
