DROP VIEW IF EXISTS application_view;

ALTER TABLE application RENAME COLUMN origin TO origin_old;
ALTER TABLE application ADD COLUMN origin application_origin_type;

UPDATE application
SET origin = (CASE
    WHEN application.origin_old = 1 THEN 'ELECTRONIC'::application_origin_type
    WHEN application.origin_old = 2 THEN 'PAPER'::application_origin_type
END);

ALTER TABLE application ALTER COLUMN origin SET NOT NULL;
ALTER TABLE application DROP COLUMN origin_old;
DROP TABLE application_origin;

DROP TABLE decision_status;
ALTER TABLE decision2 RENAME TO decision;
ALTER TYPE decision2_status RENAME TO decision_status;
ALTER TYPE decision2_type RENAME TO decision_type;

DROP TABLE approval_type;
ALTER TABLE placement_plan DROP COLUMN preparatory_start_date;
ALTER TABLE placement_plan DROP COLUMN preparatory_end_date;
