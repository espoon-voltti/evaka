ALTER TABLE daycare ADD COLUMN fee_decision_manager uuid;
ALTER TABLE daycare ADD FOREIGN KEY (fee_decision_manager)
REFERENCES employee(id) ON DELETE SET null;