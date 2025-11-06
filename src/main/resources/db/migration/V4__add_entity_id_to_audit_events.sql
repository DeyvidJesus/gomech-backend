-- Add entity_id column to audit_events table
-- This column stores the ID of the entity that was affected by the audit event

ALTER TABLE audit_events
    ADD COLUMN IF NOT EXISTS entity_id BIGINT;

-- Create index for better performance when querying by entity_id
CREATE INDEX IF NOT EXISTS idx_audit_events_entity_id ON audit_events(entity_id);

-- Add comment to document the column
COMMENT ON COLUMN audit_events.entity_id IS 'ID of the entity (client, vehicle, service order, etc.) affected by this audit event';

