CREATE TABLE calendar_event (
  id uuid PRIMARY KEY NOT NULL DEFAULT ext.uuid_generate_v1mc(),
  title text NOT NULL,
  description text NOT NULL,
  period daterange NOT NULL
);

CREATE INDEX idx$calendar_event_period ON calendar_event USING GIST (period);

CREATE TABLE calendar_event_attendee (
  id uuid PRIMARY KEY NOT NULL DEFAULT ext.uuid_generate_v1mc(),
  calendar_event_id uuid NOT NULL REFERENCES calendar_event (id) ON DELETE CASCADE,
  unit_id uuid NOT NULL REFERENCES daycare (id) ON DELETE CASCADE,
  group_id uuid REFERENCES daycare_group (id) ON DELETE CASCADE,
  child_id uuid REFERENCES child (id) ON DELETE CASCADE,

  -- if a specific child is specified, the group should be too
  CONSTRAINT check$child_has_group CHECK (child_id IS NULL OR group_id IS NOT NULL)
);

CREATE INDEX idx$calendar_event_attendee_event_id ON calendar_event_attendee (calendar_event_id);
CREATE INDEX idx$calendar_event_attendee_unit_id ON calendar_event_attendee (unit_id);
CREATE INDEX idx$calendar_event_attendee_group_id ON calendar_event_attendee (group_id);
CREATE INDEX idx$calendar_event_attendee_child_id ON calendar_event_attendee (child_id);
