ALTER TABLE varda_state
    ADD COLUMN last_success_at timestamp with time zone,
    ADD COLUMN errored_at timestamp with time zone,
    ADD COLUMN error text;

ALTER TABLE varda_state ADD CONSTRAINT check$error_nulls CHECK ((errored_at IS NULL) = (error IS NULL));
CREATE INDEX idx$varda_state_errored_at ON varda_state (errored_at) WHERE errored_at IS NOT NULL;
