ALTER TABLE assistance_need_decision
  ADD COLUMN preparer_1_phone_number TEXT,
  ADD COLUMN preparer_2_phone_number TEXT,
  ADD COLUMN assistance_services_time daterange;

UPDATE assistance_need_decision
  SET assistance_services_time = daterange(assistance_service_start, assistance_service_end, '[]');

ALTER TABLE assistance_need_decision
  DROP COLUMN assistance_service_start,
  DROP COLUMN assistance_service_end;
