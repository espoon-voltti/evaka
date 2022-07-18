CREATE TABLE daily_service_time_notification (
  id uuid PRIMARY KEY NOT NULL DEFAULT ext.uuid_generate_v1mc(),
  guardian_id uuid NOT NULL REFERENCES person (id) ON DELETE CASCADE,
  daily_service_time_id uuid NOT NULL REFERENCES daily_service_time (id) ON DELETE CASCADE,
  date_from date NOT NULL,
  has_deleted_reservations boolean NOT NULL
);

CREATE INDEX idx$daily_service_time_notification_guardian_id ON daily_service_time_notification (guardian_id);
