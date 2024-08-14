// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.occupancy

import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.attendance.StaffAttendanceType
import fi.espoo.evaka.attendance.occupancyCoefficientSeven
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.db.Row
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import fi.espoo.evaka.shared.security.actionrule.toPredicate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested

const val familyUnitPlacementCoefficient = "1.75"
const val workingDayHours = 7.65 // 7 hours 39 minutes
const val defaultOccupancyCoefficient = 7

enum class OccupancyType {
    PLANNED,
    CONFIRMED,
    REALIZED
}

interface OccupancyGroupingKey {
    val groupingId: Id<*>
    val unitId: DaycareId
}

data class UnitKey(
    val areaId: AreaId,
    val areaName: String,
    override val unitId: DaycareId,
    val unitName: String
) : OccupancyGroupingKey {
    override val groupingId = unitId
}

data class UnitGroupKey(
    val areaId: AreaId,
    val areaName: String,
    override val unitId: DaycareId,
    val unitName: String,
    val groupId: GroupId,
    val groupName: String
) : OccupancyGroupingKey {
    override val groupingId = groupId
}

data class DailyOccupancyValues<K : OccupancyGroupingKey>(
    val key: K,
    val occupancies: Map<LocalDate, OccupancyValues>
)

data class OccupancyValues(
    val sumUnder3y: Double,
    val sumOver3y: Double,
    val headcount: Int,
    val caretakers: Double? = null,
    val percentage: Double? = null
) {
    val sum = sumUnder3y + sumOver3y

    fun withPeriod(period: FiniteDateRange) =
        OccupancyPeriod(period, sum, headcount, caretakers, percentage)
}

data class OccupancyPeriod(
    val period: FiniteDateRange,
    val sum: Double,
    val headcount: Int,
    val caretakers: Double? = null,
    val percentage: Double? = null
)

data class OccupancyPeriodGroupLevel(
    val groupId: GroupId,
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
    unitFilter: AccessControlFilter<DaycareId>,
    areaId: AreaId? = null,
    providerType: ProviderType? = null,
    unitTypes: Set<CareType>? = null,
    unitId: DaycareId? = null,
    speculatedPlacements: List<Placement> = listOf()
): List<DailyOccupancyValues<UnitKey>> {
    if (type == OccupancyType.REALIZED && today < queryPeriod.start) return listOf()
    val period = getAndValidatePeriod(today, type, queryPeriod, singleUnit = unitId != null)

    val caretakerCounts =
        getCaretakers(type, period, unitFilter, areaId, providerType, unitTypes, unitId) {
            Caretakers(row<UnitKey>(), column("date"), column("caretaker_count"))
        }

    val placements =
        when (type) {
            OccupancyType.REALIZED -> getRealizedPlacements(caretakerCounts.keys, period)
            else -> getPlacements(caretakerCounts.keys, period)
        }.let { placements ->
            val childrenToRemove = speculatedPlacements.map { it.childId }
            placements.filterNot { childrenToRemove.contains(it.childId) } + speculatedPlacements
        }

    return calculateDailyOccupancies(caretakerCounts, placements, period, type)
}

fun Database.Read.calculateDailyGroupOccupancyValues(
    today: LocalDate,
    queryPeriod: FiniteDateRange,
    type: OccupancyType,
    unitFilter: AccessControlFilter<DaycareId>,
    areaId: AreaId? = null,
    providerType: ProviderType? = null,
    unitTypes: Set<CareType>? = null,
    unitId: DaycareId? = null
): List<DailyOccupancyValues<UnitGroupKey>> {
    if (type == OccupancyType.REALIZED && today < queryPeriod.start) return listOf()
    val period = getAndValidatePeriod(today, type, queryPeriod, singleUnit = unitId != null)

    val caretakerCounts =
        getCaretakers(type, period, unitFilter, areaId, providerType, unitTypes, unitId) {
            Caretakers(row<UnitGroupKey>(), column("date"), column("caretaker_count"))
        }

    val placements =
        when (type) {
            OccupancyType.REALIZED -> getRealizedPlacements(caretakerCounts.keys, period)
            else -> getPlacements(caretakerCounts.keys, period)
        }

    return calculateDailyOccupancies(caretakerCounts, placements, period, type)
}

