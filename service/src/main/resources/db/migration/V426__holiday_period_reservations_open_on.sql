ALTER TABLE holiday_period ADD COLUMN reservations_open_on date;
UPDATE holiday_period SET reservations_open_on = created::date;
ALTER TABLE holiday_period ALTER COLUMN reservations_open_on SET NOT NULL;
