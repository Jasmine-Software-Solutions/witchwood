package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.EvaluationSnapshot;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.PermissionSetSnapshot;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.PermissionSnapshot;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.ServiceDirectory;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.ResourcePathCompiler;
import com.jasminesoftwaresolutions.witchwood.models.*;
import io.github.jamsesso.jsonlogic.ast.*;

import java.time.Instant;
import java.util.*;

/**
 * Generates realistic model objects ({@link IPrincipal}, {@link IRole},
 * {@link IPolicy}, {@link IPolicyStatement}) for benchmarking the
 * {@link ModelSnapshotCompiler}.
 *
 * <p>Uses a "hot set" strategy: a subset of roles, policies, actions, and
 * resources are shared across many principals and statements, creating
 * realistic hit rates when downstream benchmarks sample from the same pools.
 *
 * <p>Also pre-computes {@link QueryPools} — categorized (principal, action,
 * resource) triples with known evaluation outcomes — so that evaluation
 * benchmarks can control the ratio of ALLOW / explicit DENY / default DENY
 * precisely.
 */
public class DummyModelGenerator {
    protected static final String[] OPERATIONS = new String[] {
            "==", "!=", ">", "<", "+", "-"
    };

    protected final int numPrincipals;
    protected final int numRolesPerPrincipal;
    protected final int numDirectPoliciesPerPrincipal;
    protected final int numHotRoles;
    protected final int numHotDirectPolicies;
    protected final double hotRoleCoverage;
    protected final double hotDirectPolicyCoverage;
    protected final int numPoliciesPerRole;
    protected final int numStatementsPerPolicy;
    protected final int numActionsPerStatement;
    protected final int numResourcesPerStatement;
    protected final int numServices;
    protected final int numActionsPerService;
    protected final int numHotActions;
    protected final int numHotResources;
    protected final int allowToDenyEffectRatio;
    protected final double conditionProbability;
    protected final int numOperationsPerCondition;
    protected final int numVariablesPerEvaluation;
    protected final double wildcardPatternProbability;
    protected final int numResources;
    protected final int numResourceTypes;
    protected final int numNodesPerResource;

    protected final Random random = new Random();

    protected final List<UUID> principals = new ArrayList<>();
    protected final List<String> resources = new ArrayList<>();
    protected final List<CompiledResourcePath> compiledResources = new ArrayList<>();
    protected final List<String> actions = new ArrayList<>();
    protected final List<String> variables = new ArrayList<>();

    protected final List<String> hotActionPool = new ArrayList<>();
    protected final List<String> hotResourcePool = new ArrayList<>();

    protected ServiceDirectory serviceDirectory;
    protected List<IPrincipal> principalObjects;

    protected QueryPools queryPools;

    protected DummyModelGenerator(
            int numPrincipals,
            int numRolesPerPrincipal,
            int numDirectPoliciesPerPrincipal,
            int numHotRoles,
            int numHotDirectPolicies,
            double hotRoleCoverage,
            double hotDirectPolicyCoverage,
            int numPoliciesPerRole,
            int numStatementsPerPolicy,
            int numActionsPerStatement,
            int numResourcesPerStatement,
            int numServices,
            int numActionsPerService,
            int numHotActions,
            int numHotResources,
            int allowToDenyEffectRatio,
            double conditionProbability,
            int numOperationsPerCondition,
            int numVariablesPerEvaluation,
            double wildcardPatternProbability,
            int numResources,
            int numResourceTypes,
            int numNodesPerResource) {
        this.numPrincipals = numPrincipals;
        this.numRolesPerPrincipal = numRolesPerPrincipal;
        this.numDirectPoliciesPerPrincipal = numDirectPoliciesPerPrincipal;
        this.numHotRoles = numHotRoles;
        this.numHotDirectPolicies = numHotDirectPolicies;
        this.hotRoleCoverage = hotRoleCoverage;
        this.hotDirectPolicyCoverage = hotDirectPolicyCoverage;
        this.numPoliciesPerRole = numPoliciesPerRole;
        this.numStatementsPerPolicy = numStatementsPerPolicy;
        this.numActionsPerStatement = numActionsPerStatement;
        this.numResourcesPerStatement = numResourcesPerStatement;
        this.numServices = numServices;
        this.numActionsPerService = numActionsPerService;
        this.numHotActions = numHotActions;
        this.numHotResources = numHotResources;
        this.allowToDenyEffectRatio = allowToDenyEffectRatio;
        this.conditionProbability = conditionProbability;
        this.numOperationsPerCondition = numOperationsPerCondition;
        this.numVariablesPerEvaluation = numVariablesPerEvaluation;
        this.wildcardPatternProbability = wildcardPatternProbability;
        this.numResources = numResources;
        this.numResourceTypes = numResourceTypes;
        this.numNodesPerResource = numNodesPerResource;
    }

