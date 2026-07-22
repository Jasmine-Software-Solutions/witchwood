package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.EvaluationSnapshot;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.PermissionSetSnapshot;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.PermissionSnapshot;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.PolicyStatementSnapshot;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.ServiceDirectory;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import com.jasminesoftwaresolutions.witchwood.models.*;

import java.util.*;

public class ModelSnapshotCompiler {
    protected ServiceDirectory serviceDirectory;
    protected final List<IPrincipal> principals;

    protected EvaluationSnapshot snapshot;

    /**
     * Cache for {@link #resolveActionIds(Set)}. Action pattern sets that differ only
     * in iteration order are common since {@code DummyModelGenerator} uses HashSet.
     * Using a {@link HashMap} with an explicit composite key avoids redundant
     * full-scans of the action directory during wildcard resolution.
     */
    protected final Map<Integer, Set<Integer>> actionResolutionCache = new HashMap<>();

    /**
     * Cache for PermissionSetSnapshots produced from a role's policies.
     * When multiple principals share the same {@link IRole} instance (hot roles),
     * the derived snapshot is identical — we build it once and reuse.
     * Keyed by {@link IRole#id()}.
     */
    protected final Map<UUID, PermissionSetSnapshot> rolePermissionSetCache = new HashMap<>();

    /**
     * Cache for PermissionSetSnapshots produced from a principal's directly-attached
     * policies. Keyed by a combined hash of the policy ID set.
     */
    protected final Map<Integer, PermissionSetSnapshot> directPolicyPermissionSetCache = new HashMap<>();

    public ModelSnapshotCompiler(ServiceDirectory serviceDirectory, List<IPrincipal> principals) {
        this.serviceDirectory = serviceDirectory;
        this.principals = principals;
        this.snapshot = null;
    }

    public EvaluationSnapshot snapshot() {
        return snapshot;
    }

    public ModelSnapshotCompiler compile() {
        EvaluationSnapshot.Builder snapshot = new EvaluationSnapshot.Builder();
        snapshot.serviceDirectory(serviceDirectory);
        this.serviceDirectory = snapshot.serviceDirectory().build();

        for (IPrincipal principal : principals) {
            int principalId = snapshot.principalDirectory().addPrincipal(principal.id());

            // Build permission sets: one per role, plus one for directly-attached policies
            List<PermissionSetSnapshot> permissionSets =
                    buildPermissionSets(principal);

            for (PermissionSetSnapshot permissionSet : permissionSets)
                snapshot.permissionsTable().addPermissions(principalId, permissionSet);
        }

        this.snapshot = snapshot.build();
        return this;
    }

    /**
     * Build a list of PermissionSetSnapshots for a principal: one per role,
     * plus optionally one for directly-attached policies.
     */
    protected List<PermissionSetSnapshot> buildPermissionSets(IPrincipal principal) {
        List<PermissionSetSnapshot> result = new ArrayList<>();

        // One PermissionSet per role — cached by role identity for shared (hot) roles
        for (IPrincipalRoleAssignment roleAssignment : principal.roles()) {
            IRole role = roleAssignment.role();
            PermissionSetSnapshot permissionSet = rolePermissionSetCache.computeIfAbsent(
                    role.id(),
                    k -> buildPermissionSetFromPolicies(role.policies()));
            result.add(permissionSet);
        }

        // One PermissionSet for directly-attached policies
        Set<IPrincipalPolicyAssignment> directPolicies = principal.policies();
        if (!directPolicies.isEmpty()) {
            // Use a composite key of all policy IDs for the combined permission set.
            // For the common case of a single direct policy (numDirectPoliciesPerPrincipal=1),
            // this degenerates to policy-level caching.
            int combinedKey = directPolicySetKey(directPolicies);
            PermissionSetSnapshot directSet = directPolicyPermissionSetCache.get(combinedKey);
            if (directSet == null) {
                directSet = buildPermissionSetFromPolicyAssignments(directPolicies);
                directPolicyPermissionSetCache.put(combinedKey, directSet);
            }
            result.add(directSet);
        }

        return result;
    }

    /**
     * Build a PermissionSet from a set of IRolePolicyAssignments.
     * Dereferences each assignment to its IPolicy and collects all statements.
     */
    protected PermissionSetSnapshot buildPermissionSetFromPolicies(Set<IRolePolicyAssignment> assignments) {
        return buildPermissionSetFromStatements(collectStatementsFromRolePolicies(assignments));
    }

