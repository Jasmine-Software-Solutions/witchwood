package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot;

import java.util.ArrayList;
import java.util.List;

public class PermissionSnapshot {
    protected final PolicyStatementSnapshot[] allowStatements;
    protected final PolicyStatementSnapshot[] denyStatements;

    protected PermissionSnapshot(PolicyStatementSnapshot[] allowStatements, PolicyStatementSnapshot[] denyStatements) {
        this.allowStatements = allowStatements;
        this.denyStatements = denyStatements;
    }

    public PolicyStatementSnapshot[] allowStatements() {
        return allowStatements;
    }

    public PolicyStatementSnapshot[] denyStatements() {
        return denyStatements;
    }

    public static class Builder {
        protected final List<PolicyStatementSnapshot> allowStatements = new ArrayList<>();
        protected final List<PolicyStatementSnapshot> denyStatements = new ArrayList<>();

        public Builder addAllowStatement(PolicyStatementSnapshot statement) {
            allowStatements.add(statement);
            return this;
        }

        public Builder addDenyStatement(PolicyStatementSnapshot statement) {
            denyStatements.add(statement);
            return this;
        }

        public PermissionSnapshot build() {
            return new PermissionSnapshot(
                    allowStatements.toArray(new PolicyStatementSnapshot[0]),
                    denyStatements.toArray(new PolicyStatementSnapshot[0])
            );
        }
    }
}
