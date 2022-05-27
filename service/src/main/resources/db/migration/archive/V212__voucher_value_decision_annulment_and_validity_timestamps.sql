ALTER TABLE voucher_value_decision
    ADD COLUMN annulled_at timestamp with time zone,
    ADD COLUMN validity_updated_at timestamp with time zone;

UPDATE voucher_value_decision SET annulled_at = updated WHERE status = 'ANNULLED'::voucher_value_decision_status;
