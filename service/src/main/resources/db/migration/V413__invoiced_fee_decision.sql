CREATE TABLE invoiced_fee_decision (
    invoice_id uuid REFERENCES invoice(id) ON DELETE CASCADE,
    fee_decision_id uuid REFERENCES fee_decision(id) ON DELETE CASCADE
);

ALTER TABLE invoiced_fee_decision ADD PRIMARY KEY (invoice_id, fee_decision_id);
CREATE INDEX fk$invoiced_fee_decision_fee_decision_id ON invoiced_fee_decision (fee_decision_id);
