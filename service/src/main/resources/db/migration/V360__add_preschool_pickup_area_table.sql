CREATE TABLE preschool_pickup_area (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone NOT NULL DEFAULT now(),
    municipality_code int NOT NULL,
    street_name_fi character varying,
    street_name_sv character varying,
    house_number character varying NOT NULL,
    area_name_fi character varying,
    area_name_sv character varying
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON preschool_pickup_area FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
