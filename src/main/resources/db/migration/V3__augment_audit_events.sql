ALTER TABLE audit_events
    ADD COLUMN IF NOT EXISTS operation VARCHAR(255) NOT NULL DEFAULT 'UNKNOWN';

ALTER TABLE audit_events
    ADD COLUMN IF NOT EXISTS user_email VARCHAR(255) NOT NULL DEFAULT 'system@gomech';

ALTER TABLE audit_events
    ADD COLUMN IF NOT EXISTS module_name VARCHAR(255) NOT NULL DEFAULT 'general';

ALTER TABLE audit_events
    ADD COLUMN IF NOT EXISTS user_role VARCHAR(64) NOT NULL DEFAULT 'SYSTEM';

ALTER TABLE audit_events
    ADD COLUMN IF NOT EXISTS occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE audit_events
SET occurred_at = COALESCE(occurred_at, created_at),
    operation = CASE WHEN operation = 'UNKNOWN' THEN event_type ELSE operation END;
