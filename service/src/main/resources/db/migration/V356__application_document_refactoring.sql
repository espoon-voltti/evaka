DROP VIEW IF EXISTS application_view;

ALTER TABLE application
    ADD COLUMN document      jsonb,
    ADD COLUMN form_modified timestamp with time zone;

UPDATE application a SET
    document = (SELECT document FROM application_form af WHERE a.id = af.application_id AND af.latest = true),
    form_modified = (SELECT created FROM application_form af WHERE a.id = af.application_id AND af.latest = true);
