-- Create agent_credentials table for AI agent authentication
CREATE TABLE IF NOT EXISTS agent_credentials (
    agent_id VARCHAR(255) PRIMARY KEY,
    owner_id VARCHAR(255) NOT NULL,
    api_key_hash VARCHAR(255) NOT NULL,
    encrypted_secrets TEXT,
    agent_type VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP WITHOUT TIME ZONE,
    daily_spend_limit DECIMAL(10,2),
    monthly_spend_limit DECIMAL(10,2),
    per_transaction_limit DECIMAL(10,2),
    capabilities VARCHAR(500)  -- comma-separated capabilities
);

-- Index for fast API key lookups during authentication (H2 compatible)
CREATE INDEX IF NOT EXISTS idx_agent_credentials_api_key_hash 
ON agent_credentials(api_key_hash);

-- Index for owner queries (H2 compatible) 
CREATE INDEX IF NOT EXISTS idx_agent_credentials_owner 
ON agent_credentials(owner_id);

-- Insert demo agent for testing (H2 compatible syntax)
-- API key "test" has hashCode() = 3556498 (Java String.hashCode()) 
INSERT INTO agent_credentials (
    agent_id, 
    owner_id, 
    api_key_hash, 
    agent_type, 
    daily_spend_limit, 
    monthly_spend_limit, 
    per_transaction_limit, 
    capabilities
) 
SELECT 'demo-agent-001', 'dev-123', '3556498', 'demo-bot', 1000.00, 5000.00, 500.00, 'digital_goods,api_calls'
WHERE NOT EXISTS (SELECT 1 FROM agent_credentials WHERE agent_id = 'demo-agent-001');