package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources;

import java.util.UUID;

import static com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath.*;

public class ResourcePathCompiler {
    public static CompiledResourcePath compilePattern(String path) {
        return compile(path, true);
    }

    public static CompiledResourcePath compileResource(String path) {
        return compile(path, false);
    }

    private static CompiledResourcePath compile(String path, boolean pattern) {
        int separator = path.indexOf(SERVICE_SEPARATOR);

        if (separator <= 0)
            throw new IllegalArgumentException("Invalid resource path: " + path);

        String service = path.substring(0, separator);
        String resourcePath = path.substring(separator + 3);

        if (resourcePath.isEmpty())
            return new CompiledResourcePath(service, null);

        CompiledResourcePath.Node child = null;
        int end = resourcePath.length();
        if (resourcePath.charAt(end - 1) == RESOURCE_SEPARATOR)
            end = end - 1;

        while (end > 0) {
            int start = resourcePath.lastIndexOf(RESOURCE_SEPARATOR, end - 1);
            child = compileSegment(resourcePath, start + 1, end, child, pattern);

            if (start < 0)
                break;

            end = start;
        }

        return new CompiledResourcePath(service, child);
    }

    private static CompiledResourcePath.Node compileSegment(
            String path,
            int start,
            int end,
            CompiledResourcePath.Node child,
            boolean pattern) {
        int length = end - start;

        if (length == 1 && path.charAt(start) == WILDCARD) {
            requirePattern(pattern, "*");

            return new CompiledResourcePath.WildcardNode(child, false);
        }

        if (length == 2 &&
                path.charAt(start) == WILDCARD &&
                path.charAt(start + 1) == WILDCARD) {
            requirePattern(pattern, "**");

            return new CompiledResourcePath.WildcardNode(child, true);
        }

        int separator = path.indexOf(RESOURCE_TYPE_ID_SEPARATOR, start);
        if (separator <= start || separator >= end)
            throw new IllegalArgumentException(
                    "Invalid resource segment: " + path.substring(start, end));

        String type = path.substring(start, separator);
        int idStart = separator + 1;
        CompiledResourcePath.ResourceId resourceId;

        if (end - idStart == 1 && path.charAt(idStart) == WILDCARD) {
            requirePattern(pattern, "*");
            resourceId = new CompiledResourcePath.WildcardId();
        } else {
            try {
                resourceId = new CompiledResourcePath.DefiniteId(
                        UUID.fromString(path.substring(idStart, end))
                );
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid UUID in segment: " + path.substring(start, end));
            }
        }

        return new CompiledResourcePath.ResourceNode(type, resourceId, child);
    }

    private static void requirePattern(boolean pattern, String segment) {
        if (pattern) return;

        throw new IllegalArgumentException("Wildcard not allowed in resource: " + segment);
    }
}
