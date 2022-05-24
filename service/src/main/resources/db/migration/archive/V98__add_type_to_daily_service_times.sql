CREATE TYPE daily_service_time_type AS ENUM ('REGULAR', 'IRREGULAR');

ALTER TABLE daily_service_time ADD COLUMN type daily_service_time_type;

UPDATE daily_service_time
    SET type = CASE
        WHEN regular THEN 'REGULAR'::daily_service_time_type
        ELSE 'IRREGULAR'::daily_service_time_type
    END;

ALTER TABLE daily_service_time ALTER COLUMN type SET NOT NULL;

ALTER TABLE daily_service_time DROP COLUMN regular;