    public DummyModelGenerator generate() {
        this.principals.clear();
        this.resources.clear();
        this.compiledResources.clear();
        this.actions.clear();
        this.variables.clear();
        this.hotActionPool.clear();
        this.hotResourcePool.clear();

        this.principalObjects = null;
        this.queryPools = null;

        generateServiceDirectory();
        generateResources();
        generateActions();
        generateVariables();
        generatePrincipals();

        return this;
    }

    public ServiceDirectory serviceDirectory() {
        return serviceDirectory;
    }

    public List<IPrincipal> principalObjects() {
        return principalObjects;
    }

    public List<UUID> principals() {
        return principals;
    }

    public List<String> resources() {
        return resources;
    }

    public List<CompiledResourcePath> compiledResources() {
        return compiledResources;
    }

    public List<String> actions() {
        return actions;
    }

    public List<String> variables() {
        return variables;
    }

    /**
     * Returns pre-computed query pools for use in evaluation benchmarks.
     * Each pool contains (principal, action, resource) triples with a known
     * evaluation outcome.
     */
    public QueryPools queryPools() {
        if (queryPools == null)
            queryPools = computeQueryPools();

        return queryPools;
    }

    protected void generateServiceDirectory() {
        ServiceDirectory.Builder builder = new ServiceDirectory.Builder();

        for (int service = 0; service < numServices; service++) {
            String serviceName = "service" + service;
            builder.addServicePrincipal(serviceName);

            for (int action = 0; action < numActionsPerService; action++) {
                String actionName = serviceName + ":" + action;
                builder.addAction(actionName);
                this.actions.add(actionName);
            }
        }

        this.serviceDirectory = builder.build();
    }

    protected void generateResources() {
        int totalTypes = numResourceTypes;
        List<String> typeNames = new ArrayList<>(totalTypes);
        for (int t = 0; t < totalTypes; t++)
            typeNames.add("type" + t);

        for (int i = 0; i < numResources; i++) {
            StringBuilder sb = new StringBuilder("service" + (i % numServices) + "://");
            for (int j = 0; j < numNodesPerResource; j++) {
                sb.append(typeNames.get(i % totalTypes));
                sb.append(':');
                sb.append(new UUID(i, j));
                sb.append('/');
            }

            String resourcePath = sb.toString();
            this.resources.add(resourcePath);
            this.compiledResources.add(ResourcePathCompiler.compileResource(resourcePath));
        }

        // Build hot resource pool
        int hotCount = Math.min(numHotResources, resources.size());
        for (int i = 0; i < hotCount; i++)
            hotResourcePool.add(resources.get(i));
    }

    protected void generateActions() {
        // Build hot action pool
        int hotCount = Math.min(numHotActions, actions.size());
        for (int i = 0; i < hotCount; i++)
            hotActionPool.add(actions.get(i));
    }

    protected void generateVariables() {
        for (int i = 0; i < numVariablesPerEvaluation; i++)
            variables.add("var" + i);
    }