fun <K : OccupancyGroupingKey> reduceDailyOccupancyValues(
    dailyOccupancies: List<DailyOccupancyValues<K>>
): Map<K, List<OccupancyPeriod>> {
    return dailyOccupancies.associateByTo(
        destination = mutableMapOf(),
        keySelector = { it.key },
        valueTransform = { reduceDailyOccupancyValues(it.occupancies) }
    )
}

private fun reduceDailyOccupancyValues(
    dailyOccupancies: Map<LocalDate, OccupancyValues>
): List<OccupancyPeriod> {
    return dailyOccupancies.entries
        .sortedBy { it.key }
        .fold(listOf<Pair<FiniteDateRange, OccupancyValues>>()) { acc, (date, values) ->
            when {
                acc.isEmpty() -> listOf(FiniteDateRange(date, date) to values)
                else ->
                    acc.last().let { (lastPeriod, lastValues) ->
                        when {
                            values == lastValues && lastPeriod.end.isEqual(date.minusDays(1)) ->
                                acc.dropLast(1) +
                                    (FiniteDateRange(lastPeriod.start, date) to values)
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
        if (type == OccupancyType.REALIZED) {
            queryPeriod.copy(end = minOf(queryPeriod.end, today))
        } else {
            queryPeriod
        }

    if (period.start.plusDays(maxLength.toLong()) < period.end) {
        throw BadRequest(
            "Date range ${period.start} - ${period.end} is too long. Maximum range is $maxLength days."
        )
    }

    return period
}

private inline fun <reified K : OccupancyGroupingKey> Database.Read.getCaretakers(
    type: OccupancyType,
    period: FiniteDateRange,
    unitFilter: AccessControlFilter<DaycareId>,
    areaId: AreaId?,
    providerType: ProviderType?,
    unitTypes: Set<CareType>?,
    unitId: DaycareId?,
    noinline mapper: Row.() -> Caretakers<K>
): Map<K, List<Caretakers<K>>> {
    val unitPredicate =
        Predicate.allNotNull(
            Predicate {
                where("daterange($it.opening_date, $it.closing_date, '[]') && ${bind(period)}")
            },
            if (areaId != null) Predicate { where("$it.care_area_id = ${bind(areaId)}") } else null,
            if (unitId != null) Predicate { where("$it.id = ${bind(unitId)}") } else null,
            unitFilter.toPredicate(),
            if (providerType != null)
                Predicate { where("$it.provider_type = ${bind(providerType)}") }
            else null,
            if (unitTypes?.isEmpty() == false) Predicate { where("$it.type && ${bind(unitTypes)}") }
            else null
        )
    val caretakersSum =
        if (type == OccupancyType.REALIZED) {
            """
            sum(
                CASE
                    WHEN sar.arrived IS NOT NULL
                        THEN ROUND(EXTRACT(EPOCH FROM (
                            LEAST(sar.departed, timezone('Europe/Helsinki', (t::date + 1)::date::timestamp)) - GREATEST(sar.arrived, timezone('Europe/Helsinki', t::date::timestamp))
                        )) / 3600 / $workingDayHours * sar.occupancy_coefficient / $defaultOccupancyCoefficient, 4)
                    ELSE s.count
                END
            )
            """
        } else {
            "sum(c.amount)"
        }

    val presentStaffAttendanceTypes =
        "'{${StaffAttendanceType.values().filter { it.presentInGroup() }.joinToString()}}'::staff_attendance_type[]"
    val caretakersJoin =
        if (type == OccupancyType.REALIZED) {
            """
            LEFT JOIN (
                SELECT group_id, arrived, departed, occupancy_coefficient
                FROM staff_attendance_realtime
                WHERE departed IS NOT NULL AND type = ANY($presentStaffAttendanceTypes)
                UNION ALL
                SELECT group_id, arrived, departed, occupancy_coefficient
                FROM staff_attendance_external
                WHERE departed IS NOT NULL
            ) sar ON g.id = sar.group_id AND (t = DATE(sar.arrived) OR t = DATE(sar.departed))
            LEFT JOIN staff_attendance s ON g.id = s.group_id AND t = s.date
            """
        } else {
            """
            LEFT JOIN daycare_caretaker c ON g.id = c.group_id AND daterange(c.start_date, c.end_date, '[]') @> t::date
            """
        }

    val (keyColumns, groupBy) =
        when (K::class) {
            UnitKey::class ->
                "a.id AS area_id, a.name AS area_name, u.id AS unit_id, u.name AS unit_name" to
                    "a.id, u.id"
            UnitGroupKey::class ->
                "a.id AS area_id, a.name AS area_name, g.id AS group_id, g.name AS group_name, u.id AS unit_id, u.name AS unit_name" to
                    "a.id, g.id, u.id"
            else -> error("Unsupported caretakers query class parameter (${K::class})")
        }

    return createQuery {
            sql(
                """
SELECT $keyColumns, t::date AS date,
coalesce(
    $caretakersSum,
    0.0
) AS caretaker_count
FROM generate_series(${bind(period.start)}, ${bind(period.end)}, '1 day') t
CROSS JOIN daycare_group g
JOIN daycare u ON g.daycare_id = u.id AND daterange(g.start_date, g.end_date, '[]') @> t::date
JOIN care_area a ON a.id = u.care_area_id
$caretakersJoin
LEFT JOIN holiday h ON t = h.date AND NOT u.shift_care_open_on_holidays
WHERE date_part('isodow', t) = ANY(coalesce(u.shift_care_operation_days, u.operation_days)) AND h.date IS NULL
AND ${predicate(unitPredicate.forTable("u"))}
GROUP BY $groupBy, t
"""
            )
        }
        .toList(mapper)
        .groupBy { it.key }
}

private inline fun <reified K : OccupancyGroupingKey> Database.Read.getPlacements(
    keys: Set<K>,
    period: FiniteDateRange
): Iterable<Placement> {
    val (groupingId, daterange, additionalJoin) =
        when (K::class) {
            UnitKey::class -> Triple("u.id", "daterange(p.start_date, p.end_date, '[]')", "")
            UnitGroupKey::class ->
                Triple(
                    "gp.daycare_group_id",
                    "daterange(greatest(p.start_date, gp.start_date), least(p.end_date, gp.end_date), '[]')",
                    "JOIN daycare_group_placement gp ON gp.daycare_placement_id = p.id"
                )
            else -> error("Unsupported placement query class parameter (${K::class})")
        }

    return this.createQuery {
            sql(
                """
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
WHERE $daterange && ${bind(period)} AND $groupingId = ANY(${bind(keys.map { it.groupingId })})
"""
            )
        }
        .toList<Placement>()
}

private inline fun <reified K : OccupancyGroupingKey> Database.Read.getRealizedPlacements(
    keys: Set<K>,
    period: FiniteDateRange
): Iterable<Placement> {
    val placements = getPlacements(keys, period)

    // remove periods where child is away in backup care
    val periodsAwayByChild =
        getPeriodsAwayInBackupCareByChildId(period, placements.map { it.childId }.toSet())
    val placementsWithChildPresent =
        placements.flatMap { placement ->
            val periodsAway = periodsAwayByChild.getOrDefault(placement.childId, emptyList())
            val periodsPresent = placement.period.complement(periodsAway)
            periodsPresent.map { period -> placement.copy(period = period) }
        }

    // add backup care placements into these units/groups
    return placementsWithChildPresent + getBackupCarePlacements(keys, period)
}

private fun Database.Read.getPeriodsAwayInBackupCareByChildId(
    period: FiniteDateRange,
    childIds: Set<ChildId>
): Map<ChildId, List<FiniteDateRange>> {
    data class QueryResult(val childId: ChildId, val startDate: LocalDate, val endDate: LocalDate)

    return this.createQuery {
            sql(
                """
SELECT bc.child_id, bc.start_date, bc.end_date
FROM backup_care bc
WHERE daterange(bc.start_date, bc.end_date, '[]') && ${bind(period)} AND bc.child_id = ANY(${bind(childIds)})
"""
            )
        }
        .toList<QueryResult>()
        .groupBy { it.childId }
        .mapValues { entry -> entry.value.map { FiniteDateRange(it.startDate, it.endDate) } }
}

private inline fun <reified K : OccupancyGroupingKey> Database.Read.getBackupCarePlacements(
    keys: Set<K>,
    period: FiniteDateRange
): Iterable<Placement> {
    val groupingId =
        when (K::class) {
            UnitKey::class -> "bc.unit_id"
            UnitGroupKey::class -> "bc.group_id"
            else -> error("Unsupported placement query class parameter (${K::class})")
        }

    return this.createQuery {
            sql(
                """
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
WHERE daterange(greatest(bc.start_date, p.start_date), least(bc.end_date, p.end_date), '[]') && ${bind(period)} AND $groupingId = ANY(${bind(keys.map { it.groupingId })})
"""
            )
        }
        .toList<Placement>()
}

private fun <K : OccupancyGroupingKey> Database.Read.calculateDailyOccupancies(
    caretakerCounts: Map<K, List<Caretakers<K>>>,
    placements: Iterable<Placement>,
    range: FiniteDateRange,
    type: OccupancyType
): List<DailyOccupancyValues<K>> {
    val placementPlans =
        if (type == OccupancyType.PLANNED) {
            this.getPlacementPlans(range, caretakerCounts.keys.map { it.unitId })
        } else {
            listOf()
        }

    val childIds = (placements.map { it.childId } + placementPlans.map { it.childId }).toSet()

    val childBirthdays =
        this.createQuery {
                sql("SELECT id, date_of_birth FROM person WHERE id = ANY(${bind(childIds)})")
            }
            .toMap { columnPair<ChildId, LocalDate>("id", "date_of_birth") }

    val serviceNeeds =
        this.createQuery {
                sql(
                    """
SELECT
    sn.placement_id,
    CASE
        WHEN u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] THEN $familyUnitPlacementCoefficient
        ELSE sno.occupancy_coefficient
    END AS occupancy_coefficient,
    CASE
        WHEN u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] THEN $familyUnitPlacementCoefficient
        ELSE sno.occupancy_coefficient_under_3y
    END AS occupancy_coefficient_under_3y,
    CASE
        WHEN u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] THEN $familyUnitPlacementCoefficient
        ELSE sno.realized_occupancy_coefficient
    END AS realized_occupancy_coefficient,
    CASE
        WHEN u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] THEN $familyUnitPlacementCoefficient
        ELSE sno.realized_occupancy_coefficient_under_3y
    END AS realized_occupancy_coefficient_under_3y,
    daterange(sn.start_date, sn.end_date, '[]') AS period
FROM service_need sn
JOIN placement pl ON sn.placement_id = pl.id
JOIN service_need_option sno ON sn.option_id = sno.id
JOIN person ch ON ch.id = pl.child_id
JOIN daycare u ON pl.unit_id = u.id
WHERE sn.placement_id = ANY(${bind(placements.map { it.placementId })})
"""
                )
            }
            .toList<ServiceNeed>()
            .groupBy { it.placementId }

    val defaultServiceNeedCoefficients =
        this.createQuery {
                sql(
                    """
                    SELECT 
                        occupancy_coefficient, 
                        occupancy_coefficient_under_3y, 
                        realized_occupancy_coefficient, 
                        realized_occupancy_coefficient_under_3y, 
                        valid_placement_type
                    FROM service_need_option WHERE default_option
                    """
                )
            }
            .toMap {
                column<PlacementType>("valid_placement_type") to row<ServiceNeedCoefficients>()
            }

    val assistanceFactors =
        createQuery {
                sql(
                    "SELECT child_id, capacity_factor, valid_during AS period FROM assistance_factor WHERE child_id = ANY(${bind(childIds)})"
                )
            }
            .toList<AssistanceFactor>()
            .groupBy { it.childId }

    val absences =
        if (type == OccupancyType.REALIZED) {
            this.createQuery {
                    sql(
                        "SELECT child_id, date, category FROM absence WHERE child_id = ANY(${bind(childIds)}) AND between_start_and_end(${bind(range)}, date)"
                    )
                }
                .toList<Absence>()
                .groupBy { it.childId }
        } else {
            mapOf()
        }

    fun getCoefficient(date: LocalDate, placement: Placement): Pair<BigDecimal, Boolean> {
        val assistanceCoefficient =
            assistanceFactors[placement.childId]?.find { it.period.includes(date) }?.capacityFactor
                ?: BigDecimal.ONE

        val dateOfBirth =
            childBirthdays[placement.childId]
                ?: error("No date of birth found for child ${placement.childId}")
        val under3y = date < dateOfBirth.plusYears(3)

        val serviceNeedCoefficient = run {
            val coefficients =
                serviceNeeds[placement.placementId]
                    ?.let { it.find { sn -> sn.period.includes(date) } }
                    ?.let {
                        ServiceNeedCoefficients(
                            it.occupancyCoefficient,
                            it.occupancyCoefficientUnder3y,
                            it.realizedOccupancyCoefficient,
                            it.realizedOccupancyCoefficientUnder3y
                        )
                    }
                    ?: defaultServiceNeedCoefficients[placement.type]
                    ?: error("No occupancy coefficients found for placement type ${placement.type}")

            when (type) {
                OccupancyType.REALIZED ->
                    when {
                        placement.familyUnitPlacement -> BigDecimal(familyUnitPlacementCoefficient)
                        under3y -> coefficients.realizedOccupancyCoefficientUnder3y
                        else -> coefficients.realizedOccupancyCoefficient
                    }
                else ->
                    when {
                        placement.familyUnitPlacement -> BigDecimal(familyUnitPlacementCoefficient)
                        under3y -> coefficients.occupancyCoefficientUnder3y
                        else -> coefficients.occupancyCoefficient
                    }
            }
        }

        return assistanceCoefficient * serviceNeedCoefficient to under3y
    }

    val placementsAndPlans = (placements + placementPlans).groupBy { it.groupingId }

    return caretakerCounts.map { (key, values) ->
        val occupancies =
            values.associate { caretakers ->
                val placementsOnDate =
                    (placementsAndPlans[key.groupingId] ?: listOf())
                        .filter { it.period.includes(caretakers.date) }
                        .filterNot {
                            childWasAbsentWholeDay(
                                caretakers.date,
                                it.type,
                                absences[it.childId] ?: listOf()
                            )
                        }

                val coefficientSum =
                    placementsOnDate
                        .groupBy { it.childId }
                        .mapNotNull { (_, childPlacements) ->
                            childPlacements
                                .map { getCoefficient(caretakers.date, it) }
                                .maxByOrNull { it.first }
                        }
                        .fold(CoefficientSum.ZERO) { sum, (coefficient, under3y) ->
                            if (under3y) sum.copy(under3y = sum.under3y + coefficient)
                            else sum.copy(over3y = sum.over3y + coefficient)
                        }

                val percentage =
                    if (caretakers.caretakerCount.compareTo(BigDecimal.ZERO) == 0) {
                        null
                    } else {
                        coefficientSum.sum
                            .divide(
                                caretakers.caretakerCount * occupancyCoefficientSeven,
                                4,
                                RoundingMode.HALF_EVEN
                            )
                            .times(BigDecimal(100))
                            .setScale(1, RoundingMode.HALF_EVEN)
                    }

                caretakers.date to
                    OccupancyValues(
                        sumUnder3y = coefficientSum.under3y.toDouble(),
                        sumOver3y = coefficientSum.over3y.toDouble(),
                        headcount = placementsOnDate.size,
                        percentage = percentage?.toDouble(),
                        caretakers = caretakers.caretakerCount.toDouble().takeUnless { it == 0.0 }
                    )
            }

        DailyOccupancyValues(key = key, occupancies = occupancies)
    }
}

private data class ServiceNeedCoefficients(
    val occupancyCoefficient: BigDecimal,
    val occupancyCoefficientUnder3y: BigDecimal,
    val realizedOccupancyCoefficient: BigDecimal,
    val realizedOccupancyCoefficientUnder3y: BigDecimal
)

private data class CoefficientSum(val under3y: BigDecimal, val over3y: BigDecimal) {
    val sum = under3y + over3y

    companion object {
        val ZERO = CoefficientSum(BigDecimal.ZERO, BigDecimal.ZERO)
    }
}

private fun Database.Read.getPlacementPlans(
    period: FiniteDateRange,
    unitIds: Collection<DaycareId>
): List<Placement> {
    return this.createQuery {
            sql(
                """
SELECT
    u.id AS grouping_id,
    p.id AS placement_id,
    a.child_id,
    p.unit_id,
    p.type,
    u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] AS family_unit_placement,
    daterange(p.start_date, p.end_date, '[]') AS period,
    preschool_daycare_start_date,
    preschool_daycare_end_date
FROM placement_plan p
JOIN application a ON p.application_id = a.id
JOIN daycare u ON p.unit_id = u.id AND u.id = ANY(${bind(unitIds)})
WHERE NOT p.deleted AND (
    daterange(p.start_date, p.end_date, '[]') && ${bind(period)}
    OR (
        preschool_daycare_start_date IS NOT NULL
        AND preschool_daycare_end_date IS NOT NULL
        AND daterange(preschool_daycare_start_date, preschool_daycare_end_date, '[]') && ${bind(period)}
    )
)
"""
            )
        }
        .toList<PlacementPlan>()
        .flatMap { placementPlan ->
            // If the placement plan has preschool daycare dates set it means that the placement
            // plan could in reality
            // be split into parts with and without preschool daycare, so take that into account.
            if (
                placementPlan.preschoolDaycareStartDate == null ||
                    placementPlan.preschoolDaycareEndDate == null
            ) {
                listOf(placementPlan.placement)
            } else {
                val preschoolDaycarePeriod =
                    FiniteDateRange(
                        placementPlan.preschoolDaycareStartDate,
                        placementPlan.preschoolDaycareEndDate
                    )
                val onlyPreschoolPeriods =
                    placementPlan.placement.period.complement(preschoolDaycarePeriod)

                onlyPreschoolPeriods.map {
                    placementPlan.placement.copy(
                        type =
                            when (placementPlan.placement.type) {
                                PlacementType.PREPARATORY_DAYCARE -> PlacementType.PREPARATORY
                                PlacementType.PRESCHOOL_DAYCARE -> PlacementType.PRESCHOOL
                                PlacementType.PRESCHOOL_CLUB -> PlacementType.PRESCHOOL
                                else -> placementPlan.placement.type
                            },
                        period = it
                    )
                } + placementPlan.placement.copy(period = preschoolDaycarePeriod)
            }
        }
}

private fun childWasAbsentWholeDay(
    date: LocalDate,
    childPlacementType: PlacementType,
    childAbsences: List<Absence>
): Boolean {
    val absencesOnDate = childAbsences.filter { it.date == date }.map { it.category }.toSet()
    return absencesOnDate.isNotEmpty() && absencesOnDate == childPlacementType.absenceCategories()
}

private data class Caretakers<K : OccupancyGroupingKey>(
    val key: K,
    val date: LocalDate,
    val caretakerCount: BigDecimal
)

data class Placement(
    val groupingId: DaycareId,
    val placementId: PlacementId,
    val childId: ChildId,
    val unitId: DaycareId,
    val type: PlacementType,
    val familyUnitPlacement: Boolean,
    val period: FiniteDateRange
)

private data class PlacementPlan(
    @Nested val placement: Placement,
    val preschoolDaycareStartDate: LocalDate?,
    val preschoolDaycareEndDate: LocalDate?
)

data class ServiceNeed(
    val placementId: PlacementId,
    val occupancyCoefficient: BigDecimal,
    val occupancyCoefficientUnder3y: BigDecimal,
    val realizedOccupancyCoefficient: BigDecimal,
    val realizedOccupancyCoefficientUnder3y: BigDecimal,
    val period: FiniteDateRange
)

data class AssistanceFactor(
    val childId: ChildId,
    val capacityFactor: BigDecimal,
    val period: FiniteDateRange
)

data class Absence(val childId: ChildId, val date: LocalDate, val category: AbsenceCategory)
