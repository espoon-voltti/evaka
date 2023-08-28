CREATE TABLE service_need_option_fee (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone NOT NULL DEFAULT now(),
    service_need_option_id uuid NOT NULL REFERENCES service_need_option (id),
    validity daterange NOT NULL,
    base_fee int NOT NULL,
    sibling_discount_2 numeric(5,4) NOT NULL,
    sibling_fee_2 int NOT NULL,
    sibling_discount_2_plus numeric(5,4) NOT NULL,
    sibling_fee_2_plus int NOT NULL,

    CONSTRAINT service_need_option_fee$no_overlaps EXCLUDE USING gist (service_need_option_id WITH =, validity WITH &&)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON service_need_option_fee FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

ALTER TABLE fee_thresholds DROP COLUMN preschool_club_fee;
ALTER TABLE fee_thresholds DROP COLUMN preschool_club_sibling_discount;
