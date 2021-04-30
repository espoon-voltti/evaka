// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.occupancy

import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.util.UUID

enum class OccupancyType {
    PLANNED,
    CONFIRMED,
    REALIZED
}

data class OccupancyPeriod(
    val period: FiniteDateRange,
    val sum: Double,
    val headcount: Int,
    val caretakers: Double? = null,
    val percentage: Double? = null
)

data class OccupancyPeriodGroupLevel(
    val groupId: UUID,
    val period: FiniteDateRange,
    val sum: Double,
    val headcount: Int,
    val caretakers: Double? = null,
    val percentage: Double? = null
)

private fun locationsQuery(
    byUnitId: Boolean,
    byCareAreaId: Boolean,
    includeGroups: Boolean
) =
    """
    SELECT
        u.id AS unit_id,
        u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] AS is_family_unit,
        u.operation_days
        ${if (includeGroups) """,
        dg.id AS group_id,
        dg.name AS group_name
        """ else ""}
    FROM daycare u
    ${if (includeGroups) """
    JOIN daycare_group dg on u.id = dg.daycare_id
    """ else ""}
    WHERE 1=1
    ${if (byUnitId) "AND u.id = :unitId" else ""}
    ${if (byCareAreaId) "AND u.care_area_id = :careAreaId AND 'CLUB'::care_types != ANY(u.type)" else ""}
    """.trimIndent()

private fun datesQuery(realized: Boolean) =
    """
    SELECT t
    FROM generate_series(:startDate, ${if (realized) "LEAST(now(), :endDate)" else ":endDate"}, '1 day') t
    """.trimIndent()

private fun realizedPlacements(includeGroups: Boolean) =
    """
    SELECT
        loc.unit_id,
        ${if (includeGroups) "loc.group_id," else ""}
        t AS day,
        p.child_id AS child_id,
        p.type AS type
    FROM locations loc
    CROSS JOIN dates t
    ${if (includeGroups) """
    LEFT JOIN daycare_group_placement gp
        ON gp.daycare_group_id = loc.group_id AND daterange(gp.start_date, gp.end_date, '[]') @> t::date
    LEFT JOIN placement p ON p.id = gp.daycare_placement_id
    """ else """
    LEFT JOIN placement p ON p.unit_id = loc.unit_id AND daterange(p.start_date, p.end_date, '[]') @> t::date
    """}
    WHERE NOT EXISTS (
        SELECT 1 FROM backup_care bc
        WHERE bc.child_id = p.child_id AND daterange(bc.start_date, bc.end_date, '[]') @> t::date
    )

    UNION

    SELECT
        loc.unit_id,
        ${if (includeGroups) "loc.group_id," else ""}
        t AS day,
        p2.child_id AS child_id,
        p2.type AS type
    FROM locations loc
    CROSS JOIN dates t
    JOIN backup_care bc ON bc.unit_id = loc.unit_id ${if (includeGroups) "AND bc.group_id = loc.group_id " else ""}AND daterange(bc.start_date, bc.end_date, '[]') @> t::date
    JOIN placement p2 ON p2.child_id = bc.child_id AND daterange(p2.start_date, p2.end_date, '[]') @> t::date
    """.trimIndent()

