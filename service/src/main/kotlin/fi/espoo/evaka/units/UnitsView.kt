// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.units

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationUnitSummary
import fi.espoo.evaka.application.getApplicationUnitSummaries
import fi.espoo.evaka.backupcare.UnitBackupCare
import fi.espoo.evaka.backupcare.getBackupCaresForDaycare
import fi.espoo.evaka.daycare.getDaycareGroups
import fi.espoo.evaka.daycare.getGroupStats
import fi.espoo.evaka.daycare.getUnitStats
import fi.espoo.evaka.daycare.service.DaycareGroup
import fi.espoo.evaka.daycare.service.Stats
import fi.espoo.evaka.occupancy.OccupancyPeriod
import fi.espoo.evaka.occupancy.OccupancyPeriodGroupLevel
import fi.espoo.evaka.occupancy.OccupancyResponse
import fi.espoo.evaka.occupancy.OccupancyType
import fi.espoo.evaka.occupancy.calculateOccupancyPeriods
import fi.espoo.evaka.occupancy.calculateOccupancyPeriodsGroupLevel
import fi.espoo.evaka.placement.DaycarePlacementWithDetails
import fi.espoo.evaka.placement.MissingGroupPlacement
import fi.espoo.evaka.placement.PlacementPlanDetails
import fi.espoo.evaka.placement.getDetailedDaycarePlacements
import fi.espoo.evaka.placement.getMissingGroupPlacements
import fi.espoo.evaka.placement.getPlacementPlans
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

val basicDataRoles = arrayOf(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER)
val detailedDataRoles = arrayOf(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN, UserRole.UNIT_SUPERVISOR)

@Controller
@RequestMapping("/views/units")
class UnitsView(private val acl: AccessControlList) {
    @GetMapping("/{unitId}")
    fun getUnitViewData(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam(
            value = "to",
            required = false
        ) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ): ResponseEntity<UnitDataResponse> {
        Audit.UnitView.log(targetId = unitId)
        val currentUserRoles = acl.getRolesForUnit(user, unitId)
        currentUserRoles.requireOneOfRoles(*basicDataRoles)

        val period = FiniteDateRange(from, to)
        val unitData = db.read {
            val groups = it.getDaycareGroups(unitId, from, to)
            val placements = it.getDetailedDaycarePlacements(unitId, null, from, to).toList()
            val backupCares = it.getBackupCaresForDaycare(unitId, period)
            val missingGroupPlacements = getMissingGroupPlacements(it, unitId)
            val caretakers = Caretakers(
                unitCaretakers = it.getUnitStats(unitId, from, to),
                groupCaretakers = it.getGroupStats(unitId, from, to)
            )

            val basicData = UnitDataResponse(
                groups = groups,
                placements = placements,
                backupCares = backupCares,
                missingGroupPlacements = missingGroupPlacements,
                caretakers = caretakers
            )

            if (currentUserRoles.hasOneOfRoles(*detailedDataRoles)) {
                val unitOccupancies = getUnitOccupancies(it, unitId, period)
                val groupOccupancies = getGroupOccupancies(it, unitId, period)
                val placementProposals = it.getPlacementPlans(unitId, null, null, listOf(ApplicationStatus.WAITING_UNIT_CONFIRMATION))
                val placementPlans = it.getPlacementPlans(unitId, null, null, listOf(ApplicationStatus.WAITING_CONFIRMATION, ApplicationStatus.WAITING_MAILING))
                val applications = it.getApplicationUnitSummaries(unitId)

                basicData.copy(
                    unitOccupancies = unitOccupancies,
                    groupOccupancies = groupOccupancies,
                    placementProposals = placementProposals,
                    placementPlans = placementPlans,
                    applications = applications
                )
            } else {
                basicData
            }
        }
        return ResponseEntity.ok(unitData)
    }
}

data class UnitDataResponse(
    val groups: List<DaycareGroup>,
    val placements: List<DaycarePlacementWithDetails>,
    val backupCares: List<UnitBackupCare>,
    val missingGroupPlacements: List<MissingGroupPlacement>,
    val caretakers: Caretakers,
    val unitOccupancies: UnitOccupancies? = null,
    val groupOccupancies: GroupOccupancies? = null,
    val placementProposals: List<PlacementPlanDetails>? = null,
    val placementPlans: List<PlacementPlanDetails>? = null,
    val applications: List<ApplicationUnitSummary>? = null
)

data class Caretakers(
    val unitCaretakers: Stats,
    val groupCaretakers: Map<GroupId, Stats>
)

data class UnitOccupancies(
    val planned: OccupancyResponse,
    val confirmed: OccupancyResponse,
    val realized: OccupancyResponse
)

private fun getUnitOccupancies(
    tx: Database.Read,
    unitId: DaycareId,
    period: FiniteDateRange
): UnitOccupancies {
    return UnitOccupancies(
        planned = getOccupancyResponse(tx.calculateOccupancyPeriods(unitId, period, OccupancyType.PLANNED)),
        confirmed = getOccupancyResponse(tx.calculateOccupancyPeriods(unitId, period, OccupancyType.CONFIRMED)),
        realized = getOccupancyResponse(tx.calculateOccupancyPeriods(unitId, period, OccupancyType.REALIZED))
    )
}

private fun getOccupancyResponse(occupancies: List<OccupancyPeriod>): OccupancyResponse {
    return OccupancyResponse(
        occupancies = occupancies,
        max = occupancies.filter { it.percentage != null }.maxByOrNull { it.percentage!! },
        min = occupancies.filter { it.percentage != null }.minByOrNull { it.percentage!! }
    )
}

data class GroupOccupancies(
    val confirmed: Map<GroupId, OccupancyResponse>,
    val realized: Map<GroupId, OccupancyResponse>
)

private fun getGroupOccupancies(
    tx: Database.Read,
    unitId: DaycareId,
    period: FiniteDateRange
): GroupOccupancies {
    return GroupOccupancies(
        confirmed = getGroupOccupancyResponses(
            tx.calculateOccupancyPeriodsGroupLevel(
                unitId,
                period,
                OccupancyType.CONFIRMED
            )
        ),
        realized = getGroupOccupancyResponses(
            tx.calculateOccupancyPeriodsGroupLevel(
                unitId,
                period,
                OccupancyType.REALIZED
            )
        )
    )
}

private fun getGroupOccupancyResponses(occupancies: List<OccupancyPeriodGroupLevel>): Map<GroupId, OccupancyResponse> {
    return occupancies
        .groupBy { it.groupId }
        .mapValues { (_, value) ->
            val occupancyPeriods = value.map {
                OccupancyPeriod(
                    period = it.period,
                    sum = it.sum,
                    headcount = it.headcount,
                    caretakers = it.caretakers,
                    percentage = it.percentage
                )
            }

            OccupancyResponse(
                occupancies = occupancyPeriods,
                max = occupancyPeriods.filter { it.percentage != null }.maxByOrNull { it.percentage!! },
                min = occupancyPeriods.filter { it.percentage != null }.minByOrNull { it.percentage!! }
            )
        }
}
