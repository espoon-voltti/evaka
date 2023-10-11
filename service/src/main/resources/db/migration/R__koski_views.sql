DROP FUNCTION IF EXISTS koski_active_study_right(today date);
DROP FUNCTION IF EXISTS koski_voided_study_right(today date);
DROP FUNCTION IF EXISTS koski_placement(today date);

CREATE FUNCTION koski_placement(today date) RETURNS
TABLE (
    child_id uuid, unit_id uuid, type koski_study_right_type,
    full_range daterange, placements datemultirange, all_placements_in_past bool, last_of_child bool
) AS $$
    SELECT
        child_id, unit_id, type,
        daterange(min(start_date), max(end_date), '[]') AS full_range,
        range_agg(daterange(start_date, end_date, '[]')) AS placements,
        (max(end_date) < today) AS all_placements_in_past,
        bool_or(last_of_child) AS last_of_child
    FROM (
        SELECT
            child_id, unit_id, start_date, end_date,
            end_date = max(end_date) OVER child AS last_of_child,
            placement.type::koski_study_right_type AS type
        FROM placement
        WHERE start_date <= today
        AND placement.type IN ('PRESCHOOL', 'PRESCHOOL_DAYCARE', 'PRESCHOOL_CLUB', 'PREPARATORY', 'PREPARATORY_DAYCARE')
        WINDOW child AS (PARTITION BY child_id)
    ) p
    WHERE NOT EXISTS (SELECT FROM person duplicate WHERE duplicate.duplicate_of = p.child_id)
    GROUP BY child_id, unit_id, type
$$ LANGUAGE SQL STABLE;

CREATE FUNCTION koski_active_study_right(today date) RETURNS
TABLE (
    child_id uuid, unit_id uuid, type koski_study_right_type,
    oph_unit_oid text, oph_organizer_oid text,
    full_range daterange, placements datemultirange, all_placements_in_past bool, last_of_child bool, preparatory_absences jsonb,
    special_support_with_decision_level_1 datemultirange, special_support_with_decision_level_2 datemultirange,
    transport_benefit datemultirange
) AS $$
    SELECT
        p.child_id,
        p.unit_id,
        p.type,
        d.oph_unit_oid,
        d.oph_organizer_oid,
        full_range,
        placements,
        all_placements_in_past,
        last_of_child,
        preparatory_absences,
        special_support_with_decision_level_1,
        special_support_with_decision_level_2,
        transport_benefit
    FROM koski_placement(today) p
    JOIN daycare d ON p.unit_id = d.id
    JOIN person pr ON p.child_id = pr.id
    LEFT JOIN LATERAL (
        SELECT jsonb_agg(jsonb_build_object('date', a.date, 'type', a.absence_type) ORDER BY a.date) AS preparatory_absences
        FROM absence a
        WHERE a.child_id = p.child_id
        AND a.category = 'NONBILLABLE'
        AND between_start_and_end(full_range, a.date)
        AND a.date > '2020-08-01'
    ) pa ON p.type = 'PREPARATORY'
    LEFT JOIN LATERAL (
        SELECT range_agg(valid_during) AS transport_benefit
        FROM other_assistance_measure oam
        WHERE oam.child_id = p.child_id
        AND oam.valid_during && full_range
        AND type = 'TRANSPORT_BENEFIT'
    ) oam ON TRUE
    LEFT JOIN LATERAL (
        SELECT
            range_agg(valid_during) FILTER (
                WHERE level = 'SPECIAL_SUPPORT_WITH_DECISION_LEVEL_1'
            ) AS special_support_with_decision_level_1,
            range_agg(valid_during) FILTER (
                WHERE level = 'SPECIAL_SUPPORT_WITH_DECISION_LEVEL_2'
            ) AS special_support_with_decision_level_2
        FROM preschool_assistance pa
        WHERE pa.child_id = p.child_id
        AND pa.valid_during && full_range
    ) pras ON TRUE
    WHERE d.upload_to_koski IS TRUE
    AND (nullif(pr.social_security_number, '') IS NOT NULL OR nullif(pr.oph_person_oid, '') IS NOT NULL)
    AND nullif(d.oph_unit_oid, '') IS NOT NULL
    AND nullif(d.oph_organizer_oid, '') IS NOT NULL;
$$ LANGUAGE SQL STABLE;

CREATE FUNCTION koski_voided_study_right(today date) RETURNS
TABLE (
    child_id uuid, unit_id uuid, type koski_study_right_type,
    oph_unit_oid text, oph_organizer_oid text,
    void_date date
) AS $$
    SELECT
        ksr.child_id,
        ksr.unit_id,
        ksr.type,
        d.oph_unit_oid,
        d.oph_organizer_oid,
        ksr.void_date
    FROM koski_study_right ksr
    JOIN daycare d ON ksr.unit_id = d.id
    JOIN person pr ON ksr.child_id = pr.id
    WHERE NOT EXISTS (
        SELECT 1
        FROM koski_placement(today) kp
        WHERE (kp.child_id, kp.unit_id, kp.type) = (ksr.child_id, ksr.unit_id, ksr.type)
    )
    AND ksr.study_right_oid IS NOT NULL
    AND (nullif(pr.social_security_number, '') IS NOT NULL OR nullif(pr.oph_person_oid, '') IS NOT NULL)
    AND d.upload_to_koski IS TRUE
    AND nullif(d.oph_unit_oid, '') IS NOT NULL
    AND nullif(d.oph_organizer_oid, '') IS NOT NULL;
$$ LANGUAGE SQL STABLE;
