ALTER TABLE sfi_message_event ADD COLUMN event_time timestamp with time zone;

-- Existing rows predate storing the timestamp reported by Suomi.fi, so the ingest time
-- (nightly poll) is the closest approximation available.
UPDATE sfi_message_event SET event_time = created_at;

ALTER TABLE sfi_message_event ALTER COLUMN event_time SET NOT NULL;
