ALTER TABLE message
    DROP CONSTRAINT message_thread_id_fkey,
    ADD CONSTRAINT message_thread_id_fkey
        FOREIGN KEY (thread_id) REFERENCES message_thread(id) ON DELETE CASCADE,
    DROP CONSTRAINT message_content_id_fkey,
    ADD CONSTRAINT message_content_id_fkey
        FOREIGN KEY (content_id) REFERENCES message_content(id) ON DELETE CASCADE;

ALTER TABLE message_recipients
    DROP CONSTRAINT message_recipients_message_id_fkey,
    ADD CONSTRAINT message_recipients_message_id_fkey
        FOREIGN KEY (message_id) REFERENCES message(id) ON DELETE CASCADE;

ALTER TABLE message_thread_children
    DROP CONSTRAINT message_thread_children_thread_id_fkey,
    ADD CONSTRAINT message_thread_children_thread_id_fkey
        FOREIGN KEY (thread_id) REFERENCES message_thread(id) ON DELETE CASCADE;
