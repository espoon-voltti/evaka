// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.occupancy

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.fetchApplicationDetails
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.placement.PlacementPlanService
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/occupancy")
class OccupancyController(
    private val accessControl: AccessControl,
    private val placementPlanService: PlacementPlanService
) {
    @GetMapping("/by-unit/{unitId}/realtime")
    fun getRealtimeOccupancy(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): RealtimeOccupancy {
        Audit.OccupancyRead.log(targetId = unitId)
        accessControl.requirePermissionFor(user, Action.Unit.READ_OCCUPANCIES, unitId)

        return db.connect { dbc ->
            dbc.read {
                RealtimeOccupancy(
                    childAttendances = it.getChildOccupancyAttendances(unitId, date),
                    staffAttendances = it.getStaffOccupancyAttendances(unitId, date)
                )
            }
        }
    }

    @GetMapping("/by-unit/{unitId}")
    fun getOccupancyPeriods(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
        @RequestParam type: OccupancyType
    ): OccupancyResponse {
        Audit.OccupancyRead.log(targetId = unitId)
        accessControl.requirePermissionFor(user, Action.Unit.READ_OCCUPANCIES, unitId)

        val occupancies = db.connect { dbc ->
            dbc.read {
                it.calculateOccupancyPeriods(unitId, FiniteDateRange(from, to), type)
            }
        }

        return OccupancyResponse(
            occupancies = occupancies,
            max = occupancies.filter { it.percentage != null }.maxByOrNull { it.percentage!! },
            min = occupancies.filter { it.percentage != null }.minByOrNull { it.percentage!! }
        )
    }

    @GetMapping("/by-unit/{unitId}/speculated/{applicationId}")
    fun getOccupancyPeriodsSpeculated(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable applicationId: ApplicationId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) preschoolDaycareFrom: LocalDate?,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) preschoolDaycareTo: LocalDate?,
    ): OccupancyResponseSpeculated {
        Audit.OccupancySpeculatedRead.log(targetId = Pair(unitId, applicationId))
        accessControl.requirePermissionFor(user, Action.Unit.READ_OCCUPANCIES, unitId)
        accessControl.requirePermissionFor(user, Action.Application.READ_WITHOUT_ASSISTANCE_NEED, applicationId)

        val period = FiniteDateRange(from, to)
        val preschoolDaycarePeriod = if (preschoolDaycareFrom != null && preschoolDaycareTo != null) {
            FiniteDateRange(preschoolDaycareFrom, preschoolDaycareTo)
        } else {
            null
        }

        return db.connect { dbc ->
            dbc.read { tx ->
                val unit = tx.getDaycare(unitId) ?: throw NotFound("Unit $unitId not found")
                val application =
                    tx.fetchApplicationDetails(applicationId) ?: throw NotFound("Application $applicationId not found")

                val speculatedPlacements = placementPlanService.calculateSpeculatedPlacements(
                    tx,
                    unitId,
                    application,
                    period,
                    preschoolDaycarePeriod
                ).map {
                    Placement(
                        groupingId = it.unitId,
                        placementId = it.id,
                        childId = it.childId,
                        unitId = it.unitId,
                        type = it.type,
                        familyUnitPlacement = unit.type.contains(CareType.FAMILY) || unit.type.contains(CareType.GROUP_FAMILY),
                        period = FiniteDateRange(it.startDate, it.endDate),
                    )
                }

                val (threeMonths, sixMonths) = calculateSpeculatedMaxOccupancies(
                    tx,
                    LocalDate.now(),
                    unitId,
                    speculatedPlacements,
                    from,
                    lengthsInMonths = listOf(3, 6),
                )
                OccupancyResponseSpeculated(
                    max3Months = threeMonths.current,
                    max6Months = sixMonths.current,
                    max3MonthsSpeculated = threeMonths.speculated,
                    max6MonthsSpeculated = sixMonths.speculated,
                )
            }
        }
    }

    @GetMapping("/by-unit/{unitId}/groups")
    fun getOccupancyPeriodsOnGroups(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
        @RequestParam type: OccupancyType
    ): List<OccupancyResponseGroupLevel> {
        Audit.OccupancyRead.log(targetId = unitId)
        accessControl.requirePermissionFor(user, Action.Unit.READ_OCCUPANCIES, unitId)

        val occupancies = db.connect { dbc ->
            dbc.read {
                it.calculateOccupancyPeriodsGroupLevel(unitId, FiniteDateRange(from, to), type)
            }
        }

        return occupancies.groupBy({ it.groupId }) {
            OccupancyPeriod(it.period, it.sum, it.headcount, it.caretakers, it.percentage)
        }.entries.map { (key, value) ->
            OccupancyResponseGroupLevel(
                groupId = key,
                occupancies = OccupancyResponse(
                    occupancies = value,
                    max = value.filter { it.percentage != null }.maxByOrNull { it.percentage!! },
                    min = value.filter { it.percentage != null }.minByOrNull { it.percentage!! }
                )
            )
        }
    }
}

