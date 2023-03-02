ALTER TABLE koski_study_right ADD COLUMN input_data_version int;
UPDATE koski_study_right SET input_data_version = 0;
ALTER TABLE koski_study_right ALTER COLUMN input_data_version SET NOT NULL;
