package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources;

import java.util.UUID;

public class CompiledResourcePath {
    public static final String SERVICE_SEPARATOR = "://";
    public static final char RESOURCE_SEPARATOR = '/';
    public static final char RESOURCE_TYPE_ID_SEPARATOR = ':';

    public static final char WILDCARD = '*';

    private static final String WILDCARD_STRING = String.valueOf(WILDCARD);
    private static final String GREEDY_WILDCARD_STRING = WILDCARD_STRING + WILDCARD_STRING;

    protected final String service;
    protected final Node root;

    public CompiledResourcePath(String service, Node root) {
        this.service = service;
        this.root = root;
    }

    public String service() {
        return service;
    }

    public Node root() {
        return root;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompiledResourcePath that)) return false;
        if (!service.equals(that.service)) return false;
        return nodesEqual(root, that.root);
    }

    private static boolean nodesEqual(Node a, Node b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (!a.equals(b)) return false;
        return nodesEqual(a.child(), b.child());
    }

    @Override
    public int hashCode() {
        int result = service.hashCode();
        Node node = root;
        while (node != null) {
            result = 31 * result + node.hashCode();
            node = node.child();
        }
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(service + SERVICE_SEPARATOR);

        Node node = root;
        while (node != null) {
            sb.append(node);
            sb.append(RESOURCE_SEPARATOR);
            node = node.child();
        }

        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public sealed interface Node permits WildcardNode, ResourceNode {
        Node child();
    }

    public record WildcardNode(Node child, boolean greedy) implements Node {
        @Override
        public String toString() {
            return greedy ? GREEDY_WILDCARD_STRING : WILDCARD_STRING;
        }
    }

    public record ResourceNode(String type, ResourceId resourceId, Node child) implements Node {
        @Override
        public String toString() {
            return type + RESOURCE_TYPE_ID_SEPARATOR + resourceId.toString();
        }
    }

    public sealed interface ResourceId permits WildcardId, DefiniteId {}

    public record WildcardId() implements ResourceId {
        @Override
        public String toString() {
            return WILDCARD_STRING;
        }
    }

    public record DefiniteId(UUID id) implements ResourceId {
        @Override
        public String toString() {
            return id.toString();
        }
    }
}
