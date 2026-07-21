package com.jasminesoftwaresolutions.witchwood.models;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;

import java.util.Set;

public interface IPolicyStatement {
    IPolicy policy();
    int id();

    PolicyEffect effect();
    Set<String> actions();
    Set<CompiledResourcePath> resources();

    JsonLogicNode condition();
}
