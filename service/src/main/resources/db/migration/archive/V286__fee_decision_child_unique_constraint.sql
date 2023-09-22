ALTER TABLE fee_decision_child ADD CONSTRAINT unique_child_decision_pair UNIQUE (fee_decision_id, child_id);
