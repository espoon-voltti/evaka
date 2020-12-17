CREATE TABLE preschool_term (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    finnish_preschool daterange NOT NULL,
    swedish_preschool daterange NOT NULL,
    extended_term daterange NOT NULL,
    application_period daterange NOT NULL,

    CONSTRAINT preschool_term$extended_care_contain_finnish_preschool CHECK ( extended_term @> finnish_preschool ),
    CONSTRAINT preschool_term$extended_care_contain_swedish_preschool CHECK ( extended_term @> swedish_preschool ),
    CONSTRAINT preschool_term$no_overlaps EXCLUDE ( extended_term WITH && )
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON preschool_term FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
