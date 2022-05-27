DROP VIEW IF EXISTS application_view;

ALTER TABLE application ADD COLUMN status application_status_type;

UPDATE application
    SET status = (CASE
        WHEN application_status = 1 THEN 'CREATED'::application_status_type
        WHEN application_status = 2 THEN 'SENT'::application_status_type
        WHEN application_status = 3 THEN 'WAITING_PLACEMENT'::application_status_type
        WHEN application_status = 7 THEN 'WAITING_CONFIRMATION'::application_status_type
        WHEN application_status = 10 THEN 'REJECTED'::application_status_type
        WHEN application_status = 11 THEN 'ACTIVE'::application_status_type
        WHEN application_status = 14 THEN 'WAITING_DECISION'::application_status_type
        WHEN application_status = 15 THEN 'WAITING_MAILING'::application_status_type
        WHEN application_status = 16 THEN 'WAITING_UNIT_CONFIRMATION'::application_status_type
        WHEN application_status = 300 THEN 'CANCELLED'::application_status_type
    END);

ALTER TABLE application ALTER COLUMN status SET NOT NULL;
ALTER TABLE application DROP COLUMN application_status;
DROP TABLE application_status;
