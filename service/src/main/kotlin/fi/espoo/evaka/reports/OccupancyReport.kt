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
import org.jdbi.v3.core.mapper.Nested
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

        val occupancies = db.read {
            if (useNewServiceNeeds == true)
                it.calculateOccupancyUnitReportV2(
                    LocalDate.now(),
                    careAreaId,
                    FiniteDateRange(from, to),
                    type
                )
            else
                it.calculateOccupancyUnitReport(
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
        @RequestParam month: Int
    ): ResponseEntity<List<OccupancyGroupReportResultRow>> {
        Audit.OccupancyReportRead.log(targetId = careAreaId)
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.ADMIN, UserRole.DIRECTOR)
        val from = LocalDate.of(year, month, 1)
        val to = from.plusMonths(1).minusDays(1)

        val occupancies = db.read {
            it.calculateOccupancyGroupReport(
                careAreaId,
                FiniteDateRange(from, to),
                type
            )
        }

        val result = occupancies
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

        return ResponseEntity.ok(result)
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
    val unitId: UUID,
    val unitName: String
)

private data class UnitGroupKey(
    val unitId: UUID,
    val unitName: String,
    val groupId: UUID,
    val groupName: String
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

private data class Caretakers(
    @Nested
    val unit: UnitKey,
    val date: LocalDate,
    val caretakerCount: BigDecimal
)

private data class Placement(
    val id: UUID,
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

private fun Database.Read.calculateOccupancyUnitReportV2(
    today: LocalDate,
    areaId: UUID,
    queryPeriod: FiniteDateRange,
    type: OccupancyType
): List<OccupancyUnitReportResultRow> {
    val period =
        if (type == OccupancyType.REALIZED) queryPeriod.copy(end = minOf(queryPeriod.end, today))
        else queryPeriod

    if (period.start.plusDays(50) < period.end) {
        throw BadRequest("Date range ${period.start} - ${period.end} is too long. Maximum range is 50 days.")
    }

    val caretakers =
        if (type == OccupancyType.REALIZED)
            this.createQuery(
                """
SELECT u.id AS unit_id, u.name AS unit_name, t::date AS date, coalesce(sum(s.count), 0.0) AS caretaker_count
FROM generate_series(:start, :end, '1 day') t
CROSS JOIN daycare_group g
JOIN daycare u ON g.daycare_id = u.id AND u.care_area_id = :areaId AND t BETWEEN g.start_date AND g.end_date AND NOT 'CLUB'::care_types = ANY(u.type)
LEFT JOIN staff_attendance s ON g.id = s.group_id AND t = s.date
LEFT JOIN holiday h ON t = h.date AND NOT u.operation_days @> ARRAY[1, 2, 3, 4, 5, 6, 7]
WHERE date_part('isodow', t) = ANY(u.operation_days) AND h.date IS NULL
GROUP BY u.id, t
"""
            )
                .bind("areaId", areaId)
                .bind("start", period.start)
                .bind("end", period.end)
                .mapTo<Caretakers>()
                .groupBy { it.unit }
        else
            this.createQuery(
                """
SELECT u.id AS unit_id, u.name AS unit_name, t::date AS date, coalesce(sum(c.amount), 0.0) AS caretaker_count
FROM generate_series(:start, :end, '1 day') t
CROSS JOIN daycare_group g
JOIN daycare u ON g.daycare_id = u.id AND u.care_area_id = :areaId AND t BETWEEN g.start_date AND g.end_date AND NOT 'CLUB'::care_types = ANY(u.type)
LEFT JOIN daycare_caretaker c ON g.id = c.group_id AND t BETWEEN c.start_date AND c.end_date
LEFT JOIN holiday h ON t = h.date AND NOT u.operation_days @> ARRAY[1, 2, 3, 4, 5, 6, 7]
WHERE date_part('isodow', t) = ANY(u.operation_days) AND h.date IS NULL
GROUP BY u.id, t
"""
            )
                .bind("areaId", areaId)
                .bind("start", period.start)
                .bind("end", period.end)
                .mapTo<Caretakers>()
                .groupBy { it.unit }

    val placements = this.createQuery(
        """
SELECT p.id, p.child_id, p.unit_id, p.type, u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] AS family_unit_placement, daterange(p.start_date, p.end_date, '[]') AS period
FROM placement p
JOIN daycare u ON p.unit_id = u.id AND u.care_area_id = :areaId
WHERE daterange(p.start_date, p.end_date, '[]') && :period
"""
    )
        .bind("areaId", areaId)
        .bind("period", period)
        .mapTo<Placement>()

    val placementPlans =
        if (type == OccupancyType.PLANNED)
            this.createQuery(
                """
SELECT p.id, a.child_id, p.unit_id, p.type, u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] AS family_unit_placement, daterange(p.start_date, p.end_date, '[]') AS period
FROM placement_plan p
JOIN application a ON p.application_id = a.id
JOIN daycare u ON p.unit_id = u.id AND u.care_area_id = :areaId
WHERE NOT p.deleted AND daterange(p.start_date, p.end_date, '[]') && :period
"""
            )
                .bind("areaId", areaId)
                .bind("period", period)
                .mapTo<Placement>()
        else
            listOf()

    val childIds = (placements.map { it.childId } + placementPlans.map { it.childId }).toTypedArray()
    val childBirthdays = this.createQuery("SELECT id, date_of_birth FROM person WHERE id = ANY(:childIds)")
        .bind("childIds", childIds)
        .mapTo<Child>()
        .map { it.id to it.dateOfBirth }
        .toMap()

    val serviceNeedCoefficients = this.createQuery(
        """
SELECT sn.placement_id, sno.occupancy_coefficient, daterange(sn.start_date, sn.end_date, '[]') AS period
FROM new_service_need sn
JOIN service_need_option sno ON sn.option_id = sno.id
WHERE sn.placement_id = ANY(:placementIds)
"""
    )
        .bind("placementIds", placements.map { it.id }.toList().toTypedArray())
        .mapTo<ServiceNeed>()
        .groupBy { it.placementId }

    val defaultServiceNeedCoefficients = this.createQuery("SELECT occupancy_coefficient, valid_placement_type FROM service_need_option WHERE default_option")
        .map { row -> row.mapColumn<PlacementType>("valid_placement_type") to row.mapColumn<BigDecimal>("occupancy_coefficient") }
        .toMap()

    val assistanceNeedCoefficients = this.createQuery("SELECT child_id, capacity_factor, daterange(start_date, end_date, '[]') AS period FROM assistance_need WHERE child_id = ANY(:childIds)")
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
        val assistanceCoefficient = assistanceNeedCoefficients[placement.childId]
            ?.find { it.period.includes(date) }
            ?.capacityFactor
            ?: BigDecimal.ONE

        val dateOfBirth = childBirthdays[placement.childId]
            ?: error("No date of birth found for child ${placement.childId}")

        val serviceNeedCoefficient = when {
            placement.familyUnitPlacement -> BigDecimal(youngChildOccupancyCoefficient)
            date < dateOfBirth.plusYears(3) -> BigDecimal(youngChildOccupancyCoefficient)
            else -> serviceNeedCoefficients[placement.id]
                ?.let { placementServiceNeeds ->
                    placementServiceNeeds.find { it.period.includes(date) }?.occupancyCoefficient
                }
                ?: defaultServiceNeedCoefficients[placement.type]
                ?: error("No default service need found for placement type ${placement.type}")
        }

        return assistanceCoefficient * serviceNeedCoefficient
    }

    val placementsAndPlans = (placements + placementPlans).groupBy { it.unitId }

    return caretakers
        .map { (unit, values) ->
            val occupancies = values
                .associate { (_, date, caretakers) ->
                    val unitPlacementsOnDate = (placementsAndPlans[unit.unitId] ?: listOf())
                        .filter { it.period.includes(date) }
                        .filterNot { absences[it.childId]?.any { absence -> absence.date == date } ?: false }

                    val sum = unitPlacementsOnDate
                        .groupBy { it.childId }
                        .mapNotNull { (_, placements) -> placements.map { getCoefficient(date, it) }.maxOrNull() }
                        .fold(BigDecimal.ZERO) { sum, value -> sum + value }

                    val percentage =
                        if (caretakers.compareTo(BigDecimal.ZERO) == 0) null
                        else sum
                            .divide(caretakers * BigDecimal(7), 10, RoundingMode.HALF_UP)
                            .times(BigDecimal(100))
                            .setScale(1, RoundingMode.HALF_UP)

                    date to OccupancyReportRowGroupedValues(
                        sum = sum.toDouble(),
                        headcount = unitPlacementsOnDate.size,
                        percentage = percentage?.toDouble(),
                        caretakers = caretakers.toDouble().takeUnless { it == 0.0 }
                    )
                }

            OccupancyUnitReportResultRow(
                unitId = unit.unitId,
                unitName = unit.unitName,
                occupancies = occupancies
            )
        }
        .sortedBy { it.unitName }
}
