CREATE TYPE voucher_value_decision_type AS ENUM ('NORMAL', 'RELIEF_REJECTED', 'RELIEF_PARTLY_ACCEPTED', 'RELIEF_ACCEPTED');

ALTER TABLE voucher_value_decision
  ADD COLUMN decision_type voucher_value_decision_type NOT NULL DEFAULT 'NORMAL'::voucher_value_decision_type;
