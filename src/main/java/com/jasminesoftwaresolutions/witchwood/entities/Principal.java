package com.jasminesoftwaresolutions.witchwood.entities;

import com.jasminesoftwaresolutions.witchwood.models.IPrincipal;
import com.jasminesoftwaresolutions.witchwood.models.IPrincipalPolicyAssignment;
import com.jasminesoftwaresolutions.witchwood.models.IPrincipalRoleAssignment;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Canonical mutable implementation of {@link IPrincipal}.
 *
 * <p>Uses {@link LinkedHashSet} for deterministic iteration order, which is
 * important for reproducible benchmarks and tests.</p>
 */
public class Principal implements IPrincipal {
    private final UUID id;
    private final Set<IPrincipalRoleAssignment> roles = new LinkedHashSet<>();
    private final Set<IPrincipalPolicyAssignment> policies = new LinkedHashSet<>();

    public Principal(UUID id) {
        this.id = id;
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public Set<IPrincipalRoleAssignment> roles() {
        return roles;
    }

    @Override
    public Set<IPrincipalPolicyAssignment> policies() {
        return policies;
    }

    public Principal addRole(IPrincipalRoleAssignment role) {
        roles.add(role);
        return this;
    }

    public Principal addPolicy(IPrincipalPolicyAssignment policy) {
        policies.add(policy);
        return this;
    }
}
