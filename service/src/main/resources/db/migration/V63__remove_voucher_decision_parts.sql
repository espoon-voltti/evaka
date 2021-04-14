ALTER TABLE voucher_value_decision
    DROP CONSTRAINT exclude$voucher_value_decision_no_overlapping_draft,
    DROP CONSTRAINT exclude$voucher_value_decision_no_overlapping_sent;

ALTER TABLE voucher_value_decision
    ADD COLUMN child uuid NOT NULL REFERENCES person (id),
    ADD COLUMN date_of_birth date NOT NULL,
    ADD COLUMN base_co_payment int NOT NULL,
    ADD COLUMN sibling_discount int NOT NULL,
    ADD COLUMN placement_unit uuid NOT NULL REFERENCES daycare (id),
    ADD COLUMN placement_type text NOT NULL,
    ADD COLUMN service_need text NOT NULL,
    ADD COLUMN co_payment int NOT NULL,
    ADD COLUMN fee_alterations jsonb NOT NULL,
    ADD COLUMN hours_per_week numeric(4,2),
    ADD COLUMN base_value int NOT NULL,
    ADD COLUMN age_coefficient int NOT NULL,
    ADD COLUMN service_coefficient int NOT NULL,
    ADD COLUMN voucher_value int NOT NULL,
    ADD CONSTRAINT exclude$voucher_value_decision_no_overlapping_draft EXCLUDE USING gist (child WITH =, daterange(valid_from, valid_to, '[]') WITH &&) WHERE (status = 'DRAFT'),
    ADD CONSTRAINT exclude$voucher_value_decision_no_overlapping_sent EXCLUDE USING gist (child WITH =, daterange(valid_from, valid_to, '[]') WITH &&) WHERE (status = ANY('{SENT,WAITING_FOR_SENDING,WAITING_FOR_MANUAL_SENDING}'::voucher_value_decision_status[]));

CREATE INDEX idx$voucher_value_decision_child ON voucher_value_decision (child);
CREATE INDEX idx$voucher_value_decision_placement_unit ON voucher_value_decision (placement_unit);

ALTER TABLE voucher_value_report_decision_part RENAME TO voucher_value_report_decision;
ALTER TABLE voucher_value_report_decision
    ADD COLUMN decision_id uuid NOT NULL REFERENCES voucher_value_decision (id),
    DROP COLUMN decision_part_id;

CREATE INDEX idx$voucher_value_report_decision ON voucher_value_report_decision (decision_id);

DROP TABLE voucher_value_decision_part;
