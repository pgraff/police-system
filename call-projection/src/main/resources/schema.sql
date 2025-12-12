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
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS call_status_history (
    id BIGSERIAL PRIMARY KEY,
    call_id VARCHAR NOT NULL REFERENCES call_projection(call_id),
    status VARCHAR NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_call_status_history_call_changed_at
    ON call_status_history (call_id, changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_call_projection_status_priority
    ON call_projection (status, priority);

CREATE INDEX IF NOT EXISTS idx_call_projection_call_number
    ON call_projection (call_number);

