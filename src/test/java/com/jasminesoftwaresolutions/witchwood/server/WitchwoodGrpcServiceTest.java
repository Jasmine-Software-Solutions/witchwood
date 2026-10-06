package com.jasminesoftwaresolutions.witchwood.server;

import com.jasminesoftwaresolutions.witchwood.entities.Policy;
import com.jasminesoftwaresolutions.witchwood.entities.PolicyStatement;
import com.jasminesoftwaresolutions.witchwood.entities.Principal;
import com.jasminesoftwaresolutions.witchwood.entities.PrincipalPolicyAssignment;
import com.jasminesoftwaresolutions.witchwood.proto.v1.*;
import com.jasminesoftwaresolutions.witchwood.evaluation.EvaluationEngine;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.ServiceDirectory;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers.ModelSnapshotCompiler;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import com.jasminesoftwaresolutions.witchwood.models.PolicyEffect;
import com.jasminesoftwaresolutions.witchwood.persistence.ResourcePathSerde;
import com.jasminesoftwaresolutions.witchwood.repository.PolicyRepository;
import com.jasminesoftwaresolutions.witchwood.repository.PrincipalRepository;
import com.jasminesoftwaresolutions.witchwood.repository.RoleRepository;
import com.jasminesoftwaresolutions.witchwood.util.JsonLogicEvaluators;
import io.grpc.Context;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
class WitchwoodGrpcServiceTest {

    private static final String TEST_SUB = "onboarding.jasmine.software";
    private static final String SERVICE = "onboarding.jasmine.software";
    private static final String ACTION = SERVICE + ":CreateCandidate";

    @Mock private PrincipalRepository principalRepo;
    @Mock private PolicyRepository policyRepo;
    @Mock private RoleRepository roleRepo;

    private EvaluationEngine engine;
    private GrpcCleanupRule grpcCleanup;
    private WitchwoodGrpc.WitchwoodBlockingStub stub;

    private Principal testPrincipal;
    private ServiceDirectory serviceDirectory;
    private CompiledResourcePath testResource;

    @BeforeEach
    void setUp() throws Exception {
        grpcCleanup = new GrpcCleanupRule();

        serviceDirectory = ServiceDirectory.from(Map.of(
                SERVICE, Set.of("CreateCandidate"),
                "witchwood.jasmine.software", Set.of("CreatePrincipal", "GetPrincipal", "UpdatePrincipal",
                        "DeletePrincipal", "CreatePolicy", "GetPolicy", "UpdatePolicy",
                        "DeletePolicy", "CreateRole", "GetRole", "UpdateRole",
                        "DeleteRole", "Evaluate")));
        UUID servicePrincipalId = serviceDirectory.principalId(SERVICE);

        var testResource = new CompiledResourcePath(SERVICE,
                new CompiledResourcePath.ResourceNode("candidate",
                        new CompiledResourcePath.DefiniteId(UUID.randomUUID()), null));
        this.testResource = testResource;

        testPrincipal = new Principal(servicePrincipalId);
        var policy = new Policy(UUID.randomUUID());
        policy.addStatement(new PolicyStatement(policy, 1, PolicyEffect.ALLOW,
                Set.of(ACTION), Set.of(testResource), null));
        testPrincipal.addPolicy(new PrincipalPolicyAssignment(testPrincipal, policy));

        // Grant Witchwood CRUD + Evaluate permissions to the onboarding principal.
        // SERVICE_ROOT (null root) covers create ops; greedy wildcard covers all others.
        var greedyWitchwood = new CompiledResourcePath("witchwood.jasmine.software",
                new CompiledResourcePath.WildcardNode(null, true));
        var witchwoodPermPolicy = new Policy(UUID.randomUUID());
        witchwoodPermPolicy.addStatement(new PolicyStatement(witchwoodPermPolicy, 1, PolicyEffect.ALLOW,
                Set.of("witchwood.jasmine.software:CreatePrincipal", "witchwood.jasmine.software:GetPrincipal",
                        "witchwood.jasmine.software:UpdatePrincipal", "witchwood.jasmine.software:DeletePrincipal",
                        "witchwood.jasmine.software:CreatePolicy", "witchwood.jasmine.software:GetPolicy",
                        "witchwood.jasmine.software:UpdatePolicy", "witchwood.jasmine.software:DeletePolicy",
                        "witchwood.jasmine.software:CreateRole", "witchwood.jasmine.software:GetRole",
                        "witchwood.jasmine.software:UpdateRole", "witchwood.jasmine.software:DeleteRole",
                        "witchwood.jasmine.software:Evaluate"),
                Set.of(WitchwoodResources.SERVICE_ROOT, greedyWitchwood), null));
        testPrincipal.addPolicy(new PrincipalPolicyAssignment(testPrincipal, witchwoodPermPolicy));

        var snapshot = new ModelSnapshotCompiler(serviceDirectory, List.of(testPrincipal))
                .compile()
                .snapshot();
        engine = new EvaluationEngine(snapshot, JsonLogicEvaluators.withAllExpressions());

        var service = new WitchwoodGrpcService(principalRepo, policyRepo, roleRepo, org.mockito.Mockito.mock(com.jasminesoftwaresolutions.witchwood.repository.ServiceRepository.class), engine);

        String serverName = InProcessServerBuilder.generateName();
        grpcCleanup.register(InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(service)
                .intercept(new TestAuthInterceptor(TEST_SUB, false))
                .build()
                .start());

        var channel = grpcCleanup.register(InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build());
        stub = WitchwoodGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
    }

