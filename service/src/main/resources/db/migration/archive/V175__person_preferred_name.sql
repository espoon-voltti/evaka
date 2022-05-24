ALTER TABLE person ADD COLUMN preferred_name text NOT NULL DEFAULT '';

UPDATE person SET preferred_name = child.preferred_name FROM child WHERE person.id = child.id AND child.preferred_name IS NOT NULL;

ALTER TABLE child DROP COLUMN preferred_name;
