package com.jasminesoftwaresolutions.witchwood.server;

import java.util.Arrays;
import java.util.List;

/**
 * Canonical action permission strings for Witchwood gRPC operations.
 *
 * <p>These are used by {@link WitchwoodGrpcService} to authorize callers
 * via the {@link com.jasminesoftwaresolutions.witchwood.evaluation.EvaluationEngine}.
 */
public final class WitchwoodActions {
    private WitchwoodActions() {}

    private static final String SVC = "witchwood.jasmine.software";

    public static final String CREATE_PRINCIPAL = SVC + ":CreatePrincipal";
    public static final String GET_PRINCIPAL = SVC + ":GetPrincipal";
    public static final String UPDATE_PRINCIPAL = SVC + ":UpdatePrincipal";
    public static final String DELETE_PRINCIPAL = SVC + ":DeletePrincipal";

    public static final String CREATE_POLICY = SVC + ":CreatePolicy";
    public static final String GET_POLICY = SVC + ":GetPolicy";
    public static final String UPDATE_POLICY = SVC + ":UpdatePolicy";
    public static final String DELETE_POLICY = SVC + ":DeletePolicy";

    public static final String CREATE_ROLE = SVC + ":CreateRole";
    public static final String GET_ROLE = SVC + ":GetRole";
    public static final String UPDATE_ROLE = SVC + ":UpdateRole";
    public static final String DELETE_ROLE = SVC + ":DeleteRole";

    public static final String EVALUATE = SVC + ":Evaluate";
    public static final String QUERY_RESOURCES = SVC + ":QueryResources";

    public static final String CREATE_SERVICE = SVC + ":CreateService";
    public static final String DELETE_SERVICE = SVC + ":DeleteService";
    public static final String UPDATE_SERVICE = SVC + ":UpdateService";

    public static final List<String> ALL = Arrays.asList(
            CREATE_PRINCIPAL, GET_PRINCIPAL, UPDATE_PRINCIPAL, DELETE_PRINCIPAL,
            CREATE_POLICY, GET_POLICY, UPDATE_POLICY, DELETE_POLICY,
            CREATE_ROLE, GET_ROLE, UPDATE_ROLE, DELETE_ROLE,
            EVALUATE, QUERY_RESOURCES,
            CREATE_SERVICE, DELETE_SERVICE, UPDATE_SERVICE
    );
}
