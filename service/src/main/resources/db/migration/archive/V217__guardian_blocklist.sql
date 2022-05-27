CREATE TABLE guardian_blocklist (
    guardian_id uuid NOT NULL CONSTRAINT fk$guardian REFERENCES person (id) ON DELETE CASCADE,
    child_id uuid NOT NULL CONSTRAINT fk$child REFERENCES person (id) ON DELETE CASCADE,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON guardian_blocklist FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE UNIQUE INDEX uniq$guardian_blocklist_guardian_child ON guardian_blocklist (guardian_id, child_id);
CREATE INDEX idx$guardian_blocklist_child ON guardian_blocklist (child_id);
