ALTER TABLE assistance_need_voucher_coefficient
    ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN modified_by uuid REFERENCES evaka_user;

UPDATE assistance_need_voucher_coefficient SET modified_at = updated;

ALTER TABLE assistance_need_voucher_coefficient
    ALTER COLUMN modified_at SET NOT NULL;

CREATE INDEX fk$assistance_need_voucher_coefficient_modified_by ON assistance_need_voucher_coefficient(modified_by);
