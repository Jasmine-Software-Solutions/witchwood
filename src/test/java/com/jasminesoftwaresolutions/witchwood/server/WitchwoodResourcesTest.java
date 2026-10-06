package com.jasminesoftwaresolutions.witchwood.server;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WitchwoodResourcesTest {

    @Test
    void serviceRootHasNullRootNode() {
        var path = WitchwoodResources.SERVICE_ROOT;
        assertEquals("witchwood.jasmine.software", path.service());
        assertNull(path.root());
    }

    @Test
    void principalBuildsCorrectPath() {
        UUID id = UUID.randomUUID();
        var path = WitchwoodResources.principal(id);

        assertEquals("witchwood.jasmine.software", path.service());
        assertNotNull(path.root());
        assertInstanceOf(CompiledResourcePath.ResourceNode.class, path.root());

        var node = (CompiledResourcePath.ResourceNode) path.root();
        assertEquals("principal", node.type());
        assertInstanceOf(CompiledResourcePath.DefiniteId.class, node.resourceId());
        assertEquals(id, ((CompiledResourcePath.DefiniteId) node.resourceId()).id());
        assertNull(node.child());
    }

    @Test
    void policyBuildsCorrectPath() {
        UUID id = UUID.randomUUID();
        var path = WitchwoodResources.policy(id);

        assertEquals("witchwood.jasmine.software", path.service());
        var node = (CompiledResourcePath.ResourceNode) path.root();
        assertEquals("policy", node.type());
        assertEquals(id, ((CompiledResourcePath.DefiniteId) node.resourceId()).id());
    }

    @Test
    void roleBuildsCorrectPath() {
        UUID id = UUID.randomUUID();
        var path = WitchwoodResources.role(id);

        assertEquals("witchwood.jasmine.software", path.service());
        var node = (CompiledResourcePath.ResourceNode) path.root();
        assertEquals("role", node.type());
        assertEquals(id, ((CompiledResourcePath.DefiniteId) node.resourceId()).id());
    }

    @Test
    void evaluatePermissionUsesDeterministicUUIDs() {
        String serviceName = "Onboarding";
        String actionName = "CreateCandidate";

        UUID expectedServiceId = UUID.nameUUIDFromBytes(serviceName.getBytes());
        UUID expectedActionId = UUID.nameUUIDFromBytes(actionName.getBytes());

        var path = WitchwoodResources.evaluatePermission(serviceName, actionName);

        assertEquals("witchwood.jasmine.software", path.service());
        var serviceNode = (CompiledResourcePath.ResourceNode) path.root();
        assertEquals("service", serviceNode.type());
        assertEquals(expectedServiceId, ((CompiledResourcePath.DefiniteId) serviceNode.resourceId()).id());

        var actionNode = (CompiledResourcePath.ResourceNode) serviceNode.child();
        assertNotNull(actionNode);
        assertEquals("action", actionNode.type());
        assertEquals(expectedActionId, ((CompiledResourcePath.DefiniteId) actionNode.resourceId()).id());
        assertNull(actionNode.child());
    }

    @Test
    void evaluatePermissionIsIdempotent() {
        var path1 = WitchwoodResources.evaluatePermission("X", "Y");
        var path2 = WitchwoodResources.evaluatePermission("X", "Y");
        assertEquals(path1, path2);
        assertEquals(path1.hashCode(), path2.hashCode());
    }

    @Test
    void evaluatePermissionDifferentServicesProduceDifferentPaths() {
        var path1 = WitchwoodResources.evaluatePermission("A", "x");
        var path2 = WitchwoodResources.evaluatePermission("B", "x");
        assertNotEquals(path1, path2);
    }

    @Test
    void evaluatePermissionDifferentActionsProduceDifferentPaths() {
        var path1 = WitchwoodResources.evaluatePermission("S", "a");
        var path2 = WitchwoodResources.evaluatePermission("S", "b");
        assertNotEquals(path1, path2);
    }

    @Test
    void distinctPrincipalsBuildDistinctPaths() {
        var p1 = WitchwoodResources.principal(UUID.randomUUID());
        var p2 = WitchwoodResources.principal(UUID.randomUUID());
        assertNotEquals(p1, p2);
    }

    @Test
    void serviceRootIsSameInstance() {
        assertSame(WitchwoodResources.SERVICE_ROOT, WitchwoodResources.SERVICE_ROOT);
    }
}
