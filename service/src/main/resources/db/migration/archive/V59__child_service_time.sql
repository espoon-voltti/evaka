CREATE TABLE daily_service_time (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    child_id uuid REFERENCES person (id) NOT NULL,

    regular bool NOT NULL,

    regular_start text,
    regular_end text,

    monday_start text,
    monday_end text,
    tuesday_start text,
    tuesday_end text,
    wednesday_start text,
    wednesday_end text,
    thursday_start text,
    thursday_end text,
    friday_start text,
    friday_end text

    CONSTRAINT no_partial_time_ranges CHECK (
        (regular_start IS NULL) = (regular_end IS NULL) AND
        (monday_start IS NULL) = (monday_end IS NULL) AND
        (tuesday_start IS NULL) = (tuesday_end IS NULL) AND
        (wednesday_start IS NULL) = (wednesday_end IS NULL) AND
        (thursday_start IS NULL) = (thursday_end IS NULL) AND
        (friday_start IS NULL) = (friday_end IS NULL)
    ),
    CONSTRAINT regular_times CHECK ( regular IS FALSE OR (regular_start IS NOT NULL AND regular_end IS NOT NULL)),
    CONSTRAINT irregular_times CHECK ( regular IS TRUE OR (regular_start IS NULL AND regular_end IS NULL))
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON daily_service_time FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE UNIQUE INDEX uniq$daily_service_time_child ON daily_service_time (child_id);
