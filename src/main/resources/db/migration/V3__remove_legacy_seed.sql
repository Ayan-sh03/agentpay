-- Remove legacy demo seed that used Java String.hashCode()
DELETE FROM agent_credentials WHERE agent_id = 'demo-agent-001';
DELETE FROM agent_credentials WHERE api_key_hash = '3556498';

-- This migration is idempotent; DELETE has no effect if rows are absent

