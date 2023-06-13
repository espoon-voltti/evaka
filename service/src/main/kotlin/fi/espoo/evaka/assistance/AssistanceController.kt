// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistance

import fi.espoo.evaka.Audit
import fi.espoo.evaka.assistanceaction.AssistanceAction
import fi.espoo.evaka.assistanceaction.AssistanceActionOption
import fi.espoo.evaka.assistanceaction.AssistanceActionRequest
import fi.espoo.evaka.assistanceaction.AssistanceActionResponse
import fi.espoo.evaka.assistanceaction.AssistanceActionService
import fi.espoo.evaka.assistanceaction.getAssistanceActionsByChild
import fi.espoo.evaka.assistanceneed.AssistanceBasisOption
import fi.espoo.evaka.assistanceneed.AssistanceNeed
import fi.espoo.evaka.assistanceneed.AssistanceNeedRequest
import fi.espoo.evaka.assistanceneed.AssistanceNeedResponse
import fi.espoo.evaka.assistanceneed.AssistanceNeedService
import fi.espoo.evaka.assistanceneed.getAssistanceNeedsByChild
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.getPlacementsForChild
import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.AssistanceNeedId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AssistanceController(
    private val accessControl: AccessControl,
    private val assistanceNeedService: AssistanceNeedService,
    private val assistanceActionService: AssistanceActionService,
) {
    data class AssistanceResponse(
        val assistanceNeeds: List<AssistanceNeedResponse>,
        val assistanceActions: List<AssistanceActionResponse>,
    )

    @GetMapping("/children/{child}/assistance")
    fun getChildAssistance(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable child: ChildId
    ): AssistanceResponse =
        db.connect { dbc ->
            dbc.read { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Child.READ_ASSISTANCE,
                    child
                )
                val relevantPreschoolPlacements =
                    tx.getPlacementsForChild(child).filter {
                        (it.type == PlacementType.PRESCHOOL ||
                            it.type == PlacementType.PRESCHOOL_DAYCARE ||
                            it.type == PlacementType.PRESCHOOL_CLUB) &&
                            it.startDate <= clock.today()
                    }
                val isPrePreschoolDate = { date: LocalDate ->
                    relevantPreschoolPlacements.isEmpty() ||
                        relevantPreschoolPlacements.any { placement ->
                            placement.startDate.isBefore(date) || placement.startDate == date
                        }
                }
                val assistanceNeeds =
                    if (
                        accessControl.hasPermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Child.READ_ASSISTANCE_NEED,
                            child
                        )
                    ) {
                        tx.getAssistanceNeedsByChild(child)
                            .let { allAssistanceNeeds ->
                                val prePreschool =
                                    allAssistanceNeeds.filterNot {
                                        isPrePreschoolDate(it.startDate)
                                    }
                                val decisions =
                                    accessControl.checkPermissionFor(
                                        tx,
                                        user,
                                        clock,
                                        Action.AssistanceNeed.READ_PRE_PRESCHOOL_ASSISTANCE_NEED,
                                        prePreschool.map { it.id }
                                    )
                                allAssistanceNeeds.filter {
                                    decisions[it.id]?.isPermitted() ?: true
                                }
                            }
                            .let { rows ->
                                val actions: Map<AssistanceNeedId, Set<Action.AssistanceNeed>> =
                                    accessControl.getPermittedActions(
                                        tx,
                                        user,
                                        clock,
                                        rows.map { it.id }
                                    )
                                rows.map {
                                    AssistanceNeedResponse(it, actions[it.id] ?: emptySet())
                                }
                            }
                    } else emptyList()
                val assistanceActions =
                    if (
                        accessControl.hasPermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Child.READ_ASSISTANCE_ACTION,
                            child
                        )
                    ) {
                        tx.getAssistanceActionsByChild(child)
                            .let { allAssistanceActions ->
                                val prePreschool =
                                    allAssistanceActions.filterNot {
                                        isPrePreschoolDate(it.startDate)
                                    }
                                val decisions =
                                    accessControl.checkPermissionFor(
                                        tx,
                                        user,
                                        clock,
                                        Action.AssistanceAction
                                            .READ_PRE_PRESCHOOL_ASSISTANCE_ACTION,
                                        prePreschool.map { it.id }
                                    )
                                allAssistanceActions.filter {
                                    decisions[it.id]?.isPermitted() ?: true
                                }
                            }
                            .let { rows ->
                                val actions: Map<AssistanceActionId, Set<Action.AssistanceAction>> =
                                    accessControl.getPermittedActions(
                                        tx,
                                        user,
                                        clock,
                                        rows.map { it.id }
                                    )
                                rows.map {
                                    AssistanceActionResponse(it, actions[it.id] ?: emptySet())
                                }
                            }
                    } else emptyList()

                AssistanceResponse(
                    assistanceNeeds = assistanceNeeds,
                    assistanceActions = assistanceActions,
                )
            }
        }

    @PostMapping("/children/{childId}/assistance-needs")
    fun createAssistanceNeed(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: AssistanceNeedRequest
    ): AssistanceNeed {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Child.CREATE_ASSISTANCE_NEED,
                        childId
                    )
                }
                assistanceNeedService.createAssistanceNeed(
                    dbc,
                    user,
                    clock,
                    childId = childId,
                    data = body
                )
            }
            .also { assistanceNeed ->
                Audit.ChildAssistanceNeedCreate.log(
                    targetId = childId,
                    objectId = assistanceNeed.id
                )
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
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.AssistanceNeed.UPDATE,
                        assistanceNeedId
                    )
                }
                assistanceNeedService.updateAssistanceNeed(
                    dbc,
                    user,
                    clock,
                    id = assistanceNeedId,
                    data = body
                )
            }
            .also { Audit.ChildAssistanceNeedUpdate.log(targetId = assistanceNeedId) }
    }

    @DeleteMapping("/assistance-needs/{id}")
    fun deleteAssistanceNeed(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable("id") assistanceNeedId: AssistanceNeedId
    ) {
        db.connect { dbc ->
            dbc.read {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.AssistanceNeed.DELETE,
                    assistanceNeedId
                )
            }
            assistanceNeedService.deleteAssistanceNeed(dbc, clock, assistanceNeedId)
        }
        Audit.ChildAssistanceNeedDelete.log(targetId = assistanceNeedId)
    }

    @GetMapping("/assistance-basis-options")
    fun getAssistanceBasisOptions(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock
    ): List<AssistanceBasisOption> {
        return db.connect { dbc ->
            dbc.read {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Global.READ_ASSISTANCE_BASIS_OPTIONS
                )
            }
            assistanceNeedService.getAssistanceBasisOptions(dbc)
        }
    }

    @PostMapping("/children/{childId}/assistance-actions")
    fun createAssistanceAction(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: AssistanceActionRequest
    ): AssistanceAction {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Child.CREATE_ASSISTANCE_ACTION,
                        childId
                    )
                }
                assistanceActionService.createAssistanceAction(
                    dbc,
                    user = user,
                    childId = childId,
                    data = body
                )
            }
            .also { assistanceAction ->
                Audit.ChildAssistanceActionCreate.log(
                    targetId = childId,
                    objectId = assistanceAction.id
                )
            }
    }

    @PutMapping("/assistance-actions/{id}")
    fun updateAssistanceAction(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable("id") assistanceActionId: AssistanceActionId,
        @RequestBody body: AssistanceActionRequest
    ): AssistanceAction {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.AssistanceAction.UPDATE,
                        assistanceActionId
                    )
                }
                assistanceActionService.updateAssistanceAction(
                    dbc,
                    user = user,
                    id = assistanceActionId,
                    data = body
                )
            }
            .also { Audit.ChildAssistanceActionUpdate.log(targetId = assistanceActionId) }
    }

    @DeleteMapping("/assistance-actions/{id}")
    fun deleteAssistanceAction(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable("id") assistanceActionId: AssistanceActionId
    ) {
        db.connect { dbc ->
            dbc.read {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.AssistanceAction.DELETE,
                    assistanceActionId
                )
            }
            assistanceActionService.deleteAssistanceAction(dbc, assistanceActionId)
        }
        Audit.ChildAssistanceActionDelete.log(targetId = assistanceActionId)
    }

    @GetMapping("/assistance-action-options")
    fun getAssistanceActionOptions(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock
    ): List<AssistanceActionOption> {
        return db.connect { dbc ->
            dbc.read {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Global.READ_ASSISTANCE_ACTION_OPTIONS
                )
            }
            assistanceActionService.getAssistanceActionOptions(dbc)
        }
    }
}
