ALTER TABLE service_need_option
    ADD COLUMN daycare_hours_per_month int,
    ADD CONSTRAINT check$no_contract_days_and_hours CHECK (contract_days_per_month IS NULL OR daycare_hours_per_month IS NULL);
