ALTER TABLE daycare_daily_note ADD CONSTRAINT unique_daycare_daily_note UNIQUE (child_id, group_id, date);

