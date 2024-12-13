WITH disallowed_devices AS (
    SELECT id
    FROM mobile_device
    WHERE
        employee_id IS NOT NULL AND
        NOT EXISTS (
            SELECT FROM daycare_acl a
            JOIN daycare d ON d.id = a.daycare_id
            WHERE
                (d.provider_type = 'MUNICIPAL' OR d.provider_type = 'MUNICIPAL_SCHOOL') AND
                a.employee_id = mobile_device.employee_id AND
                a.role = 'UNIT_SUPERVISOR'
        )
), push_subscriptions AS (
    DELETE FROM mobile_device_push_subscription
    WHERE device IN (SELECT id FROM disallowed_devices)
), push_groups AS (
    DELETE FROM mobile_device_push_group
    WHERE device IN (SELECT id FROM disallowed_devices)
), pairings AS (
    DELETE FROM pairing
    WHERE mobile_device_id IN (SELECT id FROM disallowed_devices)
)
DELETE FROM mobile_device
WHERE id IN (SELECT id FROM disallowed_devices);
