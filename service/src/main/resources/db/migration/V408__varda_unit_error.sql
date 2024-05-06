ALTER TABLE varda_unit
    ALTER COLUMN varda_unit_id DROP NOT NULL,
    ALTER COLUMN uploaded_at DROP NOT NULL,
    ADD COLUMN updated_at timestamp with time zone NOT NULL DEFAULT now(),
    ADD COLUMN errored_at timestamp with time zone,
    ADD COLUMN error text;
ALTER TABLE varda_unit RENAME uploaded_at TO last_success_at;

CREATE TRIGGER set_timestamp BEFORE UPDATE ON varda_unit FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

ALTER TABLE varda_unit ADD CONSTRAINT check$error_nulls CHECK ((errored_at IS NULL) = (error IS NULL));
CREATE INDEX idx$varda_unit_errored_at ON varda_unit (errored_at) WHERE errored_at IS NOT NULL;
