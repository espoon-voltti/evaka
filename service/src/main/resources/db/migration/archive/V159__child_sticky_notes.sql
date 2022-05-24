CREATE TABLE child_sticky_note (
    id uuid DEFAULT ext.uuid_generate_v1mc() PRIMARY KEY,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    child_id uuid NOT NULL REFERENCES person,
    note text NOT NULL,
    modified_at timestamp with time zone NOT NULL DEFAULT now(),
    expires date NOT NULL DEFAULT now() + interval '7 days'
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON child_sticky_note FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX idx$child_sticky_note_child_id ON child_sticky_note(child_id);
CREATE INDEX idx$child_sticky_note_expires ON child_sticky_note(expires);

ALTER TABLE group_note
    ADD COLUMN expires date NOT NULL DEFAULT now() + interval '7 days';

CREATE INDEX idx$group_note_expires ON group_note(expires);
