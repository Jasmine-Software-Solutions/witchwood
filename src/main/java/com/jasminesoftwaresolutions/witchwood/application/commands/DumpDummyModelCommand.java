package com.jasminesoftwaresolutions.witchwood.application.commands;

import com.jasminesoftwaresolutions.witchwood.persistence.DummyModelDumper;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers.DummyModelGeneratorBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.sql.DriverManager;

@Command(
        name = "dump-dummy-model",
        description = "Generates a dummy model and inserts it into a PostgreSQL database",
        mixinStandardHelpOptions = true)
public class DumpDummyModelCommand implements Runnable {
    @Option(names = "--jdbc-url", required = true,
            description = "PostgreSQL JDBC URL")
    protected String jdbcUrl;

    @Option(names = "--jdbc-user", required = true)
    protected String jdbcUser;

    @Option(names = "--jdbc-password", required = true)
    protected String jdbcPassword;

    @Option(names = "--num-principals", defaultValue = "1000000")
    protected int numPrincipals;

    @Option(names = "--num-roles-per-principal", defaultValue = "5")
    protected int numRolesPerPrincipal;

    @Option(names = "--num-direct-policies-per-principal", defaultValue = "2")
    protected int numDirectPoliciesPerPrincipal;

    @Option(names = "--num-hot-roles", defaultValue = "100")
    protected int numHotRoles;

    @Option(names = "--num-hot-direct-policies", defaultValue = "30")
    protected int numHotDirectPolicies;

    @Option(names = "--hot-role-coverage", defaultValue = "0.7")
    protected double hotRoleCoverage;

    @Option(names = "--hot-direct-policy-coverage", defaultValue = "0.4")
    protected double hotDirectPolicyCoverage;

    @Option(names = "--num-policies-per-role", defaultValue = "3")
    protected int numPoliciesPerRole;

    @Option(names = "--num-statements-per-policy", defaultValue = "10")
    protected int numStatementsPerPolicy;

    @Option(names = "--num-actions-per-statement", defaultValue = "3")
    protected int numActionsPerStatement;

    @Option(names = "--num-resources-per-statement", defaultValue = "2")
    protected int numResourcesPerStatement;

    @Option(names = "--num-services", defaultValue = "100")
    protected int numServices;

    @Option(names = "--num-actions-per-service", defaultValue = "50")
    protected int numActionsPerService;

    @Option(names = "--num-hot-actions", defaultValue = "100")
    protected int numHotActions;

    @Option(names = "--num-hot-resources", defaultValue = "200")
    protected int numHotResources;

    @Option(names = "--allow-to-deny-effect-ratio", defaultValue = "9")
    protected int allowToDenyEffectRatio;

    @Option(names = "--condition-probability", defaultValue = "0.25")
    protected double conditionProbability;

    @Option(names = "--num-operations-per-condition", defaultValue = "2")
    protected int numOperationsPerCondition;

    @Option(names = "--num-variables-per-evaluation", defaultValue = "10")
    protected int numVariablesPerEvaluation;

    @Option(names = "--wildcard-pattern-probability", defaultValue = "0.1")
    protected double wildcardPatternProbability;

    @Option(names = "--num-resources", defaultValue = "100000")
    protected int numResources;

    @Option(names = "--num-resource-types", defaultValue = "100")
    protected int numResourceTypes;

    @Option(names = "--num-nodes-per-resource", defaultValue = "3")
    protected int numNodesPerResource;

    @Override
    public void run() {
        var generator = new DummyModelGeneratorBuilder()
                .setNumPrincipals(numPrincipals)
                .setNumRolesPerPrincipal(numRolesPerPrincipal)
                .setNumDirectPoliciesPerPrincipal(numDirectPoliciesPerPrincipal)
                .setNumHotRoles(numHotRoles)
                .setNumHotDirectPolicies(numHotDirectPolicies)
                .setHotRoleCoverage(hotRoleCoverage)
                .setHotDirectPolicyCoverage(hotDirectPolicyCoverage)
                .setNumPoliciesPerRole(numPoliciesPerRole)
                .setNumStatementsPerPolicy(numStatementsPerPolicy)
                .setNumActionsPerStatement(numActionsPerStatement)
                .setNumResourcesPerStatement(numResourcesPerStatement)
                .setNumServices(numServices)
                .setNumActionsPerService(numActionsPerService)
                .setNumHotActions(numHotActions)
                .setNumHotResources(numHotResources)
                .setAllowToDenyEffectRatio(allowToDenyEffectRatio)
                .setConditionProbability(conditionProbability)
                .setNumOperationsPerCondition(numOperationsPerCondition)
                .setNumVariablesPerEvaluation(numVariablesPerEvaluation)
                .setWildcardPatternProbability(wildcardPatternProbability)
                .setNumResources(numResources)
                .setNumResourceTypes(numResourceTypes)
                .setNumNodesPerResource(numNodesPerResource)
                .build()
                .generate();

        System.out.println("Generated " + numPrincipals + " principals.");
        System.out.println("Dumping to database...");

        long start = System.currentTimeMillis();

        try (var conn = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword)) {
            var dumper = new DummyModelDumper(conn);
            dumper.dump(generator);

            long elapsed = System.currentTimeMillis() - start;
            System.out.printf("Dumped in %,d ms (%,.0f principals/sec)%n",
                    elapsed, numPrincipals / (elapsed / 1000.0));
        } catch (Exception e) {
            throw new RuntimeException("Dump failed", e);
        }
    }
}
