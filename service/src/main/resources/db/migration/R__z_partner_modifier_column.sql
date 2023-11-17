-- TODO rename this file into V###__xxx.sql and add to migrations.txt

-- TODO remove next line once ready for review
ALTER TABLE fridge_partner DROP COLUMN IF EXISTS created_at;
ALTER TABLE fridge_partner ADD COLUMN created_at timestamp with time zone default null;

-- TODO remove next line once ready for review
ALTER TABLE fridge_partner DROP COLUMN IF EXISTS created_by;
ALTER TABLE fridge_partner ADD COLUMN created_by uuid default null;
ALTER TABLE fridge_partner
    add foreign key (created_by) references evaka_user;

-- TODO remove next line once ready for review
ALTER TABLE fridge_partner DROP COLUMN IF EXISTS modified_at;
ALTER TABLE fridge_partner ADD COLUMN modified_at timestamp with time zone default null;

-- TODO remove next line once ready for review
ALTER TABLE fridge_partner DROP COLUMN IF EXISTS modified_by;
ALTER TABLE fridge_partner ADD COLUMN modified_by uuid default null;
ALTER TABLE fridge_partner
    add foreign key (modified_by) references evaka_user;