    protected void generatePrincipals() {
        // Create hot roles (shared across many principals)
        List<Role> hotRoles = new ArrayList<>();
        for (int i = 0; i < Math.min(numHotRoles, numRolesPerPrincipal * numPrincipals); i++) {
            Role role = new Role(UUID.randomUUID());
            for (int p = 0; p < numPoliciesPerRole; p++)
                role.addPolicy(new RolePolicyAssignment(role, generatePolicy()));
            hotRoles.add(role);
        }

        // Create hot direct policies
        List<Policy> hotDirectPolicies = new ArrayList<>();
        for (int i = 0; i < numHotDirectPolicies; i++)
            hotDirectPolicies.add(generatePolicy());

        this.principalObjects = new ArrayList<>(numPrincipals);

        for (int i = 0; i < numPrincipals; i++) {
            UUID principalId = UUID.randomUUID();
            principals.add(principalId);
            Principal principal = new Principal(principalId);

            // Assign hot roles based on coverage
            int hotAssigned = 0;
            for (Role hotRole : hotRoles) {
                if (random.nextDouble() < hotRoleCoverage) {
                    principal.addRole(new PrincipalRoleAssignment(principal, hotRole));
                    hotAssigned++;

                    if (hotAssigned >= numRolesPerPrincipal)
                        break;
                }
            }

            // Fill remaining role slots with unique roles
            for (int r = hotAssigned; r < numRolesPerPrincipal; r++) {
                Role uniqueRole = new Role(UUID.randomUUID());
                for (int p = 0; p < numPoliciesPerRole; p++)
                    uniqueRole.addPolicy(new RolePolicyAssignment(uniqueRole, generatePolicy()));

                principal.addRole(new PrincipalRoleAssignment(principal, uniqueRole));
            }

            // Assign hot direct policies
            int directAssigned = 0;
            for (Policy hotPolicy : hotDirectPolicies) {
                if (random.nextDouble() < hotDirectPolicyCoverage) {
                    principal.addPolicy(new PrincipalPolicyAssignment(principal, hotPolicy));
                    directAssigned++;

                    if (directAssigned >= numDirectPoliciesPerPrincipal)
                        break;
                }
            }

            // Fill remaining direct policy slots with unique policies
            for (int d = directAssigned; d < numDirectPoliciesPerPrincipal; d++)
                principal.addPolicy(new PrincipalPolicyAssignment(principal, generatePolicy()));

            principalObjects.add(principal);
        }
    }

    protected Policy generatePolicy() {
        Policy policy = new Policy(UUID.randomUUID());
        int numHotPerStatement = Math.min(numActionsPerStatement, hotActionPool.size());

        for (int s = 0; s < numStatementsPerPolicy; s++) {
            PolicyEffect effect = (random.nextInt(allowToDenyEffectRatio + 1) > 0)
                    ? PolicyEffect.ALLOW : PolicyEffect.DENY;

            Set<String> statementActions = new HashSet<>();

            // Mix hot and cold actions
            int hotInThisStatement = Math.min(numHotPerStatement, numActionsPerStatement);
            for (int a = 0; a < hotInThisStatement; a++)
                statementActions.add(hotActionPool.get(random.nextInt(hotActionPool.size())));

            while (statementActions.size() < numActionsPerStatement) {
                String coldAction = actions.get(random.nextInt(actions.size()));
                if (random.nextDouble() < wildcardPatternProbability)
                    coldAction = toWildcardPattern(coldAction);

                statementActions.add(coldAction);
            }

            Set<CompiledResourcePath> statementResources = new HashSet<>();
            int hotResourcesInStatement = Math.min(numResourcesPerStatement, hotResourcePool.size());
            for (int r = 0; r < hotResourcesInStatement; r++) {
                int idx = !hotResourcePool.isEmpty()
                        ? random.nextInt(hotResourcePool.size())
                        : random.nextInt(resources.size());

                statementResources.add(compiledResources.get(idx));
            }

            while (statementResources.size() < numResourcesPerStatement)
                statementResources.add(compiledResources.get(random.nextInt(compiledResources.size())));

            JsonLogicNode condition = null;
            if (random.nextDouble() < conditionProbability && numOperationsPerCondition > 0)
                condition = nextCondition();

            policy.addStatement(new PolicyStatement(policy, s, effect, statementActions,
                    statementResources, condition));
        }

        return policy;
    }

    protected String toWildcardPattern(String action) {
        int colon = action.indexOf(':');
        if (colon < 0)
            return action + ":*";

        String service = action.substring(0, colon);
        String key = action.substring(colon + 1);

        double r = random.nextDouble();
        if (r < 0.33)
            return service + ":*";
        else if (r < 0.66)
            return service + ":" + key.substring(0, Math.max(1, key.length() / 2)) + "*";

        return service + ":*" + key.substring(Math.min(key.length(), key.length() / 2));
    }

    protected JsonLogicNode nextCondition() {
        if (numOperationsPerCondition <= 0)
            return null;
        else if (numOperationsPerCondition == 1)
            return nextOperation();

        List<JsonLogicNode> ops = new ArrayList<>(numOperationsPerCondition);
        for (int i = 0; i < numOperationsPerCondition; i++)
            ops.add(nextOperation());

        JsonLogicArray args = new JsonLogicArray(ops);
        return new JsonLogicOperation(random.nextBoolean() ? "and" : "or", args);
    }

