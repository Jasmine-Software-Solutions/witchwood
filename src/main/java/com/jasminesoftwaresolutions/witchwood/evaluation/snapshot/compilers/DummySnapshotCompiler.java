package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.EvaluationSnapshot;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.PermissionSetSnapshot;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.PermissionSnapshot;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.PolicyStatementSnapshot;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.ResourcePathCompiler;
import io.github.jamsesso.jsonlogic.ast.*;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.*;

public class DummySnapshotCompiler {
    protected static final String[] OPERATIONS = new String[] {
            "==", "!=", ">", "<", "+", "-"
    };

    protected final int numPrincipals;
    protected final int numResources;
    protected final int numPermissionSets;
    protected final int numPermissionSetsPerPrincipal;
    protected final int numServices;
    protected final int numActionsPerService;
    protected final int numStatementsPerPermissionSet;
    protected final int numActionsPerStatement;
    protected final int numResourcePatternsPerStatement;
    protected final int numOperationsPerCondition;
    protected final int numVariablesPerEvaluation;
    protected final int numNodesPerResourcePattern;
    protected final int numResourceTypesPerService;
    protected final int statementToConditionRatio;
    protected final int allowToDenyEffectRatio;

    protected final Random random = new Random();

    protected final List<UUID> principals = new ArrayList<>();
    protected final List<String> resources = new ArrayList<>();
    protected final List<CompiledResourcePath> compiledResources = new ArrayList<>();
    protected final List<String> actions = new ArrayList<>();
    protected final List<String> variables = new ArrayList<>();

    protected EvaluationSnapshot snapshot = null;

    public DummySnapshotCompiler(int numPrincipals, int numResources, int numPermissionSets, int numPermissionSetsPerPrincipal, int numServices, int numActionsPerService, int numStatementsPerPermissionSet, int numActionsPerStatement, int numResourcePatternsPerStatement, int numOperationsPerCondition, int numVariablesPerEvaluation, int numNodesPerResourcePattern, int numResourceTypesPerService, int statementToConditionRatio, int allowToDenyEffectRatio) {
        this.numPrincipals = numPrincipals;
        this.numResources = numResources;
        this.numPermissionSets = numPermissionSets;
        this.numPermissionSetsPerPrincipal = numPermissionSetsPerPrincipal;
        this.numServices = numServices;
        this.numActionsPerService = numActionsPerService;
        this.numStatementsPerPermissionSet = numStatementsPerPermissionSet;
        this.numActionsPerStatement = numActionsPerStatement;
        this.numResourcePatternsPerStatement = numResourcePatternsPerStatement;
        this.numOperationsPerCondition = numOperationsPerCondition;
        this.numVariablesPerEvaluation = numVariablesPerEvaluation;
        this.numNodesPerResourcePattern = numNodesPerResourcePattern;
        this.numResourceTypesPerService = numResourceTypesPerService;
        this.statementToConditionRatio = statementToConditionRatio;
        this.allowToDenyEffectRatio = allowToDenyEffectRatio;
    }

    public DummySnapshotCompiler compile() {
        this.principals.clear();
        this.resources.clear();
        this.compiledResources.clear();
        this.actions.clear();

        createSnapshot();
        return this;
    }

