ALTER TABLE assistance_need_decision
    ADD COLUMN end_date_not_known BOOLEAN DEFAULT FALSE NOT NULL;

ALTER TABLE assistance_need_decision
    ALTER COLUMN end_date_not_known DROP DEFAULT;
