ALTER TABLE document_template
    ADD COLUMN process_definition_number text,
    ADD COLUMN archive_duration_months smallint;

ALTER TABLE document_template
    ADD CONSTRAINT check$process_definition_number_not_blank
        CHECK ( process_definition_number <> '' );

ALTER TABLE document_template
    ADD CONSTRAINT check$archive_duration_months_positive
        CHECK ( archive_duration_months IS NULL OR archive_duration_months > 0 );

ALTER TABLE document_template
    ADD CONSTRAINT check$archive_fields_nullability
        CHECK (
            (process_definition_number IS NOT NULL ) =
            (archive_duration_months IS NOT NULL )
        );

-- this table is shared by all process types so that the uniqueness
-- of (process_definition_number, year, number) can be guaranteed
CREATE TABLE archived_process (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    process_definition_number text NOT NULL,
    year smallint NOT NULL,
    number integer NOT NULL,
    organization text NOT NULL
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON archived_process
    FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE UNIQUE INDEX uniq$process_number ON archived_process (process_definition_number, year, number);

CREATE TYPE archived_process_state AS ENUM ('INITIAL', 'PREPARATION', 'DECIDING', 'COMPLETED');

CREATE TABLE archived_process_history (
    process_id uuid NOT NULL REFERENCES archived_process,
    row_index smallint NOT NULL, -- incrementing number
    state archived_process_state NOT NULL,
    entered_at timestamp with time zone NOT NULL,
    entered_by uuid NOT NULL REFERENCES evaka_user
);

CREATE UNIQUE INDEX uniq$archived_process_history_row_index ON archived_process_history (process_id, row_index);

-- assume a 1-to-1 relationship between child_document and archived_process for now
ALTER TABLE child_document
    ADD COLUMN process_id uuid REFERENCES archived_process,
    ADD COLUMN created_by uuid REFERENCES employee;

CREATE INDEX fk$child_document_process_id ON child_document(process_id);
