CREATE TABLE finance_note(
    id UUID PRIMARY KEY NOT NULL, //TODO
    content TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID REFERENCES evaka_user,
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by UUID REFERENCES evaka_user,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX fk$finance_note_created_by ON finance_note(created_by);
CREATE INDEX fk$finance_note_modified_by ON finance_note(modified_by);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON finance_note FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
