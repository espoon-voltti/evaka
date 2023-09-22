UPDATE daycare SET round_the_clock = false WHERE round_the_clock IS NULL;
ALTER TABLE daycare ALTER COLUMN round_the_clock SET NOT NULL;