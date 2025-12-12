CREATE TABLE IF NOT EXISTS dispatch_projection (
    dispatch_id VARCHAR PRIMARY KEY,
    dispatch_time TIMESTAMP WITH TIME ZONE,
    dispatch_type VARCHAR,
    status VARCHAR,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS dispatch_status_history (
    id BIGSERIAL PRIMARY KEY,
    dispatch_id VARCHAR NOT NULL REFERENCES dispatch_projection(dispatch_id),
    status VARCHAR NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dispatch_status_history_dispatch_changed_at
    ON dispatch_status_history (dispatch_id, changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_dispatch_projection_status_type
    ON dispatch_projection (status, dispatch_type);

