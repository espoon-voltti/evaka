INSERT INTO evaka_user (id, type, mobile_device_id, name)
SELECT id, 'MOBILE_DEVICE', id, name
FROM mobile_device
ON CONFLICT (id) DO NOTHING;
