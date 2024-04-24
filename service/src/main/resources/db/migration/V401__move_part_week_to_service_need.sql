ALTER TABLE service_need ADD COLUMN part_week boolean NOT NULL DEFAULT FALSE;
ALTER TABLE service_need ALTER COLUMN part_week DROP DEFAULT;

UPDATE service_need sn
SET part_week = updates.part_week
FROM (
    SELECT sn.id, sno.part_week
    FROM service_need sn
    JOIN service_need_option sno ON sn.option_id = sno.id
    WHERE sno.part_week IS TRUE
) AS updates
WHERE sn.id = updates.id;

ALTER TABLE service_need_option ALTER COLUMN part_week DROP NOT NULL;
