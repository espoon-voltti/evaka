CREATE UNIQUE INDEX only_one_finance_account ON message_account (type) WHERE type = 'FINANCE';

INSERT INTO message_account (employee_id, daycare_group_id, person_id, type) VALUES (NULL, NULL, NULL, 'FINANCE');
