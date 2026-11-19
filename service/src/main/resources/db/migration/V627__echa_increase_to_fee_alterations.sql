-- Fee decisions got a 93 € increase for every child whose head of family or partner had an income marked as working
-- at ECHA. The increases become fee alterations covering exactly the decisions that have one, so regenerating those
-- decisions gives the same result.
INSERT INTO fee_alteration (id, person_id, type, amount, is_absolute, valid_from, valid_to, notes, modified_by, modified_at)
SELECT
    ext.uuid_generate_v1mc(),
    child_id,
    'INCREASE',
    93,
    true,
    lower(echa_range),
    upper(echa_range) - 1,
    'ECHA',
    '00000000-0000-0000-0000-000000000000',
    now()
FROM (
    SELECT fdc.child_id, unnest(range_agg(fd.valid_during)) AS echa_range
    FROM fee_decision fd
    JOIN fee_decision_child fdc ON fdc.fee_decision_id = fd.id
    WHERE fd.status IN ('SENT', 'WAITING_FOR_SENDING', 'WAITING_FOR_MANUAL_SENDING')
      AND (fd.head_of_family_income ->> 'worksAtECHA' = 'true' OR fd.partner_income ->> 'worksAtECHA' = 'true')
      AND fdc.fee_alterations @> '[{"type": "INCREASE", "amount": 93, "isAbsolute": true}]'
    GROUP BY fdc.child_id
) echa_child;

ALTER TABLE income DROP COLUMN works_at_echa;
