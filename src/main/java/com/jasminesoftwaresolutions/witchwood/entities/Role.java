package com.jasminesoftwaresolutions.witchwood.entities;

import com.jasminesoftwaresolutions.witchwood.models.IRole;
import com.jasminesoftwaresolutions.witchwood.models.IRolePolicyAssignment;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Canonical mutable implementation of {@link IRole}.
 *
 * <p>Uses {@link LinkedHashSet} for deterministic iteration order.</p>
 */
public class Role implements IRole {
    private final UUID id;
    private final Set<IRolePolicyAssignment> policies = new LinkedHashSet<>();

    public Role(UUID id) {
        this.id = id;
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public Set<IRolePolicyAssignment> policies() {
        return policies;
    }

    public Role addPolicy(IRolePolicyAssignment policy) {
        policies.add(policy);
        return this;
    }
}
