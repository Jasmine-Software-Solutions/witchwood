package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CompiledResourcePathBuilderTest {

    @Test
    void serviceOnlyProducesNullRoot() {
        var path = CompiledResourcePath.builder()
                .service("onboarding.jasmine.software")
                .build();

        assertEquals("onboarding.jasmine.software", path.service());
        assertNull(path.root());
        assertEquals("onboarding.jasmine.software://", path.toString());
        assertNull(path.root());
    }

    @Test
    void singleOneNode() {
        UUID id = UUID.randomUUID();
        var path = CompiledResourcePath.builder()
                .service("onboarding.jasmine.software")
                .one("candidate", id)
                .build();

        assertEquals("onboarding.jasmine.software", path.service());
        assertNotNull(path.root());
        assertInstanceOf(CompiledResourcePath.ResourceNode.class, path.root());
        var node = (CompiledResourcePath.ResourceNode) path.root();
        assertEquals("candidate", node.type());
        assertInstanceOf(CompiledResourcePath.DefiniteId.class, node.resourceId());
        assertEquals(id, ((CompiledResourcePath.DefiniteId) node.resourceId()).id());
        assertNull(node.child());
    }

    @Test
    void chainedOneNodes() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        var path = CompiledResourcePath.builder()
                .service("iam")
                .one("account", id1)
                .one("user", id2)
                .build();

        assertEquals("iam", path.service());
        var root = (CompiledResourcePath.ResourceNode) path.root();
        assertEquals("account", root.type());
        assertEquals(id1, ((CompiledResourcePath.DefiniteId) root.resourceId()).id());

        var child = (CompiledResourcePath.ResourceNode) root.child();
        assertEquals("user", child.type());
        assertEquals(id2, ((CompiledResourcePath.DefiniteId) child.resourceId()).id());
        assertNull(child.child());

        assertEquals("iam://account:" + id1 + "/user:" + id2, path.toString());
    }

    @Test
    void anyNonGreedyWildcard() {
        var path = CompiledResourcePath.builder()
                .service("svc")
                .any()
                .build();

        assertEquals("svc", path.service());
        assertInstanceOf(CompiledResourcePath.WildcardNode.class, path.root());
        var node = (CompiledResourcePath.WildcardNode) path.root();
        assertFalse(node.greedy());
        assertNull(node.child());
        assertEquals("svc://*", path.toString());
    }

    @Test
    void anyGreedyWildcard() {
        var path = CompiledResourcePath.builder()
                .service("svc")
                .any(true)
                .build();

        assertInstanceOf(CompiledResourcePath.WildcardNode.class, path.root());
        assertTrue(((CompiledResourcePath.WildcardNode) path.root()).greedy());
        assertEquals("svc://**", path.toString());
    }

    @Test
    void anyWithTypeWildcardId() {
        var path = CompiledResourcePath.builder()
                .service("storage")
                .any("bucket")
                .build();

        assertInstanceOf(CompiledResourcePath.ResourceNode.class, path.root());
        var node = (CompiledResourcePath.ResourceNode) path.root();
        assertEquals("bucket", node.type());
        assertInstanceOf(CompiledResourcePath.WildcardId.class, node.resourceId());
        assertEquals("storage://bucket:*", path.toString());
    }

    @Test
    void mixedNodesInOrder() {
        UUID id = UUID.randomUUID();
        var path = CompiledResourcePath.builder()
                .service("svc")
                .one("account", id)
                .any()
                .any("bucket")
                .any(true)
                .build();

        assertEquals("svc", path.service());

        // Root: account:id
        var root = (CompiledResourcePath.ResourceNode) path.root();
        assertEquals("account", root.type());
        assertEquals(id, ((CompiledResourcePath.DefiniteId) root.resourceId()).id());

        // Child 1: * (non-greedy)
        var wc1 = (CompiledResourcePath.WildcardNode) root.child();
        assertFalse(wc1.greedy());

        // Child 2: bucket:*
        var rn = (CompiledResourcePath.ResourceNode) wc1.child();
        assertEquals("bucket", rn.type());
        assertInstanceOf(CompiledResourcePath.WildcardId.class, rn.resourceId());

        // Child 3: ** (greedy)
        var wc2 = (CompiledResourcePath.WildcardNode) rn.child();
        assertTrue(wc2.greedy());
        assertNull(wc2.child());

        assertEquals("svc://account:" + id + "/*/bucket:*/**", path.toString());
    }

    @Test
    void equalsMatchesDirectConstructor() {
        UUID id = UUID.randomUUID();
        var fromBuilder = CompiledResourcePath.builder()
                .service("svc")
                .one("res", id)
                .build();

        var direct = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("res",
                        new CompiledResourcePath.DefiniteId(id), null));

        assertEquals(direct, fromBuilder);
        assertEquals(direct.hashCode(), fromBuilder.hashCode());
    }

    @Test
    void buildWithoutServiceThrows() {
        assertThrows(IllegalStateException.class, () ->
                CompiledResourcePath.builder().build());
    }
}
