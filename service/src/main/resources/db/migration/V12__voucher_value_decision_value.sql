CREATE TABLE voucher_value (
    id uuid NOT NULL,
    validity daterange NOT NULL,
    voucher_value integer NOT NULL
);

ALTER TABLE voucher_value
    ADD CONSTRAINT pk$voucher_value PRIMARY KEY (id),
    ADD CONSTRAINT exclude$voucher_value_validity EXCLUDE USING gist (validity WITH &&);


ALTER TABLE voucher_value_decision_part
    ADD COLUMN hours_per_week numeric(4, 2),
    ADD COLUMN base_value integer,
    ADD COLUMN age_coefficient integer,
    ADD COLUMN service_coefficient integer,
    ADD COLUMN voucher_value integer;

UPDATE voucher_value_decision_part SET base_value = 0, age_coefficient = 0, service_coefficient = 0, voucher_value = 0;

ALTER TABLE voucher_value_decision_part
    ALTER COLUMN base_value SET NOT NULL,
    ALTER COLUMN age_coefficient SET NOT NULL,
    ALTER COLUMN service_coefficient SET NOT NULL,
    ALTER COLUMN voucher_value SET NOT NULL;
