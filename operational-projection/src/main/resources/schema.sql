-- Incident table (base entity)
CREATE TABLE IF NOT EXISTS incident_projection (
    incident_id VARCHAR PRIMARY KEY,
    incident_number VARCHAR,
    priority VARCHAR,
    status VARCHAR,
    reported_time TIMESTAMP WITH TIME ZONE,
    dispatched_time TIMESTAMP WITH TIME ZONE,
    arrived_time TIMESTAMP WITH TIME ZONE,
    cleared_time TIMESTAMP WITH TIME ZONE,
    description VARCHAR,
    incident_type VARCHAR,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR
);

-- Call table (linked to incident)
CREATE TABLE IF NOT EXISTS call_projection (
    call_id VARCHAR PRIMARY KEY,
    call_number VARCHAR,
    priority VARCHAR,
    status VARCHAR,
    received_time TIMESTAMP WITH TIME ZONE,
    dispatched_time TIMESTAMP WITH TIME ZONE,
    arrived_time TIMESTAMP WITH TIME ZONE,
    cleared_time TIMESTAMP WITH TIME ZONE,
    description VARCHAR,
    call_type VARCHAR,
    incident_id VARCHAR REFERENCES incident_projection(incident_id),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR
);

-- Dispatch table (linked to call)
CREATE TABLE IF NOT EXISTS dispatch_projection (
    dispatch_id VARCHAR PRIMARY KEY,
    dispatch_time TIMESTAMP WITH TIME ZONE,
    dispatch_type VARCHAR,
    status VARCHAR,
    call_id VARCHAR REFERENCES call_projection(call_id),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR
);

-- Activity table (linked to incident)
CREATE TABLE IF NOT EXISTS activity_projection (
    activity_id VARCHAR PRIMARY KEY,
    activity_time TIMESTAMP WITH TIME ZONE,
    activity_type VARCHAR,
    description VARCHAR,
    status VARCHAR,
    completed_time TIMESTAMP WITH TIME ZONE,
    incident_id VARCHAR REFERENCES incident_projection(incident_id),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR
);

-- Assignment table (linked to incident, call, dispatch)
CREATE TABLE IF NOT EXISTS assignment_projection (
    assignment_id VARCHAR PRIMARY KEY,
    assigned_time TIMESTAMP WITH TIME ZONE,
    assignment_type VARCHAR,
    status VARCHAR,
    incident_id VARCHAR REFERENCES incident_projection(incident_id),
    call_id VARCHAR REFERENCES call_projection(call_id),
    dispatch_id VARCHAR REFERENCES dispatch_projection(dispatch_id),
    completed_time TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR
);

-- Involved Party table (role entity, linked to incident, call, activity)
CREATE TABLE IF NOT EXISTS involved_party_projection (
    involvement_id VARCHAR PRIMARY KEY,
    person_id VARCHAR NOT NULL,
    party_role_type VARCHAR,
    description VARCHAR,
    involvement_start_time TIMESTAMP WITH TIME ZONE,
    involvement_end_time TIMESTAMP WITH TIME ZONE,
    incident_id VARCHAR REFERENCES incident_projection(incident_id),
    call_id VARCHAR REFERENCES call_projection(call_id),
    activity_id VARCHAR REFERENCES activity_projection(activity_id),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR,
    CONSTRAINT chk_involved_party_target CHECK (
        (incident_id IS NOT NULL AND call_id IS NULL AND activity_id IS NULL) OR
        (incident_id IS NULL AND call_id IS NOT NULL AND activity_id IS NULL) OR
        (incident_id IS NULL AND call_id IS NULL AND activity_id IS NOT NULL)
    )
);

-- Resource Assignment table (role entity, linked to assignment)
CREATE TABLE IF NOT EXISTS resource_assignment_projection (
    id BIGSERIAL PRIMARY KEY,
    assignment_id VARCHAR NOT NULL REFERENCES assignment_projection(assignment_id),
    resource_id VARCHAR NOT NULL,
    resource_type VARCHAR NOT NULL,
    role_type VARCHAR,
    status VARCHAR,
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR,
    CONSTRAINT uq_resource_assignment UNIQUE (assignment_id, resource_id, resource_type)
);

-- Status history tables for each entity
CREATE TABLE IF NOT EXISTS incident_status_history (
    id BIGSERIAL PRIMARY KEY,
    incident_id VARCHAR NOT NULL REFERENCES incident_projection(incident_id),
    status VARCHAR NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR,
    CONSTRAINT uq_incident_status_history_event UNIQUE (event_id)
);

