-- Let's store archive_duration to a centralized place
ALTER TABLE archived_process
    ADD COLUMN archive_duration_months smallint;

-- Copy values from document templates
UPDATE archived_process
SET archive_duration_months = update.archive_duration_months
FROM (
    SELECT ap.id, dt.archive_duration_months
    FROM document_template dt
    JOIN child_document cd ON dt.id = cd.template_id
    JOIN archived_process ap ON cd.process_id = ap.id
) update
WHERE archived_process.id = update.id;

-- This shouldn't do anything but to be sure us default of 120 years (used for vasu/leops documents)
UPDATE archived_process
SET archive_duration_months = 120 * 12
WHERE archive_duration_months IS NULL ;

-- Enforce not null
ALTER TABLE archived_process ALTER COLUMN archive_duration_months SET NOT NULL;

-- Add necessary metadata fields for daycare and preschool assistance decisions
ALTER TABLE assistance_need_decision
    ADD COLUMN process_id uuid REFERENCES archived_process ON DELETE SET NULL,
    ADD COLUMN created_by uuid REFERENCES employee;
ALTER TABLE assistance_need_preschool_decision
    ADD COLUMN process_id uuid REFERENCES archived_process ON DELETE SET NULL,
    ADD COLUMN created_by uuid REFERENCES employee;