    /**
     * Collect all statements from policies referenced by role policy assignments.
     */
    protected List<IPolicyStatement> collectStatementsFromRolePolicies(Set<IRolePolicyAssignment> assignments) {
        List<IPolicyStatement> statements = new ArrayList<>();
        for (IRolePolicyAssignment assignment : assignments)
            statements.addAll(assignment.policy().statements());

        return statements;
    }

    /**
     * Collect all statements from policies directly attached to a principal.
     */
    protected List<IPolicyStatement> collectStatementsFromPrincipalPolicies(Set<IPrincipalPolicyAssignment> assignments) {
        List<IPolicyStatement> statements = new ArrayList<>();
        for (IPrincipalPolicyAssignment assignment : assignments)
            statements.addAll(assignment.policy().statements());

        return statements;
    }

    /**
     * Build a PermissionSet from a set of IPrincipalPolicyAssignments
     * (directly-attached policies on the principal).
     */
    protected PermissionSetSnapshot buildPermissionSetFromPolicyAssignments(Set<IPrincipalPolicyAssignment> assignments) {
        return buildPermissionSetFromStatements(collectStatementsFromPrincipalPolicies(assignments));
    }

    /**
     * Stable hash combining all policy IDs in a set of policy assignments,
     * for use as a cache key for the combined PermissionSetSnapshot.
     */
    private static int directPolicySetKey(Set<IPrincipalPolicyAssignment> assignments) {
        int h = 0;
        for (IPrincipalPolicyAssignment a : assignments)
            h += a.policy().id().hashCode();
        return h;
    }

    /**
     * Build a PermissionSetSnapshot from a list of policy statements.
     * Statements are grouped by action (expanding wildcards), effect, and de-duplicated.
     *
     * <p>Snapshot construction for each statement is deferred until after dedup to avoid
     * creating garbage objects that would be immediately discarded.</p>
     */
    protected PermissionSetSnapshot buildPermissionSetFromStatements(List<IPolicyStatement> statements) {
        PermissionSetSnapshot.Builder permissionSet = new PermissionSetSnapshot.Builder();

        // Map of actionId -> ActionEntry (builder + dedup sets)
        Map<Integer, ActionEntry> actionEntries = new HashMap<>();

        for (IPolicyStatement statement : statements) {
            Set<Integer> resolvedActionIds = resolveActionIds(statement.actions());

            for (int actionId : resolvedActionIds) {
                ActionEntry entry = actionEntries.computeIfAbsent(actionId,
                        k -> new ActionEntry(new PermissionSnapshot.Builder()));

                addStatementIfNotDuplicate(entry, statement);
            }
        }

        // Build and add all PermissionSnapshots to the PermissionSet
        for (Map.Entry<Integer, ActionEntry> entry : actionEntries.entrySet())
            permissionSet.addPermission(entry.getKey(), entry.getValue().builder.build());

        return permissionSet.build();
    }

    /**
     * Holds a PermissionSnapshot.Builder and dedup sets for allow/deny statements.
     *
     * <p>The dedup sets use a lightweight {@link StatementFingerprint} key instead of
     * full {@link PolicyStatementSnapshot} objects to amortize hash/equality costs.
     * The {@link PolicyStatementSnapshot} is built only when a statement is actually added.</p>
     */
    protected static class ActionEntry {
        final PermissionSnapshot.Builder builder;
        final Set<StatementFingerprint> allowSet = new HashSet<>();
        final Set<StatementFingerprint> denySet = new HashSet<>();

        ActionEntry(PermissionSnapshot.Builder builder) {
            this.builder = builder;
        }
    }

    /**
     * Lightweight fingerprint for de-duplicating statements without allocating
     * a full {@link PolicyStatementSnapshot}.
     */
    protected record StatementFingerprint(int resourcesHash, int conditionHash) {}

    /**
     * Compute a stable hash for a statement's resources without allocating a
     * {@link PolicyStatementSnapshot} or performing deep path traversal.
     */
    private static int resourcesFingerprintHash(IPolicyStatement statement) {
        int h = 1;
        for (CompiledResourcePath r : statement.resources())
            h = 31 * h + r.hashCode();
        return h;
    }

    /**
     * Convert an IPolicyStatement into a PolicyStatementSnapshot.
     */
    protected PolicyStatementSnapshot buildStatementSnapshot(IPolicyStatement statement) {
        PolicyStatementSnapshot.Builder builder = new PolicyStatementSnapshot.Builder();
        for (var resource : statement.resources())
            builder.addResource(resource);

        builder.setCondition(statement.condition());
        return builder.build();
    }

