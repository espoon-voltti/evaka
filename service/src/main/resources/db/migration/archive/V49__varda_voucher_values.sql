ALTER TABLE varda_fee_data ADD COLUMN evaka_voucher_value_decision_id uuid REFERENCES voucher_value_decision(id);
ALTER TABLE varda_fee_data ALTER COLUMN evaka_fee_decision_id DROP NOT NULL;
ALTER TABLE varda_fee_data ADD CONSTRAINT evaka_decision_reference CHECK (
    (evaka_fee_decision_id IS NOT NULL AND evaka_voucher_value_decision_id IS NULL)
    OR (evaka_fee_decision_id IS NULL AND evaka_voucher_value_decision_id IS NOT NULL)
);
CREATE INDEX varda_fee_data_evaka_fee_decision_id ON varda_fee_data (evaka_fee_decision_id);
CREATE INDEX varda_fee_data_evaka_voucher_value_decision_id ON varda_fee_data (evaka_voucher_value_decision_id);
