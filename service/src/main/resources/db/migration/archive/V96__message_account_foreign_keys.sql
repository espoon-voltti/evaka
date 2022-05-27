ALTER TABLE message_account DROP CONSTRAINT message_account_created_for_fk;

-- CASCADE also drops the message_account_name_view, but it's recreated in an R__ migration
ALTER TABLE message_account DROP COLUMN deleted_owner_name CASCADE;

ALTER TABLE message_account ADD CONSTRAINT message_account_created_for_fk CHECK (
    num_nonnulls(daycare_group_id, employee_id, person_id) = 1
);
