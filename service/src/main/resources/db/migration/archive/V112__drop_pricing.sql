ALTER TABLE new_fee_decision RENAME COLUMN pricing TO fee_thresholds;
ALTER TABLE voucher_value_decision RENAME COLUMN pricing TO fee_thresholds;

DROP TABLE pricing;
