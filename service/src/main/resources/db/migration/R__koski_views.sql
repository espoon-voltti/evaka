DROP VIEW IF EXISTS koski_voided_view;
DROP VIEW IF EXISTS koski_active_view;

DROP FUNCTION IF EXISTS koski_active_study_right(today date);
DROP FUNCTION IF EXISTS koski_voided_study_right(today date);
DROP FUNCTION IF EXISTS koski_placement(today date);

CREATE FUNCTION koski_placement(today date) RETURNS
TABLE (
    child_id uuid, unit_id uuid, type koski_study_right_type,
    full_range daterange, placement_ranges daterange[], all_placements_in_past bool, last_of_child bool
) AS $$
    SELECT
        child_id, unit_id, type,
        daterange(min(start_date), max(end_date), '[]') AS full_range,
        array_agg(daterange(start_date, end_date, '[]') ORDER BY start_date ASC) AS placement_ranges,
        (max(end_date) < today) AS all_placements_in_past,
        bool_or(last_of_child) AS last_of_child
    FROM (
        SELECT
            child_id, unit_id, start_date, end_date,
            end_date = max(end_date) OVER child AS last_of_child,
            (CASE placement.type
                WHEN 'PRESCHOOL' THEN 'PRESCHOOL'
                WHEN 'PRESCHOOL_DAYCARE' THEN 'PRESCHOOL'
                WHEN 'PREPARATORY' THEN 'PREPARATORY'
                WHEN 'PREPARATORY_DAYCARE' THEN 'PREPARATORY'
            END)::koski_study_right_type AS type
        FROM placement
        WHERE start_date <= today
        AND placement.type IN ('PRESCHOOL', 'PRESCHOOL_DAYCARE', 'PREPARATORY', 'PREPARATORY_DAYCARE')
        WINDOW child AS (PARTITION BY child_id)
    ) p
    GROUP BY child_id, unit_id, type
$$ LANGUAGE SQL STABLE;

CREATE FUNCTION koski_active_study_right(today date) RETURNS
TABLE (
    child_id uuid, unit_id uuid, type koski_study_right_type,
    oph_unit_oid text, oph_organizer_oid text,
    full_range daterange, placement_ranges daterange[], all_placements_in_past bool, last_of_child bool, preparatory_absences jsonb,
    developmental_disability_1 daterange[], developmental_disability_2 daterange[],
    extended_compulsory_education daterange, transport_benefit daterange,
    special_assistance_decision_with_group daterange[], special_assistance_decision_without_group daterange[]
) AS $$
    SELECT
        p.child_id,
        p.unit_id,
        p.type,
        d.oph_unit_oid,
        d.oph_organizer_oid,
        full_range,
        placement_ranges,
        all_placements_in_past,
        last_of_child,
        preparatory_absences,
        developmental_disability_1,
        developmental_disability_2,
        extended_compulsory_education,
        transport_benefit,
        special_assistance_decision_with_group,
        special_assistance_decision_without_group
    FROM koski_placement(today) p
    JOIN daycare d ON p.unit_id = d.id
    JOIN person pr ON p.child_id = pr.id
    LEFT JOIN LATERAL (
        SELECT jsonb_agg(jsonb_build_object('date', a.date, 'type', a.absence_type) ORDER BY a.date) AS preparatory_absences
        FROM absence a
        WHERE a.child_id = p.child_id
        AND a.care_type = 'PRESCHOOL'
        AND between_start_and_end(full_range, a.date)
        AND a.date > '2020-08-01'
    ) pa ON p.type = 'PREPARATORY'
    JOIN LATERAL (
        SELECT
            array_agg(date_interval) FILTER (WHERE 'DEVELOPMENTAL_DISABILITY_1' = ANY(bases)) AS developmental_disability_1,
            array_agg(date_interval) FILTER (WHERE 'DEVELOPMENTAL_DISABILITY_2' = ANY(bases)) AS developmental_disability_2
        FROM (
            SELECT
                an.id,
                daterange(an.start_date, an.end_date, '[]') AS date_interval,
                array_remove(array_agg(abo.value), null) AS bases
            FROM assistance_need an
            LEFT JOIN assistance_basis_option_ref abor ON abor.need_id = an.id
            LEFT JOIN assistance_basis_option abo ON abo.id = abor.option_id
            WHERE an.child_id = p.child_id
            AND daterange(an.start_date, an.end_date, '[]') && full_range
            GROUP BY an.id, daterange(an.start_date, an.end_date, '[]')
            ORDER BY an.id
        ) matching_assistance_need
    ) an ON true
    JOIN LATERAL (
        SELECT
            nullif(daterange(
                min(start_date) FILTER (WHERE 'EXTENDED_COMPULSORY_EDUCATION' = ANY(measures)),
                max(end_date) FILTER (WHERE 'EXTENDED_COMPULSORY_EDUCATION' = ANY(measures)),
                '[]'
            ), daterange(null, null)) AS extended_compulsory_education,
            nullif(daterange(
                min(start_date) FILTER (WHERE 'TRANSPORT_BENEFIT' = ANY(measures)),
                max(end_date) FILTER (WHERE 'TRANSPORT_BENEFIT' = ANY(measures)),
                '[]'
            ), daterange(null, null)) AS transport_benefit,
            array_agg(date_interval) FILTER (
                WHERE 'SPECIAL_ASSISTANCE_DECISION' = ANY(measures) AND 'SPECIAL_GROUP' = ANY(actions)
            ) AS special_assistance_decision_with_group,
            array_agg(date_interval) FILTER (
                WHERE 'SPECIAL_ASSISTANCE_DECISION' = ANY(measures) AND 'SPECIAL_GROUP' != ANY(actions)
            ) AS special_assistance_decision_without_group
        FROM (
            SELECT
                aa.id,
                daterange(aa.start_date, aa.end_date, '[]') AS date_interval,
                aa.start_date,
                aa.end_date,
                aa.measures,
                array_remove(array_agg(aao.value), null) AS actions
            FROM assistance_action aa
            LEFT JOIN assistance_action_option_ref aaor ON aaor.action_id = aa.id
            LEFT JOIN assistance_action_option aao ON aao.id = aaor.option_id
            WHERE aa.child_id = p.child_id
            AND daterange(aa.start_date, aa.end_date, '[]') && full_range
            GROUP BY aa.id, daterange(aa.start_date, aa.end_date, '[]'), aa.start_date, aa.end_date, aa.measures
            ORDER BY aa.id
        ) matching_assistance_action
    ) aa ON true
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
