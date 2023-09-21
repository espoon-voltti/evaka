ALTER TABLE voucher_value_decision
    ALTER COLUMN placement_unit_id DROP NOT NULL,
    ALTER COLUMN placement_type DROP NOT NULL,
    ALTER COLUMN service_need_fee_coefficient DROP NOT NULL,
    ALTER COLUMN service_need_voucher_value_coefficient DROP NOT NULL,
    ALTER COLUMN service_need_fee_description_fi DROP NOT NULL,
    ALTER COLUMN service_need_fee_description_sv DROP NOT NULL,
    ALTER COLUMN service_need_voucher_value_description_fi DROP NOT NULL,
    ALTER COLUMN service_need_voucher_value_description_sv DROP NOT NULL,
    ADD CONSTRAINT non_empty_voucher_value_decisions CHECK (
        0 = num_nonnulls(placement_unit_id, placement_type, service_need_fee_coefficient, service_need_voucher_value_coefficient, service_need_fee_description_fi, service_need_fee_description_sv, service_need_voucher_value_description_fi, service_need_voucher_value_description_sv)
        OR 8 = num_nonnulls(placement_unit_id, placement_type, service_need_fee_coefficient, service_need_voucher_value_coefficient, service_need_fee_description_fi, service_need_fee_description_sv, service_need_voucher_value_description_fi, service_need_voucher_value_description_sv)
    );