data class OccupancyResponseGroupLevel(
    val groupId: GroupId,
    val occupancies: OccupancyResponse
)

data class OccupancyResponse(
    val occupancies: List<OccupancyPeriod>,
    val max: OccupancyPeriod?,
    val min: OccupancyPeriod?
)

data class OccupancyResponseSpeculated(
    val max3Months: OccupancyValues?,
    val max6Months: OccupancyValues?,
    val max3MonthsSpeculated: OccupancyValues?,
    val max6MonthsSpeculated: OccupancyValues?,
)

/*
 * Series of occupancy rates in a given unit during given closed period
 *
 * The coefficient of a child is determined as a product of following values:
 * - age coefficient, children under 3 years old = 1.75, children at least 3 years old = 1.0
 * - service need coefficient, full time daycare / preschool = 1.0, part time daycare = 0.54, part time daycare with preschool = 0.5
 * - assistance need coefficient = X, or default = 1.0
 */
fun Database.Read.calculateOccupancyPeriods(
    unitId: DaycareId,
    period: FiniteDateRange,
    type: OccupancyType
): List<OccupancyPeriod> {
    if (period.start.plusYears(2) < period.end) {
        throw BadRequest("Date range ${period.start} - ${period.end} is too long. Maximum range is two years.")
    }

    return reduceDailyOccupancyValues(
        calculateDailyUnitOccupancyValues(LocalDate.now(), period, type, unitId = unitId)
    ).flatMap { (_, values) -> values }
}

fun Database.Read.calculateOccupancyPeriodsGroupLevel(
    unitId: DaycareId,
    period: FiniteDateRange,
    type: OccupancyType
): List<OccupancyPeriodGroupLevel> {
    if (period.start.plusYears(2) < period.end) {
        throw BadRequest("Date range ${period.start} - ${period.end} is too long. Maximum range is two years.")
    }

    return reduceDailyOccupancyValues(
        calculateDailyGroupOccupancyValues(LocalDate.now(), period, type, unitId = unitId)
    ).flatMap { (groupKey, values) ->
        values.map { value ->
            OccupancyPeriodGroupLevel(
                groupId = groupKey.groupId,
                period = value.period,
                sum = value.sum,
                headcount = value.headcount,
                percentage = value.percentage,
                caretakers = value.caretakers
            )
        }
    }
}

private data class SpeculatedMaxOccupancies(
    val current: OccupancyValues?,
    val speculated: OccupancyValues?,
)

private fun calculateSpeculatedMaxOccupancies(
    tx: Database.Read,
    now: LocalDate,
    unitId: DaycareId,
    speculatedPlacements: List<Placement>,
    from: LocalDate,
    lengthsInMonths: List<Long>,
): List<SpeculatedMaxOccupancies> {
    val longestLength = lengthsInMonths.maxOrNull() ?: throw Error("TODO")
    val longestPeriod = FiniteDateRange(from, from.plusMonths(longestLength).minusDays(1))

    val currentOccupancies = tx.calculateDailyUnitOccupancyValues(
        now,
        longestPeriod,
        OccupancyType.PLANNED,
        unitId = unitId
    )
    val speculatedOccupancies = tx.calculateDailyUnitOccupancyValues(
        now,
        longestPeriod,
        OccupancyType.PLANNED,
        unitId = unitId,
        speculatedPlacements = speculatedPlacements
    )

    return lengthsInMonths.map { months ->
        val period = FiniteDateRange(from, from.plusMonths(months).minusDays(1))
        SpeculatedMaxOccupancies(
            current = maxUnitOccupancyValues(currentOccupancies, unitId, period),
            speculated = maxUnitOccupancyValues(speculatedOccupancies, unitId, period)
        )
    }
}

private fun maxUnitOccupancyValues(
    occupancies: List<DailyOccupancyValues<UnitKey>>,
    unitId: DaycareId,
    period: FiniteDateRange
): OccupancyValues? {
    if (occupancies.isEmpty()) return null

    val unitOccupancies = occupancies
        .single { it.key.unitId == unitId }
        .occupancies

    return unitOccupancies
        .filter { period.includes(it.key) }
        .values
        .filter { it.percentage != null }
        .maxByOrNull { it.percentage!! }
}
