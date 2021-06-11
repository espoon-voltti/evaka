-- drop these backups later if not needed
ALTER TABLE service_need RENAME TO old_service_need;
ALTER TABLE fee_decision RENAME TO old_fee_decision;
ALTER TABLE fee_decision_part RENAME TO old_fee_decision_part;

ALTER TABLE new_service_need RENAME TO service_need;
ALTER TABLE new_fee_decision RENAME TO fee_decision;
ALTER TABLE new_fee_decision_child RENAME TO fee_decision_child;

-- temporary redirect views to avoid issue during deployment
CREATE VIEW new_service_need AS (
    SELECT * FROM service_need
);
CREATE VIEW new_fee_decision AS (
    SELECT * FROM fee_decision
);
CREATE VIEW new_fee_decision_child AS (
    SELECT * FROM fee_decision_child
);
