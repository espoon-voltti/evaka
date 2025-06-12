-- Update any rows that would violate the new constraint
UPDATE holiday_questionnaire_answer
SET open_ranges = '{}'
WHERE open_ranges IS NULL;

-- Add not null constraints
ALTER TABLE holiday_questionnaire_answer
    ALTER COLUMN modified_by SET NOT NULL;
ALTER TABLE holiday_questionnaire_answer
    ALTER COLUMN questionnaire_id SET NOT NULL;
ALTER TABLE holiday_questionnaire_answer
    ALTER COLUMN child_id SET NOT NULL;
ALTER TABLE holiday_questionnaire_answer
    ALTER COLUMN open_ranges SET NOT NULL;