    public EvaluationSnapshot snapshot() {
        return snapshot;
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

    protected void createSnapshot() {
        EvaluationSnapshot.Builder snapshot = new EvaluationSnapshot.Builder();

        for (int i = 0; i < numPrincipals; i++) {
            UUID principal = UUID.randomUUID();

            snapshot.principalDirectory()
                    .addPrincipal(principal);

            this.principals.add(principal);
        }

        for (int service = 0; service < numServices; service++) {
            String serviceName = "service" + service;

            snapshot.serviceDirectory()
                    .addServicePrincipal(
                            serviceName,
                            UUID.randomUUID()
                    );

            for (int action = 0; action < numActionsPerService; action++) {
                String actionName = serviceName + ":" + action;

                snapshot.serviceDirectory()
                        .addAction(actionName);

                this.actions.add(actionName);
            }
        }

        final int numResourceTypes = numServices * numResourceTypesPerService;
        List<String> resourceTypes = new ArrayList<>(numResourceTypes);
        for (int type = 0; type < numResourceTypes; type++)
            resourceTypes.add("type" + type);

        List<String> resourcePool = new ArrayList<>(numResources);

        for (int i = 0; i < numResources; i++) {
            StringBuilder resourcePathBuilder = new StringBuilder("service" + (i % numServices) + "://");
            for (int j = 0; j < numNodesPerResourcePattern; j++) {
                resourcePathBuilder.append(resourceTypes.get(i % numResourceTypes));
                resourcePathBuilder.append(':');
                resourcePathBuilder.append(new UUID(i, j));
                resourcePathBuilder.append('/');
            }

            String resourcePath = resourcePathBuilder.toString();

            resourcePool.add(resourcePath);
            this.resources.add(resourcePath);
            this.compiledResources.add(ResourcePathCompiler.compileResource(resourcePath));
        }

        for (int i = 0; i < numVariablesPerEvaluation; i++)
            this.variables.add("var" + i);

        List<PermissionSetSnapshot> permissionSets =
                new ArrayList<>(numPermissionSets);

        int statementId = 0;
        for (int i = 0; i < numPermissionSets; i++) {
            PermissionSetSnapshot.Builder permissionSet =
                    new PermissionSetSnapshot.Builder();

            Set<Integer> usedActions = new HashSet<>();
            while (usedActions.size() < numActionsPerStatement) {
                usedActions.add(random.nextInt(
                        numServices * numActionsPerService
                ));
            }

            for (int actionId : usedActions) {
                PermissionSnapshot.Builder permission =
                        new PermissionSnapshot.Builder();

                int numStatements = numStatementsPerPermissionSet * numActionsPerStatement / (numServices * numActionsPerService);
                for (int s = 0; s < numStatements; s++) {
                    PolicyStatementSnapshot.Builder statement =
                            new PolicyStatementSnapshot.Builder();
                    statementId++;

                    boolean allow = statementId % allowToDenyEffectRatio > 0;

                    for (int r = 0; r < numResourcePatternsPerStatement; r++) {
                        CompiledResourcePath resource =
                                this.compiledResources.get(random.nextInt(this.compiledResources.size()));

                        boolean[] wildcard = new boolean[numNodesPerResourcePattern];
                        for (int w = 0; w < wildcard.length; w++)
                            wildcard[w] = random.nextDouble() < (1.0 / numNodesPerResourcePattern);

                        CompiledResourcePath updatedPath = new CompiledResourcePath(
                                resource.service(),
                                createPattern(resource.root(), wildcard, 0)
                        );

                        statement.addResource(updatedPath);

                        boolean hasCondition = statementId % statementToConditionRatio == 0;
                        if (hasCondition)
                            statement.setCondition(nextCondition());
                    }

                    if (allow)
                        permission.addAllowStatement(statement.build());
                    else permission.addDenyStatement(statement.build());
                }

                permissionSet.addPermission(actionId, permission.build());
            }

            permissionSets.add(permissionSet.build());
        }

        for (int principalId = 0; principalId < numPrincipals; principalId++) {
            IntSet assigned = new IntOpenHashSet();
            while (assigned.size() < numPermissionSetsPerPrincipal)
                assigned.add(random.nextInt(permissionSets.size()));

            for (int permissionSetId : assigned) {
                snapshot.permissionsTable()
                        .addPermissions(principalId, permissionSets.get(permissionSetId));
            }
        }

        this.snapshot = snapshot.build();
    }

    private static CompiledResourcePath.Node createPattern(
        CompiledResourcePath.Node node,
        boolean[] wildcard,
        int depth
    ) {
        if (node == null)
            return null;

        CompiledResourcePath.Node child =
                createPattern(node.child(), wildcard, depth + 1);

        if (wildcard[depth])
            return new CompiledResourcePath.WildcardNode(child, false);

        CompiledResourcePath.ResourceNode resource =
                (CompiledResourcePath.ResourceNode) node;

        return new CompiledResourcePath.ResourceNode(
                resource.type(),
                resource.resourceId(),
                child
        );
    }

    private JsonLogicNode nextCondition() {
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

    private JsonLogicNode nextOperation() {
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
}