    /**
     * Add a statement to the permission builder only if it is not already present
     * (de-duplicated by effect). Uses a lightweight fingerprint to avoid building
     * the full {@link PolicyStatementSnapshot} until the statement is confirmed new.
     */
    protected void addStatementIfNotDuplicate(ActionEntry entry, IPolicyStatement statement) {
        int resourcesHash = resourcesFingerprintHash(statement);
        int conditionHash = Objects.hashCode(statement.condition());
        StatementFingerprint fp = new StatementFingerprint(resourcesHash, conditionHash);

        if (statement.effect() == PolicyEffect.ALLOW) {
            if (entry.allowSet.add(fp))
                entry.builder.addAllowStatement(buildStatementSnapshot(statement));
        } else {
            if (entry.denySet.add(fp))
                entry.builder.addDenyStatement(buildStatementSnapshot(statement));
        }
    }

    /**
     * Resolve wildcard action patterns into concrete action IDs using the
     * source {@link ServiceDirectory} for lookup.
     *
     * <p>Results are cached keyed on a combined hash of the pattern set so that
     * repeated patterns (common in large dummy-generated models) avoid redundant
     * full-scans of the action directory.</p>
     *
     * <p>Valid patterns:</p>
     * <ul>
     *   <li>{@code "*"} — matches all actions across all services</li>
     *   <li>{@code "service:*"} — matches all actions under {@code service}</li>
     *   <li>{@code "service:prefix*"} — matches actions starting with {@code prefix}</li>
     *   <li>{@code "service:*suffix"} — matches actions ending with {@code suffix}</li>
     * </ul>
     */
    protected Set<Integer> resolveActionIds(Set<String> actionPatterns) {
        // Fast path: single exact action (no wildcards at all)
        if (actionPatterns.size() == 1) {
            String single = actionPatterns.iterator().next();
            if (single.indexOf('*') == -1 && single.indexOf(':') >= 0) {
                int id = serviceDirectory.actionId(single);
                if (id != -1)
                    return Collections.singleton(id);
            }
        }

        // Check cache using a combined hash of the pattern set
        int cacheKey = actionHash(actionPatterns);
        Set<Integer> cached = actionResolutionCache.get(cacheKey);
        if (cached != null)
            return cached;

        Set<Integer> result = resolveActionIdsUncached(actionPatterns);
        actionResolutionCache.put(cacheKey, result);
        return result;
    }

    /**
     * Stable hash over a set of action pattern strings (order-independent).
     */
    private static int actionHash(Set<String> patterns) {
        int h = 0;
        for (String p : patterns)
            h += p.hashCode();
        return h;
    }

    /**
     * Actual resolution logic (uncached).
     */
    private Set<Integer> resolveActionIdsUncached(Set<String> actionPatterns) {
        Set<Integer> result = new HashSet<>();

        Set<String> allActions = serviceDirectory.actions();

        for (String pattern : actionPatterns) {
            if ("*".equals(pattern)) {
                // bare "*" — match all actions of all services
                for (String action : allActions)
                    result.add(serviceDirectory.actionId(action));
                continue;
            }

            int colonIndex = pattern.indexOf(':');
            if (colonIndex == -1) {
                // Invalid pattern; skip
                continue;
            }

            String service = pattern.substring(0, colonIndex);
            String key = pattern.substring(colonIndex + 1);

            if ("*".equals(service) && "*".equals(key)) {
                // "*:*" — match all actions of all services
                for (String action : allActions)
                    result.add(serviceDirectory.actionId(action));
                continue;
            }

            if ("*".equals(key)) {
                // "service:*" — match all actions under this service
                String prefix = service + ":";
                for (String action : allActions) {
                    if (action.startsWith(prefix))
                        result.add(serviceDirectory.actionId(action));
                }
                continue;
            }

            // "service:prefix*" or "service:*suffix"
            boolean startsWild = key.startsWith("*");
            boolean endsWild = key.endsWith("*");

            String servicePrefix = service + ":";
            for (String action : allActions) {
                if (!action.startsWith(servicePrefix)) continue;

                String actionKey = action.substring(servicePrefix.length());

                boolean matches;
                if (startsWild && endsWild) {
                    String middle = key.substring(1, key.length() - 1);
                    matches = actionKey.contains(middle);
                } else if (startsWild) {
                    String suffix = key.substring(1);
                    matches = actionKey.endsWith(suffix);
                } else if (endsWild) {
                    String prefix = key.substring(0, key.length() - 1);
                    matches = actionKey.startsWith(prefix);
                } else {
                    matches = actionKey.equals(key);
                }

                if (matches)
                    result.add(serviceDirectory.actionId(action));
            }
        }

        return result;
    }
}