    protected JsonLogicNode nextOperation() {
        String op = OPERATIONS[random.nextInt(OPERATIONS.length)];
        String variable = "var" + random.nextInt(numVariablesPerEvaluation);
        boolean first = random.nextBoolean();
        int value = random.nextInt(100);

        JsonLogicNode variableKey = new JsonLogicString(variable);
        JsonLogicNode variableNode = new JsonLogicVariable(variableKey, new JsonLogicNumber(random.nextInt(100)));
        JsonLogicNode valueNode = new JsonLogicNumber(value);

        final JsonLogicArray args = new JsonLogicArray(
                first ? Arrays.asList(variableNode, valueNode) : Arrays.asList(valueNode, variableNode)
        );

        return new JsonLogicOperation(op, args);
    }

    protected QueryPools computeQueryPools() {
        List<QueryPools.Query> allowQueries = new ArrayList<>();
        List<QueryPools.Query> explicitDenyQueries = new ArrayList<>();
        List<QueryPools.Query> defaultDenyQueries = new ArrayList<>();

        if (principals.isEmpty() || actions.isEmpty() || resources.isEmpty())
            return new QueryPools(allowQueries, explicitDenyQueries, defaultDenyQueries);

        // We walk a subset of principals (max 1000) to keep setup time reasonable.
        int principalSampleSize = Math.min(principals.size(), 1000);
        int actionSampleSize = Math.min(actions.size(), 200);
        int resourceSampleSize = Math.min(resources.size(), 200);

        List<IPrincipal> sampledPrincipals = principalObjects.subList(0, principalSampleSize);

        for (IPrincipal principal : sampledPrincipals) {
            // Compile a snapshot for just this one principal
            ModelSnapshotCompiler compiler = new ModelSnapshotCompiler(serviceDirectory, List.of(principal))
                    .compile();

            EvaluationSnapshot snapshot = compiler.snapshot();

            // Walk the snapshot's permission table to find actions that are pure-allow
            // (no deny statements), pure-deny, or mixed, along with their resources.
            var permissionSets = snapshot.permissionsTable().permissions(
                    snapshot.principalDirectory().principalId(principal.id()));

            List<QueryWithEffect> principalQueries = new ArrayList<>();
            for (int psi = permissionSets.start(); psi < permissionSets.end(); psi++) {
                PermissionSetSnapshot ps = permissionSets.array()[psi];

                // Scan all possible action IDs (the array is sparse but indexed by actionId)
                int maxActionId = numServices * numActionsPerService;
                for (int actionId = 0; actionId < maxActionId; actionId++) {
                    PermissionSnapshot perm = ps.permission(actionId);
                    if (perm == null)
                        continue;

                    boolean hasAllow = perm.allowStatements().length > 0;
                    boolean hasDeny = perm.denyStatements().length > 0;

                    final int aid = actionId;
                    String action = serviceDirectory.actions().stream()
                            .filter(a -> serviceDirectory.actionId(a) == aid)
                            .findFirst().orElse(null);

                    if (action == null)
                        continue;

                    // Collect resources from both allow and deny statements
                    List<CompiledResourcePath> actionResources = new ArrayList<>();
                    for (var stmt : perm.allowStatements())
                        Collections.addAll(actionResources, stmt.resources());
                    for (var stmt : perm.denyStatements())
                        Collections.addAll(actionResources, stmt.resources());

                    if (hasAllow && !hasDeny) {
                        // Pure allow — add as allow query with each resource
                        for (var r : actionResources)
                            principalQueries.add(new QueryWithEffect(
                                    new QueryPools.Query(principal.id(), action, r), true));
                    } else if (hasDeny && !hasAllow) {
                        // Pure deny — add as explicit deny
                        for (var r : actionResources)
                            principalQueries.add(new QueryWithEffect(
                                    new QueryPools.Query(principal.id(), action, r), false));
                    }
                }
            }

            // Sample from the collected queries, distributing across all three pools
            int sampleCount = actionSampleSize * resourceSampleSize;
            int defaultDenyCount = sampleCount / 3;
            int allowDenyCount = sampleCount - defaultDenyCount;

            for (int i = 0; i < allowDenyCount && !principalQueries.isEmpty(); i++) {
                var q = principalQueries.get(random.nextInt(principalQueries.size()));
                if (q.isAllow)
                    allowQueries.add(q.query);
                else explicitDenyQueries.add(q.query);
            }

            // Add default-deny queries: pick random actions not in this principal's policies
            Set<String> principalActionSet = new HashSet<>();
            for (var q : principalQueries)
                principalActionSet.add(q.query.action());

            List<String> otherActions = new ArrayList<>();
            for (String a : actions)
                if (!principalActionSet.contains(a) && a.indexOf('*') == -1)
                    otherActions.add(a);

            for (int i = 0; i < defaultDenyCount && !otherActions.isEmpty(); i++) {
                String action = otherActions.get(random.nextInt(otherActions.size()));
                CompiledResourcePath resourcePath = compiledResources.get(random.nextInt(compiledResources.size()));
                defaultDenyQueries.add(new QueryPools.Query(principal.id(), action, resourcePath));
            }
        }

        return new QueryPools(allowQueries, explicitDenyQueries, defaultDenyQueries);
    }

