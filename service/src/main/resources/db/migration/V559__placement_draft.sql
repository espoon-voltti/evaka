CREATE TABLE placement_draft (
    application_id uuid PRIMARY KEY REFERENCES application(id) ON DELETE CASCADE,
    unit_id uuid NOT NULL REFERENCES daycare(id) ON DELETE CASCADE,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    created_by uuid NOT NULL REFERENCES evaka_user(id),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    modified_at timestamp with time zone NOT NULL DEFAULT now(),
    modified_by uuid NOT NULL REFERENCES evaka_user(id)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON placement_draft
    FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

CREATE INDEX fk$placement_draft_unit_id ON placement_draft(unit_id);
CREATE INDEX fk$placement_draft_created_by ON placement_draft(created_by);
CREATE INDEX fk$placement_draft_modified_by ON placement_draft(modified_by);
