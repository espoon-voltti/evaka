ALTER TABLE voucher_value_decision RENAME COLUMN head_of_family TO head_of_family_id;
ALTER TABLE voucher_value_decision RENAME COLUMN partner TO partner_id;
ALTER TABLE voucher_value_decision RENAME COLUMN child TO child_id;
ALTER TABLE voucher_value_decision RENAME COLUMN date_of_birth TO child_date_of_birth;
ALTER TABLE voucher_value_decision RENAME COLUMN placement_unit TO placement_unit_id;
ALTER TABLE voucher_value_decision RENAME COLUMN created_at TO created;
ALTER TABLE voucher_value_decision
    DROP COLUMN service_need,
    DROP COLUMN hours_per_week,
    DROP COLUMN service_coefficient,
    DROP COLUMN age_coefficient,
    ADD COLUMN final_co_payment int NOT NULL,
    ADD COLUMN service_need_fee_coefficient numeric(4,2) NOT NULL,
    ADD COLUMN service_need_voucher_value_coefficient numeric(4,2) NOT NULL,
    ADD COLUMN service_need_fee_description_fi text NOT NULL,
    ADD COLUMN service_need_fee_description_sv text NOT NULL,
    ADD COLUMN service_need_voucher_value_description_fi text NOT NULL,
    ADD COLUMN service_need_voucher_value_description_sv text NOT NULL,
    ADD COLUMN age_coefficient numeric(4,2) NOT NULL,
    ADD COLUMN updated timestamp with time zone NOT NULL DEFAULT now(),
    ALTER COLUMN placement_type TYPE placement_type USING placement_type::placement_type;

CREATE TRIGGER set_timestamp BEFORE UPDATE ON voucher_value_decision FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

ALTER TABLE service_need_option
    ADD COLUMN fee_description_fi text NOT NULL,
    ADD COLUMN fee_description_sv text NOT NULL,
    ADD COLUMN voucher_value_description_fi text NOT NULL,
    ADD COLUMN voucher_value_description_sv text NOT NULL;
