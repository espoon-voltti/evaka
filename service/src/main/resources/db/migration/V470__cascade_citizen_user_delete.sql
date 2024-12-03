ALTER TABLE citizen_user
    DROP CONSTRAINT fk$person,
    ADD CONSTRAINT fk$person FOREIGN KEY (id) REFERENCES person (id) ON DELETE CASCADE;
