-- Fix evaka_user data where some mobile_device entries had their references in the employee_id column
ALTER TABLE evaka_user DROP CONSTRAINT type_id_match;

UPDATE evaka_user SET mobile_device_id = employee_id, employee_id = NULL
WHERE type = 'MOBILE_DEVICE' AND employee_id IS NOT NULL AND mobile_device_id IS NULL
AND NOT EXISTS (SELECT 1 FROM evaka_user eu2 WHERE evaka_user.employee_id = eu2.mobile_device_id);

ALTER TABLE evaka_user ADD CONSTRAINT type_id_match CHECK (
  CASE type
    WHEN 'SYSTEM' THEN id = '00000000-0000-0000-0000-000000000000' AND num_nonnulls(citizen_id, employee_id, mobile_device_id) = 0
    WHEN 'CITIZEN' THEN (id = citizen_id OR citizen_id IS NULL) AND employee_id IS NULL AND mobile_device_id IS NULL
    WHEN 'EMPLOYEE' THEN (id = employee_id OR employee_id IS NULL) AND citizen_id IS NULL AND mobile_device_id IS NULL
    WHEN 'MOBILE_DEVICE' THEN (id = mobile_device_id OR mobile_device_id IS NULL) AND citizen_id IS NULL AND employee_id IS NULL
    WHEN 'UNKNOWN' THEN num_nonnulls(citizen_id, employee_id, mobile_device_id) = 0
  END
);


-- Remove mobile devices from employee table
ALTER TABLE mobile_device DROP CONSTRAINT mobile_device_id_fkey;
ALTER TABLE mobile_device ALTER COLUMN id SET DEFAULT ext.uuid_generate_v1mc();
DELETE FROM employee WHERE id IN (SELECT id FROM mobile_device);
