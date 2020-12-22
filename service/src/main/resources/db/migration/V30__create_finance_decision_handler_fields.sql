ALTER TABLE daycare ADD COLUMN finance_decision_handler uuid REFERENCES employee(id) ON DELETE SET NULL;
ALTER TABLE fee_decision ADD COLUMN decision_handler uuid REFERENCES employee(id);
ALTER TABLE voucher_value_decision ADD COLUMN decision_handler uuid REFERENCES employee(id);