private fun coefficientParameters(realized: Boolean, includeGroups: Boolean) =
    """
    SELECT
        loc.unit_id,
        ${if (includeGroups) "loc.group_id," else ""}
        t AS day,
        c.id AS child_id,
        date_part('year', age(t, c.date_of_birth))::integer AS age,
        date_part('year', c.date_of_birth) AS birth_year,
        (CASE
            WHEN date_part('month', t) <= 7 THEN date_part('year', t) - 1
            ELSE date_part('year', t)
        END) AS term_start_year,
        sn.hours_per_week AS hours,
        p.type AS placement_type,
        an.capacity_factor AS assistance_coefficient,
        loc.is_family_unit
        ${if (realized) """,
            EXISTS (
                SELECT 1 FROM absence ab
                WHERE ab.child_id = c.id AND ab.date = t::date AND ab.absence_type != 'PRESENCE'
            ) AS absent
        """ else ""}
    FROM locations loc
    CROSS JOIN dates t
    ${if (includeGroups) """
        ${if (realized) """
            LEFT JOIN realized_placements p ON p.group_id = loc.group_id AND p.day = t::date
        """ else """
            LEFT JOIN daycare_group_placement gp ON gp.daycare_group_id = loc.group_id AND daterange(gp.start_date, gp.end_date, '[]') @> t::date
            LEFT JOIN placement p ON p.id = gp.daycare_placement_id
        """}
    """ else """
        ${if (realized) """
            LEFT JOIN realized_placements p ON p.unit_id = loc.unit_id AND p.day = t::date
        """ else """
            LEFT JOIN placement p ON p.unit_id = loc.unit_id AND daterange(p.start_date, p.end_date, '[]') @> t::date
        """}
    """}
    LEFT JOIN person c ON c.id = p.child_id
    LEFT JOIN service_need sn ON c.id = sn.child_id AND daterange(sn.start_date, sn.end_date, '[]') @> t::date
    LEFT JOIN assistance_need an ON c.id = an.child_id AND daterange(an.start_date, an.end_date, '[]') @> t::date
    """
        .trimIndent()

private fun coefficients(excludeAbsent: Boolean, includeGroups: Boolean) =
    """
    SELECT
        unit_id,
        ${if (includeGroups) "group_id," else ""}
        day,
        child_id,
        coalesce(assistance_coefficient, 1.0) * (CASE
            WHEN age IS NULL THEN 0.0
            WHEN placement_type IS NULL THEN 0.0
            ${if (excludeAbsent) "WHEN absent THEN 0.0" else ""}
            WHEN is_family_unit THEN 1.75
            WHEN age < 3 THEN 1.75
            WHEN placement_type = 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS' AND COALESCE(hours, 0.0) <= 20.0 THEN 0.5
            WHEN placement_type IN ('DAYCARE_FIVE_YEAR_OLDS', 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS') AND hours <= 20 THEN 0.5
            WHEN placement_type IN ('DAYCARE_PART_TIME', 'TEMPORARY_DAYCARE_PART_DAY') THEN 0.54
            WHEN placement_type = 'PRESCHOOL' AND COALESCE(hours, 0.0) <= 20.0 THEN 0.5
            WHEN placement_type = 'PREPARATORY' AND COALESCE(hours, 0.0) <= 25.0 THEN 0.5
            WHEN placement_type = 'PRESCHOOL_DAYCARE' AND hours <= 20.0 THEN 0.5
            WHEN placement_type = 'PREPARATORY_DAYCARE' AND hours <= 25.0 THEN 0.5
            ELSE 1.0
        END) AS coefficient
    FROM coefficient_parameters
    """.trimIndent()

private val plannedCoefficientParameters =
    """
    SELECT
        loc.unit_id,
        t AS day,
        c.id AS child_id,
        p.updated AS updated,
        date_part('year', age(t, c.date_of_birth))::integer AS age,
        date_part('year', c.date_of_birth) AS birth_year,
        (CASE
            WHEN date_part('month', t) <= 7 THEN date_part('year', t) - 1
            ELSE date_part('year', t)
        END) AS term_start_year,
        (CASE
            -- if the date of a preschool + daycare plan is not included in the daycare date range
            -- handle the plan as a normal preschool plan
            WHEN
                p.type IN ('PRESCHOOL_DAYCARE', 'PREPARATORY_DAYCARE') AND
                NOT daterange(p.preschool_daycare_start_date, p.preschool_daycare_end_date, '[]') @> t::date
            THEN 'PRESCHOOL'::placement_type
            ELSE p.type
        END) AS placement_type,
        an.capacity_factor AS assistance_coefficient,
        loc.is_family_unit
    FROM locations loc
    CROSS JOIN dates t
    LEFT JOIN placement_plan p ON p.unit_id = loc.unit_id AND daterange(p.start_date, p.end_date, '[]') @> t::date
    LEFT JOIN application a ON p.application_id = a.id
    LEFT JOIN person c ON a.child_id = c.id
    LEFT JOIN assistance_need an ON c.id = an.child_id AND daterange(an.start_date, an.end_date, '[]') @> t::date
    WHERE p.deleted = false
    """.trimIndent()

