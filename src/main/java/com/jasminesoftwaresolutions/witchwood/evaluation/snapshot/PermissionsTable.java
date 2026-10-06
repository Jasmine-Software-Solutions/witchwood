package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot;

import com.jasminesoftwaresolutions.witchwood.util.ArrayView;

import java.util.ArrayList;
import java.util.List;

public class PermissionsTable {
    protected final int[] offset;
    protected final PermissionSetSnapshot[] permissions;

    protected PermissionsTable(int[] offset, PermissionSetSnapshot[] permissions) {
        this.offset = offset;
        this.permissions = permissions;
    }

    public ArrayView<PermissionSetSnapshot> permissions(int principalId) {
        if (principalId >= offset.length || offset.length == 0)
            return new ArrayView<>(permissions, 0, 0);

        int nextPrincipalId = principalId + 1;

        int start = offset[principalId];
        int end = permissions.length;

        if (nextPrincipalId < offset.length)
            end = offset[nextPrincipalId];

        return new ArrayView<>(permissions, start, end);
    }

    public static class Builder {
        /**
         * PermissionSetSnapshots for each principal, indexed by principal ID.
         * Principals are assigned sequential IDs (0, 1, 2, ...) so a flat
         * {@link ArrayList} indexed by ID provides O(1) access.
         */
        protected final List<List<PermissionSetSnapshot>> permissionsByPrincipal = new ArrayList<>();
        protected int size = 0;

        public Builder addPermissions(int principalId, PermissionSetSnapshot permissions) {
            // Grow the list to accommodate the principal ID if needed
            while (permissionsByPrincipal.size() <= principalId)
                permissionsByPrincipal.add(null);

            List<PermissionSetSnapshot> permissionSets = permissionsByPrincipal.get(principalId);
            if (permissionSets == null) {
                permissionSets = new ArrayList<>();
                permissionsByPrincipal.set(principalId, permissionSets);
            }

            permissionSets.add(permissions);
            size++;

            return this;
        }

        public PermissionsTable build() {
            final int offsetLen = permissionsByPrincipal.size();
            final int[] offset = new int[offsetLen];
            final PermissionSetSnapshot[] permissions = new PermissionSetSnapshot[size];

            int currentOffset = 0;
            int currentPermission = 0;

            for (int i = 0; i < offsetLen; i++) {
                final List<PermissionSetSnapshot> subset = permissionsByPrincipal.get(i);
                if (subset == null) {
                    offset[i] = currentOffset;
                    continue;
                }

                offset[i] = currentOffset;
                for (PermissionSetSnapshot permission : subset)
                    permissions[currentPermission++] = permission;

                currentOffset += subset.size();
            }

            return new PermissionsTable(offset, permissions);
        }
    }
}
