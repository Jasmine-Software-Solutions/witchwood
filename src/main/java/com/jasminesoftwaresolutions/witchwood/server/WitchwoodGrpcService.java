package com.jasminesoftwaresolutions.witchwood.server;

import com.jasminesoftwaresolutions.witchwood.evaluation.EvaluationEngine;
import com.jasminesoftwaresolutions.witchwood.evaluation.ResourceQuery;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import com.jasminesoftwaresolutions.witchwood.persistence.ResourcePathSerde;
import com.jasminesoftwaresolutions.witchwood.proto.v1.*;
import com.jasminesoftwaresolutions.witchwood.repository.PolicyRepository;
import com.jasminesoftwaresolutions.witchwood.repository.PrincipalRepository;
import com.jasminesoftwaresolutions.witchwood.repository.RoleRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class WitchwoodGrpcService extends WitchwoodGrpc.WitchwoodImplBase {
    private final PrincipalRepository principalRepository;
    private final PolicyRepository policyRepository;
    private final RoleRepository roleRepository;
    private final com.jasminesoftwaresolutions.witchwood.repository.ServiceRepository serviceRepository;
    private final EvaluationEngine evaluationEngine;

    public WitchwoodGrpcService(PrincipalRepository principalRepository,
                                PolicyRepository policyRepository,
                                RoleRepository roleRepository,
                                com.jasminesoftwaresolutions.witchwood.repository.ServiceRepository serviceRepository,
                                EvaluationEngine evaluationEngine) {
        this.principalRepository = principalRepository;
        this.policyRepository = policyRepository;
        this.roleRepository = roleRepository;
        this.serviceRepository = serviceRepository;
        this.evaluationEngine = evaluationEngine;
    }

    // -----------------------------------------------------------------------
    // Principal
    // -----------------------------------------------------------------------

    @Override
    public void createPrincipal(ProtoCreatePrincipalRequest request,
                                StreamObserver<ProtoCreatePrincipalResponse> responseObserver) {
        if (!authorize(WitchwoodActions.CREATE_PRINCIPAL, WitchwoodResources.SERVICE_ROOT, responseObserver)) return;
        UUID id = request.getId().isEmpty()
                ? UUID.randomUUID()
                : UUID.fromString(request.getId());

        principalRepository.createPrincipal(id);

        if (!request.getRolesList().isEmpty()) {
            principalRepository.replaceRoleAssignments(id,
                    request.getRolesList().stream()
                            .map(r -> new PrincipalRepository.RoleAssignment(
                                    UUID.fromString(r.getRoleId()),
                                    toInstant(r.getWindow().getEffective()),
                                    toInstant(r.getWindow().getExpiration())))
                            .toList());
        }

        if (!request.getPoliciesList().isEmpty()) {
            principalRepository.replacePolicyAssignments(id,
                    request.getPoliciesList().stream()
                            .map(p -> UUID.fromString(p.getPolicyId()))
                            .toList());
        }

        responseObserver.onNext(ProtoCreatePrincipalResponse.newBuilder().setId(id.toString()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getPrincipal(ProtoGetPrincipalRequest request,
                             StreamObserver<ProtoGetPrincipalResponse> responseObserver) {
        UUID id = UUID.fromString(request.getId());
        if (!authorize(WitchwoodActions.GET_PRINCIPAL, WitchwoodResources.principal(id), responseObserver)) return;
        if (!principalRepository.principalExists(id)) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Principal not found: " + id).asRuntimeException());
            return;
        }

        responseObserver.onNext(ProtoGetPrincipalResponse.newBuilder()
                .setPrincipal(ProtoPrincipal.newBuilder()
                        .setId(id.toString()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void updatePrincipal(ProtoUpdatePrincipalRequest request,
                                StreamObserver<ProtoUpdatePrincipalResponse> responseObserver) {
        UUID id = UUID.fromString(request.getId());
        if (!authorize(WitchwoodActions.UPDATE_PRINCIPAL, WitchwoodResources.principal(id), responseObserver)) return;
        if (!principalRepository.principalExists(id)) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Principal not found: " + id).asRuntimeException());
            return;
        }

        if (!request.getRolesList().isEmpty()) {
            principalRepository.replaceRoleAssignments(id,
                    request.getRolesList().stream()
                            .map(r -> new PrincipalRepository.RoleAssignment(
                                    UUID.fromString(r.getRoleId()),
                                    toInstant(r.getWindow().getEffective()),
                                    toInstant(r.getWindow().getExpiration())))
                            .toList());
        } else {
            principalRepository.replaceRoleAssignments(id, List.of());
        }

        if (!request.getPoliciesList().isEmpty()) {
            principalRepository.replacePolicyAssignments(id,
                    request.getPoliciesList().stream()
                            .map(p -> UUID.fromString(p.getPolicyId()))
                            .toList());
        } else {
            principalRepository.replacePolicyAssignments(id, List.of());
        }

        responseObserver.onNext(ProtoUpdatePrincipalResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void deletePrincipal(ProtoDeletePrincipalRequest request,
                                StreamObserver<ProtoDeletePrincipalResponse> responseObserver) {
        UUID id = UUID.fromString(request.getId());
        if (!authorize(WitchwoodActions.DELETE_PRINCIPAL, WitchwoodResources.principal(id), responseObserver)) return;
        if (!principalRepository.principalExists(id)) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Principal not found: " + id).asRuntimeException());
            return;
        }

        principalRepository.deletePrincipal(id);
        responseObserver.onNext(ProtoDeletePrincipalResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    // -----------------------------------------------------------------------
    // Policy
    // -----------------------------------------------------------------------

    @Override
    public void createPolicy(ProtoCreatePolicyRequest request,
                             StreamObserver<ProtoCreatePolicyResponse> responseObserver) {
        if (!authorize(WitchwoodActions.CREATE_POLICY, WitchwoodResources.SERVICE_ROOT, responseObserver)) return;
        UUID id = request.getId().isEmpty()
                ? UUID.randomUUID()
                : UUID.fromString(request.getId());

        policyRepository.createPolicy(id);

        if (!request.getStatementsList().isEmpty()) {
            policyRepository.replaceStatements(id,
                    request.getStatementsList().stream()
                            .map(this::toStatementData)
                            .toList());
        }

        responseObserver.onNext(ProtoCreatePolicyResponse.newBuilder().setId(id.toString()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getPolicy(ProtoGetPolicyRequest request,
                          StreamObserver<ProtoGetPolicyResponse> responseObserver) {
        UUID id = UUID.fromString(request.getId());
        if (!authorize(WitchwoodActions.GET_POLICY, WitchwoodResources.policy(id), responseObserver)) return;
        if (!policyRepository.policyExists(id)) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Policy not found: " + id).asRuntimeException());
            return;
        }

        responseObserver.onNext(ProtoGetPolicyResponse.newBuilder()
                .setPolicy(ProtoPolicy.newBuilder()
                        .setId(id.toString()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void updatePolicy(ProtoUpdatePolicyRequest request,
                             StreamObserver<ProtoUpdatePolicyResponse> responseObserver) {
        UUID id = UUID.fromString(request.getId());
        if (!authorize(WitchwoodActions.UPDATE_POLICY, WitchwoodResources.policy(id), responseObserver)) return;
        if (!policyRepository.policyExists(id)) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Policy not found: " + id).asRuntimeException());
            return;
        }

        policyRepository.replaceStatements(id,
                request.getStatementsList().stream()
                        .map(this::toStatementData)
                        .toList());

        responseObserver.onNext(ProtoUpdatePolicyResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void deletePolicy(ProtoDeletePolicyRequest request,
                             StreamObserver<ProtoDeletePolicyResponse> responseObserver) {
        UUID id = UUID.fromString(request.getId());
        if (!authorize(WitchwoodActions.DELETE_POLICY, WitchwoodResources.policy(id), responseObserver)) return;
        if (!policyRepository.policyExists(id)) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Policy not found: " + id).asRuntimeException());
            return;
        }

        policyRepository.deletePolicy(id);
        responseObserver.onNext(ProtoDeletePolicyResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    // -----------------------------------------------------------------------
    // Role
    // -----------------------------------------------------------------------

    @Override
    public void createRole(ProtoCreateRoleRequest request,
                           StreamObserver<ProtoCreateRoleResponse> responseObserver) {
        if (!authorize(WitchwoodActions.CREATE_ROLE, WitchwoodResources.SERVICE_ROOT, responseObserver)) return;
        UUID id = request.getId().isEmpty()
                ? UUID.randomUUID()
                : UUID.fromString(request.getId());

        roleRepository.createRole(id);

        if (!request.getPolicyIdsList().isEmpty()) {
            roleRepository.replacePolicyAssignments(id,
                    request.getPolicyIdsList().stream()
                            .map(UUID::fromString)
                            .toList());
        }

        responseObserver.onNext(ProtoCreateRoleResponse.newBuilder().setId(id.toString()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getRole(ProtoGetRoleRequest request,
                        StreamObserver<ProtoGetRoleResponse> responseObserver) {
        UUID id = UUID.fromString(request.getId());
        if (!authorize(WitchwoodActions.GET_ROLE, WitchwoodResources.role(id), responseObserver)) return;
        if (!roleRepository.roleExists(id)) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Role not found: " + id).asRuntimeException());
            return;
        }

        responseObserver.onNext(ProtoGetRoleResponse.newBuilder()
                .setRole(ProtoRole.newBuilder()
                        .setId(id.toString()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void updateRole(ProtoUpdateRoleRequest request,
                           StreamObserver<ProtoUpdateRoleResponse> responseObserver) {
        UUID id = UUID.fromString(request.getId());
        if (!authorize(WitchwoodActions.UPDATE_ROLE, WitchwoodResources.role(id), responseObserver)) return;
        if (!roleRepository.roleExists(id)) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Role not found: " + id).asRuntimeException());
            return;
        }

        roleRepository.replacePolicyAssignments(id,
                request.getPolicyIdsList().stream()
                        .map(UUID::fromString)
                        .toList());

        responseObserver.onNext(ProtoUpdateRoleResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteRole(ProtoDeleteRoleRequest request,
                           StreamObserver<ProtoDeleteRoleResponse> responseObserver) {
        UUID id = UUID.fromString(request.getId());
        if (!authorize(WitchwoodActions.DELETE_ROLE, WitchwoodResources.role(id), responseObserver)) return;
        if (!roleRepository.roleExists(id)) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Role not found: " + id).asRuntimeException());
            return;
        }

        roleRepository.deleteRole(id);
        responseObserver.onNext(ProtoDeleteRoleResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    // -- Incremental role-policy assignments --

    @Override
    public void assignPolicyToRole(ProtoAssignPolicyToRoleRequest request,
                                   StreamObserver<ProtoAssignPolicyToRoleResponse> responseObserver) {
        UUID roleId = UUID.fromString(request.getRoleId());
        UUID policyId = UUID.fromString(request.getPolicyId());
        if (!authorize(WitchwoodActions.UPDATE_ROLE, WitchwoodResources.role(roleId), responseObserver)) return;
        if (!roleRepository.roleExists(roleId)) {
            roleRepository.createRole(roleId);
        }
        if (!policyRepository.policyExists(policyId)) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Policy not found: " + policyId).asRuntimeException());
            return;
        }
        roleRepository.addPolicyAssignment(roleId, policyId);
        responseObserver.onNext(ProtoAssignPolicyToRoleResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void unassignPolicyFromRole(ProtoUnassignPolicyFromRoleRequest request,
                                       StreamObserver<ProtoUnassignPolicyFromRoleResponse> responseObserver) {
        UUID roleId = UUID.fromString(request.getRoleId());
        UUID policyId = UUID.fromString(request.getPolicyId());
        if (!authorize(WitchwoodActions.UPDATE_ROLE, WitchwoodResources.role(roleId), responseObserver)) return;
        roleRepository.removePolicyAssignment(roleId, policyId);
        responseObserver.onNext(ProtoUnassignPolicyFromRoleResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    // -- Incremental principal-role assignments --

    @Override
    public void assignRoleToPrincipal(ProtoAssignRoleToPrincipalRequest request,
                                      StreamObserver<ProtoAssignRoleToPrincipalResponse> responseObserver) {
        UUID principalId = UUID.fromString(request.getPrincipalId());
        UUID roleId = UUID.fromString(request.getRoleId());
        if (!authorize(WitchwoodActions.UPDATE_PRINCIPAL, WitchwoodResources.principal(principalId), responseObserver)) return;
        if (!principalRepository.principalExists(principalId)) {
            principalRepository.createPrincipal(principalId);
        }
        if (!roleRepository.roleExists(roleId)) {
            roleRepository.createRole(roleId);
        }
        principalRepository.addRoleAssignment(principalId, roleId,
                toInstant(request.getWindow().getEffective()),
                toInstant(request.getWindow().getExpiration()));
        responseObserver.onNext(ProtoAssignRoleToPrincipalResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void unassignRoleFromPrincipal(ProtoUnassignRoleFromPrincipalRequest request,
                                          StreamObserver<ProtoUnassignRoleFromPrincipalResponse> responseObserver) {
        UUID principalId = UUID.fromString(request.getPrincipalId());
        UUID roleId = UUID.fromString(request.getRoleId());
        if (!authorize(WitchwoodActions.UPDATE_PRINCIPAL, WitchwoodResources.principal(principalId), responseObserver)) return;
        principalRepository.removeRoleAssignment(principalId, roleId);
        responseObserver.onNext(ProtoUnassignRoleFromPrincipalResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    // -- Incremental principal-policy assignments --

    @Override
    public void assignPolicyToPrincipal(ProtoAssignPolicyToPrincipalRequest request,
                                        StreamObserver<ProtoAssignPolicyToPrincipalResponse> responseObserver) {
        UUID principalId = UUID.fromString(request.getPrincipalId());
        UUID policyId = UUID.fromString(request.getPolicyId());
        if (!authorize(WitchwoodActions.UPDATE_PRINCIPAL, WitchwoodResources.principal(principalId), responseObserver)) return;
        if (!principalRepository.principalExists(principalId)) {
            principalRepository.createPrincipal(principalId);
        }
        if (!policyRepository.policyExists(policyId)) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Policy not found: " + policyId).asRuntimeException());
            return;
        }
        principalRepository.addPolicyAssignment(principalId, policyId);
        responseObserver.onNext(ProtoAssignPolicyToPrincipalResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void unassignPolicyFromPrincipal(ProtoUnassignPolicyFromPrincipalRequest request,
                                            StreamObserver<ProtoUnassignPolicyFromPrincipalResponse> responseObserver) {
        UUID principalId = UUID.fromString(request.getPrincipalId());
        UUID policyId = UUID.fromString(request.getPolicyId());
        if (!authorize(WitchwoodActions.UPDATE_PRINCIPAL, WitchwoodResources.principal(principalId), responseObserver)) return;
        principalRepository.removePolicyAssignment(principalId, policyId);
        responseObserver.onNext(ProtoUnassignPolicyFromPrincipalResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    // -----------------------------------------------------------------------
    // Evaluate
    // -----------------------------------------------------------------------

    @Override
    public void evaluate(ProtoEvaluateRequest request,
                         StreamObserver<ProtoEvaluateResponse> responseObserver) {
        System.out.println("[ww-server-eval] evaluate called: principal=" + request.getPrincipal() + " action=" + request.getAction());
        CompiledResourcePath compiledResource;
        if (request.hasResource()) {
            compiledResource = ResourcePathSerde.fromProto(request.getResource());
        } else {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("resource is required").asRuntimeException());
            return;
        }

        // The caller must have Witchwood:Evaluate on the service:action pair
        // corresponding to the action they're evaluating.
        int colon = request.getAction().indexOf(':');
        if (colon > 0) {
            String svc = request.getAction().substring(0, colon);
            String act = request.getAction().substring(colon + 1);

            System.out.println("[ww-server-eval] authorizing caller for Evaluate on " + svc + ":" + act);
            if (!authorize(WitchwoodActions.EVALUATE,
                    WitchwoodResources.evaluatePermission(svc, act),
                    responseObserver)) return;
            System.out.println("[ww-server-eval] caller authorized");
        }

        Map<String, Object> context = new HashMap<>(request.getContextMap());

        String principalStr = request.getPrincipal();
        UUID principalUuid = UUID.fromString(principalStr);

        System.out.println("[ww-server-eval] calling evaluationEngine.evaluate...");
        boolean allowed = evaluationEngine.evaluate(
                principalUuid,
                request.getAction(),
                compiledResource,
                context);
        System.out.println("[ww-server-eval] evaluation result: allowed=" + allowed);

        responseObserver.onNext(ProtoEvaluateResponse.newBuilder().setAllowed(allowed).build());
        responseObserver.onCompleted();
    }

    // -----------------------------------------------------------------------
    // Service & Action Registration
    // -----------------------------------------------------------------------

    @Override
    public void registerService(ProtoRegisterServiceRequest request,
                                StreamObserver<ProtoRegisterServiceResponse> responseObserver) {
        String fqdn = request.getName();
        if (!authorize(WitchwoodActions.CREATE_SERVICE, WitchwoodResources.SERVICE_ROOT, responseObserver)) return;

        UUID id = serviceRepository.registerService(fqdn);
        responseObserver.onNext(ProtoRegisterServiceResponse.newBuilder()
                .setId(id.toString()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void deregisterService(ProtoDeregisterServiceRequest request,
                                  StreamObserver<ProtoDeregisterServiceResponse> responseObserver) {
        String fqdn = request.getName();
        if (!authorize(WitchwoodActions.DELETE_SERVICE, WitchwoodResources.SERVICE_ROOT, responseObserver)) return;

        serviceRepository.deregisterService(fqdn);
        responseObserver.onNext(ProtoDeregisterServiceResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void putActions(ProtoPutActionsRequest request,
                           StreamObserver<ProtoPutActionsResponse> responseObserver) {
        if (!authorize(WitchwoodActions.UPDATE_SERVICE, WitchwoodResources.SERVICE_ROOT, responseObserver)) return;

        // Normalize: strip service prefix if caller passes fully-qualified
        // actions (e.g. "id.jasmine.software:ListRoles" → "ListRoles").
        List<String> normalized = request.getActionsList().stream()
                .map(a -> {
                    int idx = a.lastIndexOf(':');
                    return idx >= 0 ? a.substring(idx + 1) : a;
                })
                .toList();
        serviceRepository.putActions(request.getService(), normalized);
        responseObserver.onNext(ProtoPutActionsResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void queryResources(ProtoQueryResourcesRequest request,
                               StreamObserver<ProtoQueryResourcesResponse> responseObserver) {
        // The caller must have Witchwood:QueryResources on the service:action pair
        // corresponding to the action they're querying.
        int colon = request.getAction().indexOf(':');
        if (colon > 0) {
            String svc = request.getAction().substring(0, colon);
            String act = request.getAction().substring(colon + 1);

            if (!authorize(WitchwoodActions.QUERY_RESOURCES,
                    WitchwoodResources.evaluatePermission(svc, act),
                    responseObserver)) return;
        }

        UUID principalUuid;
        try {
            principalUuid = UUID.fromString(request.getPrincipal());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("invalid principal UUID").asRuntimeException());
            return;
        }

        ResourceQuery query = new ResourceQuery(evaluationEngine);
        Set<CompiledResourcePath> resources = query.query(principalUuid, request.getAction());

        var responseBuilder = ProtoQueryResourcesResponse.newBuilder();
        for (CompiledResourcePath r : resources)
            responseBuilder.addResources(ResourcePathSerde.toProto(r));

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    // -----------------------------------------------------------------------
    // Authorization
    // -----------------------------------------------------------------------

    /**
     * Check that the authenticated caller has the given action on the given
     * resource. If the caller is {@code id.jasmine.software}, the check is
     * skipped (superuser).
     *
     * <p>On denial, calls {@code responseObserver.onError(PERMISSION_DENIED)}
     * and returns {@code false}. The caller should return immediately when
     * this method returns {@code false}.
     */
    private boolean authorize(String action,
                              CompiledResourcePath resource,
                              StreamObserver<?> responseObserver) {
        String sub = ServerConstants.CALLER_SUB.get();
        System.out.println("[ww-server-auth] authorize: sub=" + sub + " action=" + action);
        if (sub == null) {
            responseObserver.onError(Status.UNAUTHENTICATED
                    .withDescription("No caller identity").asRuntimeException());
            return false;
        }

        Boolean isIdService = ServerConstants.IS_TRUSTED_SUB.get();
        if (isIdService != null && isIdService) {
            System.out.println("[ww-server-auth] trusted sub, allowing");
            return true;
        }

        System.out.println("[ww-server-auth] checking permission via evaluationEngine...");
        boolean allowed = evaluationEngine.evaluate(
                UUID.nameUUIDFromBytes(sub.getBytes(StandardCharsets.UTF_8)), action, resource, new HashMap<>());
        System.out.println("[ww-server-auth] permission check result: " + allowed);
        if (!allowed) {
            responseObserver.onError(Status.PERMISSION_DENIED
                    .withDescription("Permission denied for " + action + " on " + resource)
                    .asRuntimeException());
            return false;
        }

        return true;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private PolicyRepository.StatementData toStatementData(
            ProtoPolicyStatement stmt) {
        Set<String> actions = new LinkedHashSet<>(stmt.getActionsList());
        Set<CompiledResourcePath> resources = stmt.getResourcesList().stream()
                .map(ResourcePathSerde::fromProto)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        String condition = stmt.hasCondition() && !stmt.getCondition().getJson().isBlank()
                ? stmt.getCondition().getJson()
                : null;

        return new PolicyRepository.StatementData(
                stmt.getId(), stmt.getEffect(), actions, resources, condition);
    }

    private static Instant toInstant(long epochMillis) {
        // 0 is the proto default for unset int64 fields — treat as unbounded
        if (epochMillis == 0) return null;
        if (epochMillis == Long.MAX_VALUE) return null;
        return Instant.ofEpochMilli(epochMillis);
    }
}
