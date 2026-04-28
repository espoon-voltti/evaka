// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.placement

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.application.cancelAllActiveTransferApplications
import evaka.core.daycare.getUnitFeatures
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.NotFound
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import evaka.core.shared.security.PilotFeature
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class PlacementControllerCitizen(
    private val accessControl: AccessControl,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {

    data class ChildPlacementResponse(val placements: List<TerminatablePlacementGroup>)

    @GetMapping("/citizen/children/{childId}/placements")
    fun getPlacements(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
    ): ChildPlacementResponse {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Citizen.Child.READ_PLACEMENT,
                        childId,
                    )
                    ChildPlacementResponse(
                        placements =
                            mapToTerminatablePlacements(
                                it.getCitizenChildPlacements(clock.today(), childId),
                                clock.today(),
                            )
                    )
                }
            }
            .also {
                Audit.PlacementSearch.log(
                    targetId = AuditId(childId),
                    meta = mapOf("count" to it.placements.size),
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
        @RequestBody body: PlacementTerminationRequestBody,
    ) {
        val terminationDate =
            body.terminationDate.also {
                if (it.isBefore(clock.today())) throw BadRequest("Invalid terminationDate")
            }

        db.connect { dbc ->
                val terminatablePlacementGroup = dbc.read { tx ->
                    if (
                        tx.getUnitFeatures(body.unitId)
                            ?.features
                            ?.contains(PilotFeature.PLACEMENT_TERMINATION) != true
                    ) {
                        throw Forbidden(
                            "Placement termination not enabled for unit",
                            "PLACEMENT_TERMINATION_DISABLED",
                        )
                    }
                    tx.getCitizenChildPlacements(clock.today(), childId)
                        .also { placements ->
                            accessControl.requirePermissionFor(
                                tx,
                                user,
                                clock,
                                Action.Citizen.Placement.TERMINATE,
                                placements.map { it.id },
                            )
                        }
                        .let { mapToTerminatablePlacements(it, clock.today()) }
                        .find { it.unitId == body.unitId && it.type == body.type }
                        ?: throw NotFound("Matching placement type not found")
                }

                val cancelableTransferApplicationIds = dbc.transaction { tx ->
                    if (body.terminateDaycareOnly == true) {
                        terminateBilledDaycare(
                            tx,
                            user,
                            terminatablePlacementGroup,
                            terminationDate,
                            childId,
                            body.unitId,
                            clock.now(),
                        )
                    } else {
                        // normal termination simply cancels or terminates the placements
                        terminatablePlacementGroup
                            .let { it.placements + it.additionalPlacements }
                            .filter { it.endDate.isAfter(terminationDate) }
                            .forEach {
                                cancelOrTerminatePlacement(
                                    tx,
                                    it,
                                    terminationDate,
                                    user,
                                    clock.now(),
                                )
                            }
                    }
                    // Make sure termination bookkeeping happens when no placement is terminated
                    // from the middle i.e. some placements have been completely deleted and the
                    // termination date is the end date of some placement.
                    val allPlacements =
                        terminatablePlacementGroup.placements +
                            terminatablePlacementGroup.additionalPlacements
                    if (allPlacements.any { it.startDate > terminationDate }) {
                        val endingPlacement = allPlacements.find { it.endDate == terminationDate }
                        if (endingPlacement != null) {
                            tx.updatePlacementTermination(
                                endingPlacement.id,
                                clock.today(),
                                user.evakaUserId,
                            )
                        }
                    }

                    val cancelableTransferApplicationIds =
                        tx.cancelAllActiveTransferApplications(childId, clock, user.evakaUserId)

                    tx.deleteFutureReservationsAndAbsencesOutsideValidPlacements(
                        childId,
                        clock.today(),
                    )
                    asyncJobRunner.plan(
                        tx,
                        listOf(
                            AsyncJob.GenerateFinanceDecisions.forChild(
                                childId,
                                DateRange(terminationDate, null),
                            )
                        ),
                        runAt = clock.now(),
                    )

                    cancelableTransferApplicationIds
                }
                Pair(terminatablePlacementGroup, cancelableTransferApplicationIds)
            }
            .also { (terminatablePlacementGroup, cancelableTransferApplicationIds) ->
                val placements =
                    terminatablePlacementGroup.placements +
                        terminatablePlacementGroup.additionalPlacements
                Audit.PlacementTerminate.log(
                    targetId = AuditId(listOf(body.unitId, childId)),
                    objectId = AuditId(placements.map { it.id } + cancelableTransferApplicationIds),
                    meta =
                        mapOf(
                            "type" to body.type,
                            "placementIds" to placements.map { it.id },
                            "transferApplicationIds" to cancelableTransferApplicationIds,
                        ),
                )
            }
    }
}
