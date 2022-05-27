CREATE TABLE voucher_value_decision (
    id uuid NOT NULL,
    status text NOT NULL,
    valid_from date,
    valid_to date,
    decision_number bigint,
    head_of_family uuid NOT NULL,
    partner uuid,
    head_of_family_income jsonb,
    partner_income jsonb,
    family_size integer NOT NULL,
    pricing jsonb NOT NULL,
    document_key text,
    created_at timestamp with time zone DEFAULT now(),
    approved_by uuid,
    approved_at timestamp with time zone,
    sent_at timestamp with time zone,
    cancelled_at timestamp with time zone
);

ALTER TABLE voucher_value_decision
    ADD CONSTRAINT pk$voucher_value_decision PRIMARY KEY (id),
    ADD CONSTRAINT exclude$voucher_value_decision_no_overlapping_draft EXCLUDE USING gist (head_of_family WITH =, daterange(valid_from, valid_to, '[]'::text) WITH &&) WHERE (status = 'DRAFT'::text),
    ADD CONSTRAINT exclude$voucher_value_decision_no_overlapping_sent EXCLUDE USING gist (head_of_family WITH =, daterange(valid_from, valid_to, '[]'::text) WITH &&) WHERE (status = ANY (ARRAY['SENT'::text, 'WAITING_FOR_SENDING'::text, 'WAITING_FOR_MANUAL_SENDING'::text])),
    ADD CONSTRAINT fk$head_of_family FOREIGN KEY (head_of_family) REFERENCES person(id) ON UPDATE CASCADE ON DELETE CASCADE,
    ADD CONSTRAINT fk$partner FOREIGN KEY (partner) REFERENCES person(id) ON UPDATE CASCADE ON DELETE CASCADE,
    ADD CONSTRAINT fk$approved_by FOREIGN KEY (approved_by) REFERENCES employee(id) ON UPDATE CASCADE ON DELETE CASCADE;

CREATE TABLE voucher_value_decision_part (
    id uuid NOT NULL,
    voucher_value_decision_id uuid NOT NULL,
    child uuid NOT NULL,
    date_of_birth date NOT NULL,
    base_co_payment integer NOT NULL,
    sibling_discount integer NOT NULL,
    placement_unit uuid NOT NULL,
    placement_type text NOT NULL,
    service_need text NOT NULL,
    co_payment integer NOT NULL,
    fee_alterations jsonb NOT NULL
);

ALTER TABLE voucher_value_decision_part
    ADD CONSTRAINT pk$voucher_value_decision_part PRIMARY KEY (id),
    ADD CONSTRAINT fk$voucher_value_decision FOREIGN KEY (voucher_value_decision_id) REFERENCES voucher_value_decision(id) ON UPDATE CASCADE ON DELETE CASCADE,
    ADD CONSTRAINT fk$child FOREIGN KEY (child) REFERENCES person(id) ON UPDATE CASCADE ON DELETE CASCADE,
    ADD CONSTRAINT fk$placement_unit FOREIGN KEY (placement_unit) REFERENCES daycare(id) ON UPDATE CASCADE ON DELETE CASCADE;

CREATE SEQUENCE voucher_value_decision_number_sequence;
