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
