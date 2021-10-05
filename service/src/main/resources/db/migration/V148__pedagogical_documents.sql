CREATE TABLE IF NOT EXISTS pedagogical_document (
                                                    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
                                                    child_id uuid REFERENCES person(id) NOT NULL,
                                                    description text,
                                                    created timestamp with time zone DEFAULT now() NOT NULL,
                                                    created_by uuid REFERENCES employee(id),
                                                    updated timestamp with time zone DEFAULT now() NOT NULL,
                                                    updated_by uuid REFERENCES employee(id)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON pedagogical_document FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
CREATE INDEX idx$pedagogical_document_child ON pedagogical_document (child_id);

ALTER TABLE attachment
    ADD COLUMN pedagogical_document_id UUID,
    ADD CONSTRAINT attachment_pedagogical_document_id_fkey FOREIGN KEY (pedagogical_document_id) REFERENCES pedagogical_document (id),
    DROP CONSTRAINT created_for_fk,
    ADD CONSTRAINT created_for_fk CHECK (num_nonnulls(application_id, income_statement_id, message_content_id, message_draft_id, pedagogical_document_id) <= 1);

CREATE INDEX idx$attachment_pedagogic_document ON attachment (pedagogical_document_id) WHERE pedagogical_document_id IS NOT NULL;
