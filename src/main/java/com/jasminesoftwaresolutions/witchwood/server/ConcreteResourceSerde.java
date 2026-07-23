package com.jasminesoftwaresolutions.witchwood.server;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Use ConcreteResourceSerde to drastically improve throughput
 * via the gRPC interface. ConcreteResourceSerde will precompile
 * known resource paths. As the resource server, you should
 * implement it like this:
 * <pre>
 * {@code
 *   public final class ServiceResources {
 *      private ServiceResources() {}
 * <p>
 *      public static final PrecompiledConcreteResource DOCUMENT
 *          = ConcreteResourceSerde.precompile(CompiledResourcePath.builder()
 *              .service("service.com")
 *              .any("document")
 *              .build());
 * </pre>
 *   }
 * }
 *
 * Then, you may invoke {@code ConcreteResourceSerde.serialize(DOCUMENT, documentId); }.
 * Serialization and deserialization is 11x faster with PrecompiledConcreteResource
 * versus Protobuf.
 */
public final class ConcreteResourceSerde {
    private ConcreteResourceSerde() {}

    public record PrecompiledConcreteResource(byte[] template, int[] offset) {}

    public static PrecompiledConcreteResource precompile(CompiledResourcePath pattern) {
        int nodeCount = 0;
        for (CompiledResourcePath.Node n = pattern.root(); n != null; n = n.child())
            nodeCount++;

        byte[] serviceBytes = pattern.service().getBytes(StandardCharsets.UTF_8);
        int totalSize = 2 + serviceBytes.length; // serviceLen + service + nodeCount

        CompiledResourcePath.Node[] nodes = new CompiledResourcePath.Node[nodeCount];
        int idx = 0;
        for (CompiledResourcePath.Node n = pattern.root(); n != null; n = n.child()) {
            CompiledResourcePath.ResourceNode r = (CompiledResourcePath.ResourceNode) n;
            totalSize += 1 + r.type().length() + 16; // typeLen + type + UUID
            nodes[idx++] = r;
        }

        byte[] template = new byte[totalSize];
        int pos = 0;

        // Service
        template[pos++] = (byte) serviceBytes.length;
        System.arraycopy(serviceBytes, 0, template, pos, serviceBytes.length);
        pos += serviceBytes.length;

        // Node count
        template[pos++] = (byte) nodeCount;

        // Nodes in reverse order (deepest first) matching serialize()
        int[] offset = new int[nodeCount];
        for (int i = nodeCount - 1; i >= 0; i--) {
            CompiledResourcePath.ResourceNode r = (CompiledResourcePath.ResourceNode) nodes[i];
            byte[] typeBytes = r.type().getBytes(StandardCharsets.UTF_8);
            template[pos++] = (byte) typeBytes.length;
            System.arraycopy(typeBytes, 0, template, pos, typeBytes.length);
            pos += typeBytes.length;

            offset[i] = pos;
            pos += 16;
        }

        return new PrecompiledConcreteResource(template, offset);
    }

    public static byte[] serialize(CompiledResourcePath resource) {
        int nodeCount = 0;
        for (CompiledResourcePath.Node n = resource.root(); n != null; n = n.child())
            nodeCount++;

        byte[] serviceBytes = resource.service().getBytes(StandardCharsets.UTF_8);
        int totalSize = 2 + serviceBytes.length; // serviceLen + service + nodeCount
        CompiledResourcePath.Node[] nodes = new CompiledResourcePath.Node[nodeCount];
        int idx = 0;
        for (CompiledResourcePath.Node n = resource.root(); n != null; n = n.child()) {
            CompiledResourcePath.ResourceNode r = (CompiledResourcePath.ResourceNode) n;
            totalSize += 1 + r.type().length() + 16; // typeLen + type + UUID
            nodes[idx++] = r;
        }

        byte[] data = new byte[totalSize];
        int pos = 0;

        // Service
        data[pos++] = (byte) serviceBytes.length;
        System.arraycopy(serviceBytes, 0, data, pos, serviceBytes.length);
        pos += serviceBytes.length;

        // Node count
        data[pos++] = (byte) nodeCount;

        for (int i = nodeCount - 1; i >= 0; i--) {
            CompiledResourcePath.ResourceNode r = (CompiledResourcePath.ResourceNode) nodes[i];
            CompiledResourcePath.DefiniteId rId = (CompiledResourcePath.DefiniteId) r.resourceId();

            byte[] typeBytes = r.type().getBytes(StandardCharsets.UTF_8);
            data[pos++] = (byte) typeBytes.length;
            System.arraycopy(typeBytes, 0, data, pos, typeBytes.length);
            pos += typeBytes.length;

            putUuid(data, pos, rId.id());
            pos += 16;
        }

        return data;
    }

    public static byte[] serialize(PrecompiledConcreteResource resource, UUID... ids) {
        byte[] data = new byte[resource.template.length];
        System.arraycopy(resource.template, 0, data, 0, resource.template.length);
        for (int i = 0; i < ids.length; i++) {
            int offset = resource.offset[i];
            putUuid(data, offset, ids[i]);
        }

        return data;
    }

    public static CompiledResourcePath deserialize(byte[] data) {
        int pos = 0;

        int serviceLen = data[pos++] & 0xFF;
        String service = new String(data, pos, serviceLen, StandardCharsets.UTF_8);
        pos += serviceLen;

        int nodeCount = data[pos++] & 0xFF;

        CompiledResourcePath.Node root = null;
        for (int i = 0; i < nodeCount; i++) {
            int typeLen = data[pos++] & 0xFF;
            String type = new String(data, pos, typeLen, StandardCharsets.UTF_8);
            pos += typeLen;

            UUID id = getUuid(data, pos);
            pos += 16;

            root = new CompiledResourcePath.ResourceNode(
                    type,
                    new CompiledResourcePath.DefiniteId(id),
                    root);
        }

        return new CompiledResourcePath(service, root);
    }

    private static void putUuid(byte[] buf, int off, UUID id) {
        long msb = id.getMostSignificantBits();
        long lsb = id.getLeastSignificantBits();

        for (int i = 0; i < 8; i++)
            buf[off + i] = (byte) (msb >>> (56 - 8 * i));

        for (int i = 0; i < 8; i++)
            buf[off + 8 + i] = (byte) (lsb >>> (56 - 8 * i));
    }

    private static UUID getUuid(byte[] buf, int off) {
        long msb = ((long) (buf[off] & 0xFF) << 56);
        for (int i = 1; i < 8; i++)
            msb |= ((long) (buf[off + i] & 0xFF) << (56 - 8 * i));

        long lsb = ((long) (buf[off + 8] & 0xFF) << 56);
        for (int i = 1; i < 8; i++)
            lsb |= ((long) (buf[off + 8 + i] & 0xFF) << (56 - 8 * i));

        return new UUID(msb, lsb);
    }
}