    // -------------------------------------------------------------------
    // ProtoPrincipal CRUD
    // -------------------------------------------------------------------

    @Test
    void createPrincipalGeneratesIdWhenNoneProvided() {
        when(principalRepo.createPrincipal(any())).thenReturn(null);

        var resp = stub.createPrincipal(
                ProtoCreatePrincipalRequest.newBuilder().build());
        assertNotNull(resp.getId());
        assertFalse(resp.getId().isEmpty());
    }

    @Test
    void createPrincipalUsesProvidedId() {
        UUID id = UUID.randomUUID();
        when(principalRepo.createPrincipal(id)).thenReturn(id);

        var resp = stub.createPrincipal(
                ProtoCreatePrincipalRequest.newBuilder()
                        .setId(id.toString()).build());
        assertEquals(id.toString(), resp.getId());
    }

    @Test
    void createPrincipalWithRolesAndPolicies() {
        UUID id = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();
        when(principalRepo.createPrincipal(id)).thenReturn(id);

        var resp = stub.createPrincipal(
                ProtoCreatePrincipalRequest.newBuilder()
                        .setId(id.toString())
                        .addRoles(ProtoPrincipalRoleAssignment.newBuilder()
                                .setRoleId(roleId.toString()))
                        .addPolicies(ProtoPrincipalPolicyAssignment.newBuilder()
                                .setPolicyId(policyId.toString()))
                        .build());

        assertEquals(id.toString(), resp.getId());
        verify(principalRepo).replaceRoleAssignments(eq(id), any());
        verify(principalRepo).replacePolicyAssignments(eq(id), any());
    }

    @Test
    void getPrincipalReturnsId() {
        UUID id = UUID.randomUUID();
        when(principalRepo.principalExists(id)).thenReturn(true);

        var resp = stub.getPrincipal(
                ProtoGetPrincipalRequest.newBuilder()
                        .setId(id.toString()).build());
        assertEquals(id.toString(), resp.getPrincipal().getId());
    }

