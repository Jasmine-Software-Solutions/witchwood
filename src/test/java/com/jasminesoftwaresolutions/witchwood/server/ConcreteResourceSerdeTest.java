package com.jasminesoftwaresolutions.witchwood.server;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConcreteResourceSerdeTest {

    @Test
    void roundTripSingleNode() {
        UUID id = UUID.randomUUID();
        var path = CompiledResourcePath.builder()
                .service("iam")
                .one("account", id)
                .build();

        byte[] data = ConcreteResourceSerde.serialize(path);
        var result = ConcreteResourceSerde.deserialize(data);

        assertEquals(path, result);
    }

    @Test
    void roundTripMultiNode() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        var path = CompiledResourcePath.builder()
                .service("storage")
                .one("region", a)
                .one("bucket", b)
                .one("object", c)
                .build();

        byte[] data = ConcreteResourceSerde.serialize(path);
        var result = ConcreteResourceSerde.deserialize(data);

        assertEquals(path, result);
    }

    @Test
    void roundTripServiceOnly() {
        var path = CompiledResourcePath.builder()
                .service("svc")
                .build();

        byte[] data = ConcreteResourceSerde.serialize(path);
        var result = ConcreteResourceSerde.deserialize(data);

        assertEquals(path, result);
    }

    @Test
    void precompiledMatchesDirectSerialize() {
        UUID id = UUID.randomUUID();
        var path = CompiledResourcePath.builder()
                .service("svc")
                .one("res", id)
                .build();

        var precompiled = ConcreteResourceSerde.precompile(path);

        byte[] direct = ConcreteResourceSerde.serialize(path);
        byte[] viaPrecompiled = ConcreteResourceSerde.serialize(precompiled, id);

        var directResult = ConcreteResourceSerde.deserialize(direct);
        var precompiledResult = ConcreteResourceSerde.deserialize(viaPrecompiled);

        assertEquals(path, directResult);
        assertEquals(path, precompiledResult);
    }

    @Test
    void precompiledWithDifferentUuids() {
        UUID placeholder = UUID.randomUUID();
        var template = CompiledResourcePath.builder()
                .service("iam")
                .one("account", placeholder)
                .one("user", placeholder)
                .build();

        var precompiled = ConcreteResourceSerde.precompile(template);

        UUID realA = UUID.randomUUID();
        UUID realB = UUID.randomUUID();
        var expected = CompiledResourcePath.builder()
                .service("iam")
                .one("account", realA)
                .one("user", realB)
                .build();

        byte[] data = ConcreteResourceSerde.serialize(precompiled, realA, realB);
        var result = ConcreteResourceSerde.deserialize(data);

        assertEquals(expected, result);
    }

    @Test
    void deserializeHasCorrectServiceAndTypes() {
        UUID id = UUID.randomUUID();
        var path = CompiledResourcePath.builder()
                .service("onboarding.jasmine.software")
                .one("candidate", id)
                .build();

        byte[] data = ConcreteResourceSerde.serialize(path);
        var result = ConcreteResourceSerde.deserialize(data);

        assertEquals("onboarding.jasmine.software", result.service());
        var root = (CompiledResourcePath.ResourceNode) result.root();
        assertEquals("candidate", root.type());
        assertEquals(id, ((CompiledResourcePath.DefiniteId) root.resourceId()).id());
        assertNull(root.child());
    }
}
