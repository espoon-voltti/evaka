CREATE TABLE titania_errors(
    request_time timestamp with time zone NOT NULL,
    employee_id uuid NOT NULL REFERENCES employee(id),
    shift_date date NOT NULL,
    shift_begins time NOT NULL,
    shift_ends time NOT NULL,
    overlapping_shift_begins time NOT NULL,
    overlapping_shift_ends time NOT NULL
);

CREATE INDEX fk$titania_errors_employee_id ON titania_errors(employee_id);
