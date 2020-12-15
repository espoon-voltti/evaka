ALTER TABLE daycare ADD COLUMN finance_decision_manager uuid;
ALTER TABLE daycare ADD FOREIGN KEY (finance_decision_manager)
REFERENCES employee(id) ON DELETE SET null;
