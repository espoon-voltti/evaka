CREATE TABLE service_need_option_voucher_value (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone NOT NULL DEFAULT now(),
    service_need_option_id uuid NOT NULL REFERENCES service_need_option (id),
    validity daterange NOT NULL,
    base_value int NOT NULL,
    coefficient numeric(3, 2) NOT NULL,
    value int NOT NULL,
    base_value_under_3y int NOT NULL,
    coefficient_under_3y numeric(3, 2) NOT NULL,
    value_under_3y int NOT NULL,

    CONSTRAINT service_need_option_voucher_value$no_overlaps EXCLUDE USING gist (service_need_option_id WITH =, validity WITH &&)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON service_need_option_voucher_value FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

INSERT INTO service_need_option_voucher_value (
    service_need_option_id,
    validity,
    base_value,
    coefficient,
    value,
    base_value_under_3y,
    coefficient_under_3y,
    value_under_3y
)
SELECT
    sno.id,
    vv.validity,
    vv.base_value,
    sno.voucher_value_coefficient,
    trunc(sno.voucher_value_coefficient * vv.base_value),
    vv.base_value_age_under_three,
    sno.voucher_value_coefficient,
    trunc(sno.voucher_value_coefficient * vv.base_value_age_under_three)
FROM service_need_option sno, voucher_value vv;

ALTER TABLE service_need_option DROP COLUMN voucher_value_coefficient;
DROP TABLE voucher_value;
