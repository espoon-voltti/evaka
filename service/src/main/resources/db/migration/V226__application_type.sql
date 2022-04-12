CREATE TYPE application_type AS ENUM ('CLUB', 'DAYCARE', 'PRESCHOOL');

LOCK TABLE application;

ALTER TABLE application ADD COLUMN type application_type;
UPDATE application SET type = (
    SELECT (document ->> 'type')::application_type
    FROM application_form
    WHERE application_id = application.id AND latest IS TRUE
);
ALTER TABLE application ALTER COLUMN type SET NOT NULL;
