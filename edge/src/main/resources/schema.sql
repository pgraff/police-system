-- Webhook subscriptions table
CREATE TABLE IF NOT EXISTS webhook_subscriptions (
    id VARCHAR(255) PRIMARY KEY,
    url VARCHAR(2048) NOT NULL,
    secret VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS webhook_subscription_events (
    subscription_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    PRIMARY KEY (subscription_id, event_type),
    FOREIGN KEY (subscription_id) REFERENCES webhook_subscriptions(id) ON DELETE CASCADE
);

-- Idempotency records table
CREATE TABLE IF NOT EXISTS idempotency_records (
    id VARCHAR(255) PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    endpoint VARCHAR(500) NOT NULL,
    request_hash VARCHAR(64),
    response TEXT,
    http_status INTEGER,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_idempotency_key_endpoint ON idempotency_records(idempotency_key, endpoint);
CREATE INDEX IF NOT EXISTS idx_idempotency_expires_at ON idempotency_records(expires_at);
