// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.occupancy.OccupancyType
import fi.espoo.evaka.occupancy.OccupancyValues
import fi.espoo.evaka.occupancy.UnitGroupKey
import fi.espoo.evaka.occupancy.UnitKey
import fi.espoo.evaka.occupancy.calculateDailyGroupOccupancyValues
import fi.espoo.evaka.occupancy.calculateDailyUnitOccupancyValues
import fi.espoo.evaka.occupancy.getSql
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
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
                                    OccupancyValues(
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
                                    OccupancyValues(
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
    val occupancies: Map<LocalDate, OccupancyValues>
)

data class OccupancyGroupReportResultRow(
    val unitId: UUID,
    val unitName: String,
    val groupId: UUID,
    val groupName: String,
    val occupancies: Map<LocalDate, OccupancyValues>
)

private data class OccupancyReportRowGroupedValuesDaily(
    val date: LocalDate,
    val sum: Double,
    val headcount: Int,
    val caretakers: Double? = null,
    val percentage: Double? = null
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

private fun Database.Read.calculateUnitOccupancyReportV2(
    today: LocalDate,
    areaId: UUID,
    queryPeriod: FiniteDateRange,
    type: OccupancyType
): List<OccupancyUnitReportResultRow> {
    return calculateDailyUnitOccupancyValues(today, queryPeriod, type, areaId = areaId)
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

    return calculateDailyGroupOccupancyValues(today, queryPeriod, type, areaId = areaId)
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
