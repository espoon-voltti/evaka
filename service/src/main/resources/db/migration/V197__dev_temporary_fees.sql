ALTER TABLE fee_thresholds
ADD COLUMN temporary_fee int NOT NULL,
ADD COLUMN temporary_fee_part_day int NOT NULL,
ADD COLUMN temporary_fee_sibling int NOT NULL,
ADD COLUMN temporary_fee_sibling_part_day int NOT NULL;

UPDATE fee_thresholds SET temporary_fee = 2800 WHERE temporary_fee IS NULL;
UPDATE fee_thresholds SET temporary_fee_part_day = 1500 WHERE temporary_fee_part_day IS NULL;
UPDATE fee_thresholds SET temporary_fee_sibling = 1500 WHERE temporary_fee_sibling IS NULL;
UPDATE fee_thresholds SET temporary_fee_sibling_part_day = 800 WHERE temporary_fee_sibling_part_day IS NULL;