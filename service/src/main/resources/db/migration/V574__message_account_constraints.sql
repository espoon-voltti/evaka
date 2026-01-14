CREATE UNIQUE INDEX only_one_municipal_account ON message_account (type) WHERE type = 'MUNICIPAL';

ALTER TABLE message_account ADD CONSTRAINT check$message_account_foreign_keys CHECK (
    (type = 'PERSONAL' AND employee_id IS NOT NULL AND daycare_group_id IS NULL AND person_id IS NULL) OR
    (type = 'GROUP' AND employee_id IS NULL AND daycare_group_id IS NOT NULL AND person_id IS NULL) OR
    (type = 'CITIZEN' AND employee_id IS NULL AND daycare_group_id IS NULL AND person_id IS NOT NULL) OR
    (type <> 'PERSONAL' AND type <> 'GROUP' AND type <> 'CITIZEN' AND employee_id IS NULL AND daycare_group_id IS NULL AND person_id IS NULL)
);
ALTER TABLE message_account DROP CONSTRAINT message_account_created_for_fk;
