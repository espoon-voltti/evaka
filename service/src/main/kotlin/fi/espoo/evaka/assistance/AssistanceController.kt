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
import fi.espoo.evaka.shared.AssistanceFactorId
import fi.espoo.evaka.shared.AssistanceNeedId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareAssistanceId
import fi.espoo.evaka.shared.OtherAssistanceMeasureId
import fi.espoo.evaka.shared.PreschoolAssistanceId
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
    data class AssistanceFactorResponse(
        val data: AssistanceFactor,
        val permittedActions: Set<Action.AssistanceFactor>,
    )

    data class DaycareAssistanceResponse(
        val data: DaycareAssistance,
        val permittedActions: Set<Action.DaycareAssistance>,
    )

    data class PreschoolAssistanceResponse(
        val data: PreschoolAssistance,
        val permittedActions: Set<Action.PreschoolAssistance>,
    )

    data class OtherAssistanceMeasureResponse(
        val data: OtherAssistanceMeasure,
        val permittedActions: Set<Action.OtherAssistanceMeasure>,
    )

    data class AssistanceResponse(
        val assistanceFactors: List<AssistanceFactorResponse>,
        val daycareAssistances: List<DaycareAssistanceResponse>,
        val preschoolAssistances: List<PreschoolAssistanceResponse>,
        val otherAssistanceMeasures: List<OtherAssistanceMeasureResponse>,
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

                val assistanceFactors =
                    if (
                        accessControl.hasPermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Child.READ_ASSISTANCE_FACTORS,
                            child
                        )
                    )
                        tx.getAssistanceFactors(child)
                            .let { rows ->
                                val prePreschool =
                                    rows.filterNot { isPrePreschoolDate(it.validDuring.start) }
                                val decisions =
                                    accessControl.checkPermissionFor(
                                        tx,
                                        user,
                                        clock,
                                        Action.AssistanceFactor.READ_PRE_PRESCHOOL,
                                        prePreschool.map { it.id }
                                    )
                                rows.filter { decisions[it.id]?.isPermitted() ?: true }
                            }
                            .let { rows ->
                                val actions: Map<AssistanceFactorId, Set<Action.AssistanceFactor>> =
                                    accessControl.getPermittedActions(
                                        tx,
                                        user,
                                        clock,
                                        rows.map { it.id }
                                    )
                                rows.map {
                                    AssistanceFactorResponse(it, actions[it.id] ?: emptySet())
                                }
                            }
                    else emptyList()

                val daycareAssistances =
                    if (
                        accessControl.hasPermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Child.READ_DAYCARE_ASSISTANCES,
                            child
                        )
                    )
                        tx.getDaycareAssistances(child)
                            .let { rows ->
                                val prePreschool =
                                    rows.filterNot { isPrePreschoolDate(it.validDuring.start) }
                                val decisions =
                                    accessControl.checkPermissionFor(
                                        tx,
                                        user,
                                        clock,
                                        Action.DaycareAssistance.READ_PRE_PRESCHOOL,
                                        prePreschool.map { it.id }
                                    )
                                rows.filter { decisions[it.id]?.isPermitted() ?: true }
                            }
                            .let { rows ->
                                val actions:
                                    Map<DaycareAssistanceId, Set<Action.DaycareAssistance>> =
                                    accessControl.getPermittedActions(
                                        tx,
                                        user,
                                        clock,
                                        rows.map { it.id }
                                    )
                                rows.map {
                                    DaycareAssistanceResponse(it, actions[it.id] ?: emptySet())
                                }
                            }
                    else emptyList()

                val preschoolAssistances =
                    if (
                        accessControl.hasPermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Child.READ_PRESCHOOL_ASSISTANCES,
                            child
                        )
                    )
                        tx.getPreschoolAssistances(child).let { rows ->
                            val actions:
                                Map<PreschoolAssistanceId, Set<Action.PreschoolAssistance>> =
                                accessControl.getPermittedActions(
                                    tx,
                                    user,
                                    clock,
                                    rows.map { it.id }
                                )
                            rows.map {
                                PreschoolAssistanceResponse(it, actions[it.id] ?: emptySet())
                            }
                        }
                    else emptyList()

                val otherAssistanceMeasures =
                    if (
                        accessControl.hasPermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Child.READ_OTHER_ASSISTANCE_MEASURES,
                            child
                        )
                    )
                        tx.getOtherAssistanceMeasures(child)
                            .let { rows ->
                                val prePreschool =
                                    rows.filterNot { isPrePreschoolDate(it.validDuring.start) }
                                val decisions =
                                    accessControl.checkPermissionFor(
                                        tx,
                                        user,
                                        clock,
                                        Action.OtherAssistanceMeasure.READ_PRE_PRESCHOOL,
                                        prePreschool.map { it.id }
                                    )
                                rows.filter { decisions[it.id]?.isPermitted() ?: true }
                            }
                            .let { rows ->
                                val actions:
                                    Map<
                                        OtherAssistanceMeasureId, Set<Action.OtherAssistanceMeasure>
                                    > =
                                    accessControl.getPermittedActions(
                                        tx,
                                        user,
                                        clock,
                                        rows.map { it.id }
                                    )
                                rows.map {
                                    OtherAssistanceMeasureResponse(it, actions[it.id] ?: emptySet())
                                }
                            }
                    else emptyList()

                AssistanceResponse(
                    assistanceFactors = assistanceFactors,
                    daycareAssistances = daycareAssistances,
                    preschoolAssistances = preschoolAssistances,
                    assistanceActions = assistanceActions,
                    otherAssistanceMeasures = otherAssistanceMeasures,
                    assistanceNeeds = assistanceNeeds,
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
            .also { Audit.AssistanceBasisOptionsRead.log() }
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
            .also { Audit.AssistanceActionOptionsRead.log() }
    }

    @PostMapping("/children/{child}/assistance-factors")
    fun createAssistanceFactor(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable child: ChildId,
        @RequestBody body: AssistanceFactorUpdate
    ): AssistanceFactorId =
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.CREATE_ASSISTANCE_FACTOR,
                        child
                    )
                    tx.insertAssistanceFactor(user, clock.now(), child, body)
                }
            }
            .also { id -> Audit.AssistanceFactorCreate.log(targetId = child, objectId = id) }

    @PostMapping("/assistance-factors/{id}")
    fun updateAssistanceFactor(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable id: AssistanceFactorId,
        @RequestBody body: AssistanceFactorUpdate
    ) =
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.AssistanceFactor.UPDATE,
                        id
                    )
                    tx.updateAssistanceFactor(user, clock.now(), id, body)
                }
            }
            .also { Audit.AssistanceFactorUpdate.log(targetId = id) }

    @DeleteMapping("/assistance-factors/{id}")
    fun deleteAssistanceFactor(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable id: AssistanceFactorId,
    ) =
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl
                        .checkPermissionFor(
                            tx,
                            user,
                            clock,
                            Action.AssistanceFactor.DELETE,
                            id,
                        )
                        .let {
                            if (it.isPermitted()) {
                                tx.deleteAssistanceFactor(id)
                                id
                            } else {
                                null
                            }
                        }
                }
            }
            .also { deletedId ->
                deletedId?.let { Audit.AssistanceFactorDelete.log(targetId = it) }
            }

    @PostMapping("/children/{child}/daycare-assistances")
    fun createDaycareAssistance(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable child: ChildId,
        @RequestBody body: DaycareAssistanceUpdate
    ): DaycareAssistanceId =
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.CREATE_DAYCARE_ASSISTANCE,
                        child
                    )
                    tx.insertDaycareAssistance(user, clock.now(), child, body)
                }
            }
            .also { id -> Audit.DaycareAssistanceCreate.log(targetId = child, objectId = id) }

    @PostMapping("/daycare-assistances/{id}")
    fun updateDaycareAssistance(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable id: DaycareAssistanceId,
        @RequestBody body: DaycareAssistanceUpdate
    ) =
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.DaycareAssistance.UPDATE,
                        id
                    )
                    tx.updateDaycareAssistance(user, clock.now(), id, body)
                }
            }
            .also { Audit.DaycareAssistanceUpdate.log(targetId = id) }

    @DeleteMapping("/daycare-assistances/{id}")
    fun deleteDaycareAssistance(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable id: DaycareAssistanceId,
    ) =
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl
                        .checkPermissionFor(
                            tx,
                            user,
                            clock,
                            Action.DaycareAssistance.DELETE,
                            id,
                        )
                        .let {
                            if (it.isPermitted()) {
                                tx.deleteDaycareAssistance(id)
                                id
                            } else {
                                null
                            }
                        }
                }
            }
            .also { deletedId ->
                deletedId?.let { Audit.DaycareAssistanceDelete.log(targetId = it) }
            }

    @PostMapping("/children/{child}/preschool-assistances")
    fun createPreschoolAssistance(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable child: ChildId,
        @RequestBody body: PreschoolAssistanceUpdate
    ): PreschoolAssistanceId =
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.CREATE_PRESCHOOL_ASSISTANCE,
                        child
                    )
                    tx.insertPreschoolAssistance(user, clock.now(), child, body)
                }
            }
            .also { id -> Audit.PreschoolAssistanceCreate.log(targetId = child, objectId = id) }

    @PostMapping("/preschool-assistances/{id}")
    fun updatePreschoolAssistance(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable id: PreschoolAssistanceId,
        @RequestBody body: PreschoolAssistanceUpdate
    ) =
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.PreschoolAssistance.UPDATE,
                        id
                    )
                    tx.updatePreschoolAssistance(user, clock.now(), id, body)
                }
            }
            .also { Audit.PreschoolAssistanceUpdate.log(targetId = id) }

    @DeleteMapping("/preschool-assistances/{id}")
    fun deletePreschoolAssistance(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable id: PreschoolAssistanceId,
    ) =
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl
                        .checkPermissionFor(
                            tx,
                            user,
                            clock,
                            Action.PreschoolAssistance.DELETE,
                            id,
                        )
                        .let {
                            if (it.isPermitted()) {
                                tx.deletePreschoolAssistance(id)
                                id
                            } else {
                                null
                            }
                        }
                }
            }
            .also { deletedId ->
                deletedId?.let { Audit.PreschoolAssistanceDelete.log(targetId = it) }
            }

    @PostMapping("/children/{child}/other-assistance-measures")
    fun createOtherAssistanceMeasure(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable child: ChildId,
        @RequestBody body: OtherAssistanceMeasureUpdate
    ): OtherAssistanceMeasureId =
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.CREATE_OTHER_ASSISTANCE_MEASURE,
                        child
                    )
                    tx.insertOtherAssistanceMeasure(user, clock.now(), child, body)
                }
            }
            .also { id -> Audit.OtherAssistanceMeasureCreate.log(targetId = child, objectId = id) }

    @PostMapping("/other-assistance-measures/{id}")
    fun updateOtherAssistanceMeasure(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable id: OtherAssistanceMeasureId,
        @RequestBody body: OtherAssistanceMeasureUpdate
    ) =
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.OtherAssistanceMeasure.UPDATE,
                        id
                    )
                    tx.updateOtherAssistanceMeasure(user, clock.now(), id, body)
                }
            }
            .also { Audit.OtherAssistanceMeasureUpdate.log(targetId = id) }

    @DeleteMapping("/other-assistance-measures/{id}")
    fun deleteOtherAssistanceMeasure(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable id: OtherAssistanceMeasureId,
    ) =
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl
                        .checkPermissionFor(
                            tx,
                            user,
                            clock,
                            Action.OtherAssistanceMeasure.DELETE,
                            id,
                        )
                        .let {
                            if (it.isPermitted()) {
                                tx.deleteOtherAssistanceMeasure(id)
                                id
                            } else {
                                null
                            }
                        }
                }
            }
            .also { deletedId ->
                deletedId?.let { Audit.OtherAssistanceMeasureDelete.log(targetId = it) }
            }
}
