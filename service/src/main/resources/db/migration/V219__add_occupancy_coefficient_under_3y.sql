ALTER TABLE service_need_option ADD COLUMN occupancy_coefficient_under_3y numeric(4,2) DEFAULT 1.75;
UPDATE service_need_option SET occupancy_coefficient_under_3y = 1.75;
ALTER TABLE service_need_option ALTER COLUMN occupancy_coefficient_under_3y SET NOT NULL;