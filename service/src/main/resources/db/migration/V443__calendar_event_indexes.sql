DROP INDEX idx$calendar_event_type;

CREATE INDEX idx$calendar_event_type_period ON calendar_event USING gist(event_type, period);

CREATE INDEX idx$calendar_event_time_created_by ON calendar_event_time (created_by);
CREATE INDEX idx$calendar_event_time_modified_by ON calendar_event_time (modified_by);
CREATE INDEX idx$calendar_event_time_event_id ON calendar_event_time (calendar_event_id);
CREATE INDEX idx$calendar_event_time_child ON calendar_event_time (child_id) WHERE child_id IS NOT NULL;
