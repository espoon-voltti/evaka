// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.cancelAllActiveTransferApplications
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
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class PlacementControllerCitizen(
    private val accessControl: AccessControl,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>
) {

    data class ChildPlacementResponse(val placements: List<TerminatablePlacementGroup>)

    @GetMapping("/citizen/children/{childId}/placements")
    fun getPlacements(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId
    ): ChildPlacementResponse {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Citizen.Child.READ_PLACEMENT,
                        childId
                    )
                    ChildPlacementResponse(
                        placements =
                            mapToTerminatablePlacements(
                                it.getCitizenChildPlacements(clock.today(), childId),
                                clock.today()
                            )
                    )
                }
            }
            .also {
                Audit.PlacementSearch.log(
                    targetId = childId,
                    meta = mapOf("count" to it.placements.size)
                )
            }
    }

    data class PlacementTerminationRequestBody(
        val type: TerminatablePlacementType,
        val unitId: DaycareId,
        val terminationDate: LocalDate,
        val terminateDaycareOnly: Boolean?
    )

    @PostMapping("/citizen/children/{childId}/placements/terminate")
    fun postPlacementTermination(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: PlacementTerminationRequestBody
    ) {
        val terminationDate =
            body.terminationDate.also {
                if (it.isBefore(clock.today())) throw BadRequest("Invalid terminationDate")
            }

        db.connect { dbc ->
                val terminatablePlacementGroup =
                    dbc.read { tx ->
                        if (
                            tx.getUnitFeatures(body.unitId)
                                ?.features
                                ?.contains(PilotFeature.PLACEMENT_TERMINATION) != true
                        ) {
                            throw Forbidden(
                                "Placement termination not enabled for unit",
                                "PLACEMENT_TERMINATION_DISABLED"
                            )
                        }
                        tx.getCitizenChildPlacements(clock.today(), childId)
                            .also { placements ->
                                accessControl.requirePermissionFor(
                                    tx,
                                    user,
                                    clock,
                                    Action.Citizen.Placement.TERMINATE,
                                    placements.map { it.id }
                                )
                            }
                            .let { mapToTerminatablePlacements(it, clock.today()) }
                            .find { it.unitId == body.unitId && it.type == body.type }
                            ?: throw NotFound("Matching placement type not found")
                    }

                val cancelableTransferApplicationIds =
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
                                .forEach {
                                    cancelOrTerminatePlacement(
                                        tx,
                                        clock.today(),
                                        it,
                                        terminationDate,
                                        user
                                    )
                                }
                        }

                        val cancelableTransferApplicationIds =
                            tx.cancelAllActiveTransferApplications(childId)

                        asyncJobRunner.plan(
                            tx,
                            listOf(
                                AsyncJob.GenerateFinanceDecisions.forChild(
                                    childId,
                                    DateRange(terminationDate, null)
                                )
                            ),
                            runAt = clock.now()
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
                    targetId = listOf(body.unitId, body.type, childId),
                    objectId = placements.map { it.id } + cancelableTransferApplicationIds,
                    meta =
                        mapOf(
                            "placementIds" to placements.map { it.id },
                            "transferApplicationIds" to cancelableTransferApplicationIds
                        )
                )
            }
    }
}
