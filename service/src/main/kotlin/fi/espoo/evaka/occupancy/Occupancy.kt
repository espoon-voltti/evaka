// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.occupancy

import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.CareType
import fi.espoo.evaka.daycare.service.getAbsenceCareTypes
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.result.RowView
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

const val youngChildOccupancyCoefficient = "1.75"

enum class OccupancyType {
    PLANNED,
    CONFIRMED,
    REALIZED
}

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
            WHEN is_family_unit THEN $youngChildOccupancyCoefficient
            WHEN age < 3 THEN $youngChildOccupancyCoefficient
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
            WHEN is_family_unit THEN $youngChildOccupancyCoefficient
            WHEN age < 3 THEN $youngChildOccupancyCoefficient
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

interface OccupancyGroupingKey {
    val groupingId: UUID
    val unitId: UUID
}

data class UnitKey(
    override val unitId: UUID,
    val unitName: String
) : OccupancyGroupingKey {
    override val groupingId = unitId
}

data class UnitGroupKey(
    override val unitId: UUID,
    val unitName: String,
    val groupId: UUID,
    val groupName: String
) : OccupancyGroupingKey {
    override val groupingId = groupId
}

data class DailyOccupancyValues<K : OccupancyGroupingKey>(
    val key: K,
    val occupancies: Map<LocalDate, OccupancyValues>
)

