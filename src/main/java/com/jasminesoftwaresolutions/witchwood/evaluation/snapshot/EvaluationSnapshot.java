package com.jasminesoftwaresolutions.witchwood.evaluation.snapshot;

public class EvaluationSnapshot {
    protected final ServiceDirectory serviceDirectory;
    protected final PrincipalDirectory principalDirectory;
    protected final PermissionsTable permissionsTable;

    protected EvaluationSnapshot(final ServiceDirectory serviceDirectory,
                                 final PrincipalDirectory principalDirectory,
                                 final PermissionsTable permissionsTable) {
        this.serviceDirectory = serviceDirectory;
        this.principalDirectory = principalDirectory;
        this.permissionsTable = permissionsTable;
    }

    public ServiceDirectory serviceDirectory() {
        return serviceDirectory;
    }

    public PrincipalDirectory principalDirectory() {
        return principalDirectory;
    }

    public PermissionsTable permissionsTable() {
        return permissionsTable;
    }

    public static class Builder {
        protected final ServiceDirectory.Builder serviceDirectory = new ServiceDirectory.Builder();
        protected final PrincipalDirectory.Builder principalDirectory = new PrincipalDirectory.Builder();
        protected final PermissionsTable.Builder permissionsTable = new PermissionsTable.Builder();

        public ServiceDirectory.Builder serviceDirectory() {
            return serviceDirectory;
        }

        public PrincipalDirectory.Builder principalDirectory() {
            return principalDirectory;
        }

        public PermissionsTable.Builder permissionsTable() {
            return permissionsTable;
        }

        public EvaluationSnapshot build() {
            return new EvaluationSnapshot(
                    serviceDirectory.build(),
                    principalDirectory.build(),
                    permissionsTable.build()
            );
        }
    }
}
