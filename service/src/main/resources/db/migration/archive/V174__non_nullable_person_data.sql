UPDATE person SET first_name = '' WHERE first_name IS NULL;
UPDATE person SET last_name = '' WHERE last_name IS NULL;
UPDATE person SET phone = '' WHERE phone IS NULL;
UPDATE person SET residence_code = '' WHERE residence_code IS NULL;

ALTER TABLE person
    ALTER COLUMN first_name SET NOT NULL,
    ALTER COLUMN first_name SET DEFAULT '',
    ALTER COLUMN last_name SET NOT NULL,
    ALTER COLUMN last_name SET DEFAULT '',
    ALTER COLUMN phone SET NOT NULL,
    ALTER COLUMN phone SET DEFAULT '',
    ALTER COLUMN residence_code SET NOT NULL,
    ALTER COLUMN residence_code SET DEFAULT '';
