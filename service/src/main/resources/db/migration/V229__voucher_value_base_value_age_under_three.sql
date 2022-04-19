ALTER TABLE voucher_value ADD base_value_age_under_three INTEGER;
UPDATE voucher_value SET base_value_age_under_three = base_value * age_under_three_coefficient;
ALTER TABLE voucher_value ALTER COLUMN base_value_age_under_three SET NOT NULL;
ALTER TABLE voucher_value DROP COLUMN age_under_three_coefficient;

UPDATE voucher_value_decision SET base_value = base_value * age_coefficient;
ALTER TABLE voucher_value_decision DROP COLUMN age_coefficient;
