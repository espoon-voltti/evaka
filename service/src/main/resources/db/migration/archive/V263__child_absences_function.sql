CREATE FUNCTION absences_in_range(childIds uuid[], period daterange)
    RETURNS TABLE
            (
                id               uuid,
                child_id         uuid,
                date             date,
                absence_type     absence_type,
                modified_at      timestamp with time zone,
                modified_by      uuid,
                category         absence_category,
                questionnaire_id uuid
            )
AS
$$
BEGIN
    RETURN QUERY
        SELECT *
        FROM absence a
        WHERE between_start_and_end($2, a.date)
          AND ($1 IS NULL OR a.child_id = ANY ($1))
        UNION
        SELECT dst.id                             as id,
               dst.child_id                       as child_id,
               d::date                            as date,
               'OTHER_ABSENCE'::absence_type      as absence_type,
               dst.updated                        as modified_at,
               null::uuid                         as modified_by,
               unnest(absence_categories(p.type)) as category,
               null::uuid                         as questionnaire_id
        FROM daily_service_time as dst
                 JOIN generate_series(lower($2), upper($2) - 1, '1 day') as d ON dst.validity_period @> d::date
                 JOIN placement as p ON d BETWEEN p.start_date AND p.end_date AND p.child_id = dst.child_id
        WHERE ($1 IS NULL OR dst.child_id = ANY ($1))
          AND dst.type = 'IRREGULAR'
          AND NOT EXISTS(SELECT ca.date FROM child_attendance ca WHERE ca.child_id = dst.child_id AND ca.date = d)
          AND NOT EXISTS(SELECT ar.date FROM attendance_reservation ar WHERE ar.child_id = dst.child_id AND ar.date = d)
          AND (dst.sunday_times IS NOT NULL OR dst.monday_times IS NOT NULL OR dst.tuesday_times IS NOT NULL OR
               dst.wednesday_times IS NOT NULL OR dst.thursday_times IS NOT NULL OR dst.friday_times IS NOT NULL OR
               dst.saturday_times IS NOT NULL)
          AND CASE extract(dow FROM d)
                  WHEN 0 THEN dst.sunday_times IS NULL
                  WHEN 1 THEN dst.monday_times IS NULL
                  WHEN 2 THEN dst.tuesday_times IS NULL
                  WHEN 3 THEN dst.wednesday_times IS NULL
                  WHEN 4 THEN dst.thursday_times IS NULL
                  WHEN 5 THEN dst.friday_times IS NULL
                  WHEN 6 THEN dst.saturday_times IS NULL
            END;
END;
$$
    LANGUAGE plpgsql;
