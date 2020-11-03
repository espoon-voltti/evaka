ALTER TABLE daycare ADD COLUMN operation_days integer[] default '{1,2,3,4,5}'::integer[];
UPDATE daycare SET operation_days = '{1, 2, 3, 4, 5, 6, 0}' WHERE round_the_clock = true;