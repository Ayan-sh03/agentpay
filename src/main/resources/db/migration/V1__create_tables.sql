-- Create audit_log table
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(255),
    event_type VARCHAR(255),
    details TEXT,
    timestamp TIMESTAMP WITHOUT TIME ZONE
);

-- Create token table
CREATE TABLE IF NOT EXISTS token (
    agent_id VARCHAR(255) PRIMARY KEY,
    encrypted_credential TEXT
);