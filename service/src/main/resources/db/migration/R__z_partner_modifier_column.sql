-- TODO rename this file into V###__xxx.sql and add to migrations.txt

-- TODO remove this once ready for review
ALTER TABLE fridge_partner
    DROP COLUMN IF EXISTS create_type,
    DROP COLUMN IF EXISTS created_at,
    DROP COLUMN IF EXISTS created_by,
    DROP COLUMN IF EXISTS modify_type,
    DROP COLUMN IF EXISTS modified_at,
    DROP COLUMN IF EXISTS modified_by,
    DROP COLUMN IF EXISTS created_from_application;
-- TODO remove next line once ready for review
DROP TYPE IF EXISTS create_type;
-- TODO remove next line once ready for review
DROP TYPE IF EXISTS modify_type;

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