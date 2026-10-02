ALTER TABLE child
    ADD COLUMN messages_ready_for_deletion_at timestamp with time zone;

COMMENT ON COLUMN child.messages_ready_for_deletion_at IS
    'When the data retention run of the child first found its messages to be the only thing still keeping the child, after which the message removal may delete them';
