ALTER TABLE varda_reset_child
    ADD COLUMN created timestamp with time zone DEFAULT now() NOT NULL,
    ADD COLUMN updated timestamp with time zone DEFAULT now() NOT NULL;

CREATE TRIGGER set_timestamp BEFORE UPDATE ON varda_reset_child FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
