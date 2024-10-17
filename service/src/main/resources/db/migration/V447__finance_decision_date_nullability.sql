ALTER TABLE fee_decision ADD CONSTRAINT check$valid_range CHECK (NOT (lower_inf(valid_during) OR upper_inf(valid_during)));
ALTER TABLE voucher_value_decision ALTER COLUMN valid_from SET NOT NULL;
ALTER TABLE voucher_value_decision ALTER COLUMN valid_to SET NOT NULL;
