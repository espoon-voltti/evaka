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
    JOIN placement pl ON pl.child_id = fc.child_id
      AND daterange(pl.start_date, pl.end_date, '[]') @> current_date
    JOIN person ch ON ch.id = fc.child_id
    WHERE daterange(fc.start_date, fc.end_date, '[]') @> current_date AND ch.date_of_birth IS NOT NULL
) data
WHERE rownum = 1;
