ALTER TABLE income_statement ADD COLUMN checkup_consent boolean;

-- This selection used to be mandatory, so set it to true for existing rows that have entrepreneur selected.
UPDATE income_statement SET checkup_consent = true WHERE entrepreneur_full_time IS NOT NULL;
