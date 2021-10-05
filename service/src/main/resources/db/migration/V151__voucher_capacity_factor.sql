ALTER TABLE voucher_value_decision
    ADD COLUMN capacity_factor numeric(4, 2);

UPDATE voucher_value_decision SET capacity_factor = '1.0';

ALTER TABLE voucher_value_decision
    ALTER COLUMN capacity_factor SET NOT NULL;
