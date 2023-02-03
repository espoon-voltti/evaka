ALTER TABLE person ADD COLUMN duplicate_of uuid REFERENCES person (id);
CREATE INDEX idx$person_duplicate_of ON person (duplicate_of);
