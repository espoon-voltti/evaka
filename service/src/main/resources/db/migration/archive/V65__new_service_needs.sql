CREATE TABLE service_need_option (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,

    name text NOT NULL,
    valid_placement_type placement_type NOT NULL,
    fee_coefficient numeric(6,2) NOT NULL,
    voucher_value_coefficient numeric(6,2) NOT NULL,
    occupancy_coefficient numeric(4,2) NOT NULL,
    part_day bool NOT NULL,
    part_week bool NOT NULL
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON service_need_option FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE TABLE new_service_need (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,

    option_id uuid REFERENCES service_need_option(id) NOT NULL,
    placement_id uuid NOT NULL REFERENCES placement(id) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    shift_care bool NOT NULL,

    CONSTRAINT start_before_end CHECK ((start_date <= end_date)),
    CONSTRAINT new_service_need_no_overlap EXCLUDE USING gist (placement_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON new_service_need FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX idx$new_service_need_placement ON new_service_need (placement_id);
CREATE INDEX idx$new_service_need_option ON new_service_need (option_id);
