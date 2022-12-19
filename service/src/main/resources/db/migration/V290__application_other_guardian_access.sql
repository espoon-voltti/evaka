ALTER TABLE application ADD COLUMN allow_other_guardian_access bool;
UPDATE application SET allow_other_guardian_access = false;
ALTER TABLE application ALTER COLUMN allow_other_guardian_access SET NOT NULL;

CREATE TABLE application_other_guardian (
    application_id uuid NOT NULL REFERENCES application (id),
    guardian_id uuid NOT NULL REFERENCES person (id),
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone NOT NULL DEFAULT now(),

    PRIMARY KEY (application_id, guardian_id)
);

CREATE INDEX idx$application_other_guardian_guardian ON application_other_guardian (guardian_id) INCLUDE (application_id);