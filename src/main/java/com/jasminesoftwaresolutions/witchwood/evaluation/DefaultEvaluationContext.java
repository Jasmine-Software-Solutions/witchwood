package com.jasminesoftwaresolutions.witchwood.evaluation;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DefaultEvaluationContext {
    private DefaultEvaluationContext() {}

    public static final String PRINCIPAL_ID = "principal.id";
    public static final String ACTION_NAME = "action";

    public static final String RESOURCE_PATH_SERVICE = "resource.path.service";
    public static final String RESOURCE_PATH_NODES = "resource.path.nodes";

    public static void write(final UUID principalId, final String action, final CompiledResourcePath compiledResource, final Map<String, Object> context) {
        if (!context.containsKey(PRINCIPAL_ID))
            context.put(PRINCIPAL_ID, principalId);

        if (!context.containsKey(ACTION_NAME))
            context.put(ACTION_NAME, action);

        if (!context.containsKey(RESOURCE_PATH_SERVICE))
            context.put(RESOURCE_PATH_SERVICE, compiledResource.service());

        if (!context.containsKey(RESOURCE_PATH_NODES)) {
            List<String> nodes = new ArrayList<>();

            CompiledResourcePath.Node node = compiledResource.root();
            while (node != null) {
                nodes.add(node.toString());
                node = node.child();
            }

            context.put(RESOURCE_PATH_NODES, nodes);
        }
    }
}
