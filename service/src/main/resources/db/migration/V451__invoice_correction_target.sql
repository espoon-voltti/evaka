ALTER TABLE invoice_correction
    ADD COLUMN target_month date,
    ADD CONSTRAINT check$invoice_correction_target_month CHECK (target_month IS NULL OR EXTRACT(DAY FROM target_month) = 1),
    -- applied_completely should not be used with "new-style" corrections
    ADD CONSTRAINT check$invoice_correction_applied CHECK (NOT applied_completely OR target_month IS NULL);

CREATE INDEX idx$invoice_correction_target_month_head_of_family ON invoice_correction (target_month, head_of_family_id);
