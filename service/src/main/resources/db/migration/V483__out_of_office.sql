CREATE TABLE out_of_office
(
    id          UUID PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    employee_id UUID NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL
);

CREATE INDEX idx_out_of_office_employee_id_end_date
    ON out_of_office (employee_id, end_date);

ALTER TABLE out_of_office
    ADD CONSTRAINT fk_out_of_office_employee_id FOREIGN KEY (employee_id) REFERENCES employee (id),
    ADD CONSTRAINT start_before_end CHECK ((start_date <= end_date));