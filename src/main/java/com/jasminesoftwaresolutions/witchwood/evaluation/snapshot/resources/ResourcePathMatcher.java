package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources;

import static com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath.*;

public class ResourcePathMatcher {
    public static boolean matches(CompiledResourcePath pattern, CompiledResourcePath resource) {
        if (!pattern.matchesAnyService()
                && !pattern.service().equals(resource.service()))
            return false;

        return matchesNode(pattern.root(), resource.root());
    }

    private static boolean matchesNode(
            CompiledResourcePath.Node pattern,
            CompiledResourcePath.Node resource
    ) {
        // Both ended
        if (pattern == null && resource == null)
            return true;

        // Pattern ended but resource still expects nodes
        if (pattern == null)
            return false;

        // Resource ended but pattern still expects nodes
        if (resource == null)
            return false;

        if (pattern instanceof WildcardNode(Node child, boolean greedy)) {
            if (!greedy) {
                // '*' consumes exactly one resource node
                return matchesNode(
                        child,
                        resource.child()
                );
            }

            // '**' consumes one or more resource nodes.
            // Try consuming the current node, then recurse.
            CompiledResourcePath.Node current = resource;

            while (current != null) {
                if (matchesNode(child, current.child()))
                    return true;

                current = current.child();
            }

            return false;
        }

        if (!(pattern instanceof CompiledResourcePath.ResourceNode expected))
            return false;

        if (!(resource instanceof CompiledResourcePath.ResourceNode actual))
            return false;

        if (!expected.type().equals(actual.type()))
            return false;

        if (!matchesId(expected.resourceId(), actual.resourceId()))
            return false;

        return matchesNode(expected.child(), actual.child());
    }

    private static boolean matchesId(
            CompiledResourcePath.ResourceId pattern,
            CompiledResourcePath.ResourceId resource
    ) {
        if (pattern instanceof CompiledResourcePath.WildcardId)
            return true;

        CompiledResourcePath.DefiniteId expected =
                (CompiledResourcePath.DefiniteId) pattern;

        CompiledResourcePath.DefiniteId actual =
                (CompiledResourcePath.DefiniteId) resource;

        return expected.id().equals(actual.id());
    }
}
