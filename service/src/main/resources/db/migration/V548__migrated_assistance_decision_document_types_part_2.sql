-- recreate constraints to account for the new types

ALTER TABLE document_template DROP CONSTRAINT decision_config;
ALTER TABLE document_template ADD CONSTRAINT decision_config CHECK (
    (type IN ('OTHER_DECISION', 'MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION', 'MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION')) = (end_decision_when_unit_changes IS NOT NULL)
);

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
            WHEN type = 'MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION'
                THEN status IN ('COMPLETED')
            WHEN type = 'MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION'
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

ALTER TABLE child_document DROP CONSTRAINT decision_maker_consistency;
ALTER TABLE child_document ADD CONSTRAINT decision_maker_consistency CHECK (
    CASE
        WHEN type NOT IN ('OTHER_DECISION', 'MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION', 'MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION')
            THEN decision_maker IS NULL
        WHEN type IN ('OTHER_DECISION', 'MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION', 'MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION') AND status IN ('DECISION_PROPOSAL', 'COMPLETED')
            THEN decision_maker IS NOT NULL
        ELSE TRUE
        END
    );

ALTER TABLE child_document DROP CONSTRAINT decision_consistency;
ALTER TABLE child_document ADD CONSTRAINT decision_consistency CHECK (
    (decision_id IS NOT NULL) = (status = 'COMPLETED' AND type IN ('OTHER_DECISION', 'MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION', 'MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION'))
);

ALTER TABLE child_document DROP CONSTRAINT publishing_by_status;
ALTER TABLE child_document
    ADD CONSTRAINT publishing_by_status CHECK (
        CASE
            WHEN type IN ('OTHER_DECISION', 'MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION', 'MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION') -- all/only completed decisions are published
                THEN (status = 'COMPLETED') = (published_at IS NOT NULL)
            ELSE -- for other document types all non-draft documents are published
                status = 'DRAFT' OR published_at IS NOT NULL
        END
    );
