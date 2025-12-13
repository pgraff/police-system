-- Shift table
CREATE TABLE IF NOT EXISTS shift_projection (
    shift_id VARCHAR PRIMARY KEY,
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    shift_type VARCHAR,
    status VARCHAR,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR
);

-- Shift status history table
CREATE TABLE IF NOT EXISTS shift_status_history (
    id BIGSERIAL PRIMARY KEY,
    shift_id VARCHAR NOT NULL REFERENCES shift_projection(shift_id),
    status VARCHAR NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR,
    CONSTRAINT uq_shift_status_history_event UNIQUE (event_id)
);

-- Officer shift table (role entity connecting officers to shifts)
CREATE TABLE IF NOT EXISTS officer_shift_projection (
    id BIGSERIAL PRIMARY KEY,
    shift_id VARCHAR NOT NULL REFERENCES shift_projection(shift_id),
    badge_number VARCHAR NOT NULL,
    check_in_time TIMESTAMP WITH TIME ZONE,
    check_out_time TIMESTAMP WITH TIME ZONE,
    shift_role_type VARCHAR,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR
);

-- Shift change table
CREATE TABLE IF NOT EXISTS shift_change_projection (
    shift_change_id VARCHAR PRIMARY KEY,
    shift_id VARCHAR NOT NULL REFERENCES shift_projection(shift_id),
    change_time TIMESTAMP WITH TIME ZONE,
    change_type VARCHAR,
    notes VARCHAR,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR
);

-- Indexes for status history table
CREATE INDEX IF NOT EXISTS idx_shift_status_history_shift_changed_at
    ON shift_status_history (shift_id, changed_at DESC);

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_shift_projection_status_type
    ON shift_projection (status, shift_type);

CREATE INDEX IF NOT EXISTS idx_officer_shift_projection_shift_badge
    ON officer_shift_projection (shift_id, badge_number);

CREATE INDEX IF NOT EXISTS idx_officer_shift_projection_badge
    ON officer_shift_projection (badge_number);

CREATE INDEX IF NOT EXISTS idx_shift_change_projection_shift
    ON shift_change_projection (shift_id);

CREATE INDEX IF NOT EXISTS idx_shift_change_projection_type
    ON shift_change_projection (change_type);
