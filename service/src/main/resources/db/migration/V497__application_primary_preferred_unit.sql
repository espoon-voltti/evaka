ALTER TABLE application ADD COLUMN primary_preferred_unit uuid
    GENERATED ALWAYS AS ((document -> 'apply' -> 'preferredUnits' ->> 0)::uuid) STORED
    CONSTRAINT fk$primary_preferred_unit REFERENCES daycare (id);

CREATE INDEX idx$application_primary_unit_status ON application (primary_preferred_unit);
