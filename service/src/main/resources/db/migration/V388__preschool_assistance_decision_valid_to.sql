ALTER TABLE assistance_need_preschool_decision
    ADD COLUMN valid_to DATE,
    ADD CONSTRAINT valid_from_before_valid_to CHECK (valid_from <= valid_to);
