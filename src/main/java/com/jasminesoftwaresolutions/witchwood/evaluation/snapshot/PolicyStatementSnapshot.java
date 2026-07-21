package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;

import java.util.ArrayList;
import java.util.List;

public class PolicyStatementSnapshot {
    protected final CompiledResourcePath[] resources;
    protected final JsonLogicNode condition;

    protected PolicyStatementSnapshot(CompiledResourcePath[] resources, JsonLogicNode condition) {
        this.resources = resources;
        this.condition = condition;
    }

    public CompiledResourcePath[] resources() {
        return resources;
    }

    public JsonLogicNode condition() {
        return condition;
    }

    public static class Builder {
        protected final List<CompiledResourcePath> resources = new ArrayList<>();
        protected JsonLogicNode condition = null;

        public Builder addResource(CompiledResourcePath resource) {
            resources.add(resource);
            return this;
        }

        public Builder setCondition(JsonLogicNode condition) {
            this.condition = condition;
            return this;
        }

        public PolicyStatementSnapshot build() {
            return new PolicyStatementSnapshot(resources.toArray(new CompiledResourcePath[0]), condition);
        }
    }
}
