DROP VIEW IF EXISTS primary_units_view;

CREATE VIEW primary_units_view (
    head_of_child,
    child_id,
    unit_id) AS
SELECT head_of_child, child_id, unit_id
FROM (
    SELECT
        fc.head_of_child,
        fc.child_id,
        ch.date_of_birth,
        pl.unit_id,
        row_number() OVER (PARTITION BY (head_of_child) ORDER BY fc.conflict, date_of_birth DESC, ch.id) AS rownum
    FROM fridge_child fc
    JOIN LATERAL (
        SELECT unit_id, start_date FROM placement pl
        WHERE pl.child_id = fc.child_id AND current_date <= pl.end_date
        ORDER BY start_date ASC
        LIMIT 1
    ) pl ON true
    JOIN person ch ON ch.id = fc.child_id
    WHERE daterange(fc.start_date, fc.end_date, '[]') @> greatest(current_date, pl.start_date)
) data
WHERE rownum = 1;
