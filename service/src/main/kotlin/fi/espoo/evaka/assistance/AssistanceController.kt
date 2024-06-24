// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistance

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.assistanceaction.AssistanceAction
import fi.espoo.evaka.assistanceaction.AssistanceActionOption
import fi.espoo.evaka.assistanceaction.AssistanceActionRequest
import fi.espoo.evaka.assistanceaction.AssistanceActionResponse
import fi.espoo.evaka.assistanceaction.AssistanceActionService
import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.AssistanceFactorId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareAssistanceId
import fi.espoo.evaka.shared.OtherAssistanceMeasureId
import fi.espoo.evaka.shared.PreschoolAssistanceId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.data.DateSet
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
class AssistanceController(
    private val accessControl: AccessControl,
    private val assistanceActionService: AssistanceActionService,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>
) {
    data class AssistanceFactorResponse(
        val data: AssistanceFactor,
        val permittedActions: Set<Action.AssistanceFactor>
    )

    data class DaycareAssistanceResponse(
        val data: DaycareAssistance,
        val permittedActions: Set<Action.DaycareAssistance>
    )

    data class PreschoolAssistanceResponse(
        val data: PreschoolAssistance,
        val permittedActions: Set<Action.PreschoolAssistance>
    )

    data class OtherAssistanceMeasureResponse(
        val data: OtherAssistanceMeasure,
        val permittedActions: Set<Action.OtherAssistanceMeasure>
    )

    data class AssistanceResponse(
        val assistanceFactors: List<AssistanceFactorResponse>,
        val daycareAssistances: List<DaycareAssistanceResponse>,
        val preschoolAssistances: List<PreschoolAssistanceResponse>,
        val otherAssistanceMeasures: List<OtherAssistanceMeasureResponse>,
        val assistanceActions: List<AssistanceActionResponse>
    )

    @GetMapping(
        "/children/{child}/assistance", // deprecated
        "/employee/children/{child}/assistance"
    )
    fun getChildAssistance(
        db: Database,
        user: AuthenticatedUser.Employee,
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
                val assistanceActionFilter =
                    accessControl.requireAuthorizationFilter(
                        tx,
                        user,
                        clock,
                        Action.AssistanceAction.READ
                    )
                val assistanceActions =
                    tx.getAssistanceActionsByChildId(child, assistanceActionFilter).let { rows ->
                        val actions: Map<AssistanceActionId, Set<Action.AssistanceAction>> =
                            accessControl.getPermittedActions(tx, user, clock, rows.map { it.id })
                        rows.map { AssistanceActionResponse(it, actions[it.id] ?: emptySet()) }
                    }

                val assistanceFactorFilter =
                    accessControl.requireAuthorizationFilter(
                        tx,
                        user,
                        clock,
                        Action.AssistanceFactor.READ
                    )
                val assistanceFactors =
                    tx.getAssistanceFactorsByChildId(child, assistanceFactorFilter).let { rows ->
                        val actions: Map<AssistanceFactorId, Set<Action.AssistanceFactor>> =
                            accessControl.getPermittedActions(tx, user, clock, rows.map { it.id })
                        rows.map { AssistanceFactorResponse(it, actions[it.id] ?: emptySet()) }
                    }

                val daycareAssistanceFilter =
                    accessControl.requireAuthorizationFilter(
                        tx,
                        user,
                        clock,
                        Action.DaycareAssistance.READ
                    )

                val daycareAssistances =
                    tx.getDaycareAssistanceByChildId(child, daycareAssistanceFilter).let { rows ->
                        val actions: Map<DaycareAssistanceId, Set<Action.DaycareAssistance>> =
                            accessControl.getPermittedActions(tx, user, clock, rows.map { it.id })
                        rows.map { DaycareAssistanceResponse(it, actions[it.id] ?: emptySet()) }
                    }

                val preschoolAssistanceFilter =
                    accessControl.requireAuthorizationFilter(
                        tx,
                        user,
                        clock,
                        Action.PreschoolAssistance.READ
                    )

                val preschoolAssistances =
                    tx.getPreschoolAssistanceByChildId(child, preschoolAssistanceFilter).let { rows ->
                        val actions: Map<PreschoolAssistanceId, Set<Action.PreschoolAssistance>> =
                            accessControl.getPermittedActions(tx, user, clock, rows.map { it.id })
                        rows.map { PreschoolAssistanceResponse(it, actions[it.id] ?: emptySet()) }
                    }

                val otherAssistanceMeasureFilter =
                    accessControl.requireAuthorizationFilter(
                        tx,
                        user,
                        clock,
                        Action.OtherAssistanceMeasure.READ
                    )

                val otherAssistanceMeasures =
                    tx
                        .getOtherAssistanceMeasuresByChildId(child, otherAssistanceMeasureFilter)
                        .let { rows ->
                            val actions:
                                Map<OtherAssistanceMeasureId, Set<Action.OtherAssistanceMeasure>> =
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
                AssistanceResponse(
                    assistanceFactors = assistanceFactors,
                    daycareAssistances = daycareAssistances,
                    preschoolAssistances = preschoolAssistances,
                    assistanceActions = assistanceActions,
                    otherAssistanceMeasures = otherAssistanceMeasures
                )
            }
        }

    @PostMapping(
        "/children/{childId}/assistance-actions", // deprecated
        "/employee/children/{childId}/assistance-actions"
    )
    fun createAssistanceAction(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: AssistanceActionRequest
    ): AssistanceAction =
        db
            .connect { dbc ->
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
            }.also { assistanceAction ->
                Audit.ChildAssistanceActionCreate.log(
                    targetId = AuditId(childId),
                    objectId = AuditId(assistanceAction.id)
                )
            }

    @PutMapping(
        "/assistance-actions/{id}", // deprecated
        "/employee/assistance-actions/{id}"
    )
    fun updateAssistanceAction(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: AssistanceActionId,
        @RequestBody body: AssistanceActionRequest
    ): AssistanceAction =
        db
            .connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.AssistanceAction.UPDATE,
                        id
                    )
                }
                assistanceActionService.updateAssistanceAction(
                    dbc,
                    user = user,
                    id = id,
                    data = body
                )
            }.also { Audit.ChildAssistanceActionUpdate.log(targetId = AuditId(id)) }

    @DeleteMapping(
        "/assistance-actions/{id}", // deprecated
        "/employee/assistance-actions/{id}"
    )
    fun deleteAssistanceAction(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: AssistanceActionId
    ) {
        db.connect { dbc ->
            dbc.read {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.AssistanceAction.DELETE,
                    id
                )
            }
            assistanceActionService.deleteAssistanceAction(dbc, id)
        }
        Audit.ChildAssistanceActionDelete.log(targetId = AuditId(id))
    }

    @GetMapping(
        "/assistance-action-options", // deprecated
        "/employee/assistance-action-options"
    )
    fun getAssistanceActionOptions(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock
    ): List<AssistanceActionOption> =
        db
            .connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_ASSISTANCE_ACTION_OPTIONS
                    )
                }
                assistanceActionService.getAssistanceActionOptions(dbc)
            }.also { Audit.AssistanceActionOptionsRead.log() }

    @PostMapping(
        "/children/{child}/assistance-factors", // deprecated
        "/employee/children/{child}/assistance-factors"
    )
    fun createAssistanceFactor(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable child: ChildId,
        @RequestBody body: AssistanceFactorUpdate
    ): AssistanceFactorId =
        db
            .connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.CREATE_ASSISTANCE_FACTOR,
                        child
                    )
                    tx.insertAssistanceFactor(user, clock.now(), child, body).also {
                        asyncJobRunner.plan(
                            tx,
                            listOf(
                                AsyncJob.GenerateFinanceDecisions.forChild(
                                    child,
                                    body.validDuring.asDateRange()
                                )
                            ),
                            runAt = clock.now()
                        )
                    }
                }
            }.also { id ->
                Audit.AssistanceFactorCreate.log(targetId = AuditId(child), objectId = AuditId(id))
            }

    @PostMapping(
        "/assistance-factors/{id}", // deprecated
        "/employee/assistance-factors/{id}"
    )
    fun updateAssistanceFactor(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: AssistanceFactorId,
        @RequestBody body: AssistanceFactorUpdate
    ) = db
        .connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.AssistanceFactor.UPDATE,
                    id
                )
                val original = tx.getAssistanceFactor(id)
                tx.updateAssistanceFactor(user, clock.now(), id, body)
                if (original != null) {
                    val affectedRanges = DateSet.of(original.validDuring, body.validDuring)
                    affectedRanges.spanningRange()?.let {
                        asyncJobRunner.plan(
                            tx,
                            listOf(
                                AsyncJob.GenerateFinanceDecisions.forChild(
                                    original.childId,
                                    it.asDateRange()
                                )
                            ),
                            runAt = clock.now()
                        )
                    }
                }
            }
        }.also { Audit.AssistanceFactorUpdate.log(targetId = AuditId(id)) }

    @DeleteMapping(
        "/assistance-factors/{id}", // deprecated
        "/employee/assistance-factors/{id}"
    )
    fun deleteAssistanceFactor(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: AssistanceFactorId
    ) = db
        .connect { dbc ->
            dbc.transaction { tx ->
                accessControl
                    .checkPermissionFor(
                        tx,
                        user,
                        clock,
                        Action.AssistanceFactor.DELETE,
                        id
                    ).let {
                        if (it.isPermitted()) {
                            tx.deleteAssistanceFactor(id)?.also { deleted ->
                                asyncJobRunner.plan(
                                    tx,
                                    listOf(
                                        AsyncJob.GenerateFinanceDecisions.forChild(
                                            deleted.childId,
                                            deleted.validDuring.asDateRange()
                                        )
                                    ),
                                    runAt = clock.now()
                                )
                            }
                            id
                        } else {
                            null
                        }
                    }
            }
        }.also { deletedId ->
            deletedId?.let { Audit.AssistanceFactorDelete.log(targetId = AuditId(it)) }
        }

    @PostMapping(
        "/children/{child}/daycare-assistances", // deprecated
        "/employee/children/{child}/daycare-assistances"
    )
    fun createDaycareAssistance(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable child: ChildId,
        @RequestBody body: DaycareAssistanceUpdate
    ): DaycareAssistanceId =
        db
            .connect { dbc ->
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
            }.also { id ->
                Audit.DaycareAssistanceCreate.log(targetId = AuditId(child), objectId = AuditId(id))
            }

    @PostMapping(
        "/daycare-assistances/{id}", // deprecated
        "/employee/daycare-assistances/{id}"
    )
    fun updateDaycareAssistance(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: DaycareAssistanceId,
        @RequestBody body: DaycareAssistanceUpdate
    ) = db
        .connect { dbc ->
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
        }.also { Audit.DaycareAssistanceUpdate.log(targetId = AuditId(id)) }

    @DeleteMapping(
        "/daycare-assistances/{id}", // deprecated
        "/employee/daycare-assistances/{id}"
    )
    fun deleteDaycareAssistance(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: DaycareAssistanceId
    ) = db
        .connect { dbc ->
            dbc.transaction { tx ->
                accessControl
                    .checkPermissionFor(
                        tx,
                        user,
                        clock,
                        Action.DaycareAssistance.DELETE,
                        id
                    ).let {
                        if (it.isPermitted()) {
                            tx.deleteDaycareAssistance(id)
                            id
                        } else {
                            null
                        }
                    }
            }
        }.also { deletedId ->
            deletedId?.let { Audit.DaycareAssistanceDelete.log(targetId = AuditId(it)) }
        }

    @PostMapping(
        "/children/{child}/preschool-assistances", // deprecated
        "/employee/children/{child}/preschool-assistances"
    )
    fun createPreschoolAssistance(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable child: ChildId,
        @RequestBody body: PreschoolAssistanceUpdate
    ): PreschoolAssistanceId =
        db
            .connect { dbc ->
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
            }.also { id ->
                Audit.PreschoolAssistanceCreate.log(
                    targetId = AuditId(child),
                    objectId = AuditId(id)
                )
            }

    @PostMapping(
        "/preschool-assistances/{id}", // deprecated
        "/employee/preschool-assistances/{id}"
    )
    fun updatePreschoolAssistance(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: PreschoolAssistanceId,
        @RequestBody body: PreschoolAssistanceUpdate
    ) = db
        .connect { dbc ->
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
        }.also { Audit.PreschoolAssistanceUpdate.log(targetId = AuditId(id)) }

    @DeleteMapping(
        "/preschool-assistances/{id}", // deprecated
        "/employee/preschool-assistances/{id}"
    )
    fun deletePreschoolAssistance(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: PreschoolAssistanceId
    ) = db
        .connect { dbc ->
            dbc.transaction { tx ->
                accessControl
                    .checkPermissionFor(
                        tx,
                        user,
                        clock,
                        Action.PreschoolAssistance.DELETE,
                        id
                    ).let {
                        if (it.isPermitted()) {
                            tx.deletePreschoolAssistance(id)
                            id
                        } else {
                            null
                        }
                    }
            }
        }.also { deletedId ->
            deletedId?.let { Audit.PreschoolAssistanceDelete.log(targetId = AuditId(it)) }
        }

    @PostMapping(
        "/children/{child}/other-assistance-measures", // deprecated
        "/employee/children/{child}/other-assistance-measures"
    )
    fun createOtherAssistanceMeasure(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable child: ChildId,
        @RequestBody body: OtherAssistanceMeasureUpdate
    ): OtherAssistanceMeasureId =
        db
            .connect { dbc ->
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
            }.also { id ->
                Audit.OtherAssistanceMeasureCreate.log(
                    targetId = AuditId(child),
                    objectId = AuditId(id)
                )
            }

    @PostMapping(
        "/other-assistance-measures/{id}", // deprecated
        "/employee/other-assistance-measures/{id}"
    )
    fun updateOtherAssistanceMeasure(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: OtherAssistanceMeasureId,
        @RequestBody body: OtherAssistanceMeasureUpdate
    ) = db
        .connect { dbc ->
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
        }.also { Audit.OtherAssistanceMeasureUpdate.log(targetId = AuditId(id)) }

    @DeleteMapping(
        "/other-assistance-measures/{id}", // deprecated
        "/employee/other-assistance-measures/{id}"
    )
    fun deleteOtherAssistanceMeasure(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: OtherAssistanceMeasureId
    ) = db
        .connect { dbc ->
            dbc.transaction { tx ->
                accessControl
                    .checkPermissionFor(
                        tx,
                        user,
                        clock,
                        Action.OtherAssistanceMeasure.DELETE,
                        id
                    ).let {
                        if (it.isPermitted()) {
                            tx.deleteOtherAssistanceMeasure(id)
                            id
                        } else {
                            null
                        }
                    }
            }
        }.also { deletedId ->
            deletedId?.let { Audit.OtherAssistanceMeasureDelete.log(targetId = AuditId(it)) }
        }
}
