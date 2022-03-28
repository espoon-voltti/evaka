ALTER TABLE message_account ADD COLUMN evaka_user_id uuid UNIQUE REFERENCES evaka_user (id);
UPDATE message_account SET evaka_user_id = coalesce(person_id, employee_id);
UPDATE message_account SET employee_id = NULL WHERE employee_id IS NOT NULL AND NOT active;
ALTER TABLE message_account DROP COLUMN active;

ALTER TABLE message_account
    DROP CONSTRAINT message_account_person_id_fkey,
    ADD CONSTRAINT message_account_person_id_fkey FOREIGN KEY (person_id) REFERENCES person (id) ON DELETE SET NULL,
    DROP CONSTRAINT message_account_created_for_fk,
    ADD CONSTRAINT message_account_owner_fk CHECK (
        num_nonnulls(daycare_group_id, employee_id, person_id) IN (0, 1) AND
        CASE
            WHEN daycare_group_id IS NOT NULL THEN evaka_user_id IS NULL
            ELSE evaka_user_id IS NOT NULL
        END
    );
