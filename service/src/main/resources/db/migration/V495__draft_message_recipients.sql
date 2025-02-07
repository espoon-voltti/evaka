ALTER TABLE message_draft
    ADD COLUMN recipients jsonb DEFAULT '[]'::jsonb NOT NULL;

UPDATE message_draft
SET recipients = COALESCE(
        (SELECT jsonb_agg(jsonb_build_object('accountId', id, 'starter', false))
         FROM (SELECT unnest(recipient_ids) AS id))
    , '[]'::jsonb);

ALTER TABLE message_draft
    DROP COLUMN recipient_ids;
