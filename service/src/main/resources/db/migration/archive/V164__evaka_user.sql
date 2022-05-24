CREATE TYPE evaka_user_type AS ENUM (
  'SYSTEM', 'CITIZEN', 'EMPLOYEE', 'MOBILE_DEVICE', 'UNKNOWN'
);

CREATE TABLE evaka_user(
  id uuid PRIMARY KEY,
  type evaka_user_type NOT NULL,
  citizen_id uuid CONSTRAINT fk$citizen REFERENCES person (id) ON DELETE SET NULL,
  employee_id uuid CONSTRAINT fk$employee REFERENCES employee (id) ON DELETE SET NULL,
  mobile_device_id uuid CONSTRAINT fk$mobile_device REFERENCES mobile_device (id) ON DELETE SET NULL,
  name text NOT NULL,

  CONSTRAINT fk_count CHECK (num_nonnulls(citizen_id, employee_id, mobile_device_id) <= 1),
  CONSTRAINT type_id_match CHECK (
    CASE type
      WHEN 'SYSTEM' THEN id = '00000000-0000-0000-0000-000000000000' AND num_nonnulls(citizen_id, employee_id, mobile_device_id) = 0
      WHEN 'CITIZEN' THEN id = citizen_id OR citizen_id IS NULL
      WHEN 'EMPLOYEE' THEN id = employee_id OR employee_id IS NULL
      WHEN 'MOBILE_DEVICE' THEN id = mobile_device_id OR mobile_device_id IS NULL
      WHEN 'UNKNOWN' THEN num_nonnulls(citizen_id, employee_id, mobile_device_id) = 0
    END
  )
);

INSERT INTO evaka_user (id, type, name) VALUES ('00000000-0000-0000-0000-000000000000', 'SYSTEM', 'eVaka');

INSERT INTO evaka_user (id, type, citizen_id, name)
SELECT id, 'CITIZEN', id, first_name || ' ' || last_name
FROM person
WHERE id IN (
  SELECT modified_by_guardian_id FROM absence
  UNION ALL
  SELECT uploaded_by_person FROM attachment
  UNION ALL
  SELECT created_by_guardian_id FROM attendance_reservation
);

INSERT INTO evaka_user (id, type, employee_id, name)
SELECT id, 'EMPLOYEE', id, first_name || ' ' || last_name
FROM employee
WHERE id IN (
  SELECT modified_by_employee_id FROM absence
  UNION ALL
  SELECT uploaded_by_employee FROM attachment
  UNION ALL
  SELECT created_by_employee_id FROM attendance_reservation
  UNION ALL
  SELECT created_by FROM decision
  UNION ALL
  SELECT updated_by FROM old_service_need
  UNION ALL
  SELECT created_by FROM application_note
  UNION ALL
  SELECT updated_by FROM application_note
  UNION ALL
  SELECT updated_by FROM assistance_action
  UNION ALL
  SELECT confirmed_by FROM service_need
  UNION ALL
  SELECT created_by FROM pedagogical_document
  UNION ALL
  SELECT updated_by FROM pedagogical_document
  UNION ALL
  SELECT sent_by FROM invoice
  UNION ALL
  SELECT updated_by FROM assistance_need
  UNION ALL
  SELECT updated_by FROM fee_alteration
)
AND NOT EXISTS (SELECT 1 FROM mobile_device WHERE mobile_device.id = employee.id);

INSERT INTO evaka_user (id, type, mobile_device_id, name)
SELECT id, 'MOBILE_DEVICE', id, name
FROM mobile_device;

INSERT INTO evaka_user (id, type, name)
SELECT DISTINCT ON (modified_by_deprecated) ext.uuid_generate_v1mc(), 'UNKNOWN', modified_by_deprecated
FROM absence
WHERE modified_by_deprecated IS NOT NULL
AND modified_by_guardian_id IS NULL
AND modified_by_employee_id IS NULL;

CREATE UNIQUE INDEX uniq$evaka_user_citizen ON evaka_user (citizen_id) WHERE citizen_id IS NOT NULL;
CREATE UNIQUE INDEX uniq$evaka_user_employee ON evaka_user (employee_id) WHERE employee_id IS NOT NULL;
CREATE UNIQUE INDEX uniq$evaka_user_mobile_device ON evaka_user (mobile_device_id) WHERE mobile_device_id IS NOT NULL;

CREATE INDEX idx$evaka_user_unknown ON evaka_user (name) WHERE type = 'UNKNOWN';
