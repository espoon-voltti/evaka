CREATE TYPE email_message_type AS ENUM (
    'TRANSACTIONAL',
    'MESSAGE_NOTIFICATION',
    'BULLETIN_NOTIFICATION',
    'INCOME_NOTIFICATION',
    'CALENDAR_EVENT_NOTIFICATION',
    'DECISION_NOTIFICATION',
    'DOCUMENT_NOTIFICATION',
    'INFORMAL_DOCUMENT_NOTIFICATION',
    'ATTENDANCE_RESERVATION_NOTIFICATION',
    'DISCUSSION_TIME_NOTIFICATION'
);

-- person table already has many defaults to make inserts easier, so this default may also remain
ALTER TABLE person ADD COLUMN disabled_email_types email_message_type[] NOT NULL DEFAULT '{}'::email_message_type[];

UPDATE person
SET disabled_email_types = array_remove(
    ARRAY [
        CASE
            WHEN 'MESSAGE_NOTIFICATION' = ANY(enabled_email_types)
            THEN NULL::email_message_type
            ELSE 'MESSAGE_NOTIFICATION'::email_message_type
        END,
        CASE
            WHEN 'BULLETIN_NOTIFICATION' = ANY(enabled_email_types)
            THEN NULL::email_message_type
            ELSE 'BULLETIN_NOTIFICATION'::email_message_type
        END,
        CASE
            WHEN '{OUTDATED_INCOME_NOTIFICATION,NEW_CUSTOMER_INCOME_NOTIFICATION}'::text[] && enabled_email_types
            THEN NULL::email_message_type
            ELSE 'INCOME_NOTIFICATION'::email_message_type
        END,
        CASE
            WHEN 'CALENDAR_EVENT_NOTIFICATION' = ANY(enabled_email_types)
            THEN NULL::email_message_type
            ELSE 'CALENDAR_EVENT_NOTIFICATION'::email_message_type
        END,
        CASE
            WHEN 'DECISION_NOTIFICATION' = ANY(enabled_email_types)
            THEN NULL::email_message_type
            ELSE 'DECISION_NOTIFICATION'::email_message_type
        END,
        CASE
            WHEN 'DOCUMENT_NOTIFICATION' = ANY(enabled_email_types)
            THEN NULL::email_message_type
            ELSE 'DOCUMENT_NOTIFICATION'::email_message_type
        END,
        CASE
            WHEN 'INFORMAL_DOCUMENT_NOTIFICATION' = ANY(enabled_email_types)
            THEN NULL::email_message_type
            ELSE 'INFORMAL_DOCUMENT_NOTIFICATION'::email_message_type
        END,
        CASE
            WHEN '{MISSING_ATTENDANCE_RESERVATION_NOTIFICATION,MISSING_HOLIDAY_ATTENDANCE_RESERVATION_NOTIFICATION}'::text[] && enabled_email_types
            THEN NULL::email_message_type
            ELSE 'ATTENDANCE_RESERVATION_NOTIFICATION'::email_message_type
        END,
        CASE
            WHEN '{DISCUSSION_TIME_RESERVATION_CONFIRMATION,DISCUSSION_SURVEY_CREATION_NOTIFICATION,DISCUSSION_TIME_RESERVATION_REMINDER}'::text[] && enabled_email_types
            THEN NULL::email_message_type
            ELSE 'DISCUSSION_TIME_NOTIFICATION'::email_message_type
        END
    ],
    NULL::email_message_type
)
WHERE enabled_email_types IS NOT NULL;

-- todo in next PR: ALTER TABLE person DROP COLUMN enabled_email_types;
