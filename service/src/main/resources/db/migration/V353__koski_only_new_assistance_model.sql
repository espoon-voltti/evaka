-- drop all stuff that will be removed from the repeatable migration
DROP VIEW IF EXISTS koski_voided_view;
DROP VIEW IF EXISTS koski_active_view;
DROP FUNCTION IF EXISTS koski_active_study_right(today date, new_assistance_model bool);
