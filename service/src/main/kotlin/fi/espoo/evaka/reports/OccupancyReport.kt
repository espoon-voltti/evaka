// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.occupancy.OccupancyType
import fi.espoo.evaka.occupancy.getSql
import fi.espoo.evaka.occupancy.youngChildOccupancyCoefficient
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.result.RowView
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

@RestController
class OccupancyReportController {
    @GetMapping("/reports/occupancy-by-unit")
    fun getOccupancyUnitReport(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam type: OccupancyType,
        @RequestParam careAreaId: UUID,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam(required = false) useNewServiceNeeds: Boolean? = false
    ): ResponseEntity<List<OccupancyUnitReportResultRow>> {
        Audit.OccupancyReportRead.log(targetId = careAreaId)
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.ADMIN, UserRole.DIRECTOR)
        val from = LocalDate.of(year, month, 1)
        val to = from.plusMonths(1).minusDays(1)

        val occupancies = db.read { tx ->
            if (useNewServiceNeeds == true)
                tx.calculateUnitOccupancyReportV2(
                    LocalDate.now(),
                    careAreaId,
                    FiniteDateRange(from, to),
                    type
                )
            else
                tx.calculateOccupancyUnitReport(
                    careAreaId,
                    FiniteDateRange(from, to),
                    type
                )
                    .groupBy(
                        { UnitKey(it.unitId, it.unitName) },
                        {
                            OccupancyReportRowGroupedValuesDaily(
                                date = it.date,
                                sum = it.sum,
                                headcount = it.headcount,
                                caretakers = it.caretakers,
                                percentage = it.percentage
                            )
                        }
                    )
                    .entries
                    .map { entry ->
                        OccupancyUnitReportResultRow(
                            unitId = entry.key.unitId,
                            unitName = entry.key.unitName,
                            occupancies = entry.value.associateBy(
                                { it.date },
                                {
                                    OccupancyReportRowGroupedValues(
                                        sum = it.sum,
                                        headcount = it.headcount,
                                        caretakers = it.caretakers,
                                        percentage = it.percentage
                                    )
                                }
                            ).toSortedMap()
                        )
                    }.sortedBy { it.unitName }
        }

        return ResponseEntity.ok(occupancies)
    }

    @GetMapping("/reports/occupancy-by-group")
    fun getOccupancyGroupReport(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam type: OccupancyType,
        @RequestParam careAreaId: UUID,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam(required = false) useNewServiceNeeds: Boolean? = false
    ): ResponseEntity<List<OccupancyGroupReportResultRow>> {
        Audit.OccupancyReportRead.log(targetId = careAreaId)
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.ADMIN, UserRole.DIRECTOR)
        val from = LocalDate.of(year, month, 1)
        val to = from.plusMonths(1).minusDays(1)

        val occupancies = db.read { tx ->
            if (useNewServiceNeeds == true)
                tx.calculateGroupOccupancyReportV2(
                    LocalDate.now(),
                    careAreaId,
                    FiniteDateRange(from, to),
                    type
                )
            else
                tx.calculateOccupancyGroupReport(
                    careAreaId,
                    FiniteDateRange(from, to),
                    type
                )
                    .groupBy(
                        { UnitGroupKey(it.unitId, it.unitName, it.groupId, it.groupName) },
                        {
                            OccupancyReportRowGroupedValuesDaily(
                                date = it.date,
                                sum = it.sum,
                                headcount = it.headcount,
                                caretakers = it.caretakers,
                                percentage = it.percentage
                            )
                        }
                    )
                    .entries
                    .map { entry ->
                        OccupancyGroupReportResultRow(
                            unitId = entry.key.unitId,
                            unitName = entry.key.unitName,
                            groupId = entry.key.groupId,
                            groupName = entry.key.groupName,
                            occupancies = entry.value.associateBy(
                                { it.date },
                                {
                                    OccupancyReportRowGroupedValues(
                                        sum = it.sum,
                                        headcount = it.headcount,
                                        caretakers = it.caretakers,
                                        percentage = it.percentage
                                    )
                                }
                            ).toSortedMap()
                        )
                    }.sortedBy { it.unitName }
        }

        return ResponseEntity.ok(occupancies)
    }
}

data class OccupancyUnitReportResultRow(
    val unitId: UUID,
    val unitName: String,
    val occupancies: Map<LocalDate, OccupancyReportRowGroupedValues>
)

data class OccupancyGroupReportResultRow(
    val unitId: UUID,
    val unitName: String,
    val groupId: UUID,
    val groupName: String,
    val occupancies: Map<LocalDate, OccupancyReportRowGroupedValues>
)

data class OccupancyReportRowGroupedValues(
    val sum: Double,
    val headcount: Int,
    val caretakers: Double? = null,
    val percentage: Double? = null
)

