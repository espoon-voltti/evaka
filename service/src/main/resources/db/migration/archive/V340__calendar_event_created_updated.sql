ALTER TABLE calendar_event
    ADD COLUMN created timestamp with time zone NOT NULL DEFAULT now(),
    ADD COLUMN updated timestamp with time zone NOT NULL DEFAULT now();
CREATE TRIGGER set_timestamp BEFORE UPDATE ON calendar_event FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

UPDATE calendar_event SET created = '2000-01-01T00:00:00+00:00';

CREATE INDEX idx$calendar_event_created ON calendar_event (created);
