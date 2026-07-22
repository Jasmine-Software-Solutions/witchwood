package com.jasminesoftwaresolutions.witchwood.entities;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import com.jasminesoftwaresolutions.witchwood.models.IPolicy;
import com.jasminesoftwaresolutions.witchwood.models.IPolicyStatement;
import com.jasminesoftwaresolutions.witchwood.models.PolicyEffect;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;

import java.util.Set;

/**
 * Canonical implementation of {@link IPolicyStatement}.
 */
public class PolicyStatement implements IPolicyStatement {
    private final IPolicy policy;
    private final int id;
    private final PolicyEffect effect;
    private final Set<String> actions;
    private final Set<CompiledResourcePath> resources;
    private final JsonLogicNode condition;

    public PolicyStatement(IPolicy policy, int id, PolicyEffect effect,
                           Set<String> actions, Set<CompiledResourcePath> resources,
                           JsonLogicNode condition) {
        this.policy = policy;
        this.id = id;
        this.effect = effect;
        this.actions = actions;
        this.resources = resources;
        this.condition = condition;
    }

    @Override
    public IPolicy policy() {
        return policy;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public PolicyEffect effect() {
        return effect;
    }

    @Override
    public Set<String> actions() {
        return actions;
    }

    @Override
    public Set<CompiledResourcePath> resources() {
        return resources;
    }

    @Override
    public JsonLogicNode condition() {
        return condition;
    }
}
