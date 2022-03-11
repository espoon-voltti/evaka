// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.cancelAllActiveTransferApplicationsAfterDate
import fi.espoo.evaka.daycare.getUnitFeatures
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.PilotFeature
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class PlacementControllerCitizen(
    private val accessControl: AccessControl,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {

    data class ChildPlacementResponse(
        val placements: List<TerminatablePlacementGroup>,
    )

    @GetMapping("/citizen/children/{childId}/placements")
    fun getPlacements(
        db: Database,
        user: AuthenticatedUser.Citizen,
        evakaClock: EvakaClock,
        @PathVariable childId: ChildId,
    ): ChildPlacementResponse {
        Audit.PlacementSearch.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Citizen.Child.READ_PLACEMENT, childId)

        return db.connect { dbc ->
            ChildPlacementResponse(
                placements = mapToTerminatablePlacements(
                    dbc.read {
                        it.getCitizenChildPlacements(
                            evakaClock.today(),
                            childId
                        )
                    }
                )
            )
        }
    }

    data class PlacementTerminationRequestBody(
        val type: TerminatablePlacementType,
        val unitId: DaycareId,
        val terminationDate: LocalDate,
        val terminateDaycareOnly: Boolean?,
    )

    @PostMapping("/citizen/children/{childId}/placements/terminate")
    fun postPlacementTermination(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: PlacementTerminationRequestBody
    ) {
        Audit.PlacementTerminate.log(body.unitId, body.type)

        val terminationDate = body.terminationDate.also { if (it.isBefore(clock.today())) throw BadRequest("Invalid terminationDate") }
        db.connect { dbc ->
            if (dbc.read { it.getUnitFeatures(body.unitId) }?.features?.contains(PilotFeature.PLACEMENT_TERMINATION) != true) {
                throw Forbidden("Placement termination not enabled for unit", "PLACEMENT_TERMINATION_DISABLED")
            }
            val terminatablePlacementGroup = dbc.read { it.getCitizenChildPlacements(clock.today(), childId) }
                .also { placements ->
                    accessControl.requirePermissionFor(user, Action.Citizen.Placement.TERMINATE, placements.map { it.id })
                }
                .let { mapToTerminatablePlacements(it) }
                .find { it.unitId == body.unitId && it.type == body.type }
                ?: throw NotFound("Matching placement type not found")

            dbc.transaction { tx ->
                if (body.terminateDaycareOnly == true) {
                    terminateBilledDaycare(
                        tx,
                        user,
                        clock.today(),
                        terminatablePlacementGroup,
                        terminationDate,
                        childId,
                        body.unitId
                    )
                } else {
                    // normal termination simply cancels or terminates the placements
                    terminatablePlacementGroup
                        .let { it.placements + it.additionalPlacements }
                        .filter { it.endDate.isAfter(terminationDate) }
                        .forEach { cancelOrTerminatePlacement(tx, clock.today(), it, terminationDate, user) }
                }

                tx.cancelAllActiveTransferApplicationsAfterDate(childId, terminationDate)
                asyncJobRunner.plan(tx, listOf(AsyncJob.GenerateFinanceDecisions.forChild(childId, DateRange(terminationDate, null))))
            }
        }
    }
}
