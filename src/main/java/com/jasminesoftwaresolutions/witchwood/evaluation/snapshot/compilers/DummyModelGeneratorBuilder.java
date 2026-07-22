package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers;

public class DummyModelGeneratorBuilder {
    private int numPrincipals = 100_000;
    private int numRolesPerPrincipal = 3;
    private int numDirectPoliciesPerPrincipal = 1;
    private int numHotRoles = 10;
    private int numHotDirectPolicies = 5;
    private double hotRoleCoverage = 0.7;
    private double hotDirectPolicyCoverage = 0.5;
    private int numPoliciesPerRole = 3;
    private int numStatementsPerPolicy = 10;
    private int numActionsPerStatement = 3;
    private int numResourcesPerStatement = 2;
    private int numServices = 5;
    private int numActionsPerService = 20;
    private int numHotActions = 30;
    private int numHotResources = 50;
    private int allowToDenyEffectRatio = 9;
    private double conditionProbability = 0.25;
    private int numOperationsPerCondition = 2;
    private int numVariablesPerEvaluation = 10;
    private double wildcardPatternProbability = 0.1;
    private int numResources = 10_000;
    private int numResourceTypes = 30;
    private int numNodesPerResource = 3;

    public DummyModelGeneratorBuilder setNumPrincipals(int numPrincipals) {
        this.numPrincipals = numPrincipals;
        return this;
    }

    public DummyModelGeneratorBuilder setNumRolesPerPrincipal(int numRolesPerPrincipal) {
        this.numRolesPerPrincipal = numRolesPerPrincipal;
        return this;
    }

    public DummyModelGeneratorBuilder setNumDirectPoliciesPerPrincipal(int numDirectPoliciesPerPrincipal) {
        this.numDirectPoliciesPerPrincipal = numDirectPoliciesPerPrincipal;
        return this;
    }

    public DummyModelGeneratorBuilder setNumHotRoles(int numHotRoles) {
        this.numHotRoles = numHotRoles;
        return this;
    }

    public DummyModelGeneratorBuilder setNumHotDirectPolicies(int numHotDirectPolicies) {
        this.numHotDirectPolicies = numHotDirectPolicies;
        return this;
    }

    public DummyModelGeneratorBuilder setHotRoleCoverage(double hotRoleCoverage) {
        this.hotRoleCoverage = hotRoleCoverage;
        return this;
    }

    public DummyModelGeneratorBuilder setHotDirectPolicyCoverage(double hotDirectPolicyCoverage) {
        this.hotDirectPolicyCoverage = hotDirectPolicyCoverage;
        return this;
    }

    public DummyModelGeneratorBuilder setNumPoliciesPerRole(int numPoliciesPerRole) {
        this.numPoliciesPerRole = numPoliciesPerRole;
        return this;
    }

    public DummyModelGeneratorBuilder setNumStatementsPerPolicy(int numStatementsPerPolicy) {
        this.numStatementsPerPolicy = numStatementsPerPolicy;
        return this;
    }

    public DummyModelGeneratorBuilder setNumActionsPerStatement(int numActionsPerStatement) {
        this.numActionsPerStatement = numActionsPerStatement;
        return this;
    }

    public DummyModelGeneratorBuilder setNumResourcesPerStatement(int numResourcesPerStatement) {
        this.numResourcesPerStatement = numResourcesPerStatement;
        return this;
    }

    public DummyModelGeneratorBuilder setNumServices(int numServices) {
        this.numServices = numServices;
        return this;
    }

    public DummyModelGeneratorBuilder setNumActionsPerService(int numActionsPerService) {
        this.numActionsPerService = numActionsPerService;
        return this;
    }

    public DummyModelGeneratorBuilder setNumHotActions(int numHotActions) {
        this.numHotActions = numHotActions;
        return this;
    }

    public DummyModelGeneratorBuilder setNumHotResources(int numHotResources) {
        this.numHotResources = numHotResources;
        return this;
    }

    public DummyModelGeneratorBuilder setAllowToDenyEffectRatio(int allowToDenyEffectRatio) {
        this.allowToDenyEffectRatio = allowToDenyEffectRatio;
        return this;
    }

    public DummyModelGeneratorBuilder setConditionProbability(double conditionProbability) {
        this.conditionProbability = conditionProbability;
        return this;
    }

    public DummyModelGeneratorBuilder setNumOperationsPerCondition(int numOperationsPerCondition) {
        this.numOperationsPerCondition = numOperationsPerCondition;
        return this;
    }

    public DummyModelGeneratorBuilder setNumVariablesPerEvaluation(int numVariablesPerEvaluation) {
        this.numVariablesPerEvaluation = numVariablesPerEvaluation;
        return this;
    }

    public DummyModelGeneratorBuilder setWildcardPatternProbability(double wildcardPatternProbability) {
        this.wildcardPatternProbability = wildcardPatternProbability;
        return this;
    }

    public DummyModelGeneratorBuilder setNumResources(int numResources) {
        this.numResources = numResources;
        return this;
    }

    public DummyModelGeneratorBuilder setNumResourceTypes(int numResourceTypes) {
        this.numResourceTypes = numResourceTypes;
        return this;
    }

    public DummyModelGeneratorBuilder setNumNodesPerResource(int numNodesPerResource) {
        this.numNodesPerResource = numNodesPerResource;
        return this;
    }

    public DummyModelGenerator build() {
        return new DummyModelGenerator(
                numPrincipals,
                numRolesPerPrincipal,
                numDirectPoliciesPerPrincipal,
                numHotRoles,
                numHotDirectPolicies,
                hotRoleCoverage,
                hotDirectPolicyCoverage,
                numPoliciesPerRole,
                numStatementsPerPolicy,
                numActionsPerStatement,
                numResourcesPerStatement,
                numServices,
                numActionsPerService,
                numHotActions,
                numHotResources,
                allowToDenyEffectRatio,
                conditionProbability,
                numOperationsPerCondition,
                numVariablesPerEvaluation,
                wildcardPatternProbability,
                numResources,
                numResourceTypes,
                numNodesPerResource
        );
    }
}
