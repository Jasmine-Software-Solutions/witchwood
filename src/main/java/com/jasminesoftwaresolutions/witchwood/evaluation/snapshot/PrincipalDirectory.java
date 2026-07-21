package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.UUID;

public class PrincipalDirectory {
    protected final Object2IntMap<UUID> principals;

    protected PrincipalDirectory(Object2IntMap<UUID> principals) {
        this.principals = principals;
    }

    public int principalId(UUID principal) {
        return principals.getInt(principal);
    }

    public static class Builder {
        protected final Object2IntMap<UUID> principals = new Object2IntOpenHashMap<>();
        protected int lastPrincipalId = -1;

        public int addPrincipal(UUID principal) {
            principals.put(principal, ++lastPrincipalId);
            return lastPrincipalId;
        }

        public PrincipalDirectory build() {
            return new PrincipalDirectory(principals);
        }
    }
}
