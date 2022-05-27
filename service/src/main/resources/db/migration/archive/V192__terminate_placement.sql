ALTER TABLE placement ADD COLUMN termination_requested_date date;
ALTER TABLE placement ADD COLUMN terminated_by uuid CONSTRAINT fk$terminated_by REFERENCES evaka_user (id);
