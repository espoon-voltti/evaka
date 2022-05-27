ALTER TABLE daycare_caretaker
    ALTER COLUMN end_date DROP NOT NULL,
    ALTER COLUMN end_date DROP DEFAULT;

UPDATE daycare_caretaker
SET end_date = NULL
WHERE end_date = '9999-01-01' OR end_date = 'infinity';

ALTER TABLE daycare_group
    ALTER COLUMN end_date DROP NOT NULL,
    ALTER COLUMN end_date DROP DEFAULT;

UPDATE daycare_group
SET end_date = NULL
WHERE end_date = '9999-01-01' OR end_date = 'infinity';

ALTER TABLE fridge_partner
    ALTER COLUMN end_date DROP NOT NULL,
    ALTER COLUMN end_date DROP DEFAULT;

UPDATE fridge_partner
SET end_date = NULL
WHERE end_date = '9999-01-01' OR end_date = 'infinity';
