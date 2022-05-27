ALTER TABLE service_need_option ADD COLUMN contract_days_per_month int;
ALTER TABLE fee_decision_child ADD COLUMN service_need_contract_days_per_month int;
