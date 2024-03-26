CREATE TYPE calendar_event_type AS ENUM (
    'DAYCARE_EVENT',
    'DISCUSSION_SURVEY'
    );

ALTER TABLE calendar_event
    ADD COLUMN event_type calendar_event_type;

UPDATE calendar_event ce
SET event_type = 'DAYCARE_EVENT'
WHERE NOT EXISTS (select from calendar_event_time cet where cet.calendar_event_id = ce.id);

UPDATE calendar_event ce
SET event_type = 'DISCUSSION_SURVEY'
WHERE EXISTS (select from calendar_event_time cet where cet.calendar_event_id = ce.id);

ALTER TABLE calendar_event
    ALTER COLUMN event_type SET NOT NULL;

CREATE INDEX idx$calendar_event_type ON calendar_event (event_type);