private val plannedCoefficients =
    """
    SELECT
        unit_id,
        day,
        child_id,
        updated,
        coalesce(assistance_coefficient, 1.0) * (CASE
            WHEN age IS NULL THEN 0.0
            WHEN is_family_unit THEN 1.75
            WHEN age < 3 THEN 1.75
            WHEN placement_type = 'DAYCARE_PART_TIME' AND (term_start_year - birth_year) = 5 THEN 0.5
            WHEN placement_type = 'DAYCARE_PART_TIME' THEN 0.54
            WHEN placement_type = 'PRESCHOOL' THEN 0.5
            WHEN placement_type = 'PREPARATORY' THEN 0.5
            ELSE 1.0
        END) AS coefficient
    FROM planned_coefficient_parameters
    """.trimIndent()

private val plannedMaxCoefficients =
    """
    SELECT DISTINCT ON (unit_id, day, child_id)
        unit_id,
        day,
        child_id,
        max(coefficient) OVER (PARTITION BY day, child_id) AS coefficient
    FROM planned_coefficients
    """.trimIndent()

private fun finalCoefficients(planned: Boolean) = if (planned) """
    SELECT unit_id, day, child_id, max(coefficient) AS coefficient
    FROM (
        SELECT unit_id, day, child_id, coefficient
        FROM planned_max_coefficients

        UNION ALL

        SELECT unit_id, day, child_id, coefficient
        FROM coefficients c
    ) AS all_coefficients
    GROUP BY unit_id, day, child_id
""".trimIndent() else """
    SELECT * FROM coefficients
""".trimIndent()

private fun dailyCoefficients(includeGroups: Boolean) =
    """
    SELECT unit_id, ${if (includeGroups) "group_id," else ""} day, sum(coefficient) AS sum, count(coefficient) filter(WHERE coefficient > 0) AS headcount
    FROM final_coefficients
    GROUP BY unit_id, ${if (includeGroups) "group_id," else ""} day
    """.trimIndent()

private fun caretakerAmountsPlanned(includeGroups: Boolean) =
    """
    SELECT loc.unit_id, ${if (includeGroups) "g.id AS group_id," else ""} t AS day, sum(ct.amount) AS caretakers
    FROM locations loc
    CROSS JOIN dates t
    LEFT JOIN daycare_group g
        ON g.daycare_id = loc.unit_id AND ${if (includeGroups) "g.id = loc.group_id AND" else ""} daterange(g.start_date, g.end_date, '[]') @> t::date
    LEFT JOIN daycare_caretaker ct ON g.id = ct.group_id AND daterange(ct.start_date, ct.end_date, '[]') @> t::date
    GROUP BY unit_id, ${if (includeGroups) "g.id," else ""} t
    """.trimIndent()

private fun caretakerAmountsRealised(includeGroups: Boolean) =
    """
    SELECT loc.unit_id, ${if (includeGroups) "g.id AS group_id," else ""} t AS day, sum(sa.count) AS caretakers
    FROM locations loc
    CROSS JOIN dates t
    LEFT JOIN daycare_group g
        ON g.daycare_id = loc.unit_id AND ${if (includeGroups) "g.id = loc.group_id AND" else ""} daterange(g.start_date, g.end_date, '[]') @> t::date
    LEFT JOIN staff_attendance sa ON g.id = sa.group_id AND sa.date = t::date
    GROUP BY loc.unit_id, ${if (includeGroups) "g.id," else ""} t
    """.trimIndent()

private fun finalValues(includeGroups: Boolean) =
    """
    SELECT
        unit_id,
        ${if (includeGroups) "group_id," else ""}
        day,
        sum,
        headcount,
        caretakers,
        rank() OVER (PARTITION BY sum, headcount, ${if (includeGroups) "group_id," else ""} caretakers ORDER BY day)
    FROM (
        SELECT c.unit_id, ${if (includeGroups) "c.group_id," else ""} c.day, c.sum, c.headcount, ct.caretakers
        FROM daily_coefficients c
        LEFT JOIN caretaker_amounts ct
            ON c.unit_id = ct.unit_id ${if (includeGroups) "AND c.group_id = ct.group_id" else ""} AND c.day = ct.day
    ) params
    """.trimIndent()

