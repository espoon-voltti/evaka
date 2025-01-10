CREATE TABLE out_of_office
(
    id          UUID PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    employee_id UUID      NOT NULL,
    period      DATERANGE NOT NULL
);

CREATE INDEX idx_out_of_office_employee_id_period
    ON out_of_office USING gist (employee_id, period);

ALTER TABLE out_of_office
    ADD CONSTRAINT fk_out_of_office_employee_id FOREIGN KEY (employee_id) REFERENCES employee (id),
    ADD CONSTRAINT period_not_null CHECK ((NOT (lower_inf(period) OR upper_inf(period))));
