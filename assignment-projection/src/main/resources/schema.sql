CREATE TABLE IF NOT EXISTS assignment_projection (
    assignment_id VARCHAR PRIMARY KEY,
    assigned_time TIMESTAMP WITH TIME ZONE,
    assignment_type VARCHAR,
    status VARCHAR,
    incident_id VARCHAR,
    call_id VARCHAR,
    dispatch_id VARCHAR,
    completed_time TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR
);

CREATE TABLE IF NOT EXISTS assignment_status_history (
    id BIGSERIAL PRIMARY KEY,
    assignment_id VARCHAR NOT NULL REFERENCES assignment_projection(assignment_id),
    status VARCHAR NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR
);

CREATE INDEX IF NOT EXISTS idx_assignment_status_history_assignment_changed_at
    ON assignment_status_history (assignment_id, changed_at DESC);

CREATE TABLE IF NOT EXISTS assignment_resource (
    id BIGSERIAL PRIMARY KEY,
    assignment_id VARCHAR NOT NULL REFERENCES assignment_projection(assignment_id),
    resource_id VARCHAR NOT NULL,
    resource_type VARCHAR NOT NULL,
    role_type VARCHAR,
    status VARCHAR,
    start_time TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR,
    CONSTRAINT uq_assignment_resource UNIQUE (assignment_id, resource_id, resource_type)
);

CREATE INDEX IF NOT EXISTS idx_assignment_projection_status_type
    ON assignment_projection (status, assignment_type);

CREATE INDEX IF NOT EXISTS idx_assignment_projection_dispatch_incident_call
    ON assignment_projection (dispatch_id, incident_id, call_id);

CREATE INDEX IF NOT EXISTS idx_assignment_resource_assignment
    ON assignment_resource (assignment_id);

CREATE INDEX IF NOT EXISTS idx_assignment_resource_resource
    ON assignment_resource (resource_id, resource_type);

