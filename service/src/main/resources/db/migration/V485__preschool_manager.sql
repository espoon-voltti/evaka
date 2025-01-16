ALTER TABLE daycare
    ADD COLUMN preschool_manager_name text NOT NULL DEFAULT '',
    ADD COLUMN preschool_manager_phone text NOT NULL DEFAULT '',
    ADD COLUMN preschool_manager_email text NOT NULL DEFAULT '';
