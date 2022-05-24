ALTER TABLE person ADD COLUMN ssn_adding_disabled boolean NOT NULL DEFAULT false;
ALTER TABLE person ADD CONSTRAINT person_disabled_ssn_no_ssn CHECK (NOT ssn_adding_disabled OR social_security_number IS NULL);
