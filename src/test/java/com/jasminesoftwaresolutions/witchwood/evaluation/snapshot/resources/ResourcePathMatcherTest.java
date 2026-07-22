package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ResourcePathMatcherTest {

    // ---- service matching --------------------------------------------------

    @Test
    void differentServicesDoNotMatch() {
        var pattern = new CompiledResourcePath("svc1", null);
        var resource = new CompiledResourcePath("svc2", null);

        assertFalse(ResourcePathMatcher.matches(pattern, resource));
    }

    @Test
    void sameServiceEmptyPathsMatch() {
        var pattern = new CompiledResourcePath("svc", null);
        var resource = new CompiledResourcePath("svc", null);

        assertTrue(ResourcePathMatcher.matches(pattern, resource));
    }

    // ---- exact node matching -----------------------------------------------

    @Test
    void exactSingleNodeMatches() {
        var id = UUID.randomUUID();
        var pattern = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("bucket",
                        new CompiledResourcePath.DefiniteId(id), null));
        var resource = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("bucket",
                        new CompiledResourcePath.DefiniteId(id), null));

        assertTrue(ResourcePathMatcher.matches(pattern, resource));
    }

    @Test
    void differentTypesDoNotMatch() {
        var id = UUID.randomUUID();
        var pattern = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("bucket",
                        new CompiledResourcePath.DefiniteId(id), null));
        var resource = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("object",
                        new CompiledResourcePath.DefiniteId(id), null));

        assertFalse(ResourcePathMatcher.matches(pattern, resource));
    }

    @Test
    void differentIdsDoNotMatch() {
        var pattern = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("bucket",
                        new CompiledResourcePath.DefiniteId(UUID.randomUUID()), null));
        var resource = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("bucket",
                        new CompiledResourcePath.DefiniteId(UUID.randomUUID()), null));

        assertFalse(ResourcePathMatcher.matches(pattern, resource));
    }

    @Test
    void longerPatternDoesNotMatchShorterResource() {
        var id = UUID.randomUUID();
        var pattern = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("a",
                        new CompiledResourcePath.DefiniteId(id),
                        new CompiledResourcePath.ResourceNode("b",
                                new CompiledResourcePath.DefiniteId(id), null)));
        var resource = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("a",
                        new CompiledResourcePath.DefiniteId(id), null));

        assertFalse(ResourcePathMatcher.matches(pattern, resource));
    }

    @Test
    void shorterPatternDoesNotMatchLongerResource() {
        var id = UUID.randomUUID();
        var pattern = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("a",
                        new CompiledResourcePath.DefiniteId(id), null));
        var resource = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("a",
                        new CompiledResourcePath.DefiniteId(id),
                        new CompiledResourcePath.ResourceNode("b",
                                new CompiledResourcePath.DefiniteId(id), null)));

        assertFalse(ResourcePathMatcher.matches(pattern, resource));
    }

    @Test
    void multiSegmentExactMatch() {
        var id1 = UUID.randomUUID();
        var id2 = UUID.randomUUID();

        var pattern = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("a",
                        new CompiledResourcePath.DefiniteId(id1),
                        new CompiledResourcePath.ResourceNode("b",
                                new CompiledResourcePath.DefiniteId(id2), null)));
        var resource = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("a",
                        new CompiledResourcePath.DefiniteId(id1),
                        new CompiledResourcePath.ResourceNode("b",
                                new CompiledResourcePath.DefiniteId(id2), null)));

        assertTrue(ResourcePathMatcher.matches(pattern, resource));
    }

    // ---- wildcard node matching ---------------------------------------

    @Test
    void singeWildcardNodeMatchesOneNode() {
        var id = UUID.randomUUID();
        var pattern = new CompiledResourcePath("svc",
                new CompiledResourcePath.WildcardNode(null, false));
        var resource = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("bucket",
                        new CompiledResourcePath.DefiniteId(id), null));

        assertTrue(ResourcePathMatcher.matches(pattern, resource));
    }

    @Test
    void singeWildcardConsumesExactlyOneNode() {
        var id = UUID.randomUUID();
        // pattern: svc://*/bucket:id
        var pattern = new CompiledResourcePath("svc",
                new CompiledResourcePath.WildcardNode(
                        new CompiledResourcePath.ResourceNode("bucket",
                                new CompiledResourcePath.DefiniteId(id), null),
                        false));
        // resource: svc://a:id1/bucket:id  (two nodes before bucket)
        var resource = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("a",
                        new CompiledResourcePath.DefiniteId(UUID.randomUUID()),
                        new CompiledResourcePath.ResourceNode("bucket",
                                new CompiledResourcePath.DefiniteId(id), null)));

        // * consumes "a" but then child wants "bucket" — matches
        assertTrue(ResourcePathMatcher.matches(pattern, resource));
    }

    @Test
    void singeWildcardDoesNotMatchEmptyResource() {
        var pattern = new CompiledResourcePath("svc",
                new CompiledResourcePath.WildcardNode(null, false));
        var resource = new CompiledResourcePath("svc", null);

        assertFalse(ResourcePathMatcher.matches(pattern, resource));
    }

    @Test
    void greedyWildcardMatchesMultipleNodes() {
        var pattern = new CompiledResourcePath("svc",
                new CompiledResourcePath.WildcardNode(null, true));
        var resource = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("a",
                        new CompiledResourcePath.DefiniteId(UUID.randomUUID()),
                        new CompiledResourcePath.ResourceNode("b",
                                new CompiledResourcePath.DefiniteId(UUID.randomUUID()), null)));

        assertTrue(ResourcePathMatcher.matches(pattern, resource));
    }

    @Test
    void greedyWildcardMatchesSingleNode() {
        var id = UUID.randomUUID();
        var pattern = new CompiledResourcePath("svc",
                new CompiledResourcePath.WildcardNode(null, true));
        var resource = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("a",
                        new CompiledResourcePath.DefiniteId(id), null));

        assertTrue(ResourcePathMatcher.matches(pattern, resource));
    }

    @Test
    void greedyWildcardThenExactNode() {
        var id = UUID.randomUUID();
        // svc://**/bucket:id
        var pattern = new CompiledResourcePath("svc",
                new CompiledResourcePath.WildcardNode(
                        new CompiledResourcePath.ResourceNode("bucket",
                                new CompiledResourcePath.DefiniteId(id), null),
                        true));
        // svc://a:x/b:y/bucket:id
        var resource = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("a",
                        new CompiledResourcePath.DefiniteId(UUID.randomUUID()),
                        new CompiledResourcePath.ResourceNode("b",
                                new CompiledResourcePath.DefiniteId(UUID.randomUUID()),
                                new CompiledResourcePath.ResourceNode("bucket",
                                        new CompiledResourcePath.DefiniteId(id), null))));

        assertTrue(ResourcePathMatcher.matches(pattern, resource));
    }

    @Test
    void greedyWildcardThenExactNodeNotMatchingTailReturnsFalse() {
        var id = UUID.randomUUID();
        // svc://**/bucket:id
        var pattern = new CompiledResourcePath("svc",
                new CompiledResourcePath.WildcardNode(
                        new CompiledResourcePath.ResourceNode("bucket",
                                new CompiledResourcePath.DefiniteId(id), null),
                        true));
        // svc://a:x/b:y/wrongType:id
        var resource = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("a",
                        new CompiledResourcePath.DefiniteId(UUID.randomUUID()),
                        new CompiledResourcePath.ResourceNode("b",
                                new CompiledResourcePath.DefiniteId(UUID.randomUUID()),
                                new CompiledResourcePath.ResourceNode("wrongType",
                                        new CompiledResourcePath.DefiniteId(id), null))));

        assertFalse(ResourcePathMatcher.matches(pattern, resource));
    }

    // ---- wildcard ID matching ------------------------------------------

    @Test
    void wildcardIdMatchesAnyUuid() {
        var pattern = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("bucket",
                        new CompiledResourcePath.WildcardId(), null));
        var resource = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("bucket",
                        new CompiledResourcePath.DefiniteId(UUID.randomUUID()), null));

        assertTrue(ResourcePathMatcher.matches(pattern, resource));
    }

    @Test
    void wildcardIdStillRequiresMatchingType() {
        var pattern = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("bucket",
                        new CompiledResourcePath.WildcardId(), null));
        var resource = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("object",
                        new CompiledResourcePath.DefiniteId(UUID.randomUUID()), null));

        assertFalse(ResourcePathMatcher.matches(pattern, resource));
    }

    // ---- combined wildcards -----------------------------------------------

    @Test
    void wildcardNodeThenWildcardId() {
        // svc://*/bucket:*
        var pattern = new CompiledResourcePath("svc",
                new CompiledResourcePath.WildcardNode(
                        new CompiledResourcePath.ResourceNode("bucket",
                                new CompiledResourcePath.WildcardId(), null),
                        false));
        // svc://a:id1/bucket:id2
        var resource = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("a",
                        new CompiledResourcePath.DefiniteId(UUID.randomUUID()),
                        new CompiledResourcePath.ResourceNode("bucket",
                                new CompiledResourcePath.DefiniteId(UUID.randomUUID()), null)));

        assertTrue(ResourcePathMatcher.matches(pattern, resource));
    }

    @Test
    void greedyWildcardWithWildcardIdTail() {
        // svc://**/bucket:*
        var pattern = new CompiledResourcePath("svc",
                new CompiledResourcePath.WildcardNode(
                        new CompiledResourcePath.ResourceNode("bucket",
                                new CompiledResourcePath.WildcardId(), null),
                        true));
        // svc://a:id1/b:id2/bucket:id3
        var resource = new CompiledResourcePath("svc",
                new CompiledResourcePath.ResourceNode("a",
                        new CompiledResourcePath.DefiniteId(UUID.randomUUID()),
                        new CompiledResourcePath.ResourceNode("b",
                                new CompiledResourcePath.DefiniteId(UUID.randomUUID()),
                                new CompiledResourcePath.ResourceNode("bucket",
                                        new CompiledResourcePath.DefiniteId(UUID.randomUUID()),
                                        null))));

        assertTrue(ResourcePathMatcher.matches(pattern, resource));
    }
}