    @Test
    void getPrincipalNotFoundThrows() {
        UUID id = UUID.randomUUID();
        when(principalRepo.principalExists(id)).thenReturn(false);

        var ex = assertThrows(StatusRuntimeException.class, () ->
                stub.getPrincipal(
                        ProtoGetPrincipalRequest.newBuilder()
                                .setId(id.toString()).build()));
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void updatePrincipalReplacesAssignments() {
        UUID id = UUID.randomUUID();
        when(principalRepo.principalExists(id)).thenReturn(true);

        stub.updatePrincipal(
                ProtoUpdatePrincipalRequest.newBuilder()
                        .setId(id.toString())
                        .addRoles(ProtoPrincipalRoleAssignment.newBuilder()
                                .setRoleId(UUID.randomUUID().toString()))
                        .addPolicies(ProtoPrincipalPolicyAssignment.newBuilder()
                                .setPolicyId(UUID.randomUUID().toString()))
                        .build());

        verify(principalRepo).replaceRoleAssignments(eq(id), any());
        verify(principalRepo).replacePolicyAssignments(eq(id), any());
    }

    @Test
    void updatePrincipalClearsAssignmentsWhenEmpty() {
        UUID id = UUID.randomUUID();
        when(principalRepo.principalExists(id)).thenReturn(true);

        stub.updatePrincipal(
                ProtoUpdatePrincipalRequest.newBuilder()
                        .setId(id.toString()).build());

        verify(principalRepo).replaceRoleAssignments(id, List.of());
        verify(principalRepo).replacePolicyAssignments(id, List.of());
    }

    @Test
    void deletePrincipalCallsRepository() {
        UUID id = UUID.randomUUID();
        when(principalRepo.principalExists(id)).thenReturn(true);

        stub.deletePrincipal(
                ProtoDeletePrincipalRequest.newBuilder()
                        .setId(id.toString()).build());
        verify(principalRepo).deletePrincipal(id);
    }

    // -------------------------------------------------------------------
    // ProtoPolicy CRUD
    // -------------------------------------------------------------------

    @Test
    void createPolicyGeneratesId() {
        when(policyRepo.createPolicy(any())).thenReturn(null);

        var resp = stub.createPolicy(
                ProtoCreatePolicyRequest.newBuilder().build());
        assertNotNull(resp.getId());
    }

    @Test
    void createPolicyWithStatements() {
        UUID id = UUID.randomUUID();
        when(policyRepo.createPolicy(id)).thenReturn(id);

        var resp = stub.createPolicy(
                ProtoCreatePolicyRequest.newBuilder()
                        .setId(id.toString())
                        .addStatements(ProtoPolicyStatement.newBuilder()
                                .setId(1)
                                .setEffect("ALLOW")
                                .addActions(ACTION)
                                .addResources(ResourcePathSerde.toProto(
                                        new CompiledResourcePath(SERVICE,
                                                new CompiledResourcePath.ResourceNode("candidate",
                                                        new CompiledResourcePath.DefiniteId(UUID.randomUUID()), null))))
                                .build())
                        .build());

        assertEquals(id.toString(), resp.getId());
        verify(policyRepo).replaceStatements(eq(id), any());
    }

    @Test
    void getPolicyReturnsId() {
        UUID id = UUID.randomUUID();
        when(policyRepo.policyExists(id)).thenReturn(true);

        var resp = stub.getPolicy(
                ProtoGetPolicyRequest.newBuilder()
                        .setId(id.toString()).build());
        assertEquals(id.toString(), resp.getPolicy().getId());
    }

    @Test
    void updatePolicyCallsRepository() {
        UUID id = UUID.randomUUID();
        when(policyRepo.policyExists(id)).thenReturn(true);

        stub.updatePolicy(
                ProtoUpdatePolicyRequest.newBuilder()
                        .setId(id.toString())
                        .addStatements(ProtoPolicyStatement.newBuilder()
                                .setId(1)
                                .setEffect("DENY")
                                .build())
                        .build());

        verify(policyRepo).replaceStatements(eq(id), any());
    }

    @Test
    void deletePolicyCallsRepository() {
        UUID id = UUID.randomUUID();
        when(policyRepo.policyExists(id)).thenReturn(true);

        stub.deletePolicy(
                ProtoDeletePolicyRequest.newBuilder()
                        .setId(id.toString()).build());
        verify(policyRepo).deletePolicy(id);
    }

    // -------------------------------------------------------------------
    // ProtoRole CRUD
    // -------------------------------------------------------------------

    @Test
    void createRoleGeneratesId() {
        when(roleRepo.createRole(any())).thenReturn(null);

        var resp = stub.createRole(
                ProtoCreateRoleRequest.newBuilder().build());
        assertNotNull(resp.getId());
    }

    @Test
    void createRoleWithPolicyIds() {
        UUID id = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();
        when(roleRepo.createRole(id)).thenReturn(id);

        var resp = stub.createRole(
                ProtoCreateRoleRequest.newBuilder()
                        .setId(id.toString())
                        .addPolicyIds(policyId.toString())
                        .build());

        assertEquals(id.toString(), resp.getId());
        verify(roleRepo).replacePolicyAssignments(eq(id), any());
    }

    @Test
    void getRoleReturnsId() {
        UUID id = UUID.randomUUID();
        when(roleRepo.roleExists(id)).thenReturn(true);

        var resp = stub.getRole(
                ProtoGetRoleRequest.newBuilder()
                        .setId(id.toString()).build());
        assertEquals(id.toString(), resp.getRole().getId());
    }

    @Test
    void updateRoleCallsRepository() {
        UUID id = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();
        when(roleRepo.roleExists(id)).thenReturn(true);

        stub.updateRole(
                ProtoUpdateRoleRequest.newBuilder()
                        .setId(id.toString())
                        .addPolicyIds(policyId.toString())
                        .build());

        verify(roleRepo).replacePolicyAssignments(eq(id), any());
    }

    @Test
    void deleteRoleCallsRepository() {
        UUID id = UUID.randomUUID();
        when(roleRepo.roleExists(id)).thenReturn(true);

        stub.deleteRole(
                ProtoDeleteRoleRequest.newBuilder()
                        .setId(id.toString()).build());
        verify(roleRepo).deleteRole(id);
    }

    // -------------------------------------------------------------------
    // Evaluate
    // -------------------------------------------------------------------

    @Test
    void evaluateAllowedReturnsTrue() {
        var resp = stub.evaluate(
                ProtoEvaluateRequest.newBuilder()
                        .setPrincipal(UUID.nameUUIDFromBytes(SERVICE.getBytes()).toString())
                        .setAction(ACTION)
                        .setResource(ResourcePathSerde.toProto(testResource))
                        .build());

        assertTrue(resp.getAllowed());
    }

    @Test
    void evaluateDeniedReturnsFalse() {
        var resource = new CompiledResourcePath(SERVICE,
                new CompiledResourcePath.ResourceNode("candidate",
                        new CompiledResourcePath.DefiniteId(UUID.randomUUID()), null));

        var resp = stub.evaluate(
                ProtoEvaluateRequest.newBuilder()
                        .setPrincipal(UUID.nameUUIDFromBytes("unknown.service".getBytes()).toString())
                        .setAction(ACTION)
                        .setResource(ResourcePathSerde.toProto(resource))
                        .build());

        assertFalse(resp.getAllowed());
    }

    @Test
    void evaluateWithContextPassthrough() {
        var resp = stub.evaluate(
                ProtoEvaluateRequest.newBuilder()
                        .setPrincipal(UUID.nameUUIDFromBytes(SERVICE.getBytes()).toString())
                        .setAction(ACTION)
                        .setResource(ResourcePathSerde.toProto(testResource))
                        .putContext("region", "us-east-1")
                        .build());

        assertTrue(resp.getAllowed());
    }

    // -------------------------------------------------------------------
    // Authorization
    // -------------------------------------------------------------------

    @Nested
    class Authorization {

        @Test
        void isIdServiceBypassesAuthorization() throws Exception {
            var service = new WitchwoodGrpcService(principalRepo, policyRepo, roleRepo, org.mockito.Mockito.mock(com.jasminesoftwaresolutions.witchwood.repository.ServiceRepository.class), engine);

            String serverName = InProcessServerBuilder.generateName();
            grpcCleanup.register(InProcessServerBuilder.forName(serverName)
                    .directExecutor()
                    .addService(service)
                    .intercept(new TestAuthInterceptor("any-sub", true))
                    .build()
                    .start());

            var channel = grpcCleanup.register(InProcessChannelBuilder.forName(serverName)
                    .directExecutor()
                    .build());
            var idStub = WitchwoodGrpc.newBlockingStub(channel);

            when(principalRepo.principalExists(any())).thenReturn(true);

            var resp = idStub.getPrincipal(
                    ProtoGetPrincipalRequest.newBuilder()
                            .setId(UUID.randomUUID().toString()).build());
            assertNotNull(resp);
        }

        @Test
        void unauthorizedCallerGetsPermissionDenied() throws Exception {
            var emptySnapshot = new ModelSnapshotCompiler(
                    ServiceDirectory.from(Map.of("other-service", Set.of("x"))),
                    List.of()
            ).compile().snapshot();
            var strictEngine = new EvaluationEngine(emptySnapshot, JsonLogicEvaluators.withAllExpressions());

            var service = new WitchwoodGrpcService(principalRepo, policyRepo, roleRepo, org.mockito.Mockito.mock(com.jasminesoftwaresolutions.witchwood.repository.ServiceRepository.class), strictEngine);

            String serverName = InProcessServerBuilder.generateName();
            grpcCleanup.register(InProcessServerBuilder.forName(serverName)
                    .directExecutor()
                    .addService(service)
                    .intercept(new TestAuthInterceptor("unauthorized.service", false))
                    .build()
                    .start());

            var channel = grpcCleanup.register(InProcessChannelBuilder.forName(serverName)
                    .directExecutor()
                    .build());
            var strictStub = WitchwoodGrpc.newBlockingStub(channel);

            assertThrows(StatusRuntimeException.class, () ->
                    strictStub.createPrincipal(
                            ProtoCreatePrincipalRequest.newBuilder().build()));
            assertThrows(StatusRuntimeException.class, () ->
                    strictStub.createPolicy(
                            ProtoCreatePolicyRequest.newBuilder().build()));
            assertThrows(StatusRuntimeException.class, () ->
                    strictStub.createRole(
                            ProtoCreateRoleRequest.newBuilder().build()));
        }
    }

    // -------------------------------------------------------------------
    // Test interceptor
    // -------------------------------------------------------------------

    private static class TestAuthInterceptor implements io.grpc.ServerInterceptor {
        private final String sub;
        private final boolean isIdService;

        TestAuthInterceptor(String sub, boolean isIdService) {
            this.sub = sub;
            this.isIdService = isIdService;
        }

        @Override
        public <ReqT, RespT> io.grpc.ServerCall.Listener<ReqT> interceptCall(
                io.grpc.ServerCall<ReqT, RespT> call,
                io.grpc.Metadata headers,
                io.grpc.ServerCallHandler<ReqT, RespT> next) {
            Context ctx = Context.current()
                    .withValue(ServerConstants.CALLER_SUB, sub)
                    .withValue(ServerConstants.IS_TRUSTED_SUB, isIdService);
            return io.grpc.Contexts.interceptCall(ctx, call, headers, next);
        }
    }
}
