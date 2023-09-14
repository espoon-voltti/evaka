DROP VIEW IF EXISTS application_view;

ALTER TABLE application
    ADD COLUMN document     jsonb default '{}'                     NOT NULL,
    ADD COLUMN form_modified timestamp with time zone default now() NOT NULL;

UPDATE application a SET
    document = (SELECT document FROM application_form af WHERE a.id = af.application_id AND af.latest = true),
    form_modified = (SELECT created FROM application_form af WHERE a.id = af.application_id AND af.latest = true);

DROP TABLE application_form;

create index idx$application_doc
    on application using gin (document jsonb_path_ops);
