UPDATE person SET social_security_number = NULL WHERE social_security_number = '';
ALTER TABLE person ADD CONSTRAINT person_ssn_not_empty CHECK (social_security_number <> '');

UPDATE employee SET social_security_number = NULL WHERE social_security_number = '';
ALTER TABLE employee ADD CONSTRAINT employee_ssn_not_empty CHECK (social_security_number <> '');
