CREATE TABLE IF NOT EXISTS services (
    id UUID PRIMARY KEY,
    name TEXT UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS service_actions (
    service_id UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    action TEXT NOT NULL,
    PRIMARY KEY (service_id, action)
);
CREATE INDEX IF NOT EXISTS idx_service_actions_action
    ON service_actions(action);

CREATE TABLE IF NOT EXISTS principals (
    id UUID PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS policies (
    id UUID PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS policy_statements (
    policy_id UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    statement_id INT NOT NULL,
    effect TEXT NOT NULL CHECK (effect IN ('ALLOW', 'DENY')),
    condition JSONB,  -- optional, parsed by JsonLogicParser at load time
    PRIMARY KEY (policy_id, statement_id)
);

CREATE TABLE IF NOT EXISTS statement_actions (
    policy_id UUID NOT NULL,
    statement_id INT NOT NULL,
    action TEXT NOT NULL,
    FOREIGN KEY (policy_id, statement_id)
        REFERENCES policy_statements(policy_id, statement_id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_statement_actions_policy
    ON statement_actions(policy_id, statement_id);

CREATE TABLE IF NOT EXISTS statement_resources (
    policy_id UUID NOT NULL,
    statement_id INT NOT NULL,
    resource BYTEA NOT NULL,  -- serialized CompiledResourcePath proto
    FOREIGN KEY (policy_id, statement_id)
        REFERENCES policy_statements(policy_id, statement_id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_statement_resources_policy
    ON statement_resources(policy_id, statement_id);

CREATE TABLE IF NOT EXISTS role_policy_assignments (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    policy_id UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, policy_id)
);

CREATE TABLE IF NOT EXISTS principal_role_assignments (
    principal_id UUID NOT NULL REFERENCES principals(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    effective TIMESTAMPTZ,  -- null = -infinity
    expiration TIMESTAMPTZ,  -- null = +infinity
    PRIMARY KEY (principal_id, role_id)
);
CREATE INDEX IF NOT EXISTS idx_principal_role_assignments_time
    ON principal_role_assignments(effective, expiration);

CREATE TABLE IF NOT EXISTS principal_policy_assignments (
    principal_id UUID NOT NULL REFERENCES principals(id) ON DELETE CASCADE,
    policy_id UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    PRIMARY KEY (principal_id, policy_id)
);
