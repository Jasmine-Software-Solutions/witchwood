package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot;

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
        protected PermissionSnapshot[] permissions = new PermissionSnapshot[4096];
        int maxActionId = Integer.MIN_VALUE;

        public Builder addPermission(int actionId, PermissionSnapshot permission) {
            if (actionId > maxActionId)
                maxActionId = actionId;

            permissions[actionId] = permission;
            return this;
        }

        public PermissionSetSnapshot build() {
            final PermissionSnapshot[] permissions = new PermissionSnapshot[maxActionId + 1];
            System.arraycopy(this.permissions, 0, permissions, 0, permissions.length);

            return new PermissionSetSnapshot(permissions);
        }
    }
}
