-- These queries may be a bit slow, depending on the size of message and message_recipients tables

-- Ensure consistent contents in message_thread_participant table by disallowing writes to the source tables
LOCK TABLE message IN SHARE MODE;
LOCK TABLE message_recipients IN SHARE MODE;

INSERT INTO message_thread_participant (thread_id, participant_id, last_message_timestamp, last_sent_timestamp)
SELECT thread_id, sender_id, max(sent_at), max(sent_at)
FROM message
GROUP BY thread_id, sender_id;

INSERT INTO message_thread_participant AS tp (thread_id, participant_id, last_message_timestamp, last_received_timestamp)
SELECT m.thread_id, mr.recipient_id, max(m.sent_at), max(m.sent_at)
FROM message_recipients mr
JOIN message m ON m.id = mr.message_id
GROUP BY m.thread_id, mr.recipient_id
ON CONFLICT (thread_id, participant_id) DO UPDATE SET
    last_message_timestamp = greatest(EXCLUDED.last_message_timestamp, tp.last_message_timestamp),
    last_received_timestamp = EXCLUDED.last_received_timestamp;
