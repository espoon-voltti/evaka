-- SPDX-FileCopyrightText: 2023-2025 Tampere region
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

-- Usage: \copy (SELECT * FROM message_export((SELECT id FROM message_account WHERE person_id = '82c0d479-c64b-4214-bbed-e2d0d5022ba3'), daterange('2022-08-01', null))) TO 'messages.csv' DELIMITER ',' CSV HEADER;

DROP FUNCTION IF EXISTS message_export(message_account_id uuid, period daterange);
DROP FUNCTION IF EXISTS message_account_name(type message_account_type, name text);

CREATE FUNCTION message_account_name(type message_account_type, name text) RETURNS text
    LANGUAGE SQL
RETURN CASE type
           WHEN 'MUNICIPAL' THEN 'Tampereen kaupunki'
           WHEN 'SERVICE_WORKER' THEN 'Varhaiskasvatuksen ja esiopetuksen asiakaspalvelu'
           WHEN 'FINANCE' THEN 'Tampereen varhaiskasvatuksen asiakasmaksut'
           ELSE name
    END;

CREATE FUNCTION message_export(message_account_id uuid, period daterange)
    RETURNS TABLE
            (
                otsikko        text,
                lahetysaika    timestamp without time zone,
                lahettaja      text,
                vastaanottajat text,
                sisalto        text
            )
AS
$$
SELECT message_thread.title                                                     AS otsikko,
       message.sent_at AT TIME ZONE 'Europe/Helsinki'                           AS lahetysaika,
       message_account_name(sender_account_view.type, sender_account_view.name) AS lahettaja,
       string_agg(message_account_name(recipient_account_view.type, recipient_account_view.name), ', '
                  ORDER BY recipient_account_view.name)                         AS vastaanottajat,
       message_content.content                                                  AS sisalto
FROM message
         JOIN message_thread ON message.thread_id = message_thread.id
         JOIN message_content ON message.content_id = message_content.id
         JOIN message_thread_participant ON message_thread.id = message_thread_participant.thread_id
         JOIN message_account_view sender_account_view ON message.sender_id = sender_account_view.id
         LEFT JOIN message_recipients ON message.id = message_recipients.message_id
         LEFT JOIN message_account_view recipient_account_view
                   ON message_recipients.recipient_id = recipient_account_view.id
WHERE message_thread_participant.participant_id = $1
  AND (message.sender_id = $1 OR
       EXISTS (SELECT FROM message_recipients mr WHERE mr.message_id = message.id AND mr.recipient_id = $1))
  AND $2 @> (message.sent_at AT TIME ZONE 'Europe/Helsinki')::date
GROUP BY 1, 2, 3, 5
ORDER BY 1, 2;
$$
    LANGUAGE SQL;
