ALTER TABLE voucher_value_decision ADD COLUMN service_need_missing BOOLEAN;
UPDATE voucher_value_decision SET service_need_missing = FALSE;
UPDATE voucher_value_decision SET service_need_missing = TRUE
WHERE status = 'DRAFT' AND NOT EXISTS (
    SELECT 1
    FROM placement
    JOIN service_need ON service_need.placement_id = placement.id
        AND voucher_value_decision.valid_from BETWEEN service_need.start_date AND service_need.end_date
    WHERE placement.child_id = voucher_value_decision.child_id
        AND voucher_value_decision.valid_from BETWEEN placement.start_date AND placement.end_date
);
ALTER TABLE voucher_value_decision ALTER COLUMN service_need_missing SET NOT NULL;
