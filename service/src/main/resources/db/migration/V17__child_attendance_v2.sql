ALTER TABLE child_attendance ADD COLUMN unit_id uuid NOT NULL REFERENCES daycare(id);
