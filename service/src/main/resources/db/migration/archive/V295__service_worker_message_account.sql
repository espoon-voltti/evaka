ALTER TABLE message_account DROP COLUMN type CASCADE;
ALTER TABLE message_account ADD COLUMN type message_account_type;

CREATE UNIQUE INDEX only_one_service_worker_account ON message_account (type) WHERE type = 'SERVICE_WORKER';

UPDATE message_account SET type = CASE
    WHEN employee_id IS NOT NULL THEN 'PERSONAL'::message_account_type
    WHEN daycare_group_id IS NOT NULL THEN 'GROUP'::message_account_type
    WHEN person_id IS NOT NULL THEN 'CITIZEN'::message_account_type
    ELSE 'MUNICIPAL'::message_account_type
END;

INSERT INTO message_account (employee_id, daycare_group_id, person_id, type) VALUES (NULL, NULL, NULL, 'SERVICE_WORKER');
