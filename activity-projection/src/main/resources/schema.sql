CREATE TABLE IF NOT EXISTS activity_projection (
    activity_id VARCHAR PRIMARY KEY,
    activity_time TIMESTAMP WITH TIME ZONE,
    activity_type VARCHAR,
    description VARCHAR,
    status VARCHAR,
    completed_time TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS activity_status_history (
    id BIGSERIAL PRIMARY KEY,
    activity_id VARCHAR NOT NULL REFERENCES activity_projection(activity_id),
    status VARCHAR NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_activity_status_history_activity_changed_at
    ON activity_status_history (activity_id, changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_activity_projection_status_type
    ON activity_projection (status, activity_type);

