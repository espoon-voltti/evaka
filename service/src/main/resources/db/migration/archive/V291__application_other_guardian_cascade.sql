ALTER TABLE application_other_guardian
    DROP CONSTRAINT application_other_guardian_application_id_fkey,
    DROP CONSTRAINT application_other_guardian_guardian_id_fkey;

ALTER TABLE application_other_guardian
    ADD CONSTRAINT fk$application FOREIGN KEY (application_id) REFERENCES application (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk$guardian FOREIGN KEY (guardian_id) REFERENCES person (id) ON DELETE CASCADE;
