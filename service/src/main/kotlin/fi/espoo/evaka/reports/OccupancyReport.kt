// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.occupancy.OccupancyType
import fi.espoo.evaka.occupancy.getSql
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.ClosedPeriod
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

@RestController
class OccupancyReportController(
    private val jdbi: Jdbi
) {
    @GetMapping("/reports/occupancy-by-unit")
    fun getOccupancyUnitReport(
        user: AuthenticatedUser,
        @RequestParam type: OccupancyType,
        @RequestParam careAreaId: UUID,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ResponseEntity<List<OccupancyUnitReportResultRow>> {
        Audit.OccupancyReportRead.log(targetId = careAreaId)
        user.requireOneOfRoles(Roles.SERVICE_WORKER, Roles.ADMIN, Roles.FINANCE_ADMIN, Roles.DIRECTOR)
        val from = LocalDate.of(year, month, 1)
        val to = from.plusMonths(1).minusDays(1)

        val occupancies = jdbi.transaction(
            calculateOccupancyUnitReport(
                careAreaId,
                ClosedPeriod(from, to),
                type
            )
        )

        val result = occupancies
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

        return ResponseEntity.ok(result)
    }

    @GetMapping("/reports/occupancy-by-group")
    fun getOccupancyGroupReport(
        user: AuthenticatedUser,
        @RequestParam type: OccupancyType,
        @RequestParam careAreaId: UUID,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ResponseEntity<List<OccupancyGroupReportResultRow>> {
        Audit.OccupancyReportRead.log(targetId = careAreaId)
        user.requireOneOfRoles(Roles.SERVICE_WORKER, Roles.ADMIN, Roles.FINANCE_ADMIN, Roles.DIRECTOR)
        val from = LocalDate.of(year, month, 1)
        val to = from.plusMonths(1).minusDays(1)

        val occupancies = jdbi.transaction(
            calculateOccupancyGroupReport(
                careAreaId,
                ClosedPeriod(from, to),
                type
            )
        )

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

private fun calculateOccupancyUnitReport(
    careAreaId: UUID,
    period: ClosedPeriod,
    type: OccupancyType
): (Handle) -> List<OccupancyUnitReportRowRaw> = { h ->
    if (period.start.plusDays(50) < period.end) {
        throw BadRequest("Date range ${period.start} - ${period.end} is too long. Maximum range is 50 days.")
    }

    val sql = getSql(
        type,
        singleUnit = false,
        includeGroups = false
    )

    h.createQuery(sql)
        .bind("careAreaId", careAreaId)
        .bind("startDate", period.start)
        .bind("endDate", period.end)
        .map { rs, _ ->
            OccupancyUnitReportRowRaw(
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
        .filter { it.date.dayOfWeek != DayOfWeek.SATURDAY && it.date.dayOfWeek != DayOfWeek.SUNDAY }
}

private fun calculateOccupancyGroupReport(
    careAreaId: UUID,
    period: ClosedPeriod,
    type: OccupancyType
): (Handle) -> List<OccupancyGroupReportRowRaw> = { h ->
    if (period.start.plusDays(50) < period.end) {
        throw BadRequest("Date range ${period.start} - ${period.end} is too long. Maximum range is 50 days.")
    }

    val sql = getSql(
        type,
        singleUnit = false,
        includeGroups = true
    )

    h.createQuery(sql)
        .bind("careAreaId", careAreaId)
        .bind("startDate", period.start)
        .bind("endDate", period.end)
        .map { rs, _ ->
            OccupancyGroupReportRowRaw(
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
        .filter { it.date.dayOfWeek != DayOfWeek.SATURDAY && it.date.dayOfWeek != DayOfWeek.SUNDAY }
}