private fun finalSelectorSingleUnit(includeGroups: Boolean) =
    """
    SELECT
        ${if (includeGroups) "group_id," else ""}
        min(day) AS period_start,
        max(day) AS period_end,
        sum,
        headcount,
        round(CASE
            WHEN caretakers IS NULL OR caretakers = 0 THEN NULL
            ELSE sum / (caretakers * 7)
        END, 3) * 100 AS percentage,
        (CASE
            WHEN caretakers IS NULL OR caretakers = 0 THEN NULL
            ELSE caretakers
        END) AS caretakers,
        (day - (rank * '1 day'::interval)) AS grouping_date
    FROM final_values
    GROUP BY ${if (includeGroups) "group_id," else ""} sum, headcount, caretakers, grouping_date
    ORDER BY ${if (includeGroups) "group_id," else ""} period_start
    """.trimIndent()

private fun finalSelectorReport(includeGroups: Boolean) =
    """
    SELECT
        dc.id AS unit_id,
        dc.name AS unit_name,
        ${if (includeGroups) """
        g.id AS group_id,
        g.name AS group_name,
        """ else ""}
        day,
        date_part('isodow', day) = ANY(dc.operation_days) AND h.date IS NULL is_operation_day,
        sum,
        headcount,
        round(CASE
            WHEN caretakers IS NULL OR caretakers = 0 THEN NULL
            ELSE sum / (caretakers * 7)
        END, 3) * 100 AS percentage,
        (CASE
            WHEN caretakers IS NULL OR caretakers = 0 THEN NULL
            ELSE caretakers
        END) AS caretakers
    FROM final_values
    JOIN daycare dc ON dc.id = unit_id
    LEFT JOIN holiday h ON day = h.date AND NOT dc.operation_days @> ARRAY[1, 2, 3, 4, 5, 6, 7]
    ${if (includeGroups) "JOIN daycare_group g ON g.id = group_id" else ""}
    ORDER BY unit_name, ${if (includeGroups) "group_name," else ""} day
    """.trimIndent()

fun getSql(type: OccupancyType, singleUnit: Boolean, includeGroups: Boolean): String {
    if (type == OccupancyType.PLANNED && includeGroups)
        throw BadRequest("Cannot provide group level data for planned occupancy")

    return """
        WITH locations AS (${locationsQuery(
        byUnitId = singleUnit,
        byCareAreaId = !singleUnit,
        includeGroups = includeGroups
    )}),
        ${if (type == OccupancyType.REALIZED) """
            dates as (${datesQuery(true)}),
            realized_placements AS (${realizedPlacements(includeGroups = includeGroups)}),
            coefficient_parameters AS (${coefficientParameters(realized = true, includeGroups = includeGroups)}),
            coefficients AS (${coefficients(excludeAbsent = true, includeGroups = includeGroups)}),
        """ else """
            dates as (${datesQuery(false)}),
            coefficient_parameters AS (${coefficientParameters(realized = false, includeGroups = includeGroups)}),
            coefficients AS (${coefficients(excludeAbsent = false, includeGroups = includeGroups)}),
        """}

        ${if (type == OccupancyType.PLANNED) """
            planned_coefficient_parameters AS ($plannedCoefficientParameters),
            planned_coefficients AS ($plannedCoefficients),
            planned_max_coefficients AS ($plannedMaxCoefficients),
        """ else ""}

        final_coefficients AS (${finalCoefficients(type == OccupancyType.PLANNED)}),
        daily_coefficients AS (${dailyCoefficients(includeGroups = includeGroups)}),
        caretaker_amounts AS (
            ${if (type == OccupancyType.REALIZED) caretakerAmountsRealised(includeGroups = includeGroups)
    else caretakerAmountsPlanned(includeGroups = includeGroups)}
        ),
        final_values AS (${finalValues(includeGroups = includeGroups)})
        ${if (singleUnit) finalSelectorSingleUnit(includeGroups = includeGroups)
    else finalSelectorReport(includeGroups = includeGroups)}
    """.trimIndent()
}
