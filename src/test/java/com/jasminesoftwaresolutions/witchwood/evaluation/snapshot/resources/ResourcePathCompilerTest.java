package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ResourcePathCompilerTest {

    // ---- compileResource (no wildcards allowed) ----------------------------

    @Test
    void compileSimpleResource() {
        var id = UUID.randomUUID();
        var path = ResourcePathCompiler.compileResource("svc://bucket:" + id);

        assertEquals("svc", path.service());
        assertInstanceOf(CompiledResourcePath.ResourceNode.class, path.root());
        var node = (CompiledResourcePath.ResourceNode) path.root();
        assertEquals("bucket", node.type());
        assertInstanceOf(CompiledResourcePath.DefiniteId.class, node.resourceId());
        assertEquals(id, ((CompiledResourcePath.DefiniteId) node.resourceId()).id());
        assertNull(node.child());
    }

    @Test
    void compileMultiSegmentResource() {
        var id1 = UUID.randomUUID();
        var id2 = UUID.randomUUID();
        var path = ResourcePathCompiler.compileResource("iam://account:" + id1 + "/user:" + id2);

        assertEquals("iam", path.service());
        var root = (CompiledResourcePath.ResourceNode) path.root();
        assertEquals("account", root.type());
        assertEquals(id1, ((CompiledResourcePath.DefiniteId) root.resourceId()).id());

        var child = (CompiledResourcePath.ResourceNode) root.child();
        assertEquals("user", child.type());
        assertEquals(id2, ((CompiledResourcePath.DefiniteId) child.resourceId()).id());
        assertNull(child.child());
    }

    @Test
    void resourceRejectsWildcardNode() {
        assertThrows(IllegalArgumentException.class, () ->
                ResourcePathCompiler.compileResource("svc://*"));
    }

    @Test
    void resourceRejectsGreedyWildcard() {
        assertThrows(IllegalArgumentException.class, () ->
                ResourcePathCompiler.compileResource("svc://**"));
    }

    @Test
    void resourceRejectsWildcardId() {
        assertThrows(IllegalArgumentException.class, () ->
                ResourcePathCompiler.compileResource("svc://bucket:*"));
    }

    @Test
    void compileResourceWithTrailingSlash() {
        var id = UUID.randomUUID();
        var path = ResourcePathCompiler.compileResource("svc://bucket:" + id + "/");

        assertEquals("svc", path.service());
        var node = (CompiledResourcePath.ResourceNode) path.root();
        assertEquals("bucket", node.type());
        assertEquals(id, ((CompiledResourcePath.DefiniteId) node.resourceId()).id());
    }

    @Test
    void compileEmptyResourcePath() {
        var path = ResourcePathCompiler.compileResource("svc://");

        assertEquals("svc", path.service());
        assertNull(path.root());
    }

    // ---- compilePattern (wildcards allowed) --------------------------------

    @Test
    void compilePatternWithWildcardNode() {
        var path = ResourcePathCompiler.compilePattern("svc://*");

        assertEquals("svc", path.service());
        assertInstanceOf(CompiledResourcePath.WildcardNode.class, path.root());
        var node = (CompiledResourcePath.WildcardNode) path.root();
        assertFalse(node.greedy());
        assertNull(node.child());
    }

    @Test
    void compilePatternWithGreedyWildcard() {
        var path = ResourcePathCompiler.compilePattern("svc://**");

        assertEquals("svc", path.service());
        var node = (CompiledResourcePath.WildcardNode) path.root();
        assertTrue(node.greedy());
        assertNull(node.child());
    }

    @Test
    void compilePatternWithWildcardId() {
        var path = ResourcePathCompiler.compilePattern("svc://bucket:*");

        assertEquals("svc", path.service());
        var node = (CompiledResourcePath.ResourceNode) path.root();
        assertEquals("bucket", node.type());
        assertInstanceOf(CompiledResourcePath.WildcardId.class, node.resourceId());
    }

    @Test
    void compilePatternWithMixedSegments() {
        var id = UUID.randomUUID();
        var path = ResourcePathCompiler.compilePattern("storage://bucket:" + id + "/*");

        assertEquals("storage", path.service());
        var root = (CompiledResourcePath.ResourceNode) path.root();
        assertEquals("bucket", root.type());
        assertInstanceOf(CompiledResourcePath.DefiniteId.class, root.resourceId());

        var child = (CompiledResourcePath.WildcardNode) root.child();
        assertFalse(child.greedy());
        assertNull(child.child());
    }

    @Test
    void compilePatternWithGreedyWildcardThenNode() {
        var id = UUID.randomUUID();
        var path = ResourcePathCompiler.compilePattern("svc://**/bucket:" + id);

        assertEquals("svc", path.service());
        var root = (CompiledResourcePath.WildcardNode) path.root();
        assertTrue(root.greedy());

        var child = (CompiledResourcePath.ResourceNode) root.child();
        assertEquals("bucket", child.type());
        assertEquals(id, ((CompiledResourcePath.DefiniteId) child.resourceId()).id());
    }

    @Test
    void compilePatternAllowsRegularSegments() {
        var id = UUID.randomUUID();
        var path = ResourcePathCompiler.compilePattern("svc://bucket:" + id);

        assertEquals("svc", path.service());
        var node = (CompiledResourcePath.ResourceNode) path.root();
        assertEquals("bucket", node.type());
        assertEquals(id, ((CompiledResourcePath.DefiniteId) node.resourceId()).id());
    }

    // ---- invalid paths ----------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "svc",
            "svc:",
            "://",
            "://bucket:id"
    })
    void invalidPathsThrow(String path) {
        assertThrows(IllegalArgumentException.class, () ->
                ResourcePathCompiler.compileResource(path));
    }

    @Test
    void invalidUuidThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                ResourcePathCompiler.compileResource("svc://bucket:not-a-uuid"));
    }

    @Test
    void missingTypeSeparatorThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                ResourcePathCompiler.compileResource("svc://bucket"
                        + UUID.randomUUID().toString().replace("-", "")));
    }

    // ---- equals / hashCode ------------------------------------------------

    @Test
    void identicalPathsAreEqual() {
        var id = UUID.randomUUID();
        var path = "svc://account:" + id + "/user:" + UUID.randomUUID();

        var a = ResourcePathCompiler.compileResource(path);
        var b = ResourcePathCompiler.compileResource(path);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentServicesAreNotEqual() {
        var id = UUID.randomUUID();
        var a = ResourcePathCompiler.compileResource("svc1://bucket:" + id);
        var b = ResourcePathCompiler.compileResource("svc2://bucket:" + id);

        assertNotEquals(a, b);
    }

    @Test
    void differentIdsAreNotEqual() {
        var a = ResourcePathCompiler.compileResource("svc://bucket:" + UUID.randomUUID());
        var b = ResourcePathCompiler.compileResource("svc://bucket:" + UUID.randomUUID());

        assertNotEquals(a, b);
    }

    @Test
    void differentLengthsAreNotEqual() {
        var id = UUID.randomUUID();
        var a = ResourcePathCompiler.compileResource("svc://bucket:" + id);
        var b = ResourcePathCompiler.compileResource("svc://bucket:" + id + "/obj:" + UUID.randomUUID());

        assertNotEquals(a, b);
    }

    // ---- toString round-trip ----------------------------------------------

    @Test
    void toStringRoundTripResource() {
        var id = UUID.randomUUID();
        var original = ResourcePathCompiler.compileResource("svc://bucket:" + id);
        var roundTripped = ResourcePathCompiler.compileResource(original.toString());

        assertEquals(original, roundTripped);
    }

    @Test
    void toStringRoundTripMultiSegment() {
        var original = ResourcePathCompiler.compileResource(
                "iam://account:" + UUID.randomUUID() + "/user:" + UUID.randomUUID());

        // toString produces a pattern-compatible string (no wildcards detected)
        var roundTripped = ResourcePathCompiler.compilePattern(original.toString());

        assertEquals(original, roundTripped);
    }

    @Test
    void toStringRoundTripPattern() {
        var original = ResourcePathCompiler.compilePattern("svc://bucket:*/**");

        var roundTripped = ResourcePathCompiler.compilePattern(original.toString());

        assertEquals(original, roundTripped);
    }
}
