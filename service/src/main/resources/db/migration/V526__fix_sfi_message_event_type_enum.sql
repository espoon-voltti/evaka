CREATE TYPE sfi_message_event_type_new AS ENUM (
    'ELECTRONIC_MESSAGE_CREATED',
    'ELECTRONIC_MESSAGE_FROM_END_USER',
    'ELECTRONIC_MESSAGE_READ',
    'PAPER_MAIL_CREATED',
    'POSTI_RECEIPT_CONFIRMED',
    'POSTI_RETURNED_TO_SENDER',
    'POSTI_UNRESOLVED',
    'RECEIPT_CONFIRMED',
    'SENT_FOR_PRINTING_AND_ENVELOPING'
    );

ALTER TABLE sfi_message_event
    ALTER COLUMN event_type TYPE sfi_message_event_type_new
        USING event_type::text::sfi_message_event_type_new;

-- 3. Drop the old enum type (make sure no other columns use it)
DROP TYPE sfi_message_event_type;

-- 4. Rename the new enum type to the original name
ALTER TYPE sfi_message_event_type_new RENAME TO sfi_message_event_type;