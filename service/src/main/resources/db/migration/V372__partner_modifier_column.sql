CREATE TYPE create_type AS ENUM ('USER', 'APPLICATION');
CREATE TYPE modify_type AS ENUM ('USER', 'DVV');

ALTER TABLE fridge_partner
    ADD COLUMN create_type create_type default null,
    ADD COLUMN created_at timestamp with time zone default null,
    ADD COLUMN created_by uuid default null REFERENCES evaka_user(id),
    ADD COLUMN modify_type modify_type default null,
    ADD COLUMN modified_at timestamp with time zone default null,
    ADD COLUMN modified_by uuid default null REFERENCES evaka_user(id),
    ADD COLUMN created_from_application uuid default null REFERENCES application(id);

CREATE INDEX idx$fridge_partner_created_by ON fridge_partner (created_by);
CREATE INDEX idx$fridge_partner_modified_by ON fridge_partner (modified_by);
CREATE INDEX idx$fridge_partner_created_from_application ON fridge_partner (created_from_application);