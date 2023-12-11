CREATE TYPE create_source AS ENUM ('USER', 'APPLICATION');
CREATE TYPE modify_source AS ENUM ('USER', 'DVV');

ALTER TABLE fridge_partner
    RENAME COLUMN created to created_at;

ALTER TABLE fridge_partner
    ALTER COLUMN created_at DROP default,
    ALTER COLUMN created_at SET NOT NULL,
    ADD COLUMN create_source create_source default null,
    ADD COLUMN created_by uuid default null REFERENCES evaka_user(id),
    ADD COLUMN modify_source modify_source default null,
    ADD COLUMN modified_at timestamp with time zone default null,
    ADD COLUMN modified_by uuid default null REFERENCES evaka_user(id),
    ADD COLUMN created_from_application uuid default null REFERENCES application(id);

CREATE INDEX idx$fridge_partner_created_by ON fridge_partner (created_by);
CREATE INDEX idx$fridge_partner_modified_by ON fridge_partner (modified_by);
CREATE INDEX idx$fridge_partner_created_from_application ON fridge_partner (created_from_application);

UPDATE fridge_partner
    SET modified_at = updated
    WHERE updated <> created_at;