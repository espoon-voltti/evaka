CREATE TABLE finance_note
(
    id          UUID PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    person_id   UUID REFERENCES person NOT NULL,
    content     TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  UUID REFERENCES evaka_user NOT NULL,
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by UUID REFERENCES evaka_user NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx$finance_note_person ON finance_note(person_id);
CREATE INDEX idx$finance_note_created_by ON finance_note(created_by);
CREATE INDEX idx$finance_note_modified_by ON finance_note(modified_by);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON finance_note FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
