ALTER TABLE message_account DROP CONSTRAINT message_account_created_for_fk;
ALTER TABLE message_account ADD CONSTRAINT message_account_created_for_fk CHECK (num_nonnulls(daycare_group_id, employee_id, person_id) <= 1);
ALTER TABLE message_account DROP COLUMN type CASCADE;
ALTER TABLE message_account ADD COLUMN type message_account_type
GENERATED ALWAYS AS (
    CASE
        WHEN employee_id IS NOT NULL THEN 'PERSONAL'::message_account_type
        WHEN daycare_group_id IS NOT NULL THEN 'GROUP'::message_account_type
        WHEN person_id IS NOT NULL THEN 'CITIZEN'::message_account_type
        ELSE 'MUNICIPAL'::message_account_type
    END
) STORED;
CREATE UNIQUE INDEX only_one_municipality_account ON message_account (type) WHERE type = 'MUNICIPAL';

INSERT INTO message_account (employee_id, daycare_group_id, person_id) VALUES (NULL, NULL, NULL);
