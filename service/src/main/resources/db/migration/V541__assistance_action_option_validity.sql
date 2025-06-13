CREATE TYPE assistance_action_option_category AS ENUM (
    'DAYCARE',
    'PRESCHOOL'
);

ALTER TABLE assistance_action_option
    ADD COLUMN valid_from date,
    ADD COLUMN valid_to date,
    ADD COLUMN category assistance_action_option_category NOT NULL DEFAULT 'DAYCARE';

ALTER TABLE assistance_action_option ALTER COLUMN category DROP DEFAULT;

ALTER TABLE assistance_action_option ADD CONSTRAINT check_validity
    CHECK (valid_from IS NULL OR valid_to IS NULL OR valid_from <= valid_to);

-- drop unused legacy column and type
ALTER TABLE assistance_action DROP COLUMN measures;
DROP TYPE assistance_measure;
