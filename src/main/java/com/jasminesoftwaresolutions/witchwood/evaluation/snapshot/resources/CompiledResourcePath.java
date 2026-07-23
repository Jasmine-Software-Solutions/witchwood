package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    // ---- Builder ----------------------------------------------------------

    /**
     * Create a fluent builder for constructing a resource path.
     *
     * <pre>{@code
     * CompiledResourcePath path = CompiledResourcePath.builder()
     *     .service("onboarding.jasmine.software")
     *     .one("candidate", UUID.randomUUID())
     *     .any()          // non-greedy single wildcard
     *     .any(true)      // greedy wildcard
     *     .any("bucket")  // wildcard-id resource node
     *     .build();
     * }</pre>
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String service;
        private final List<NodeDescriptor> descriptors = new ArrayList<>();

        private Builder() {}

        /** Set the service name. Required before {@link #build()}. */
        public Builder service(String service) {
            this.service = Objects.requireNonNull(service);
            return this;
        }

        /**
         * Add a definite resource node: {@code type:id}.
         */
        public Builder one(String type, UUID id) {
            descriptors.add(new NodeDescriptor(
                    Objects.requireNonNull(type),
                    new DefiniteId(Objects.requireNonNull(id)),
                    NodeKind.DEFINITE));
            return this;
        }

        /**
         * Add a non-greedy wildcard node ({@code *}). Matches exactly one
         * resource node of any type with any ID.
         */
        public Builder any() {
            descriptors.add(new NodeDescriptor(null, null, NodeKind.WILDCARD));
            return this;
        }

        /**
         * Add a wildcard node. When {@code greedy} is true, it matches zero
         * or more resource nodes ({@code **}); when false, it matches
         * exactly one ({@code *}).
         */
        public Builder any(boolean greedy) {
            descriptors.add(new NodeDescriptor(null, null,
                    greedy ? NodeKind.GREEDY_WILDCARD : NodeKind.WILDCARD));
            return this;
        }

        /**
         * Add a wildcard-ID resource node: {@code type:*}. Matches any
         * resource of the given type regardless of its ID.
         */
        public Builder any(String type) {
            descriptors.add(new NodeDescriptor(
                    Objects.requireNonNull(type),
                    new WildcardId(),
                    NodeKind.WILDCARD_ID));
            return this;
        }

        /**
         * Build the {@link CompiledResourcePath}. The node chain is
         * assembled from the accumulated descriptors in left-to-right
         * order: the first call to {@code one()} or {@code any()} becomes
         * the root, and subsequent calls become its children.
         *
         * @throws IllegalStateException if {@link #service(String)} was
         *         never called
         */
        public CompiledResourcePath build() {
            if (service == null)
                throw new IllegalStateException("service must be set before build()");

            Node root = null;
            // Build in reverse: the last descriptor is the deepest node
            // (child == null), and each predecessor wraps it.
            for (int i = descriptors.size() - 1; i >= 0; i--) {
                NodeDescriptor d = descriptors.get(i);
                root = switch (d.kind) {
                    case DEFINITE, WILDCARD_ID ->
                            new ResourceNode(d.type, d.resourceId, root);
                    case WILDCARD ->
                            new WildcardNode(root, false);
                    case GREEDY_WILDCARD ->
                            new WildcardNode(root, true);
                };
            }

            return new CompiledResourcePath(service, root);
        }

        private enum NodeKind { DEFINITE, WILDCARD_ID, WILDCARD, GREEDY_WILDCARD }

        private record NodeDescriptor(String type, ResourceId resourceId, NodeKind kind) {}
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
        final String start = service + SERVICE_SEPARATOR;
        if (root == null)
            return start;

        final StringBuilder sb = new StringBuilder(start);

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
