ALTER TABLE varda_service_need ADD COLUMN created timestamp with time zone NOT NULL DEFAULT now();
ALTER TABLE varda_service_need ADD COLUMN updated timestamp with time zone NOT NULL DEFAULT now();

CREATE TRIGGER set_timestamp BEFORE UPDATE ON varda_service_need FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
