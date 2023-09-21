CREATE UNIQUE INDEX uniq$message_account_daycare_group ON message_account (daycare_group_id)
    INCLUDE (active)
    WHERE daycare_group_id IS NOT NULL;
ALTER TABLE message_account DROP CONSTRAINT message_account_daycare_group_uniq;
DROP INDEX idx$message_account_daycare_group_id;

CREATE UNIQUE INDEX uniq$message_account_employee ON message_account (employee_id)
    INCLUDE (active)
    WHERE employee_id IS NOT NULL;
ALTER TABLE message_account DROP CONSTRAINT message_account_employee_uniq;
DROP INDEX idx$message_account_employee_id;

CREATE UNIQUE INDEX uniq$message_account_person ON message_account (person_id)
    INCLUDE (active)
    WHERE person_id IS NOT NULL;
ALTER TABLE message_account DROP CONSTRAINT message_account_person_uniq;
DROP INDEX idx$message_account_person_id;
