package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.List;

public class PermissionSetSnapshot {
    protected final PermissionSnapshot[] permissions;

    protected PermissionSetSnapshot(PermissionSnapshot[] permissions) {
        this.permissions = permissions;
    }

    public PermissionSnapshot permission(int actionId) {
        if (actionId >= permissions.length)
            return null;

        return permissions[actionId];
    }

    public static class Builder {
        protected final IntList actionIds = new IntArrayList();
        protected final List<PermissionSnapshot> permissions = new ArrayList<>();
        protected int maxActionId = Integer.MIN_VALUE;

        public Builder addPermission(int actionId, PermissionSnapshot permission) {
            if (actionId > maxActionId)
                maxActionId = actionId;

            actionIds.add(actionId);
            permissions.add(permission);
            return this;
        }

        public PermissionSetSnapshot build() {
            final PermissionSnapshot[] permissions = new PermissionSnapshot[Math.max(maxActionId + 1, 0)];

            for (int i = 0; i < actionIds.size(); i++)
                permissions[actionIds.getInt(i)] = this.permissions.get(i);

            return new PermissionSetSnapshot(permissions);
        }
    }
}
