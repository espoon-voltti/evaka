ALTER TABLE message_draft
    RENAME recipients TO recipient_account_ids;

ALTER TABLE message_draft
    ADD COLUMN recipient_names text[],
    ADD COLUMN type message_type;
