package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServiceDirectory {
    protected final Map<String, UUID> servicePrincipals;
    protected final Object2IntMap<String> actions;

    protected ServiceDirectory(Map<String, UUID> servicePrincipals, Object2IntMap<String> actions) {
        this.servicePrincipals = servicePrincipals;
        this.actions = actions;
    }

    public UUID principalId(String service) {
        return servicePrincipals.get(service);
    }

    public int actionId(String action) {
        return actions.getInt(action);
    }

    public static class Builder {
        protected final Map<String, UUID> servicePrincipals = new HashMap<>();
        protected final Object2IntMap<String> actions = new Object2IntOpenHashMap<>();
        int lastActionId = -1;

        public Builder addServicePrincipal(String service) {
            final UUID principalId = UUID.nameUUIDFromBytes(service.getBytes());
            return addServicePrincipal(service, principalId);
        }

        public Builder addServicePrincipal(String service, UUID principalId) {
            servicePrincipals.put(service, principalId);
            return this;
        }

        public int addAction(String action) {
            actions.put(action, ++lastActionId);
            return lastActionId;
        }

        public ServiceDirectory build() {
            return new ServiceDirectory(servicePrincipals, actions);
        }
    }
}