CREATE TABLE IF NOT EXISTS call_status_history (
    id BIGSERIAL PRIMARY KEY,
    call_id VARCHAR NOT NULL REFERENCES call_projection(call_id),
    status VARCHAR NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR,
    CONSTRAINT uq_call_status_history_event UNIQUE (event_id)
);

CREATE TABLE IF NOT EXISTS dispatch_status_history (
    id BIGSERIAL PRIMARY KEY,
    dispatch_id VARCHAR NOT NULL REFERENCES dispatch_projection(dispatch_id),
    status VARCHAR NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR,
    CONSTRAINT uq_dispatch_status_history_event UNIQUE (event_id)
);

CREATE TABLE IF NOT EXISTS activity_status_history (
    id BIGSERIAL PRIMARY KEY,
    activity_id VARCHAR NOT NULL REFERENCES activity_projection(activity_id),
    status VARCHAR NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR,
    CONSTRAINT uq_activity_status_history_event UNIQUE (event_id)
);

CREATE TABLE IF NOT EXISTS assignment_status_history (
    id BIGSERIAL PRIMARY KEY,
    assignment_id VARCHAR NOT NULL REFERENCES assignment_projection(assignment_id),
    status VARCHAR NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR,
    CONSTRAINT uq_assignment_status_history_event UNIQUE (event_id)
);

-- Note: involved_party_projection and resource_assignment_projection don't have status history
-- as they are role entities that track involvement/assignment periods, not status changes

-- Composite indexes for common query patterns (FK relationships)
CREATE INDEX IF NOT EXISTS idx_call_incident ON call_projection(incident_id);
CREATE INDEX IF NOT EXISTS idx_dispatch_call ON dispatch_projection(call_id);
CREATE INDEX IF NOT EXISTS idx_activity_incident ON activity_projection(incident_id);
CREATE INDEX IF NOT EXISTS idx_assignment_incident ON assignment_projection(incident_id);
CREATE INDEX IF NOT EXISTS idx_assignment_call ON assignment_projection(call_id);
CREATE INDEX IF NOT EXISTS idx_assignment_dispatch ON assignment_projection(dispatch_id);
CREATE INDEX IF NOT EXISTS idx_involved_party_incident ON involved_party_projection(incident_id);
CREATE INDEX IF NOT EXISTS idx_involved_party_call ON involved_party_projection(call_id);
CREATE INDEX IF NOT EXISTS idx_involved_party_activity ON involved_party_projection(activity_id);
CREATE INDEX IF NOT EXISTS idx_resource_assignment_assignment ON resource_assignment_projection(assignment_id);

-- Status and type indexes for filtering
CREATE INDEX IF NOT EXISTS idx_incident_status_priority ON incident_projection(status, priority);
CREATE INDEX IF NOT EXISTS idx_incident_projection_incident_number ON incident_projection(incident_number);
CREATE INDEX IF NOT EXISTS idx_call_status_priority ON call_projection(status, priority);
CREATE INDEX IF NOT EXISTS idx_call_projection_call_number ON call_projection(call_number);
CREATE INDEX IF NOT EXISTS idx_dispatch_status_type ON dispatch_projection(status, dispatch_type);
CREATE INDEX IF NOT EXISTS idx_activity_status_type ON activity_projection(status, activity_type);
CREATE INDEX IF NOT EXISTS idx_assignment_status_type ON assignment_projection(status, assignment_type);
CREATE INDEX IF NOT EXISTS idx_assignment_projection_dispatch_incident_call ON assignment_projection(dispatch_id, incident_id, call_id);
CREATE INDEX IF NOT EXISTS idx_resource_assignment_resource ON resource_assignment_projection(resource_id, resource_type);

-- Status history indexes
CREATE INDEX IF NOT EXISTS idx_incident_status_history_incident_changed_at 
    ON incident_status_history(incident_id, changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_call_status_history_call_changed_at 
    ON call_status_history(call_id, changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_dispatch_status_history_dispatch_changed_at 
    ON dispatch_status_history(dispatch_id, changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_activity_status_history_activity_changed_at 
    ON activity_status_history(activity_id, changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_assignment_status_history_assignment_changed_at 
    ON assignment_status_history(assignment_id, changed_at DESC);
