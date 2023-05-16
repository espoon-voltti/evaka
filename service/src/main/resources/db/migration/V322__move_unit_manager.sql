DROP VIEW IF EXISTS old_daycare_acl_view;

ALTER TABLE daycare
    ADD COLUMN unit_manager_name text NOT NULL DEFAULT '',
    ADD COLUMN unit_manager_phone text NOT NULL DEFAULT '',
    ADD COLUMN unit_manager_email text NOT NULL DEFAULT '';

UPDATE daycare
SET
    unit_manager_name = coalesce(um.name, ''),
    unit_manager_phone = coalesce(um.phone, ''),
    unit_manager_email = coalesce(um.email, '')
FROM unit_manager um
WHERE um.id = daycare.unit_manager_id;

ALTER TABLE daycare DROP COLUMN unit_manager_id;
DROP TABLE unit_manager;
