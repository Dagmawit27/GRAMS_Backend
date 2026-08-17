-- Flyway Migration: Create audit_log table and security indexes (SRS §10.7)

CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    actor_id VARCHAR(255),
    actor_role VARCHAR(255),
    session_id VARCHAR(255),
    module VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id VARCHAR(255),
    previous_status VARCHAR(100),
    new_status VARCHAR(100),
    outcome VARCHAR(50) NOT NULL,
    ip_address VARCHAR(100),
    user_agent VARCHAR(500),
    correlation_id VARCHAR(255),
    reason VARCHAR(1000)
);

-- Performance Indexes for filtered audit queries
CREATE INDEX IF NOT EXISTS idx_audit_log_module ON audit_log(module);
CREATE INDEX IF NOT EXISTS idx_audit_log_actor_id ON audit_log(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp ON audit_log(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_event_type ON audit_log(event_type);
