CREATE TYPE email_message_type AS ENUM (
    'TRANSACTIONAL',
    'MESSAGE_NOTIFICATION',
    'BULLETIN_NOTIFICATION',
    'BULLETIN_FROM_SUPERVISOR_NOTIFICATION',
    'OUTDATED_INCOME_NOTIFICATION',
    'NEW_CUSTOMER_INCOME_NOTIFICATION',
    'CALENDAR_EVENT_NOTIFICATION',
    'DECISION_NOTIFICATION',
    'DOCUMENT_NOTIFICATION',
    'INFORMAL_DOCUMENT_NOTIFICATION',
    'MISSING_ATTENDANCE_RESERVATION_NOTIFICATION',
    'MISSING_HOLIDAY_ATTENDANCE_RESERVATION_NOTIFICATION',
    'DISCUSSION_TIME_RESERVATION_CONFIRMATION',
    'DISCUSSION_SURVEY_CREATION_NOTIFICATION',
    'DISCUSSION_TIME_RESERVATION_REMINDER'
);

-- person table already has many defaults to make inserts easier, so this default may also remain
ALTER TABLE person ADD COLUMN disabled_email_types email_message_type[] NOT NULL DEFAULT '{}'::email_message_type[];

UPDATE person
SET disabled_email_types = (
    -- all types (that were possible to enable) minus types that were enabled
    SELECT array_agg(e)
    FROM unnest(
        ARRAY[
            'MESSAGE_NOTIFICATION',
            'BULLETIN_NOTIFICATION',
            'OUTDATED_INCOME_NOTIFICATION',
            'CALENDAR_EVENT_NOTIFICATION',
            'DECISION_NOTIFICATION',
            'DOCUMENT_NOTIFICATION',
            'INFORMAL_DOCUMENT_NOTIFICATION',
            'MISSING_ATTENDANCE_RESERVATION_NOTIFICATION',
            'DISCUSSION_TIME_RESERVATION_CONFIRMATION',
            'DISCUSSION_SURVEY_CREATION_NOTIFICATION',
            'DISCUSSION_TIME_RESERVATION_REMINDER'
        ]::email_message_type[]
    ) e
    WHERE e != ALL(enabled_email_types::email_message_type[])
)
WHERE enabled_email_types IS NOT NULL;

ALTER TABLE person DROP COLUMN enabled_email_types;

-- if the user has disabled BULLETIN_NOTIFICATION and MESSAGE_NOTIFICATION
-- then let's also disable the new BULLETIN_FROM_SUPERVISOR_NOTIFICATION by default
UPDATE person
SET disabled_email_types = array_append(disabled_email_types, 'BULLETIN_FROM_SUPERVISOR_NOTIFICATION')
WHERE 'BULLETIN_NOTIFICATION' = ANY(disabled_email_types) AND 'MESSAGE_NOTIFICATION' = ANY(disabled_email_types);
