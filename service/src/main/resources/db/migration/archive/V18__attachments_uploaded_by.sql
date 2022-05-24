ALTER TABLE attachment
    ADD COLUMN uploaded_by_person uuid REFERENCES person(id),
    ADD COLUMN uploaded_by_employee uuid REFERENCES employee(id),
    ADD CONSTRAINT uploaded_by CHECK (uploaded_by_person IS NOT NULL OR uploaded_by_employee IS NOT NULL);
