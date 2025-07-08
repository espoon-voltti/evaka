ALTER TABLE document_template ADD COLUMN end_decision_when_unit_changes BOOLEAN;

UPDATE document_template
SET end_decision_when_unit_changes = TRUE
WHERE type = 'OTHER_DECISION';

ALTER TABLE document_template
ADD CONSTRAINT decision_config
CHECK ( (type = 'OTHER_DECISION') = (end_decision_when_unit_changes IS NOT NULL) );
