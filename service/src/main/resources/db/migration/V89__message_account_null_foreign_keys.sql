ALTER TABLE message_account DROP CONSTRAINT message_account_created_for_fk;
ALTER TABLE message_account ADD CONSTRAINT message_account_created_for_fk CHECK (
        num_nonnulls(daycare_group_id, employee_id, person_id) <= 1
    );
