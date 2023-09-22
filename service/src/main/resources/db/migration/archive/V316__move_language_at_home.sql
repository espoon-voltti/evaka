ALTER TABLE child
    ADD COLUMN language_at_home text NOT NULL DEFAULT '',
    ADD COLUMN language_at_home_details text NOT NULL DEFAULT '';

UPDATE child SET (language_at_home, language_at_home_details) = (SELECT language_at_home, language_at_home_details FROM person WHERE child.id = person.id);

ALTER TABLE person
    DROP COLUMN language_at_home,
    DROP COLUMN language_at_home_details;