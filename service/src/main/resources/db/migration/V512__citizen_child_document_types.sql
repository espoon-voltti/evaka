ALTER TYPE document_template_type ADD VALUE 'CITIZEN_BASIC' BEFORE 'OTHER';
ALTER TYPE child_document_status ADD VALUE 'CITIZEN_DRAFT' BEFORE 'COMPLETED';

ALTER TABLE child_document
    ADD COLUMN answered_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN answered_by UUID REFERENCES evaka_user (id),
    ADD CONSTRAINT answered_consistency CHECK ( (answered_at IS NULL) = (answered_by IS NULL));
CREATE INDEX fk$child_document_answered_by ON child_document (answered_by);