private data class OccupancyReportRowGroupedValuesDaily(
    val date: LocalDate,
    val sum: Double,
    val headcount: Int,
    val caretakers: Double? = null,
    val percentage: Double? = null
)

private data class UnitKey(
    override val unitId: UUID,
    val unitName: String
) : Key {
    override val groupingId = unitId
}

private data class UnitGroupKey(
    override val unitId: UUID,
    val unitName: String,
    val groupId: UUID,
    val groupName: String
) : Key {
    override val groupingId = groupId
}

private data class OccupancyReportResultRow<K : Key>(
    val key: K,
    val occupancies: Map<LocalDate, OccupancyReportRowGroupedValues>
)

private data class OccupancyUnitReportRowRaw(
    val unitId: UUID,
    val unitName: String,
    val date: LocalDate,
    val sum: Double,
    val headcount: Int,
    val caretakers: Double? = null,
    val percentage: Double? = null
)

private data class OccupancyGroupReportRowRaw(
    val unitId: UUID,
    val unitName: String,
    val groupId: UUID,
    val groupName: String,
    val date: LocalDate,
    val sum: Double,
    val headcount: Int,
    val caretakers: Double? = null,
    val percentage: Double? = null
)

private fun Database.Read.calculateOccupancyUnitReport(
    careAreaId: UUID,
    period: FiniteDateRange,
    type: OccupancyType
): List<OccupancyUnitReportRowRaw> {
    if (period.start.plusDays(50) < period.end) {
        throw BadRequest("Date range ${period.start} - ${period.end} is too long. Maximum range is 50 days.")
    }

    val sql = getSql(
        type,
        singleUnit = false,
        includeGroups = false
    )

    return createQuery(sql)
        .bind("careAreaId", careAreaId)
        .bind("startDate", period.start)
        .bind("endDate", period.end)
        .map { rs, _ ->
            rs.getBoolean("is_operation_day") to OccupancyUnitReportRowRaw(
                unitId = rs.getUUID("unit_id"),
                unitName = rs.getString("unit_name"),
                date = rs.getDate("day").toLocalDate(),
                sum = rs.getDouble("sum"),
                headcount = rs.getInt("headcount"),
                percentage = rs.getBigDecimal("percentage")?.toDouble(),
                caretakers = rs.getBigDecimal("caretakers")?.toDouble()
            )
        }
        .list()
        .filter { (isOperationDay, _) -> isOperationDay }
        .map { (_, reportRow) -> reportRow }
}

private fun Database.Read.calculateOccupancyGroupReport(
    careAreaId: UUID,
    period: FiniteDateRange,
    type: OccupancyType
): List<OccupancyGroupReportRowRaw> {
    if (period.start.plusDays(50) < period.end) {
        throw BadRequest("Date range ${period.start} - ${period.end} is too long. Maximum range is 50 days.")
    }

    val sql = getSql(
        type,
        singleUnit = false,
        includeGroups = true
    )

    return createQuery(sql)
        .bind("careAreaId", careAreaId)
        .bind("startDate", period.start)
        .bind("endDate", period.end)
        .map { rs, _ ->
            rs.getBoolean("is_operation_day") to OccupancyGroupReportRowRaw(
                unitId = rs.getUUID("unit_id"),
                unitName = rs.getString("unit_name"),
                groupId = rs.getUUID("group_id"),
                groupName = rs.getString("group_name"),
                date = rs.getDate("day").toLocalDate(),
                sum = rs.getDouble("sum"),
                headcount = rs.getInt("headcount"),
                percentage = rs.getBigDecimal("percentage")?.toDouble(),
                caretakers = rs.getBigDecimal("caretakers")?.toDouble()
            )
        }
        .list()
        .filter { (isOperationDay, _) -> isOperationDay }
        .map { (_, reportRow) -> reportRow }
}

private interface Key {
    val groupingId: UUID
    val unitId: UUID
}

private data class Caretakers<K : Key>(
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
    val date: LocalDate
)

