ALTER TABLE employee
    DROP column pin;

CREATE TABLE employee_pin (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    user_id uuid references employee(id) NOT NULL,
    pin text NOT NULL,
    locked boolean DEFAULT false,
    failure_count smallint DEFAULT 0,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON employee_pin FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE UNIQUE INDEX idx$pin_user_id ON employee_pin (user_id);
