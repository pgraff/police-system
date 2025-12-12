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
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS incident_status_history (
    id BIGSERIAL PRIMARY KEY,
    incident_id VARCHAR NOT NULL REFERENCES incident_projection(incident_id),
    status VARCHAR NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_incident_status_history_incident_changed_at
    ON incident_status_history (incident_id, changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_incident_projection_status_priority
    ON incident_projection (status, priority);

CREATE INDEX IF NOT EXISTS idx_incident_projection_incident_number
    ON incident_projection (incident_number);
