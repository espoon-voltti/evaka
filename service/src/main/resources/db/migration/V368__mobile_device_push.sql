CREATE TABLE mobile_device_push_group (
    daycare_group uuid NOT NULL,
    device uuid NOT NULL,

    created_at timestamp with time zone,

    PRIMARY KEY (daycare_group, device),
    CONSTRAINT fk$daycare_group FOREIGN KEY (daycare_group) REFERENCES daycare_group ON DELETE CASCADE,
    CONSTRAINT fk$device FOREIGN KEY (device) REFERENCES mobile_device ON DELETE CASCADE
);

CREATE INDEX idx$mobile_device_push_group_reverse_pk ON mobile_device_push_group (device, daycare_group);

-- All unit-level devices that currently use push notifications get subscriptions to all their groups
INSERT INTO mobile_device_push_group (daycare_group, device, created_at)
SELECT dg.id, md.id, now()
FROM mobile_device_push_subscription mdps
JOIN mobile_device md ON mdps.device = md.id
JOIN daycare d ON md.unit_id = d.id
JOIN daycare_group dg ON d.id = dg.daycare_id
WHERE md.employee_id IS NULL;


CREATE TYPE push_notification_category AS ENUM (
    'RECEIVED_MESSAGE'
);

ALTER TABLE mobile_device ADD COLUMN push_notification_categories push_notification_category[] NOT NULL DEFAULT '{}';

-- All unit-level devices that currently use push notifications subscribe to received messages
UPDATE mobile_device SET push_notification_categories = '{RECEIVED_MESSAGE}'
WHERE EXISTS (
    SELECT FROM mobile_device_push_subscription WHERE mobile_device_push_subscription.device = mobile_device.id
) AND mobile_device.employee_id IS NULL;
