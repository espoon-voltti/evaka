ALTER TABLE child_consent
    DROP CONSTRAINT child_consent_given_by_guardian_fkey,
    ADD CONSTRAINT child_consent_given_by_guardian_fkey FOREIGN KEY (given_by_guardian) REFERENCES person (id) ON DELETE CASCADE;
