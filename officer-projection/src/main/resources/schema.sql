CREATE TABLE IF NOT EXISTS officer_projection (
    badge_number VARCHAR PRIMARY KEY,
    first_name VARCHAR,
    last_name VARCHAR,
    rank VARCHAR,
    email VARCHAR,
    phone_number VARCHAR,
    hire_date VARCHAR,
    status VARCHAR,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS officer_status_history (
    id BIGSERIAL PRIMARY KEY,
    badge_number VARCHAR NOT NULL REFERENCES officer_projection(badge_number),
    status VARCHAR NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_officer_status_history_badge_changed_at
    ON officer_status_history (badge_number, changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_officer_projection_status_rank
    ON officer_projection (status, rank);
