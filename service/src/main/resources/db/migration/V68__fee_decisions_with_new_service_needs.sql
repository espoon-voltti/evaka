ALTER TABLE service_need_option ADD COLUMN default_option bool NOT NULL DEFAULT false;

CREATE UNIQUE INDEX service_need_option_one_default_per_placement_type ON service_need_option (valid_placement_type) WHERE default_option;

CREATE TABLE new_fee_decision (
    id uuid PRIMARY KEY,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,

    status fee_decision_status NOT NULL,
    valid_during daterange NOT NULL,
    decision_type fee_decision_type NOT NULL DEFAULT 'NORMAL'::fee_decision_type,
    head_of_family_id uuid NOT NULL REFERENCES person (id),
    head_of_family_income jsonb,
    partner_id uuid REFERENCES person (id),
    partner_income jsonb,
    family_size integer NOT NULL,
    pricing jsonb NOT NULL,
    decision_number bigint,
    document_key text,
    approved_at timestamp with time zone,
    approved_by_id uuid REFERENCES employee (id),
    decision_handler_id uuid REFERENCES employee (id),
    sent_at timestamp with time zone,
    cancelled_at timestamp with time zone,

    CONSTRAINT exclude$fee_decision_no_overlapping_draft EXCLUDE USING gist (head_of_family_id WITH =, valid_during WITH &&) WHERE (status = 'DRAFT'::fee_decision_status),
    CONSTRAINT exclude$fee_decision_no_overlapping_sent EXCLUDE USING gist (head_of_family_id WITH =, valid_during WITH &&) WHERE (status = ANY ('{SENT,WAITING_FOR_SENDING,WAITING_FOR_MANUAL_SENDING}'::fee_decision_status[]))
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON new_fee_decision FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX idx$fee_decision_status ON new_fee_decision (status);
CREATE INDEX idx$fee_decision_valid_during ON new_fee_decision (valid_during);
CREATE INDEX idx$fee_decision_head_of_family_id ON new_fee_decision (head_of_family_id);
CREATE INDEX idx$fee_decision_partner_id ON new_fee_decision (partner_id);
CREATE INDEX idx$fee_decision_decision_handler_id ON new_fee_decision (decision_handler_id);

CREATE TABLE new_fee_decision_child (
    id uuid PRIMARY KEY,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,

    fee_decision_id uuid NOT NULL REFERENCES new_fee_decision (id) ON DELETE CASCADE,
    child_id uuid NOT NULL REFERENCES person (id),
    child_date_of_birth date NOT NULL,
    sibling_discount integer NOT NULL,
    placement_unit_id uuid NOT NULL REFERENCES daycare (id),
    placement_type placement_type NOT NULL,
    service_need_name text NOT NULL,
    service_need_fee_coefficient numeric(4,2) NOT NULL,
    base_fee integer NOT NULL,
    fee integer NOT NULL,
    fee_alterations jsonb NOT NULL,
    final_fee integer NOT NULL
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON new_fee_decision_child FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX idx$fee_decision_child_fee_decision_id ON new_fee_decision_child (fee_decision_id);
CREATE INDEX idx$fee_decision_child_child_id ON new_fee_decision_child (child_id);
CREATE INDEX idx$fee_decision_child_placement_unit_id ON new_fee_decision_child (placement_unit_id);
