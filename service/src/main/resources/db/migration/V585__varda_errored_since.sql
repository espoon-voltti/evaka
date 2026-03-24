ALTER TABLE varda_state ADD COLUMN errored_since timestamp with time zone;

UPDATE varda_state SET errored_since = coalesce(last_success_at, created_at) WHERE errored_at IS NOT NULL;

ALTER TABLE varda_state ADD CONSTRAINT check$errored_since_null CHECK ((errored_at IS NULL) = (errored_since IS NULL));

ALTER TABLE varda_unit ADD COLUMN errored_since timestamp with time zone;

UPDATE varda_unit SET errored_since = coalesce(last_success_at, created_at) WHERE errored_at IS NOT NULL;

ALTER TABLE varda_unit ADD CONSTRAINT check$errored_since_null CHECK ((errored_at IS NULL) = (errored_since IS NULL));
