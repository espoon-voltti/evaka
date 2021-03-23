CREATE TABLE messaging_blocklist (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now(),
    updated timestamp with time zone DEFAULT now(),
    child_id uuid NOT NULL REFERENCES person(id),
    blocked_recipient uuid NOT NULL REFERENCES person(id)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON messaging_blocklist FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE UNIQUE INDEX uniq$messaging_blocklist_child_id_blocked_recipient ON messaging_blocklist (child_id, blocked_recipient);