    /** Internal pairing of a query with its expected effect. */
    private record QueryWithEffect(QueryPools.Query query, boolean isAllow) {}

    // ---- inner model classes ---------------------------------------------

    static class Principal implements IPrincipal {
        private final UUID id;
        private final Set<IPrincipalRoleAssignment> roles = new LinkedHashSet<>();
        private final Set<IPrincipalPolicyAssignment> policies = new LinkedHashSet<>();

        Principal(UUID id) { this.id = id; }

        @Override public UUID id() { return id; }
        @Override public Set<IPrincipalRoleAssignment> roles() { return roles; }
        @Override public Set<IPrincipalPolicyAssignment> policies() { return policies; }

        Principal addRole(IPrincipalRoleAssignment role) { roles.add(role); return this; }
        Principal addPolicy(IPrincipalPolicyAssignment policy) { policies.add(policy); return this; }
    }

    static class Role implements IRole {
        private final UUID id;
        private final Set<IRolePolicyAssignment> policies = new LinkedHashSet<>();

        Role(UUID id) { this.id = id; }

        @Override public UUID id() { return id; }
        @Override public Set<IRolePolicyAssignment> policies() { return policies; }

        Role addPolicy(IRolePolicyAssignment policy) { policies.add(policy); return this; }
    }

    static class Policy implements IPolicy {
        private final UUID id;
        private final Set<IPolicyStatement> statements = new LinkedHashSet<>();

        Policy(UUID id) { this.id = id; }

        @Override public UUID id() { return id; }
        @Override public Set<IPolicyStatement> statements() { return statements; }

        Policy addStatement(IPolicyStatement statement) { statements.add(statement); return this; }
    }

    record RolePolicyAssignment(IRole role, IPolicy policy) implements IRolePolicyAssignment {}

    record PrincipalRoleAssignment(IPrincipal principal, IRole role) implements IPrincipalRoleAssignment {
        @Override public Instant effective() { return Instant.EPOCH; }
        @Override public Instant expiration() { return Instant.MAX; }
    }

    record PrincipalPolicyAssignment(IPrincipal principal, IPolicy policy) implements IPrincipalPolicyAssignment {}

    static class PolicyStatement implements IPolicyStatement {
        private final IPolicy policy;
        private final int id;
        private final PolicyEffect effect;
        private final Set<String> actions;
        private final Set<CompiledResourcePath> resources;
        private final JsonLogicNode condition;

        PolicyStatement(IPolicy policy, int id, PolicyEffect effect, Set<String> actions,
                        Set<CompiledResourcePath> resources, JsonLogicNode condition) {
            this.policy = policy;
            this.id = id;
            this.effect = effect;
            this.actions = actions;
            this.resources = resources;
            this.condition = condition;
        }

        @Override public IPolicy policy() { return policy; }
        @Override public int id() { return id; }
        @Override public PolicyEffect effect() { return effect; }
        @Override public Set<String> actions() { return actions; }
        @Override public Set<CompiledResourcePath> resources() { return resources; }
        @Override public JsonLogicNode condition() { return condition; }
    }

    // ---- query pools -----------------------------------------------------

    /**
     * Pre-computed query pools for controlled-hit-rate benchmarks.
     */
    public static class QueryPools {
        private final List<Query> allow;
        private final List<Query> explicitDeny;
        private final List<Query> defaultDeny;

        QueryPools(List<Query> allow, List<Query> explicitDeny, List<Query> defaultDeny) {
            this.allow = allow;
            this.explicitDeny = explicitDeny;
            this.defaultDeny = defaultDeny;
        }

        public List<Query> allow() { return allow; }
        public List<Query> explicitDeny() { return explicitDeny; }
        public List<Query> defaultDeny() { return defaultDeny; }

        /** A single (principal, action, resource) query for evaluation. */
        public record Query(UUID principal, String action, CompiledResourcePath resource) {}
    }
}
