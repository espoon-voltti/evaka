DROP FUNCTION IF EXISTS koski_active_preschool_study_right(today date);
DROP FUNCTION IF EXISTS koski_active_preparatory_study_right(today date);
DROP FUNCTION IF EXISTS koski_voided_study_right(today date);
DROP FUNCTION IF EXISTS koski_placement(today date);
DROP VIEW IF EXISTS koski_unit;
DROP VIEW IF EXISTS koski_child;

CREATE VIEW koski_unit (id, unit_language, provider_type, approver_name, oph_unit_oid, oph_organizer_oid) AS
SELECT
    id, language AS unit_language, provider_type, unit_manager_name AS approver_name,
    nullif(oph_unit_oid, '') AS oph_unit_oid,
    nullif(oph_organizer_oid, '') AS oph_organizer_oid
FROM daycare
WHERE upload_to_koski IS TRUE
AND nullif(oph_unit_oid, '') IS NOT NULL
AND nullif(oph_organizer_oid, '') IS NOT NULL;

CREATE VIEW koski_child (id, ssn, oph_person_oid, first_name, last_name) AS
SELECT
    id,
    nullif(social_security_number, '') AS ssn,
    nullif(oph_person_oid, '') AS oph_person_oid,
    first_name,
    last_name
FROM person
WHERE nullif(social_security_number, '') IS NOT NULL
OR nullif(oph_person_oid, '') IS NOT NULL;

