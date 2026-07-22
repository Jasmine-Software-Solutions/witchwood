package com.jasminesoftwaresolutions.witchwood.entities;

import com.jasminesoftwaresolutions.witchwood.models.IPrincipal;
import com.jasminesoftwaresolutions.witchwood.models.IPrincipalRoleAssignment;
import com.jasminesoftwaresolutions.witchwood.models.IRole;

import java.time.Instant;

/**
 * Canonical implementation of {@link IPrincipalRoleAssignment}.
 *
 * <p>The two-argument constructor defaults to an unbounded validity window
 * ({@link Instant#EPOCH} to {@link Instant#MAX}), which is the common case
 * for benchmarks and tests. Use the four-argument constructor when loading
 * persisted data with real time bounds.</p>
 */
public class PrincipalRoleAssignment implements IPrincipalRoleAssignment {
    private final IPrincipal principal;
    private final IRole role;
    private final Instant effective;
    private final Instant expiration;

    public PrincipalRoleAssignment(IPrincipal principal, IRole role) {
        this(principal, role, Instant.EPOCH, Instant.MAX);
    }

    public PrincipalRoleAssignment(IPrincipal principal, IRole role, Instant effective, Instant expiration) {
        this.principal = principal;
        this.role = role;
        this.effective = effective;
        this.expiration = expiration;
    }

    @Override
    public IPrincipal principal() {
        return principal;
    }

    @Override
    public IRole role() {
        return role;
    }

    @Override
    public Instant effective() {
        return effective;
    }

    @Override
    public Instant expiration() {
        return expiration;
    }
}
