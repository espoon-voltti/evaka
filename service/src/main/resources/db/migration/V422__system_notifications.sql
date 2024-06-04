CREATE TYPE system_notification_target_group AS ENUM ('CITIZENS', 'EMPLOYEES');

CREATE TABLE system_notification (
    -- only one per target_group allowed so using that as primary key
    target_group system_notification_target_group PRIMARY KEY,
    text text NOT NULL CHECK ( text != '' ),
    valid_to timestamp with time zone NOT NULL
);
