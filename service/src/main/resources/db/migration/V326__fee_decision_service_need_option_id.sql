ALTER TABLE fee_decision_child ADD COLUMN service_need_option_id uuid CONSTRAINT fk$service_need_option_id REFERENCES service_need_option (id);

CREATE INDEX idx$fee_decision_child_service_need_option ON fee_decision_child (service_need_option_id);
