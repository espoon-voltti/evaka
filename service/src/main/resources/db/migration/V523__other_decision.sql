-- update status constraint to include OTHER_DECISION type
ALTER TABLE child_document DROP CONSTRAINT valid_status;
ALTER TABLE child_document
    ADD CONSTRAINT valid_status CHECK (
        CASE
            WHEN type = 'PEDAGOGICAL_REPORT'
                THEN status IN ('DRAFT', 'COMPLETED')
            WHEN type = 'PEDAGOGICAL_ASSESSMENT'
                THEN status IN ('DRAFT', 'COMPLETED')
            WHEN type = 'HOJKS'
                THEN status IN ('DRAFT', 'PREPARED', 'COMPLETED')
            WHEN type = 'MIGRATED_VASU'
                THEN status IN ('COMPLETED')
            WHEN type = 'MIGRATED_LEOPS'
                THEN status IN ('COMPLETED')
            WHEN type = 'VASU'
                THEN status IN ('DRAFT', 'PREPARED', 'COMPLETED')
            WHEN type = 'LEOPS'
                THEN status IN ('DRAFT', 'PREPARED', 'COMPLETED')
            WHEN type = 'CITIZEN_BASIC'
                THEN status IN ('DRAFT', 'CITIZEN_DRAFT', 'COMPLETED')
            WHEN type = 'OTHER_DECISION'
                THEN status IN ('DRAFT', 'DECISION_PROPOSAL', 'COMPLETED')
            WHEN type = 'OTHER'
                THEN status IN ('DRAFT', 'COMPLETED')
        END
    );

-- Add decision_maker column.
-- It is only used for decision documents and must be set before entering DECISION_PROPOSAL status.
ALTER TABLE child_document ADD COLUMN decision_maker uuid REFERENCES employee;
CREATE INDEX fk$child_document_decision_maker ON child_document (decision_maker);
ALTER TABLE child_document ADD CONSTRAINT decision_maker_consistency CHECK (
    CASE
        WHEN type <> 'OTHER_DECISION'
            THEN decision_maker IS NULL
        WHEN type = 'OTHER_DECISION' AND status IN ('DECISION_PROPOSAL', 'COMPLETED')
            THEN decision_maker IS NOT NULL
        ELSE TRUE
        END
    );

-- Once a decision document moves into COMPLETED status, a separate decision row is inserted
-- with information about the made decision.

CREATE TYPE child_document_decision_status AS ENUM ('ACCEPTED', 'REJECTED', 'ANNULLED');
CREATE TABLE child_document_decision (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    created_by uuid NOT NULL REFERENCES evaka_user,
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    modified_at timestamp with time zone NOT NULL DEFAULT now(),
    modified_by uuid NOT NULL REFERENCES evaka_user,
    status child_document_decision_status NOT NULL,
    valid_from date NOT NULL,
    valid_to date CHECK ( valid_to IS NULL OR valid_to >= valid_from )
);
CREATE TRIGGER set_timestamp BEFORE UPDATE ON child_document_decision FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();
CREATE INDEX fk$child_document_decision_created_by ON child_document_decision (created_by);
CREATE INDEX fk$child_document_decision_modified_by ON child_document_decision (modified_by);

ALTER TABLE child_document ADD COLUMN decision_id uuid REFERENCES child_document_decision;
CREATE UNIQUE INDEX uniq$child_document_decision_id ON child_document (decision_id);
ALTER TABLE child_document ADD CONSTRAINT decision_consistency CHECK (
    (decision_id IS NOT NULL) = (status = 'COMPLETED' AND type = 'OTHER_DECISION')
);

-- Previously all documents that weren't in DRAFT status had to be published. For decision documents this is not the case.
ALTER TABLE child_document DROP CONSTRAINT non_drafts_are_published;
ALTER TABLE child_document
    ADD CONSTRAINT publishing_by_status CHECK (
        CASE
            WHEN type = 'OTHER_DECISION' -- all/only completed decisions are published
                THEN (status = 'COMPLETED') = (published_at IS NOT NULL)
            ELSE -- for other document types all non-draft documents are published
                status = 'DRAFT' OR published_at IS NOT NULL
        END
    );
