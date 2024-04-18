CREATE TABLE varda_state (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    child_id uuid NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    state jsonb
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON varda_state FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
CREATE UNIQUE INDEX uniq$varda_state_child_id ON varda_state (child_id);
