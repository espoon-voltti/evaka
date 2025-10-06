ALTER TABLE fee_decision
    ADD COLUMN archived_at timestamptz;
ALTER TABLE voucher_value_decision
    ADD COLUMN archived_at timestamptz;
