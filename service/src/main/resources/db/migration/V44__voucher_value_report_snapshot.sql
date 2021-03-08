CREATE TABLE voucher_value_report_snapshot (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now(),
    month int NOT NULL,
    year int NOT NULL
);

CREATE TABLE voucher_value_report_decision_part (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    voucher_value_report_snapshot_id uuid NOT NULL REFERENCES voucher_value_report_snapshot (id),
    decision_part_id uuid NOT NULL REFERENCES voucher_value_decision_part (id),
    realized_amount int NOT NULL,
    realized_period daterange NOT NULL
);

CREATE INDEX idx$voucher_value_report_decision_part_voucher_value_report_snapshot_id ON voucher_value_report_decision_part (voucher_value_report_snapshot_id);
CREATE INDEX idx$voucher_value_report_decision_part_decision_part_id ON voucher_value_report_decision_part (decision_part_id);
