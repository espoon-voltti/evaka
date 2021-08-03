ALTER TABLE voucher_value RENAME COLUMN voucher_value TO base_value;

ALTER TABLE voucher_value ADD COLUMN age_under_three_coefficient numeric(3, 2);

UPDATE voucher_value SET age_under_three_coefficient = 1.55;

ALTER TABLE voucher_value ALTER COLUMN age_under_three_coefficient SET NOT NULL;
