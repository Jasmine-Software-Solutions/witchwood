package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ServiceDirectory {
    protected final Map<String, UUID> servicePrincipals;
    protected final Object2IntMap<String> actions;

    protected ServiceDirectory(Map<String, UUID> servicePrincipals, Object2IntMap<String> actions) {
        this.servicePrincipals = servicePrincipals;

        this.actions = actions;
        this.actions.defaultReturnValue(-1);
    }

    public UUID principalId(String service) {
        return servicePrincipals.get(service);
    }

    public int actionId(String action) {
        return actions.getInt(action);
    }

    public Set<String> servicePrincipals() {
        return servicePrincipals.keySet();
    }

    /**
     * Returns the set of registered action strings in {@code "service:key"} format.
     */
    public Set<String> actions() {
        return actions.keySet();
    }

    /**
     * Create a ServiceDirectory from a map of service names to action key sets.
     * Service principals are derived from service names via
     * {@link UUID#nameUUIDFromBytes(byte[])}.
     *
     * @param actions map of {@code service -> {key, ...}}
     * @return a built ServiceDirectory
     */
    public static ServiceDirectory from(Map<String, Set<String>> actions) {
        Builder builder = new Builder();

        for (Map.Entry<String, Set<String>> entry : actions.entrySet()) {
            String service = entry.getKey();
            builder.addServicePrincipal(service);

            for (String actionKey : entry.getValue())
                builder.addAction(service + ":" + actionKey);
        }

        return builder.build();
    }

    public static class Builder {
        protected Map<String, UUID> servicePrincipals = new HashMap<>();
        protected Object2IntMap<String> actions = new Object2IntOpenHashMap<>();
        int lastActionId = -1;

        /**
         * Initialize a Builder with the content of an existing ServiceDirectory.
         * Service principals and their UUIDs are preserved; action IDs are
         * re-assigned sequentially.
         */
        public static Builder from(ServiceDirectory directory) {
            Builder builder = new Builder();

            builder.servicePrincipals.putAll(directory.servicePrincipals);
            builder.actions.putAll(directory.actions);

            return builder;
        }

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
