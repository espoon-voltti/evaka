ALTER TABLE fee_decision ADD COLUMN process_id uuid REFERENCES archived_process(id);
CREATE UNIQUE INDEX fk$fee_decision_process_id ON fee_decision(process_id);

ALTER TABLE voucher_value_decision ADD COLUMN process_id uuid REFERENCES archived_process(id);
CREATE UNIQUE INDEX fk$voucher_value_decision_process_id ON voucher_value_decision(process_id);
