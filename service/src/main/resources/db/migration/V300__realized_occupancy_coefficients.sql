ALTER TABLE service_need_option
    ADD COLUMN realized_occupancy_coefficient numeric(4, 2),
    ADD COLUMN realized_occupancy_coefficient_under_3y numeric(4, 2);

UPDATE service_need_option SET
    realized_occupancy_coefficient = occupancy_coefficient,
    realized_occupancy_coefficient_under_3y = occupancy_coefficient_under_3y;

ALTER TABLE service_need_option
    ALTER COLUMN realized_occupancy_coefficient SET NOT NULL,
    ALTER COLUMN realized_occupancy_coefficient_under_3y SET NOT NULL;
