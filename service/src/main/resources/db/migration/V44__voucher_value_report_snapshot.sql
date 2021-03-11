CREATE TYPE voucher_report_row_type AS ENUM ('ORIGINAL', 'REFUND', 'CORRECTION');

CREATE TABLE voucher_value_report_snapshot (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT now(),
    month int NOT NULL,
    year int NOT NULL
);

CREATE UNIQUE INDEX idx$voucher_value_report_snapshot_time ON voucher_value_report_snapshot (year, month);

CREATE TABLE voucher_value_report_decision_part (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    voucher_value_report_snapshot_id uuid NOT NULL REFERENCES voucher_value_report_snapshot (id),
    decision_part_id uuid NOT NULL REFERENCES voucher_value_decision_part (id),
    realized_amount int NOT NULL,
    realized_period daterange NOT NULL,
    type voucher_report_row_type

    CONSTRAINT negative_refund_else_positive CHECK ((type = 'REFUND' AND realized_amount <= 0) OR realized_amount >= 0)
    CONSTRAINT single_month_periods CHECK (
        extract(year from lower(realized_period)) = extract(year from (upper(realized_period) - interval '1 day')) AND
        extract(month from lower(realized_period)) = extract(month from (upper(realized_period) - interval '1 day'))
    )
);

CREATE INDEX idx$voucher_value_report_decision_part_snapshot_id ON voucher_value_report_decision_part (voucher_value_report_snapshot_id);
CREATE INDEX idx$voucher_value_report_decision_part_decision_part_id ON voucher_value_report_decision_part (decision_part_id);
CREATE INDEX idx$voucher_value_report_decision_part_realized_period ON voucher_value_report_decision_part (realized_period);
