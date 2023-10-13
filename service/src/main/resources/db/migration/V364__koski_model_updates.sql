DROP FUNCTION IF EXISTS koski_active_study_right(today date);

CREATE OR REPLACE FUNCTION as_koski_study_right_type(placement_type) RETURNS koski_study_right_type
LANGUAGE SQL IMMUTABLE PARALLEL SAFE
RETURN
    (CASE $1
        WHEN 'PRESCHOOL' THEN 'PRESCHOOL'
        WHEN 'PRESCHOOL_DAYCARE' THEN 'PRESCHOOL'
        WHEN 'PRESCHOOL_CLUB' THEN 'PRESCHOOL'
        WHEN 'PREPARATORY' THEN 'PREPARATORY'
        WHEN 'PREPARATORY_DAYCARE' THEN 'PREPARATORY'
    END)::koski_study_right_type;

CREATE CAST (placement_type AS koski_study_right_type)
    WITH FUNCTION as_koski_study_right_type(placement_type);

CREATE TYPE koski_preschool_input_data AS (
    oph_unit_oid text, oph_organizer_oid text,
    placements datemultirange, all_placements_in_past bool, last_of_child bool,
    special_support_with_decision_level_1 datemultirange, special_support_with_decision_level_2 datemultirange,
    transport_benefit datemultirange
);

CREATE TYPE koski_preparatory_input_data AS (
    oph_unit_oid text, oph_organizer_oid text,
    placements datemultirange, all_placements_in_past bool, last_of_child bool, last_of_type bool, absences jsonb
);

ALTER TABLE koski_study_right
    DROP COLUMN input_data,
    ADD COLUMN preschool_input_data koski_preschool_input_data,
    ADD COLUMN preparatory_input_data koski_preparatory_input_data,
    ADD CONSTRAINT check$input_data_type CHECK (CASE type
        WHEN 'PRESCHOOL' THEN preparatory_input_data IS NULL
        WHEN 'PREPARATORY' THEN preschool_input_data IS NULL
    END);

ALTER TABLE koski_study_right RENAME COLUMN input_data_version TO data_version;
