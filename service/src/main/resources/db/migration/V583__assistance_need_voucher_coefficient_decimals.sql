ALTER TABLE assistance_need_voucher_coefficient
    ALTER COLUMN coefficient TYPE numeric(6, 4);

ALTER TABLE voucher_value_decision
    ALTER COLUMN assistance_need_coefficient TYPE numeric(6, 4);
