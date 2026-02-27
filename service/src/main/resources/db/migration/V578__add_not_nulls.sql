ALTER TABLE daycare_group_placement ALTER COLUMN created SET NOT NULL;
ALTER TABLE decision ALTER COLUMN created SET NOT NULL;
ALTER TABLE fridge_child ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE guardian ALTER COLUMN created SET NOT NULL;
ALTER TABLE invoice ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE mobile_device_push_group ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE placement_plan ALTER COLUMN created SET NOT NULL;
ALTER TABLE staff_attendance ALTER COLUMN created SET NOT NULL;
ALTER TABLE voucher_value_decision ALTER COLUMN created SET NOT NULL;
ALTER TABLE voucher_value_report_snapshot ALTER COLUMN created SET NOT NULL;

ALTER TABLE daycare_group_placement ALTER COLUMN updated SET NOT NULL;
ALTER TABLE decision ALTER COLUMN updated SET NOT NULL;
ALTER TABLE fridge_child ALTER COLUMN updated SET NOT NULL;
ALTER TABLE fridge_partner ALTER COLUMN updated SET NOT NULL;
ALTER TABLE placement_plan ALTER COLUMN updated SET NOT NULL;
ALTER TABLE staff_occupancy_coefficient ALTER COLUMN updated SET NOT NULL;
