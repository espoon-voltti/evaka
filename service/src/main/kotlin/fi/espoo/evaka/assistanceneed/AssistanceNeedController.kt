// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed

import fi.espoo.evaka.Audit
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.getPlacementsForChild
import fi.espoo.evaka.shared.AssistanceNeedId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AssistanceNeedController(
    private val assistanceNeedService: AssistanceNeedService,
    private val accessControl: AccessControl
) {
    @PostMapping("/children/{childId}/assistance-needs")
    fun createAssistanceNeed(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: AssistanceNeedRequest
    ): AssistanceNeed {
        Audit.ChildAssistanceNeedCreate.log(targetId = childId)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Child.CREATE_ASSISTANCE_NEED,
            childId
        )
        return db.connect { dbc ->
            assistanceNeedService.createAssistanceNeed(
                dbc,
                user,
                clock,
                childId = childId,
                data = body
            )
        }
    }

    @GetMapping("/children/{childId}/assistance-needs")
    fun getAssistanceNeeds(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable childId: ChildId
    ): List<AssistanceNeedResponse> {
        Audit.ChildAssistanceNeedRead.log(targetId = childId)
        accessControl.requirePermissionFor(user, clock, Action.Child.READ_ASSISTANCE_NEED, childId)
        return db.connect { dbc ->
            val relevantPreschoolPlacements =
                dbc.read { tx ->
                    tx.getPlacementsForChild(childId).filter {
                        (it.type == PlacementType.PRESCHOOL ||
                            it.type == PlacementType.PRESCHOOL_DAYCARE) &&
                            it.startDate <= clock.today()
                    }
                }
            val assistanceNeeds =
                assistanceNeedService.getAssistanceNeedsByChildId(dbc, childId).let {
                    allAssistanceNeeds ->
                    val prePreschool =
                        allAssistanceNeeds.filterNot {
                            relevantPreschoolPlacements.isEmpty() ||
                                relevantPreschoolPlacements.any { placement ->
                                    placement.startDate.isBefore(it.startDate) ||
                                        placement.startDate == it.startDate
                                }
                        }
                    val decisions =
                        dbc.read { tx ->
                            accessControl.checkPermissionFor(
                                tx,
                                user,
                                clock,
                                Action.AssistanceNeed.READ_PRE_PRESCHOOL_ASSISTANCE_NEED,
                                prePreschool.map { it.id }
                            )
                        }
                    allAssistanceNeeds.filter { decisions[it.id]?.isPermitted() ?: true }
                }
            val assistanceNeedIds = assistanceNeeds.map { it.id }
            val permittedActions =
                dbc.read { tx ->
                    accessControl.getPermittedActions<AssistanceNeedId, Action.AssistanceNeed>(
                        tx,
                        user,
                        clock,
                        assistanceNeedIds
                    )
                }
            assistanceNeeds.map {
                AssistanceNeedResponse(it, permittedActions[it.id] ?: emptySet())
            }
        }
    }

    @PutMapping("/assistance-needs/{id}")
    fun updateAssistanceNeed(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable("id") assistanceNeedId: AssistanceNeedId,
        @RequestBody body: AssistanceNeedRequest
    ): AssistanceNeed {
        Audit.ChildAssistanceNeedUpdate.log(targetId = assistanceNeedId)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.AssistanceNeed.UPDATE,
            assistanceNeedId
        )
        return db.connect { dbc ->
            assistanceNeedService.updateAssistanceNeed(
                dbc,
                user,
                clock,
                id = assistanceNeedId,
                data = body
            )
        }
    }

    @DeleteMapping("/assistance-needs/{id}")
    fun deleteAssistanceNeed(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable("id") assistanceNeedId: AssistanceNeedId
    ) {
        Audit.ChildAssistanceNeedDelete.log(targetId = assistanceNeedId)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.AssistanceNeed.DELETE,
            assistanceNeedId
        )
        db.connect { dbc ->
            assistanceNeedService.deleteAssistanceNeed(dbc, clock, assistanceNeedId)
        }
    }

    @GetMapping("/assistance-basis-options")
    fun getAssistanceBasisOptions(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock
    ): List<AssistanceBasisOption> {
        accessControl.requirePermissionFor(user, clock, Action.Global.READ_ASSISTANCE_BASIS_OPTIONS)
        return db.connect { dbc -> assistanceNeedService.getAssistanceBasisOptions(dbc) }
    }
}
