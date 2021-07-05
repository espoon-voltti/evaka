ALTER TABLE daycare_group ADD CONSTRAINT start_before_end CHECK ((start_date <= end_date));
