-- Officer table
CREATE TABLE IF NOT EXISTS officer_projection (
    badge_number VARCHAR PRIMARY KEY,
    first_name VARCHAR,
    last_name VARCHAR,
    rank VARCHAR,
    email VARCHAR,
    phone_number VARCHAR,
    hire_date VARCHAR,
    status VARCHAR,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR
);

-- Vehicle table
CREATE TABLE IF NOT EXISTS vehicle_projection (
    unit_id VARCHAR PRIMARY KEY,
    vehicle_type VARCHAR,
    license_plate VARCHAR,
    vin VARCHAR,
    status VARCHAR,
    last_maintenance_date VARCHAR,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR
);

-- Unit table
CREATE TABLE IF NOT EXISTS unit_projection (
    unit_id VARCHAR PRIMARY KEY,
    unit_type VARCHAR,
    status VARCHAR,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR
);

-- Person table
CREATE TABLE IF NOT EXISTS person_projection (
    person_id VARCHAR PRIMARY KEY,
    first_name VARCHAR,
    last_name VARCHAR,
    date_of_birth VARCHAR,
    gender VARCHAR,
    race VARCHAR,
    phone_number VARCHAR,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR
);

-- Location table
CREATE TABLE IF NOT EXISTS location_projection (
    location_id VARCHAR PRIMARY KEY,
    address VARCHAR,
    city VARCHAR,
    state VARCHAR,
    zip_code VARCHAR,
    latitude VARCHAR,
    longitude VARCHAR,
    location_type VARCHAR,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR
);

-- Status history tables for entities with status
CREATE TABLE IF NOT EXISTS officer_status_history (
    id BIGSERIAL PRIMARY KEY,
    badge_number VARCHAR NOT NULL REFERENCES officer_projection(badge_number),
    status VARCHAR NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR,
    CONSTRAINT uq_officer_status_history_event UNIQUE (event_id)
);

CREATE TABLE IF NOT EXISTS vehicle_status_history (
    id BIGSERIAL PRIMARY KEY,
    unit_id VARCHAR NOT NULL REFERENCES vehicle_projection(unit_id),
    status VARCHAR NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR,
    CONSTRAINT uq_vehicle_status_history_event UNIQUE (event_id)
);

CREATE TABLE IF NOT EXISTS unit_status_history (
    id BIGSERIAL PRIMARY KEY,
    unit_id VARCHAR NOT NULL REFERENCES unit_projection(unit_id),
    status VARCHAR NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    event_id VARCHAR,
    CONSTRAINT uq_unit_status_history_event UNIQUE (event_id)
);

-- Note: Person and Location do not have status fields, so no status history tables needed

-- Indexes for status history tables
CREATE INDEX IF NOT EXISTS idx_officer_status_history_badge_changed_at
    ON officer_status_history (badge_number, changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_vehicle_status_history_unit_changed_at
    ON vehicle_status_history (unit_id, changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_unit_status_history_unit_changed_at
    ON unit_status_history (unit_id, changed_at DESC);

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_officer_projection_status_rank
    ON officer_projection (status, rank);

CREATE INDEX IF NOT EXISTS idx_vehicle_projection_status_type
    ON vehicle_projection (status, vehicle_type);

CREATE INDEX IF NOT EXISTS idx_unit_projection_status_type
    ON unit_projection (status, unit_type);

CREATE INDEX IF NOT EXISTS idx_person_projection_name
    ON person_projection (last_name, first_name);

CREATE INDEX IF NOT EXISTS idx_location_projection_city_state
    ON location_projection (city, state);
