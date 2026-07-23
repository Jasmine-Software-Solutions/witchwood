package com.jasminesoftwaresolutions.witchwood.entities;

import com.jasminesoftwaresolutions.witchwood.models.IService;
import com.jasminesoftwaresolutions.witchwood.models.IServiceAction;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Canonical mutable implementation of {@link IService}.
 *
 * <p>Uses {@link LinkedHashSet} for deterministic iteration order.</p>
 */
public class Service implements IService {
    private final UUID id;
    private final String name;
    private final Set<IServiceAction> actions = new LinkedHashSet<>();

    public Service(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Set<IServiceAction> actions() {
        return actions;
    }

    public Service addAction(IServiceAction action) {
        actions.add(action);
        return this;
    }
}
