CREATE TABLE sfi_get_events_continuation_token(
    continuation_token text PRIMARY KEY,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE sfi_message (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(), -- external id sent to SFI
    sfi_id integer, -- id returned by SFI
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    guardian_id uuid NOT NULL REFERENCES person(id),
    decision_id uuid REFERENCES decision(id),
    document_id uuid REFERENCES child_document(id),
    fee_decision_id uuid REFERENCES fee_decision(id),
    voucher_value_decision_id uuid REFERENCES voucher_value_decision(id)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON sfi_message FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

ALTER TABLE sfi_message ADD CONSTRAINT single_type CHECK (
    num_nonnulls(decision_id, document_id, fee_decision_id, voucher_value_decision_id) = 1
);

CREATE INDEX fk$sfi_message_guardian_id ON sfi_message (guardian_id);
CREATE UNIQUE INDEX fk$sfi_message_decision_id_guardian_id ON sfi_message (decision_id, guardian_id) WHERE decision_id IS NOT NULL;
CREATE UNIQUE INDEX fk$sfi_message_document_id_guardian_id ON sfi_message (document_id, guardian_id) WHERE document_id IS NOT NULL;
CREATE UNIQUE INDEX fk$sfi_message_fee_decision_id_guardian_id ON sfi_message (fee_decision_id, guardian_id) WHERE fee_decision_id IS NOT NULL;
CREATE UNIQUE INDEX fk$sfi_message_voucher_value_decision_id_guardian_id ON sfi_message (voucher_value_decision_id, guardian_id) WHERE voucher_value_decision_id IS NOT NULL;

CREATE TYPE sfi_message_event_type AS ENUM (
    'ELECTRONIC_MESSAGE_CREATED',
    'SENT_FOR_PRINTING_AND_ENVELOPING'
    'ELECTRONIC_MESSAGE_READ',
    'ELECTRONIC_MESSAGE_FROM_END_USER',
    'RECEIPT_CONFIRMED',
    'PAPER_MAIL_CREATED',
    'POSTI_RECEIPT_CONFIRMED',
    'POSTI_RETURNED_TO_SENDER',
    'POSTI_UNRESOLVED'
);

CREATE TABLE sfi_message_event (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    message_id uuid NOT NULL REFERENCES sfi_message(id),
    event_type sfi_message_event_type NOT NULL,
    CONSTRAINT sfi_message_event_unique_event_per_message UNIQUE (message_id, event_type)
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON sfi_message_event FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

CREATE INDEX fk$sfi_message_event_message_id ON sfi_message_event (message_id);