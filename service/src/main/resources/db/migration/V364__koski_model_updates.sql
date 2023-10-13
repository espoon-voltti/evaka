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

CREATE TYPE koski_input_data AS (
    oph_unit_oid text, oph_organizer_oid text,
    placements datemultirange, all_placements_in_past bool, last_of_child bool, preparatory_absences jsonb,
    special_support_with_decision_level_1 datemultirange, special_support_with_decision_level_2 datemultirange,
    transport_benefit datemultirange
);

ALTER TABLE koski_study_right
    DROP COLUMN input_data,
    ADD COLUMN input_data koski_input_data;
