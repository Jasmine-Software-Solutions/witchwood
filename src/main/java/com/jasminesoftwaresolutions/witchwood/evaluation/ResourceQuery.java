package com.jasminesoftwaresolutions.witchwood.evaluation;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.*;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.ResourcePathMatcher;
import com.jasminesoftwaresolutions.witchwood.util.ArrayView;

import java.util.*;

/**
 * Queries the evaluation snapshot for resources accessible by a given principal
 * for a specific action.
 *
 * <p>Unlike the point-evaluation in {@link EvaluationEngine}, which answers
 * "can principal P perform action A on resource R?", this class answers
 * "what resources can principal P access for action A?" by collecting the
 * resource patterns from all applicable ALLOW statements, after accounting
 * for DENY statements that shadow them.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * ResourceQuery query = new ResourceQuery(engine);
 * Set<CompiledResourcePath> resources = query.query(principalUuid, "id.jasmine.software:ListOrganizations");
 * }</pre>
 */
public class ResourceQuery {
    private final EvaluationEngine engine;

    public ResourceQuery(EvaluationEngine engine) {
        this.engine = engine;
    }

    /**
     * Query the current snapshot for all resource patterns that a principal
     * is allowed to access for the given action.
     *
     * <p>Each returned pattern is a {@link CompiledResourcePath} as stored
     * in the policy statement (e.g. {@code "*://tenant:<uuid>"} or
     * {@code "*://tenant:<uuid>/**"}). Patterns are deduplicated via their
     * {@link CompiledResourcePath#equals(Object)} implementation.</p>
     *
     * @param principal the UUID of the principal
     * @param action    the full action string (e.g. {@code "id.jasmine.software:ListOrganizations"})
     * @return a set of {@link CompiledResourcePath} patterns that grant ALLOW
     *         for the action, after removing any patterns shadowed by DENY
     *         statements. Empty set if the principal or action is unknown,
     *         or if no ALLOW patterns survive.
     */
    public Set<CompiledResourcePath> query(UUID principal, String action) {
        EvaluationSnapshot snapshot = engine.snapshot();

        int principalId = snapshot.principalDirectory().principalId(principal);
        if (principalId == -1)
            return Set.of();

        int actionId = snapshot.serviceDirectory().actionId(action);
        if (actionId == -1)
            return Set.of();

        ArrayView<PermissionSetSnapshot> permissionSets =
                snapshot.permissionsTable().permissions(principalId);

        // Phase 1: collect candidate ALLOW resource patterns and
        //          DENY resource patterns for this action across all
        //          permission sets.
        Set<CompiledResourcePath> allowPatterns = new LinkedHashSet<>();
        Set<CompiledResourcePath> denyPatterns = new LinkedHashSet<>();

        for (int i = permissionSets.start(); i < permissionSets.end(); i++) {
            PermissionSetSnapshot permissionSet = permissionSets.array()[i];
            PermissionSnapshot permissions = permissionSet.permission(actionId);
            if (permissions == null) continue;

            for (PolicyStatementSnapshot stmt : permissions.denyStatements()) {
                for (CompiledResourcePath r : stmt.resources())
                    denyPatterns.add(r);
            }

            for (PolicyStatementSnapshot stmt : permissions.allowStatements()) {
                for (CompiledResourcePath r : stmt.resources())
                    allowPatterns.add(r);
            }
        }

        // Phase 3: remove ALLOW patterns that are entirely shadowed by a
        //          DENY pattern. An ALLOW pattern is shadowed if a DENY
        //          pattern matches it (treating the ALLOW pattern as a
        //          concrete resource). This means every concrete resource
        //          that satisfies the ALLOW pattern also satisfies the
        //          DENY pattern.
        Set<CompiledResourcePath> result = new LinkedHashSet<>();
        for (CompiledResourcePath allow : allowPatterns) {
            boolean shadowed = false;
            for (CompiledResourcePath deny : denyPatterns) {
                if (ResourcePathMatcher.matches(deny, allow)) {
                    shadowed = true;
                    break;
                }
            }
            if (!shadowed)
                result.add(allow);
        }

        return result;
    }
}
