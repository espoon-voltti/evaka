ALTER TABLE message_recipients
    DROP CONSTRAINT message_recipients_recipient_id_fkey,
    ADD CONSTRAINT message_recipients_recipient_id_fkey FOREIGN KEY (recipient_id) REFERENCES message_account (id) ON DELETE CASCADE;