data class OccupancyValues(
    val sum: Double,
    val headcount: Int,
    val caretakers: Double? = null,
    val percentage: Double? = null
) {
    fun withPeriod(period: FiniteDateRange) = OccupancyPeriod(
        period, sum, headcount, caretakers, percentage
    )
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

fun Database.Read.calculateDailyUnitOccupancyValues(
    today: LocalDate,
    queryPeriod: FiniteDateRange,
    type: OccupancyType,
    areaId: UUID? = null,
    unitId: UUID? = null
): List<DailyOccupancyValues<UnitKey>> {
    if (areaId == null && unitId == null) error("Must provide areaId or unitId")
    if (type == OccupancyType.REALIZED && today < queryPeriod.start) return listOf()
    val period = getAndValidatePeriod(today, type, queryPeriod, singleUnit = unitId != null)

    val caretakerCounts = getCaretakers(type, period, areaId, unitId) { row ->
        Caretakers(
            UnitKey(
                unitId = row.mapColumn("unit_id"),
                unitName = row.mapColumn("unit_name")
            ),
            date = row.mapColumn("date"),
            caretakerCount = row.mapColumn("caretaker_count")
        )
    }

    val placements = when (type) {
        OccupancyType.REALIZED -> getRealizedPlacements(caretakerCounts.keys, period)
        else -> getPlacements(caretakerCounts.keys, period)
    }

    return calculateDailyOccupancies(caretakerCounts, placements, period, type)
}

fun Database.Read.calculateDailyGroupOccupancyValues(
    today: LocalDate,
    queryPeriod: FiniteDateRange,
    type: OccupancyType,
    areaId: UUID? = null,
    unitId: UUID? = null
): List<DailyOccupancyValues<UnitGroupKey>> {
    if (areaId == null && unitId == null) error("Must provide areaId or unitId")
    if (type == OccupancyType.REALIZED && today < queryPeriod.start) return listOf()
    val period = getAndValidatePeriod(today, type, queryPeriod, singleUnit = unitId != null)

    val caretakerCounts = getCaretakers(type, period, areaId, unitId) { row ->
        Caretakers(
            UnitGroupKey(
                unitId = row.mapColumn("unit_id"),
                unitName = row.mapColumn("unit_name"),
                groupId = row.mapColumn("group_id"),
                groupName = row.mapColumn("group_name")
            ),
            date = row.mapColumn("date"),
            caretakerCount = row.mapColumn("caretaker_count")
        )
    }

    val placements = when (type) {
        OccupancyType.REALIZED -> getRealizedPlacements(caretakerCounts.keys, period)
        else -> getPlacements(caretakerCounts.keys, period)
    }

    return calculateDailyOccupancies(caretakerCounts, placements, period, type)
}

fun <K : OccupancyGroupingKey> reduceDailyOccupancyValues(dailyOccupancies: List<DailyOccupancyValues<K>>): Map<K, List<OccupancyPeriod>> {
    return dailyOccupancies.associateByTo(
        destination = mutableMapOf(),
        keySelector = { it.key },
        valueTransform = { reduceDailyOccupancyValues(it.occupancies) }
    )
}

private fun reduceDailyOccupancyValues(dailyOccupancies: Map<LocalDate, OccupancyValues>): List<OccupancyPeriod> {
    return dailyOccupancies.entries.sortedBy { it.key }
        .fold(listOf<Pair<FiniteDateRange, OccupancyValues>>()) { acc, (date, values) ->
            when {
                acc.isEmpty() -> listOf(FiniteDateRange(date, date) to values)
                else -> acc.last().let { (lastPeriod, lastValues) ->
                    when {
                        values == lastValues && lastPeriod.end.isEqual(date.minusDays(1)) ->
                            acc.dropLast(1) + (FiniteDateRange(lastPeriod.start, date) to values)
                        else -> acc + (FiniteDateRange(date, date) to values)
                    }
                }
            }
        }
        .map { (period, values) -> values.withPeriod(period) }
}

private fun getAndValidatePeriod(
    today: LocalDate,
    type: OccupancyType,
    queryPeriod: FiniteDateRange,
    singleUnit: Boolean
): FiniteDateRange {
    val maxLength = if (singleUnit) 400 else 50

    val period =
        if (type == OccupancyType.REALIZED) queryPeriod.copy(end = minOf(queryPeriod.end, today))
        else queryPeriod

    if (period.start.plusDays(maxLength.toLong()) < period.end) {
        throw BadRequest("Date range ${period.start} - ${period.end} is too long. Maximum range is $maxLength days.")
    }

    return period
}

private inline fun <reified K : OccupancyGroupingKey> Database.Read.getCaretakers(
    type: OccupancyType,
    period: FiniteDateRange,
    areaId: UUID?,
    unitId: UUID?,
    noinline mapper: (RowView) -> Caretakers<K>
): Map<K, List<Caretakers<K>>> {
    // language=sql
    val (caretakersSum, caretakersJoin) =
        if (type == OccupancyType.REALIZED)
            "sum(s.count)" to "staff_attendance s ON g.id = s.group_id AND t = s.date"
        else
            "sum(c.amount)" to "daycare_caretaker c ON g.id = c.group_id AND t BETWEEN c.start_date AND c.end_date"

    // language=sql
    val (keyColumns, groupBy) = when (K::class) {
        UnitKey::class -> "u.id AS unit_id, u.name AS unit_name" to "u.id"
        UnitGroupKey::class -> "g.id AS group_id, g.name AS group_name, u.id AS unit_id, u.name AS unit_name" to "g.id, u.id"
        else -> error("Unsupported caretakers query class parameter (${K::class})")
    }

    // language=sql
    val query = """
SELECT $keyColumns, t::date AS date, coalesce($caretakersSum, 0.0) AS caretaker_count
FROM generate_series(:start, :end, '1 day') t
CROSS JOIN daycare_group g
JOIN daycare u ON g.daycare_id = u.id AND t BETWEEN g.start_date AND g.end_date AND NOT 'CLUB'::care_types = ANY(u.type)
LEFT JOIN $caretakersJoin
LEFT JOIN holiday h ON t = h.date AND NOT u.operation_days @> ARRAY[1, 2, 3, 4, 5, 6, 7]
WHERE date_part('isodow', t) = ANY(u.operation_days) AND h.date IS NULL
AND (:areaId::uuid IS NULL OR u.care_area_id = :areaId)
AND (:unitId::uuid IS NULL OR u.id = :unitId)
GROUP BY $groupBy, t
"""

    return createQuery(query)
        .bindNullable("areaId", areaId)
        .bindNullable("unitId", unitId)
        .bind("start", period.start)
        .bind("end", period.end)
        .map(mapper)
        .groupBy { it.key }
}

private inline fun <reified K : OccupancyGroupingKey> Database.Read.getPlacements(
    keys: Set<K>,
    period: FiniteDateRange
): Iterable<Placement> {
    // language=sql
    val (groupingId, daterange, additionalJoin) = when (K::class) {
        UnitKey::class -> Triple(
            "u.id",
            "daterange(p.start_date, p.end_date, '[]')",
            ""
        )
        UnitGroupKey::class -> Triple(
            "gp.daycare_group_id",
            "daterange(greatest(p.start_date, gp.start_date), least(p.end_date, gp.end_date), '[]')",
            "JOIN daycare_group_placement gp ON gp.daycare_placement_id = p.id"
        )
        else -> error("Unsupported placement query class parameter (${K::class})")
    }

    // language=sql
    val query = """
SELECT
    $groupingId AS grouping_id,
    p.id AS placement_id,
    p.child_id,
    p.unit_id,
    p.type,
    u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] AS family_unit_placement,
    $daterange AS period
FROM placement p
JOIN daycare u ON p.unit_id = u.id
$additionalJoin
WHERE $daterange && :period AND $groupingId = ANY(:keys)
"""

    return this.createQuery(query)
        .bind("keys", keys.map { it.groupingId }.toTypedArray())
        .bind("period", period)
        .mapTo()
}

private inline fun <reified K : OccupancyGroupingKey> Database.Read.getRealizedPlacements(
    keys: Set<K>,
    period: FiniteDateRange
): Iterable<Placement> {
    val placements = getPlacements(keys, period)

    // remove periods where child is away in backup care
    val periodsAwayByChild = getPeriodsAwayInBackupCareByChildId(period, placements.map { it.childId }.toSet())
    val placementsWithChildPresent = placements.flatMap { placement ->
        val periodsAway = periodsAwayByChild.getOrDefault(placement.childId, emptyList())
        val periodsPresent = placement.period.complement(periodsAway)
        periodsPresent.map { period -> placement.copy(period = period) }
    }

    // add backup care placements into these units/groups
    return placementsWithChildPresent + getBackupCarePlacements(keys, period)
}

private fun Database.Read.getPeriodsAwayInBackupCareByChildId(
    period: FiniteDateRange,
    childIds: Set<UUID>
): Map<UUID, List<FiniteDateRange>> {
    data class QueryResult(
        val childId: UUID,
        val startDate: LocalDate,
        val endDate: LocalDate
    )

    // language=sql
    val query = """
SELECT bc.child_id, bc.start_date, bc.end_date
FROM backup_care bc
WHERE daterange(bc.start_date, bc.end_date, '[]') && :period AND bc.child_id = ANY(:childIds)
"""

    return this.createQuery(query)
        .bind("childIds", childIds.toTypedArray())
        .bind("period", period)
        .mapTo<QueryResult>()
        .groupBy { it.childId }
        .mapValues { entry -> entry.value.map { FiniteDateRange(it.startDate, it.endDate) } }
}

private inline fun <reified K : OccupancyGroupingKey> Database.Read.getBackupCarePlacements(
    keys: Set<K>,
    period: FiniteDateRange
): Iterable<Placement> {
    val groupingId = when (K::class) {
        UnitKey::class -> "bc.unit_id"
        UnitGroupKey::class -> "bc.group_id"
        else -> error("Unsupported placement query class parameter (${K::class})")
    }

    // language=sql
    val query = """
SELECT
    $groupingId AS grouping_id,
    p.id AS placement_id,
    bc.child_id,
    bc.unit_id,
    p.type,
    u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] AS family_unit_placement,
    daterange(greatest(bc.start_date, p.start_date), least(bc.end_date, p.end_date), '[]') AS period
FROM backup_care bc
JOIN daycare u ON bc.unit_id = u.id
JOIN placement p ON bc.child_id = p.child_id AND daterange(bc.start_date, bc.end_date, '[]') && daterange(p.start_date, p.end_date, '[]')
WHERE daterange(greatest(bc.start_date, p.start_date), least(bc.end_date, p.end_date), '[]') && :period AND $groupingId = ANY(:keys)
"""

    return this.createQuery(query)
        .bind("keys", keys.map { it.groupingId }.toTypedArray())
        .bind("period", period)
        .mapTo()
}

private fun <K : OccupancyGroupingKey> Database.Read.calculateDailyOccupancies(
    caretakerCounts: Map<K, List<Caretakers<K>>>,
    placements: Iterable<Placement>,
    period: FiniteDateRange,
    type: OccupancyType
): List<DailyOccupancyValues<K>> {
    val placementPlans =
        if (type == OccupancyType.PLANNED)
            this.createQuery(
                """
SELECT u.id AS grouping_id, p.id AS placement_id, a.child_id, p.unit_id, p.type, u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] AS family_unit_placement, daterange(p.start_date, p.end_date, '[]') AS period
FROM placement_plan p
JOIN application a ON p.application_id = a.id
JOIN daycare u ON p.unit_id = u.id AND u.id = ANY(:unitIds)
WHERE NOT p.deleted AND daterange(p.start_date, p.end_date, '[]') && :period
"""
            )
                .bind("unitIds", caretakerCounts.keys.map { it.unitId }.toTypedArray())
                .bind("period", period)
                .mapTo<Placement>()
        else
            listOf()

    val childIds = (placements.map { it.childId } + placementPlans.map { it.childId }).toSet().toTypedArray()

    val childBirthdays = this.createQuery("SELECT id, date_of_birth FROM person WHERE id = ANY(:childIds)")
        .bind("childIds", childIds)
        .mapTo<Child>()
        .map { it.id to it.dateOfBirth }
        .toMap()

    val serviceNeeds = this.createQuery(
        """
SELECT sn.placement_id, sno.occupancy_coefficient, daterange(sn.start_date, sn.end_date, '[]') AS period
FROM new_service_need sn
JOIN service_need_option sno ON sn.option_id = sno.id
WHERE sn.placement_id = ANY(:placementIds)
"""
    )
        .bind("placementIds", placements.map { it.placementId }.toList().toTypedArray())
        .mapTo<ServiceNeed>()
        .groupBy { it.placementId }

    val defaultServiceNeedCoefficients = this.createQuery("SELECT occupancy_coefficient, valid_placement_type FROM service_need_option WHERE default_option")
        .map { row -> row.mapColumn<PlacementType>("valid_placement_type") to row.mapColumn<BigDecimal>("occupancy_coefficient") }
        .toMap()

    val assistanceNeeds = this.createQuery("SELECT child_id, capacity_factor, daterange(start_date, end_date, '[]') AS period FROM assistance_need WHERE child_id = ANY(:childIds)")
        .bind("childIds", childIds)
        .mapTo<AssistanceNeed>()
        .groupBy { it.childId }

    val absences =
        if (type == OccupancyType.REALIZED)
            this.createQuery("SELECT child_id, date, care_type FROM absence WHERE child_id = ANY(:childIds) AND :period @> date AND NOT absence_type = :presence")
                .bind("childIds", childIds)
                .bind("period", period)
                .bind("presence", AbsenceType.PRESENCE)
                .mapTo<Absence>()
                .groupBy { it.childId }
        else
            mapOf()

    fun getCoefficient(date: LocalDate, placement: Placement): BigDecimal {
        val assistanceCoefficient = assistanceNeeds[placement.childId]
            ?.find { it.period.includes(date) }
            ?.capacityFactor
            ?: BigDecimal.ONE

        val dateOfBirth = childBirthdays[placement.childId]
            ?: error("No date of birth found for child ${placement.childId}")

        val serviceNeedCoefficient = when {
            placement.familyUnitPlacement -> BigDecimal(youngChildOccupancyCoefficient)
            date < dateOfBirth.plusYears(3) -> BigDecimal(youngChildOccupancyCoefficient)
            else -> serviceNeeds[placement.placementId]
                ?.let { placementServiceNeeds ->
                    placementServiceNeeds.find { it.period.includes(date) }?.occupancyCoefficient
                }
                ?: defaultServiceNeedCoefficients[placement.type]
                ?: error("No default service need found for placement type ${placement.type}")
        }

        return assistanceCoefficient * serviceNeedCoefficient
    }

    val placementsAndPlans = (placements + placementPlans).groupBy { it.groupingId }

    return caretakerCounts
        .map { (key, values) ->
            val occupancies = values
                .associate { caretakers ->
                    val placementsOnDate = (placementsAndPlans[key.groupingId] ?: listOf())
                        .filter { it.period.includes(caretakers.date) }
                        .filterNot { childWasAbsentWholeDay(caretakers.date, it.type, absences[it.childId] ?: listOf()) }

                    val coefficientSum = placementsOnDate
                        .groupBy { it.childId }
                        .mapNotNull { (_, childPlacements) ->
                            childPlacements.map { getCoefficient(caretakers.date, it) }.maxOrNull()
                        }
                        .fold(BigDecimal.ZERO) { sum, coefficient -> sum + coefficient }

                    val percentage =
                        if (caretakers.caretakerCount.compareTo(BigDecimal.ZERO) == 0) null
                        else coefficientSum
                            .divide(caretakers.caretakerCount * BigDecimal(7), 4, RoundingMode.HALF_EVEN)
                            .times(BigDecimal(100))
                            .setScale(1, RoundingMode.HALF_EVEN)

                    caretakers.date to OccupancyValues(
                        sum = coefficientSum.toDouble(),
                        headcount = placementsOnDate.size,
                        percentage = percentage?.toDouble(),
                        caretakers = caretakers.caretakerCount.toDouble().takeUnless { it == 0.0 }
                    )
                }

            DailyOccupancyValues(
                key = key,
                occupancies = occupancies
            )
        }
}

private fun childWasAbsentWholeDay(
    date: LocalDate,
    childPlacementType: PlacementType,
    childAbsences: List<Absence>
): Boolean {
    val absencesOnDate = childAbsences.filter { it.date == date }.map { it.careType }.toSet()
    return absencesOnDate.isNotEmpty() && absencesOnDate == getAbsenceCareTypes(childPlacementType).toSet()
}

private data class Caretakers<K : OccupancyGroupingKey>(
    val key: K,
    val date: LocalDate,
    val caretakerCount: BigDecimal
)

private data class Placement(
    val groupingId: UUID,
    val placementId: UUID,
    val childId: UUID,
    val unitId: UUID,
    val type: PlacementType,
    val familyUnitPlacement: Boolean,
    val period: FiniteDateRange
)

private data class Child(
    val id: UUID,
    val dateOfBirth: LocalDate
)

private data class ServiceNeed(
    val placementId: UUID,
    val occupancyCoefficient: BigDecimal,
    val period: FiniteDateRange
)

private data class AssistanceNeed(
    val childId: UUID,
    val capacityFactor: BigDecimal,
    val period: FiniteDateRange
)

private data class Absence(
    val childId: UUID,
    val date: LocalDate,
    val careType: CareType
)
