ALTER TABLE fee_decision ADD COLUMN total_fee integer;

UPDATE fee_decision SET total_fee = (
    SELECT coalesce(sum(fee_decision_child.final_fee), 0) sum
    FROM fee_decision_child
    WHERE fee_decision_id = fee_decision.id
);

ALTER TABLE fee_decision ALTER COLUMN total_fee SET NOT NULL;