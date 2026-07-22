package com.jasminesoftwaresolutions.witchwood.persistence;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import com.jasminesoftwaresolutions.witchwood.proto.ResourceNode;
import com.jasminesoftwaresolutions.witchwood.proto.TypedResourceNode;
import com.jasminesoftwaresolutions.witchwood.proto.WildcardNode;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Bidirectional conversion between {@link CompiledResourcePath} and
 * its protobuf representation.
 */
public final class ResourcePathSerde {
    private ResourcePathSerde() {}

    /**
     * Serialize a {@link CompiledResourcePath} to its protobuf form.
     */
    public static com.jasminesoftwaresolutions.witchwood.proto.CompiledResourcePath toProto(
            CompiledResourcePath path) {

        var builder = com.jasminesoftwaresolutions.witchwood.proto.CompiledResourcePath.newBuilder()
                .setService(path.service());

        if (path.root() != null)
            builder.setRoot(toProtoNode(path.root()));

        return builder.build();
    }

    private static ResourceNode toProtoNode(CompiledResourcePath.Node node) {
        ResourceNode.Builder builder = ResourceNode.newBuilder();

        switch (node) {
            case CompiledResourcePath.WildcardNode w -> {
                WildcardNode.Builder wb = WildcardNode.newBuilder()
                        .setGreedy(w.greedy());
                if (w.child() != null)
                    wb.setChild(toProtoNode(w.child()));
                builder.setWildcard(wb);
            }
            case CompiledResourcePath.ResourceNode r -> {
                TypedResourceNode.Builder tb = TypedResourceNode.newBuilder()
                        .setType(r.type());
                switch (r.resourceId()) {
                    case CompiledResourcePath.DefiniteId(var id) -> {
                        ByteBuffer buf = ByteBuffer.allocate(16);
                        buf.putLong(id.getMostSignificantBits());
                        buf.putLong(id.getLeastSignificantBits());
                        tb.setDefiniteId(com.google.protobuf.ByteString.copyFrom(buf.array()));
                    }
                    case CompiledResourcePath.WildcardId __ ->
                            tb.setWildcardId(true);
                }
                if (r.child() != null)
                    tb.setChild(toProtoNode(r.child()));
                builder.setTyped(tb);
            }
        }

        return builder.build();
    }

    /**
     * Deserialize a protobuf message back to a {@link CompiledResourcePath}.
     */
    public static CompiledResourcePath fromProto(
            com.jasminesoftwaresolutions.witchwood.proto.CompiledResourcePath proto) {

        CompiledResourcePath.Node root = proto.hasRoot()
                ? fromProtoNode(proto.getRoot())
                : null;

        return new CompiledResourcePath(proto.getService(), root);
    }

    /**
     * Deserialize raw protobuf bytes to a {@link CompiledResourcePath}.
     */
    public static CompiledResourcePath fromProtoBytes(byte[] data) {
        try {
            return fromProto(
                    com.jasminesoftwaresolutions.witchwood.proto.CompiledResourcePath.parseFrom(data));
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("Failed to deserialize CompiledResourcePath", e);
        }
    }

    private static CompiledResourcePath.Node fromProtoNode(ResourceNode proto) {
        return switch (proto.getNodeCase()) {
            case WILDCARD -> {
                WildcardNode w = proto.getWildcard();
                CompiledResourcePath.Node child = w.hasChild()
                        ? fromProtoNode(w.getChild())
                        : null;
                yield new CompiledResourcePath.WildcardNode(child, w.getGreedy());
            }
            case TYPED -> {
                TypedResourceNode t = proto.getTyped();
                CompiledResourcePath.ResourceId resourceId = switch (t.getResourceIdCase()) {
                    case DEFINITE_ID -> {
                        ByteBuffer buf = ByteBuffer.wrap(t.getDefiniteId().toByteArray());
                        yield new CompiledResourcePath.DefiniteId(
                                new UUID(buf.getLong(), buf.getLong()));
                    }
                    case WILDCARD_ID -> new CompiledResourcePath.WildcardId();
                    case RESOURCEID_NOT_SET ->
                            throw new IllegalArgumentException("ResourceNode missing resource_id");
                };
                CompiledResourcePath.Node child = t.hasChild()
                        ? fromProtoNode(t.getChild())
                        : null;
                yield new CompiledResourcePath.ResourceNode(t.getType(), resourceId, child);
            }
            case NODE_NOT_SET ->
                    throw new IllegalArgumentException("ResourceNode has no node case set");
        };
    }
}
