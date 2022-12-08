CREATE TABLE bulletin (
    id uuid PRIMARY KEY NOT NULL DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone NOT NULL DEFAULT now(),
    title text NOT NULL,
    content text NOT NULL,
    urgent boolean NOT NULL,
    sender_id uuid NOT NULL REFERENCES message_account(id),
    sender_name text NOT NULL,
    sent_at timestamp with time zone NOT NULL,
    recipient_names text[] NOT NULL
);

CREATE INDEX idx$bulletin_sender_id ON bulletin (sender_id);

CREATE TABLE bulletin_recipients (
    id uuid PRIMARY KEY NOT NULL DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone NOT NULL DEFAULT now(),
    bulletin_id uuid NOT NULL REFERENCES bulletin(id),
    recipient_id uuid NOT NULL REFERENCES message_account(id),
    read_at timestamp with time zone,
    notification_sent_at timestamp with time zone,
    folder_id uuid REFERENCES message_thread_folder(id) ON DELETE SET NULL
);

CREATE INDEX idx$bulletin_recipients_bulletin_id ON bulletin_recipients (bulletin_id);
CREATE INDEX idx$bulletin_recipients_recipient_it ON bulletin_recipients (recipient_id);
CREATE INDEX idx$bulletin_recipients_folder_id ON bulletin_recipients (folder_id);

CREATE TABLE bulletin_children (
    id uuid PRIMARY KEY NOT NULL DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone NOT NULL DEFAULT now(),
    bulletin_recipients_id uuid NOT NULL REFERENCES bulletin_recipients(id),
    child_id uuid NOT NULL REFERENCES child(id)
);

CREATE INDEX idx$bulletin_children_bulletin_recipients ON bulletin_children (bulletin_recipients_id);
CREATE INDEX idx$bulletin_children_child_id ON bulletin_children (child_id);

ALTER TABLE attachment ADD COLUMN bulletin_id uuid REFERENCES bulletin(id) ON DELETE RESTRICT;
CREATE INDEX idx$attachment_bulletin ON attachment (bulletin_id);

-- Data migration
WITH bulletin_data AS (
  SELECT c.id, c.created, c.updated, t.id AS thread_id, t.title, c.content, t.urgent, m.sender_id, m.sender_name, m.sent_at, m.recipient_names, r.recipient_id, r.read_at, r.notification_sent_at, tp.folder_id, r.created AS recipient_created, r.updated AS recipient_updated
  FROM message_thread t
  JOIN LATERAL (
    SELECT * FROM message m WHERE t.id = m.thread_id ORDER BY m.sent_at ASC LIMIT 1
  ) m ON true
  JOIN message_content c ON m.content_id = c.id
  JOIN message_recipients r ON m.id = r.message_id
  LEFT JOIN message_thread_participant tp ON t.id = tp.thread_id AND r.recipient_id = tp.participant_id
  WHERE t.message_type = 'BULLETIN'
), aggregated_bulletins AS (
    SELECT id, ARRAY_AGG(created) AS created, ARRAY_AGG(updated) AS updated, ARRAY_AGG(title) AS title, ARRAY_AGG(content) AS content, ARRAY_AGG(urgent) AS urgent, ARRAY_AGG(sender_id::text) AS sender_id, ARRAY_AGG(sender_name) AS sender_name, ARRAY_AGG(sent_at) AS sent_at, ARRAY_AGG(recipient_names) AS recipient_names
    FROM bulletin_data
    GROUP BY id
), new_bulletins AS (
    INSERT INTO bulletin (id, created, updated, title, content, urgent, sender_id, sender_name, sent_at, recipient_names)
    SELECT id, created[1], updated[1], title[1], content[1], urgent[1], sender_id[1]::uuid, sender_name[1], sent_at[1], ARRAY(SELECT UNNEST(recipient_names[1:1]))
    FROM aggregated_bulletins
), new_bulletin_recipients AS (
    INSERT INTO bulletin_recipients (bulletin_id, recipient_id, read_at, notification_sent_at, folder_id, created, updated)
    SELECT id, recipient_id, read_at, notification_sent_at, folder_id, recipient_created, recipient_updated
    FROM bulletin_data
    RETURNING id, recipient_id
)
INSERT INTO bulletin_children (bulletin_recipients_id, child_id, created, updated)
SELECT br.id, c.child_id, MIN(c.created), MIN(c.updated)
FROM bulletin_data b
JOIN new_bulletin_recipients br ON b.recipient_id = br.recipient_id
JOIN message_thread_children c ON b.thread_id = c.thread_id
GROUP BY br.id, c.child_id;
