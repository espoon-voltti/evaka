ALTER TABLE fee_thresholds ADD CONSTRAINT fee_thresholds_no_overlap EXCLUDE USING GIST (valid_during WITH &&);

ALTER TABLE fee_thresholds
    ADD COLUMN created timestamp with time zone NOT NULL DEFAULT now(),
    ADD COLUMN updated timestamp with time zone NOT NULL DEFAULT now();

CREATE TRIGGER set_timestamp BEFORE UPDATE ON fee_thresholds FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
