ALTER TABLE message_account ADD active boolean DEFAULT true;

DROP INDEX idx$message_account_daycare_group_id;
DROP INDEX idx$message_account_employee_id;
DROP INDEX idx$message_account_person_id;

CREATE INDEX idx$message_account_daycare_group_id ON message_account (daycare_group_id, active);
CREATE INDEX idx$message_account_employee_id ON message_account (employee_id, active);
CREATE INDEX idx$message_account_person_id ON message_account (person_id, active);
