package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot;

import com.jasminesoftwaresolutions.witchwood.util.ArrayView;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;

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
        int nextPrincipalId = principalId + 1;

        int start = offset[principalId];
        int end = permissions.length;

        if (nextPrincipalId < offset.length)
            end = offset[nextPrincipalId];

        return new ArrayView<>(permissions, start, end);
    }

    public static class Builder {
        protected final Int2ObjectArrayMap<List<PermissionSetSnapshot>> permissions = new Int2ObjectArrayMap<>();

        protected int maxPrincipalId = Integer.MIN_VALUE;
        protected int size = 0;

        public Builder addPermissions(int principalId, PermissionSetSnapshot permissions) {
            if (principalId > maxPrincipalId)
                maxPrincipalId = principalId;

            final List<PermissionSetSnapshot> permissionSets = this.permissions.getOrDefault(principalId, new ArrayList<>());
            permissionSets.add(permissions);
            size++;

            this.permissions.put(principalId, permissionSets);
            return this;
        }

        public PermissionsTable build() {
            final int[] offset = new int[size];
            final PermissionSetSnapshot[] permissions = new PermissionSetSnapshot[size];

            int currentOffset = 0;
            int currentPermission = 0;

            for (int i = 0; i <= maxPrincipalId; i++) {
                final List<PermissionSetSnapshot> subset = this.permissions.get(i);
                if (subset == null) continue;

                offset[i] = currentOffset;
                for (PermissionSetSnapshot permission : subset)
                    permissions[currentPermission++] = permission;

                currentOffset += subset.size();
            }

            return new PermissionsTable(offset, permissions);
        }
    }
}
