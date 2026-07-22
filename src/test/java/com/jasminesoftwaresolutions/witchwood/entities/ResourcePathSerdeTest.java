package com.jasminesoftwaresolutions.witchwood.entities;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import com.jasminesoftwaresolutions.witchwood.persistence.ResourcePathSerde;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourcePathSerdeTest {

    @Test
    void roundTripSimplePath() {
        var path = new CompiledResourcePath("iam",
                new CompiledResourcePath.ResourceNode("account",
                        new CompiledResourcePath.DefiniteId(UUID.randomUUID()),
                        new CompiledResourcePath.ResourceNode("user",
                                new CompiledResourcePath.DefiniteId(UUID.randomUUID()),
                                null)));

        var proto = ResourcePathSerde.toProto(path);
        var roundTripped = ResourcePathSerde.fromProto(proto);

        assertEquals(path, roundTripped);
    }

    @Test
    void roundTripWildcardNode() {
        var path = new CompiledResourcePath("storage",
                new CompiledResourcePath.WildcardNode(
                        new CompiledResourcePath.ResourceNode("bucket",
                                new CompiledResourcePath.DefiniteId(UUID.randomUUID()),
                                null),
                        false));

        var proto = ResourcePathSerde.toProto(path);
        var roundTripped = ResourcePathSerde.fromProto(proto);

        assertEquals(path, roundTripped);
    }

    @Test
    void roundTripGreedyWildcard() {
        var path = new CompiledResourcePath("svc",
                new CompiledResourcePath.WildcardNode(null, true));

        var proto = ResourcePathSerde.toProto(path);
        var roundTripped = ResourcePathSerde.fromProto(proto);

        assertEquals(path, roundTripped);
    }

    @Test
    void roundTripWildcardId() {
        var path = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("res",
                        new CompiledResourcePath.WildcardId(),
                        null));

        var proto = ResourcePathSerde.toProto(path);
        var roundTripped = ResourcePathSerde.fromProto(proto);

        assertEquals(path, roundTripped);
    }

    @Test
    void roundTripNoRoot() {
        var path = new CompiledResourcePath("svc", null);

        var proto = ResourcePathSerde.toProto(path);
        var roundTripped = ResourcePathSerde.fromProto(proto);

        assertEquals(path, roundTripped);
    }

    @Test
    void roundTripViaBytes() {
        var path = new CompiledResourcePath("iam",
                new CompiledResourcePath.ResourceNode("account",
                        new CompiledResourcePath.DefiniteId(UUID.randomUUID()),
                        null));

        byte[] bytes = ResourcePathSerde.toProto(path).toByteArray();
        var roundTripped = ResourcePathSerde.fromProtoBytes(bytes);

        assertEquals(path, roundTripped);
    }
}