private fun Database.Read.calculateUnitOccupancyReportV2(
    today: LocalDate,
    areaId: UUID,
    queryPeriod: FiniteDateRange,
    type: OccupancyType
): List<OccupancyUnitReportResultRow> {
    val period = getAndValidatePeriod(today, type, queryPeriod)

    val caretakerCounts = getCaretakers(type, period, areaId) { row ->
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
        .map { (key, occupancies) ->
            OccupancyUnitReportResultRow(
                unitId = key.unitId,
                unitName = key.unitName,
                occupancies = occupancies
            )
        }
        .sortedBy { it.unitName }
}

private fun Database.Read.calculateGroupOccupancyReportV2(
    today: LocalDate,
    areaId: UUID,
    queryPeriod: FiniteDateRange,
    type: OccupancyType
): List<OccupancyGroupReportResultRow> {
    if (type == OccupancyType.PLANNED) throw BadRequest("Unable to calculate planned occupancy at group level")

    val period = getAndValidatePeriod(today, type, queryPeriod)

    val caretakerCounts = getCaretakers(type, period, areaId) { row ->
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
        .map { (key, occupancies) ->
            OccupancyGroupReportResultRow(
                unitId = key.unitId,
                unitName = key.unitName,
                groupId = key.groupId,
                groupName = key.groupName,
                occupancies = occupancies
            )
        }
        .sortedBy { it.unitName }
}

private fun getAndValidatePeriod(today: LocalDate, type: OccupancyType, queryPeriod: FiniteDateRange): FiniteDateRange {
    val period =
        if (type == OccupancyType.REALIZED) queryPeriod.copy(end = minOf(queryPeriod.end, today))
        else queryPeriod

    if (period.start.plusDays(50) < period.end) {
        throw BadRequest("Date range ${period.start} - ${period.end} is too long. Maximum range is 50 days.")
    }

    return period
}

private inline fun <reified K : Key> Database.Read.getCaretakers(
    type: OccupancyType,
    period: FiniteDateRange,
    areaId: UUID,
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
JOIN daycare u ON g.daycare_id = u.id AND u.care_area_id = :areaId AND t BETWEEN g.start_date AND g.end_date AND NOT 'CLUB'::care_types = ANY(u.type)
LEFT JOIN $caretakersJoin
LEFT JOIN holiday h ON t = h.date AND NOT u.operation_days @> ARRAY[1, 2, 3, 4, 5, 6, 7]
WHERE date_part('isodow', t) = ANY(u.operation_days) AND h.date IS NULL
GROUP BY $groupBy, t
"""

    return createQuery(query)
        .bind("areaId", areaId)
        .bind("start", period.start)
        .bind("end", period.end)
        .map(mapper)
        .groupBy { it.key }
}

private inline fun <reified K : Key> Database.Read.getPlacements(
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

private inline fun <reified K : Key> Database.Read.getRealizedPlacements(
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

private inline fun <reified K : Key> Database.Read.getBackupCarePlacements(
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
    daterange(greatest(bc.start_date, p.start_date, '[]'), least(bc.end_date, p.end_date), '[]') AS period
FROM backup_care bc
JOIN daycare u ON bc.unit_id = u.id
JOIN placement p ON bc.child_id = p.child_id AND daterange(bc.start_date, bc.end_date, '[]') && daterange(bc.start_date, bc.end_date, '[]')
WHERE daterange(greatest(bc.start_date, p.start_date, '[]'), least(bc.end_date, p.end_date), '[]') && :period AND $groupingId = ANY(:keys)
"""

    return this.createQuery(query)
        .bind("keys", keys.map { it.groupingId }.toTypedArray())
        .bind("period", period)
        .mapTo()
}

private fun <K : Key> Database.Read.calculateDailyOccupancies(
    caretakerCounts: Map<K, List<Caretakers<K>>>,
    placements: Iterable<Placement>,
    period: FiniteDateRange,
    type: OccupancyType
): List<OccupancyReportResultRow<K>> {
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
            this.createQuery("SELECT child_id, date FROM absence WHERE child_id = ANY(:childIds) AND :period @> date AND NOT absence_type = ANY(:nonAbsences)")
                .bind("childIds", childIds)
                .bind("period", period)
                .bind("nonAbsences", AbsenceType.nonAbsences.toTypedArray())
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
                        .filterNot { absences[it.childId]?.any { absence -> absence.date == caretakers.date } ?: false }

                    val coefficientSum = placementsOnDate
                        .groupBy { it.childId }
                        .mapNotNull { (_, childPlacements) ->
                            childPlacements.map { getCoefficient(caretakers.date, it) }.maxOrNull()
                        }
                        .fold(BigDecimal.ZERO) { sum, coefficient -> sum + coefficient }

                    val percentage =
                        if (caretakers.caretakerCount.compareTo(BigDecimal.ZERO) == 0) null
                        else coefficientSum
                            .divide(caretakers.caretakerCount * BigDecimal(7), 4, RoundingMode.HALF_UP)
                            .times(BigDecimal(100))
                            .setScale(1, RoundingMode.HALF_UP)

                    caretakers.date to OccupancyReportRowGroupedValues(
                        sum = coefficientSum.toDouble(),
                        headcount = placementsOnDate.size,
                        percentage = percentage?.toDouble(),
                        caretakers = caretakers.caretakerCount.toDouble().takeUnless { it == 0.0 }
                    )
                }

            OccupancyReportResultRow(
                key = key,
                occupancies = occupancies
            )
        }
}
