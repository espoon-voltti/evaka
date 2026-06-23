ALTER TABLE daily_service_time_notification
    ADD COLUMN created_at timestamp with time zone NOT NULL DEFAULT now();
