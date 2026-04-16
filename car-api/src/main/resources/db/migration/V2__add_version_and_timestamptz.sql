-- Add optimistic locking version column
ALTER TABLE car ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Migrate timestamps to TIMESTAMP WITH TIME ZONE for correct timezone handling
ALTER TABLE car ALTER COLUMN registration_date TYPE TIMESTAMPTZ USING registration_date AT TIME ZONE 'UTC';
ALTER TABLE car ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
ALTER TABLE car ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';
