package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers;

public class DummySnapshotCompilerBuilder {
    private int numPrincipals = 100_000;
    private int numResources = 10_000;
    private int numPermissionSets = 1_000;
    private int numPermissionSetsPerPrincipal = 5;
    private int numServices = 5;
    private int numActionsPerService = 20;
    private int numStatementsPerPermissionSet = 30;
    private int numActionsPerStatement = 5;
    private int numResourcePatternsPerStatement = 2;
    private int numOperationsPerCondition = 2;
    private int numVariablesPerEvaluation = 10;
    private int numNodesPerResourcePattern = 3;
    private int numResourceTypesPerService = 6;
    private int statementToConditionRatio = 4;
    private int allowToDenyEffectRatio = 9;

    public DummySnapshotCompilerBuilder setNumPrincipals(int numPrincipals) {
        this.numPrincipals = numPrincipals;
        return this;
    }

    public DummySnapshotCompilerBuilder setNumResources(int numResources) {
        this.numResources = numResources;
        return this;
    }

    public DummySnapshotCompilerBuilder setNumPermissionSets(int numPermissionSets) {
        this.numPermissionSets = numPermissionSets;
        return this;
    }

    public DummySnapshotCompilerBuilder setNumPermissionSetsPerPrincipal(int numPermissionSetsPerPrincipal) {
        this.numPermissionSetsPerPrincipal = numPermissionSetsPerPrincipal;
        return this;
    }

    public DummySnapshotCompilerBuilder setNumServices(int numServices) {
        this.numServices = numServices;
        return this;
    }

    public DummySnapshotCompilerBuilder setNumActionsPerService(int numActionsPerService) {
        this.numActionsPerService = numActionsPerService;
        return this;
    }

    public DummySnapshotCompilerBuilder setNumStatementsPerPermissionSet(int numStatementsPerPermissionSet) {
        this.numStatementsPerPermissionSet = numStatementsPerPermissionSet;
        return this;
    }

    public DummySnapshotCompilerBuilder setNumActionsPerStatement(int numActionsPerStatement) {
        this.numActionsPerStatement = numActionsPerStatement;
        return this;
    }

    public DummySnapshotCompilerBuilder setNumResourcePatternsPerStatement(int numResourcePatternsPerStatement) {
        this.numResourcePatternsPerStatement = numResourcePatternsPerStatement;
        return this;
    }

    public DummySnapshotCompilerBuilder setNumOperationsPerCondition(int numOperationsPerCondition) {
        this.numOperationsPerCondition = numOperationsPerCondition;
        return this;
    }

    public DummySnapshotCompilerBuilder setNumVariablesPerEvaluation(int numVariablesPerEvaluation) {
        this.numVariablesPerEvaluation = numVariablesPerEvaluation;
        return this;
    }

    public DummySnapshotCompilerBuilder setNumNodesPerResourcePattern(int numNodesPerResourcePattern) {
        this.numNodesPerResourcePattern = numNodesPerResourcePattern;
        return this;
    }

    public DummySnapshotCompilerBuilder setNumResourceTypesPerService(int numResourceTypesPerService) {
        this.numResourceTypesPerService = numResourceTypesPerService;
        return this;
    }

    public DummySnapshotCompilerBuilder setStatementToConditionRatio(int statementToConditionRatio) {
        this.statementToConditionRatio = statementToConditionRatio;
        return this;
    }

    public DummySnapshotCompilerBuilder setAllowToDenyEffectRatio(int allowToDenyEffectRatio) {
        this.allowToDenyEffectRatio = allowToDenyEffectRatio;
        return this;
    }

    public DummySnapshotCompiler build() {
        return new DummySnapshotCompiler(
                numPrincipals,
                numResources,
                numPermissionSets,
                numPermissionSetsPerPrincipal,
                numServices,
                numActionsPerService,
                numStatementsPerPermissionSet,
                numActionsPerStatement,
                numResourcePatternsPerStatement,
                numOperationsPerCondition,
                numVariablesPerEvaluation,
                numNodesPerResourcePattern,
                numResourceTypesPerService,
                statementToConditionRatio,
                allowToDenyEffectRatio);
    }
}