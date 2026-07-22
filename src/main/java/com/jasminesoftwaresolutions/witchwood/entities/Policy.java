package com.jasminesoftwaresolutions.witchwood.entities;

import com.jasminesoftwaresolutions.witchwood.models.IPolicy;
import com.jasminesoftwaresolutions.witchwood.models.IPolicyStatement;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Canonical mutable implementation of {@link IPolicy}.
 *
 * <p>Uses {@link LinkedHashSet} for deterministic iteration order.</p>
 */
public class Policy implements IPolicy {
    private final UUID id;
    private final Set<IPolicyStatement> statements = new LinkedHashSet<>();

    public Policy(UUID id) {
        this.id = id;
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public Set<IPolicyStatement> statements() {
        return statements;
    }

    public Policy addStatement(IPolicyStatement statement) {
        statements.add(statement);
        return this;
    }
}
