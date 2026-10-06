package com.jasminesoftwaresolutions.witchwood.evaluation;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.EvaluationSnapshot;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.PermissionSetSnapshot;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.PermissionSnapshot;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.PolicyStatementSnapshot;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.ResourcePathCompiler;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.ResourcePathMatcher;
import com.jasminesoftwaresolutions.witchwood.util.ArrayView;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class EvaluationEngine {
    protected final Map<String, CompiledResourcePath> compiledResourcePaths
            = new ConcurrentHashMap<>();

    protected final AtomicReference<EvaluationSnapshot> snapshot;
    protected final JsonLogicEvaluator evaluator;

    public EvaluationEngine(final EvaluationSnapshot snapshot, final JsonLogicEvaluator evaluator) {
        this.snapshot = new AtomicReference<>(snapshot);
        this.evaluator = evaluator;
    }

    public void snapshot(final EvaluationSnapshot snapshot) {
        this.snapshot.set(snapshot);
    }

    public EvaluationSnapshot snapshot() {
        return this.snapshot.get();
    }

    public boolean evaluate(final UUID principal, final String action, final String resource, final Map<String, Object> context) {
        CompiledResourcePath resourcePath = compiledResourcePaths.computeIfAbsent(
                resource, ResourcePathCompiler::compileResource);

        return evaluate(principal, action, resourcePath, context);
    }

    public boolean evaluate(final UUID principal, final String action, final CompiledResourcePath compiledResource, final Map<String, Object> context) {
        System.out.println("Evaluating ");
        System.out.println("Principal: " + principal);
        System.out.println("Action: " + action);
        System.out.println("Resource: " + compiledResource);
        final EvaluationSnapshot snapshot = this.snapshot.get();

        final int principalId = snapshot.principalDirectory().principalId(principal);
        if (principalId == -1)
            return false;

        System.out.println("A");

        int actionId = snapshot.serviceDirectory().actionId(action);
        if (actionId == -1)
            return false;

        System.out.println("B");

        DefaultEvaluationContext.write(principal, action, compiledResource, context);
        return evaluate(snapshot, principalId, actionId, compiledResource, context);
    }

    protected boolean evaluate(final EvaluationSnapshot snapshot, final int principalId, final int actionId, final CompiledResourcePath compiledResource, final Map<String, Object> context) {
        final ArrayView<PermissionSetSnapshot> permissionSets = snapshot.permissionsTable().permissions(principalId);
        System.out.println("PrincipalId: " + principalId + ", ActionId: " + actionId + ", PermissionSets: " + (permissionSets.end() - permissionSets.start()));

        for (int i = permissionSets.start(); i < permissionSets.end(); i++) {
            final PermissionSetSnapshot permissionSet = permissionSets.array()[i];
            final PermissionSnapshot permissions = permissionSet.permission(actionId);
            System.out.println("  PS[" + i + "]: permission for actionId=" + actionId + " = " + (permissions != null));
            if (permissions != null) {
                System.out.println("    allowStmt count=" + permissions.allowStatements().length + ", denyStmt count=" + permissions.denyStatements().length);
            }
            if (permissions == null) continue;

            PolicyStatementSnapshot[] deny = permissions.denyStatements();
            for (PolicyStatementSnapshot statement : deny) {
                final boolean applies = applies(statement, compiledResource, context);
                if (applies) return false;
            }
        }

        for (int i = permissionSets.start(); i < permissionSets.end(); i++) {
            final PermissionSetSnapshot permissionSet = permissionSets.array()[i];
            final PermissionSnapshot permissions = permissionSet.permission(actionId);
            if (permissions == null) continue;

            PolicyStatementSnapshot[] allow = permissions.allowStatements();
            for (PolicyStatementSnapshot statement : allow) {
                final boolean applies = applies(statement, compiledResource, context);
                if (applies) return true;
            }
        }

        return false;
    }

    protected boolean applies(final PolicyStatementSnapshot statement, final CompiledResourcePath compiledResource, final Map<String, Object> context) {
        boolean matches = false;
        for (CompiledResourcePath pattern : statement.resources()) {
            matches = ResourcePathMatcher.matches(pattern, compiledResource);
            System.out.println("    resource match: pattern=" + pattern + " resource=" + compiledResource + " matches=" + matches);
            if (matches) break;
        }

        if (!matches) {
            System.out.println("    -> no resource match, returning false");
            return false;
        }

        JsonLogicNode condition = statement.condition();
        if (condition == null)
            return true;

        boolean conditionEvaluationResult = false;
        try {
            Object val = evaluator.evaluate(condition, context, "$");
            if (val instanceof Boolean)
                conditionEvaluationResult = (Boolean) val;
        } catch (JsonLogicEvaluationException e) {}

        return conditionEvaluationResult;
    }
}
