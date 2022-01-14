ALTER TABLE fee_thresholds
ADD COLUMN temporary_fee int,
ADD COLUMN temporary_fee_part_day int,
ADD COLUMN temporary_fee_sibling int,
ADD COLUMN temporary_fee_sibling_part_day int;

UPDATE fee_thresholds SET temporary_fee = 2800 WHERE temporary_fee IS NULL;
UPDATE fee_thresholds SET temporary_fee_part_day = 1500 WHERE temporary_fee_part_day IS NULL;
UPDATE fee_thresholds SET temporary_fee_sibling = 1500 WHERE temporary_fee_sibling IS NULL;
UPDATE fee_thresholds SET temporary_fee_sibling_part_day = 800 WHERE temporary_fee_sibling_part_day IS NULL;

ALTER TABLE fee_thresholds
ALTER COLUMN temporary_fee SET NOT NULL,
ALTER COLUMN temporary_fee_part_day SET NOT NULL,
ALTER COLUMN temporary_fee_sibling SET NOT NULL,
ALTER COLUMN temporary_fee_sibling_part_day SET NOT NULL;