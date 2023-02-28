CREATE TYPE income_notification_type AS ENUM ('INITIAL_EMAIL', 'REMINDER_EMAIL', 'EXPIRED_EMAIL');

CREATE TABLE income_notification
(
    id                      uuid PRIMARY KEY         NOT NULL DEFAULT ext.uuid_generate_v1mc(),
    created                 timestamp with time zone NOT NULL DEFAULT now(),
    updated                 timestamp with time zone NOT NULL DEFAULT now(),
    receiver_id             uuid                     NOT NULL,
    notification_type       income_notification_type NOT NULL
);

CREATE INDEX idx$income_notification_receiver_id ON income_notification (receiver_id);
CREATE TRIGGER set_timestamp BEFORE UPDATE ON income_notification FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
