package com.jasminesoftwaresolutions.witchwood.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WitchwoodActionsTest {

    @Test
    void allPermissionsUseConsistentPrefix() {
        assertTrue(WitchwoodActions.CREATE_PRINCIPAL.startsWith("witchwood.jasmine.software:"));
        assertTrue(WitchwoodActions.GET_PRINCIPAL.startsWith("witchwood.jasmine.software:"));
        assertTrue(WitchwoodActions.UPDATE_PRINCIPAL.startsWith("witchwood.jasmine.software:"));
        assertTrue(WitchwoodActions.DELETE_PRINCIPAL.startsWith("witchwood.jasmine.software:"));
        assertTrue(WitchwoodActions.CREATE_POLICY.startsWith("witchwood.jasmine.software:"));
        assertTrue(WitchwoodActions.GET_POLICY.startsWith("witchwood.jasmine.software:"));
        assertTrue(WitchwoodActions.UPDATE_POLICY.startsWith("witchwood.jasmine.software:"));
        assertTrue(WitchwoodActions.DELETE_POLICY.startsWith("witchwood.jasmine.software:"));
        assertTrue(WitchwoodActions.CREATE_ROLE.startsWith("witchwood.jasmine.software:"));
        assertTrue(WitchwoodActions.GET_ROLE.startsWith("witchwood.jasmine.software:"));
        assertTrue(WitchwoodActions.UPDATE_ROLE.startsWith("witchwood.jasmine.software:"));
        assertTrue(WitchwoodActions.DELETE_ROLE.startsWith("witchwood.jasmine.software:"));
        assertTrue(WitchwoodActions.EVALUATE.startsWith("witchwood.jasmine.software:"));
        assertTrue(WitchwoodActions.CREATE_SERVICE.startsWith("witchwood.jasmine.software:"));
        assertTrue(WitchwoodActions.DELETE_SERVICE.startsWith("witchwood.jasmine.software:"));
        assertTrue(WitchwoodActions.UPDATE_SERVICE.startsWith("witchwood.jasmine.software:"));
    }

    @Test
    void allCRUDActionsAreDistinct() {
        String[] actions = {
                WitchwoodActions.CREATE_PRINCIPAL,
                WitchwoodActions.GET_PRINCIPAL,
                WitchwoodActions.UPDATE_PRINCIPAL,
                WitchwoodActions.DELETE_PRINCIPAL,
                WitchwoodActions.CREATE_POLICY,
                WitchwoodActions.GET_POLICY,
                WitchwoodActions.UPDATE_POLICY,
                WitchwoodActions.DELETE_POLICY,
                WitchwoodActions.CREATE_ROLE,
                WitchwoodActions.GET_ROLE,
                WitchwoodActions.UPDATE_ROLE,
                WitchwoodActions.DELETE_ROLE,
                WitchwoodActions.EVALUATE,
                WitchwoodActions.CREATE_SERVICE,
                WitchwoodActions.DELETE_SERVICE,
                WitchwoodActions.UPDATE_SERVICE,
        };
        assertEquals(actions.length, java.util.Set.of(actions).size(),
                "All action permissions must be unique");
    }

    @Test
    void createOpsEndWithCreate() {
        assertTrue(WitchwoodActions.CREATE_PRINCIPAL.endsWith("CreatePrincipal"));
        assertTrue(WitchwoodActions.CREATE_POLICY.endsWith("CreatePolicy"));
        assertTrue(WitchwoodActions.CREATE_ROLE.endsWith("CreateRole"));
        assertTrue(WitchwoodActions.CREATE_SERVICE.endsWith("CreateService"));
    }

    @Test
    void getOpsEndWithGet() {
        assertTrue(WitchwoodActions.GET_PRINCIPAL.endsWith("GetPrincipal"));
        assertTrue(WitchwoodActions.GET_POLICY.endsWith("GetPolicy"));
        assertTrue(WitchwoodActions.GET_ROLE.endsWith("GetRole"));
    }

    @Test
    void updateOpsEndWithUpdate() {
        assertTrue(WitchwoodActions.UPDATE_PRINCIPAL.endsWith("UpdatePrincipal"));
        assertTrue(WitchwoodActions.UPDATE_POLICY.endsWith("UpdatePolicy"));
        assertTrue(WitchwoodActions.UPDATE_ROLE.endsWith("UpdateRole"));
        assertTrue(WitchwoodActions.UPDATE_SERVICE.endsWith("UpdateService"));
    }

    @Test
    void deleteOpsEndWithDelete() {
        assertTrue(WitchwoodActions.DELETE_PRINCIPAL.endsWith("DeletePrincipal"));
        assertTrue(WitchwoodActions.DELETE_POLICY.endsWith("DeletePolicy"));
        assertTrue(WitchwoodActions.DELETE_ROLE.endsWith("DeleteRole"));
        assertTrue(WitchwoodActions.DELETE_SERVICE.endsWith("DeleteService"));
    }
}
