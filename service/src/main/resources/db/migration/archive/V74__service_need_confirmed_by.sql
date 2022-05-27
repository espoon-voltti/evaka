ALTER TABLE new_service_need
    ADD COLUMN confirmed_by uuid NOT NULL REFERENCES employee(id);

ALTER TABLE new_service_need
    ADD COLUMN confirmed_at timestamp with time zone;
