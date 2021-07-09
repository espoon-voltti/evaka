// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.occupancy

import fi.espoo.evaka.daycare.service.CareType
import fi.espoo.evaka.daycare.service.getAbsenceCareTypes
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.Nested
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

interface OccupancyGroupingKey {
    val groupingId: Id<*>
    val unitId: DaycareId
}

data class UnitKey(
    override val unitId: DaycareId,
    val unitName: String
) : OccupancyGroupingKey {
    override val groupingId = unitId
}

data class UnitGroupKey(
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
    areaId: AreaId? = null,
    unitId: DaycareId? = null
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
    areaId: AreaId? = null,
    unitId: DaycareId? = null
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
    areaId: AreaId?,
    unitId: DaycareId?,
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
JOIN daycare u ON g.daycare_id = u.id AND t BETWEEN g.start_date AND g.end_date
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
            this.getPlacementPlans(period, caretakerCounts.keys.map { it.unitId }.toTypedArray())
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
FROM service_need sn
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
            this.createQuery("SELECT child_id, date, care_type FROM absence WHERE child_id = ANY(:childIds) AND :period @> date")
                .bind("childIds", childIds)
                .bind("period", period)
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

private fun Database.Read.getPlacementPlans(period: FiniteDateRange, unitIds: Array<DaycareId>): List<Placement> {
    return this.createQuery(
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
JOIN daycare u ON p.unit_id = u.id AND u.id = ANY(:unitIds)
WHERE NOT p.deleted AND daterange(p.start_date, p.end_date, '[]') && :period
"""
    )
        .bind("unitIds", unitIds)
        .bind("period", period)
        .mapTo<PlacementPlan>()
        .flatMap { placementPlan ->
            // If the placement plan has preschool daycare dates set it means that the placement plan could in reality
            // be split into parts with and without preschool daycare, so take that into account.
            if (placementPlan.preschoolDaycareStartDate == null || placementPlan.preschoolDaycareEndDate == null)
                listOf(placementPlan.placement)
            else {
                val placementTypeOutsideDaycare = when (placementPlan.placement.type) {
                    PlacementType.PREPARATORY_DAYCARE -> PlacementType.PREPARATORY
                    PlacementType.PRESCHOOL_DAYCARE -> PlacementType.PRESCHOOL
                    else -> placementPlan.placement.type
                }

                listOfNotNull(
                    if (placementPlan.preschoolDaycareStartDate > placementPlan.placement.period.start)
                        placementPlan.placement.copy(
                            type = placementTypeOutsideDaycare,
                            period = placementPlan.placement.period.copy(
                                end = placementPlan.preschoolDaycareStartDate.minusDays(1)
                            )
                        )
                    else null,
                    placementPlan.placement.copy(
                        period = FiniteDateRange(
                            maxOf(placementPlan.preschoolDaycareStartDate, placementPlan.placement.period.start),
                            minOf(placementPlan.preschoolDaycareEndDate, placementPlan.placement.period.end)
                        )
                    ),
                    if (placementPlan.preschoolDaycareEndDate < placementPlan.placement.period.end)
                        placementPlan.placement.copy(
                            type = placementTypeOutsideDaycare,
                            period = placementPlan.placement.period.copy(
                                start = placementPlan.preschoolDaycareEndDate.plusDays(1)
                            )
                        )
                    else null,
                )
            }
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
    val groupingId: DaycareId,
    val placementId: PlacementId,
    val childId: UUID,
    val unitId: DaycareId,
    val type: PlacementType,
    val familyUnitPlacement: Boolean,
    val period: FiniteDateRange
)

private data class PlacementPlan(
    @Nested
    val placement: Placement,
    val preschoolDaycareStartDate: LocalDate?,
    val preschoolDaycareEndDate: LocalDate?
)

private data class Child(
    val id: UUID,
    val dateOfBirth: LocalDate
)

private data class ServiceNeed(
    val placementId: PlacementId,
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
