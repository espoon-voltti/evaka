ALTER TABLE daycare_daily_note ADD COLUMN sleeping_minutes integer;

UPDATE daycare_daily_note SET sleeping_minutes = (sleeping_hours * 60)::integer;

ALTER TABLE daycare_daily_note DROP COLUMN sleeping_hours;