CREATE FUNCTION koski_placement(today date) RETURNS
TABLE (
    child_id uuid, unit_id uuid, type koski_study_right_type,
    placements datemultirange, all_placements_in_past bool, last_of_child bool, last_of_type bool
)
LANGUAGE SQL STABLE PARALLEL SAFE
BEGIN ATOMIC
    SELECT
        child_id, unit_id, type,
        range_agg(daterange(start_date, end_date, '[]')) AS placements,
        (max(end_date) < today) AS all_placements_in_past,
        bool_or(last_of_child) AS last_of_child,
        bool_or(last_of_type) AS last_of_type
    FROM (
        SELECT
            child_id, unit_id, start_date, end_date,
            row_number() OVER child = count(*) OVER child AS last_of_child,
            row_number() OVER child_type = count(*) OVER child_type AS last_of_type,
            placement.type::koski_study_right_type AS type
        FROM placement
        WHERE start_date <= today
        AND placement.type IN ('PRESCHOOL', 'PRESCHOOL_DAYCARE', 'PRESCHOOL_CLUB', 'PREPARATORY', 'PREPARATORY_DAYCARE')
        WINDOW
            child AS (PARTITION BY child_id ORDER BY start_date ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING),
            child_type AS (PARTITION BY child_id, placement.type::koski_study_right_type ORDER BY start_date ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
    ) p
    WHERE NOT EXISTS (SELECT FROM person duplicate WHERE duplicate.duplicate_of = p.child_id)
    GROUP BY child_id, unit_id, type;
END;

CREATE FUNCTION koski_active_preschool_study_right(today date) RETURNS
TABLE (child_id uuid, unit_id uuid, type koski_study_right_type, input_data koski_preschool_input_data)
LANGUAGE SQL STABLE PARALLEL SAFE
BEGIN ATOMIC
    SELECT
        p.child_id,
        p.unit_id,
        p.type,
        (
            d.oph_unit_oid,
            d.oph_organizer_oid,
            placements,
            all_placements_in_past,
            last_of_child,
            coalesce(special_support, '{}'),
            coalesce(special_support_with_decision_level_1, '{}'),
            coalesce(special_support_with_decision_level_2, '{}'),
            coalesce(transport_benefit, '{}'),
            coalesce(child_support, '{}'),
            coalesce(child_support_and_extended_compulsory_education, '{}')
        ) AS input_data
    FROM koski_placement(today) p
    JOIN koski_unit d ON p.unit_id = d.id
    JOIN LATERAL (
        SELECT range_agg(valid_during) AS transport_benefit
        FROM other_assistance_measure oam
        WHERE oam.child_id = p.child_id
        AND oam.valid_during && range_merge(placements)
        AND type = 'TRANSPORT_BENEFIT'
    ) oam ON TRUE
    JOIN LATERAL (
        SELECT
            range_agg(valid_during) FILTER (
                WHERE level = 'SPECIAL_SUPPORT'
            ) AS special_support,
            range_agg(valid_during) FILTER (
                WHERE level = 'SPECIAL_SUPPORT_WITH_DECISION_LEVEL_1'
            ) AS special_support_with_decision_level_1,
            range_agg(valid_during) FILTER (
                WHERE level = 'SPECIAL_SUPPORT_WITH_DECISION_LEVEL_2'
            ) AS special_support_with_decision_level_2,
            range_agg(valid_during) FILTER (
                WHERE level = 'CHILD_SUPPORT'
            ) AS child_support,
            range_agg(valid_during) FILTER (
                WHERE level = 'CHILD_SUPPORT_AND_EXTENDED_COMPULSORY_EDUCATION'
            ) AS child_support_and_extended_compulsory_education
        FROM preschool_assistance pa
        WHERE pa.child_id = p.child_id
        AND pa.valid_during && range_merge(placements)
    ) pras ON TRUE
    WHERE p.type = 'PRESCHOOL'
    AND EXISTS (SELECT FROM koski_child WHERE koski_child.id = p.child_id);
END;

CREATE FUNCTION koski_active_preparatory_study_right(today date) RETURNS
TABLE (child_id uuid, unit_id uuid, type koski_study_right_type, input_data koski_preparatory_input_data)
LANGUAGE SQL STABLE PARALLEL SAFE
BEGIN ATOMIC
    SELECT
        p.child_id,
        p.unit_id,
        p.type,
        (
            d.oph_unit_oid,
            d.oph_organizer_oid,
            placements,
            all_placements_in_past,
            last_of_child,
            last_of_type,
            coalesce(absences, '{}')
        ) AS input_data
    FROM koski_placement(today) p
    JOIN koski_unit d ON p.unit_id = d.id
    JOIN LATERAL (
        SELECT jsonb_object_agg(absence_type, dates) AS absences
        FROM (
            SELECT a.absence_type, array_agg(a.date ORDER BY a.date) AS dates
            FROM absence a
            WHERE a.child_id = p.child_id
            AND a.category = 'NONBILLABLE'
            AND between_start_and_end(range_merge(placements), a.date)
            AND a.date > '2020-08-01'
            GROUP BY a.absence_type
        ) grouped
    ) pa ON TRUE
    WHERE p.type = 'PREPARATORY'
    AND EXISTS (SELECT FROM koski_child WHERE koski_child.id = p.child_id);
END;

CREATE FUNCTION koski_voided_study_right(today date) RETURNS
TABLE (
    child_id uuid, unit_id uuid, type koski_study_right_type,
    oph_unit_oid text, oph_organizer_oid text,
    void_date date
)
LANGUAGE SQL STABLE PARALLEL SAFE
BEGIN ATOMIC
    SELECT
        ksr.child_id,
        ksr.unit_id,
        ksr.type,
        d.oph_unit_oid,
        d.oph_organizer_oid,
        ksr.void_date
    FROM koski_study_right ksr
    JOIN koski_unit d ON ksr.unit_id = d.id
    JOIN person pr ON ksr.child_id = pr.id
    WHERE EXISTS (SELECT FROM koski_child WHERE koski_child.id = ksr.child_id)
    AND NOT EXISTS (
        SELECT 1
        FROM koski_placement(today) kp
        WHERE (kp.child_id, kp.unit_id, kp.type) = (ksr.child_id, ksr.unit_id, ksr.type)
    )
    AND ksr.study_right_oid IS NOT NULL;
END;
