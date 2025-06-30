// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.absence.application

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.absence.AbsenceService
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.daycare.PreschoolTerm
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.reservations.AbsenceRequest
import fi.espoo.evaka.shared.AbsenceApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.getPreschoolOperationalDatesForChild
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.utils.mapOfNotNullValues
import java.time.LocalDate
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/absence-application")
class AbsenceApplicationControllerEmployee(
    private val accessControl: AccessControl,
    private val absenceService: AbsenceService,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
    @GetMapping
    fun getAbsenceApplications(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam unitId: DaycareId?,
        @RequestParam childId: ChildId?,
        @RequestParam status: AbsenceApplicationStatus?,
    ): List<AbsenceApplicationSummaryEmployee> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    val permissions =
                        listOfNotNull(
                            unitId?.let {
                                accessControl.hasPermissionFor(
                                    tx,
                                    user,
                                    clock,
                                    Action.Unit.READ_ABSENCE_APPLICATIONS,
                                    it,
                                )
                            },
                            childId?.let {
                                accessControl.hasPermissionFor(
                                    tx,
                                    user,
                                    clock,
                                    Action.Child.READ_ABSENCE_APPLICATIONS,
                                    it,
                                )
                            },
                        )
                    if (permissions.isEmpty() || permissions.contains(false)) throw Forbidden()
                    val applications =
                        tx.selectAbsenceApplications(
                            unitId = unitId,
                            childId = childId,
                            status = status,
                        )
                    val actions =
                        accessControl.getPermittedActions<
                            AbsenceApplicationId,
                            Action.AbsenceApplication,
                        >(
                            tx,
                            user,
                            clock,
                            applications.map { it.id },
                        )
                    val terms = tx.getPreschoolTerms()
                    applications.mapNotNull { application ->
                        actions[application.id]
                            ?.takeIf { it.contains(Action.AbsenceApplication.READ) }
                            ?.let {
                                AbsenceApplicationSummaryEmployee(
                                    application,
                                    it.filter { action ->
                                            action != Action.AbsenceApplication.DECIDE_MAX_WEEK ||
                                                isMaxWeek(tx, application, terms)
                                        }
                                        .toSet(),
                                )
                            }
                    }
                }
            }
            .also {
                Audit.AbsenceApplicationRead.log(
                    meta =
                        mapOfNotNullValues(
                            "unitId" to unitId?.let { AuditId(it) },
                            "childId" to childId?.let { AuditId(it) },
                        )
                )
            }
    }

    @PostMapping("/{id}/accept")
    fun acceptAbsenceApplication(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: AbsenceApplicationId,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    val application = getApplicationForDecision(tx, user, clock, id)
                    tx.decideAbsenceApplication(
                        application.id,
                        AbsenceApplicationStatus.ACCEPTED,
                        clock.now(),
                        user.evakaUserId,
                        rejectedReason = null,
                    )
                    absenceService.createAbsences(
                        tx,
                        user,
                        clock,
                        AbsenceRequest(
                            setOf(application.childId),
                            FiniteDateRange(application.startDate, application.endDate),
                            AbsenceType.OTHER_ABSENCE,
                        ),
                    )
                    asyncJobRunner.plan(
                        tx,
                        listOf(AsyncJob.SendAbsenceApplicationDecidedEmail(application.id)),
                        runAt = clock.now(),
                    )
                    application
                }
            }
            .also {
                Audit.AbsenceApplicationAccept.log(
                    targetId = AuditId(it.id),
                    objectId = AuditId(it.childId),
                )
            }
    }

    @PostMapping("/{id}/reject")
    fun rejectAbsenceApplication(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: AbsenceApplicationId,
        @RequestBody body: AbsenceApplicationRejectRequest,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    val application = getApplicationForDecision(tx, user, clock, id)
                    tx.decideAbsenceApplication(
                        application.id,
                        AbsenceApplicationStatus.REJECTED,
                        clock.now(),
                        user.evakaUserId,
                        body.reason,
                    )
                    asyncJobRunner.plan(
                        tx,
                        listOf(AsyncJob.SendAbsenceApplicationDecidedEmail(application.id)),
                        runAt = clock.now(),
                    )
                    application
                }
            }
            .also {
                Audit.AbsenceApplicationReject.log(
                    targetId = AuditId(it.id),
                    objectId = AuditId(it.childId),
                )
            }
    }

    private fun getApplicationForDecision(
        tx: Database.Read,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        id: AbsenceApplicationId,
    ): AbsenceApplication {
        val application = tx.selectAbsenceApplication(id, forUpdate = true) ?: throw NotFound()
        accessControl.requirePermissionFor(
            tx,
            user,
            clock,
            if (isMaxWeek(tx, application)) Action.AbsenceApplication.DECIDE_MAX_WEEK
            else Action.AbsenceApplication.DECIDE,
            application.id,
        )
        if (application.status != AbsenceApplicationStatus.WAITING_DECISION)
            throw BadRequest("Absence application ${application.id} is not waiting decision")
        return application
    }
}

@RestController
@RequestMapping("/citizen/absence-application")
class AbsenceApplicationControllerCitizen(private val accessControl: AccessControl) {
    @PostMapping
    fun postAbsenceApplication(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: AbsenceApplicationCreateRequest,
    ): AbsenceApplicationId {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.CREATE_ABSENCE_APPLICATION,
                        body.childId,
                    )
                    if (body.startDate.isBefore(clock.today()))
                        throw BadRequest("Start date cannot be before today")
                    if (
                        !tx.getChildrenWithAbsenceApplicationPossibleOnSomeDate(
                                setOf(body.childId),
                                clock.today(),
                            )
                            .contains(body.childId)
                    )
                        throw BadRequest("Child does not have preschool placement")
                    tx.insertAbsenceApplication(body, clock.now(), user.evakaUserId)
                }
            }
            .also {
                Audit.AbsenceApplicationCreate.log(
                    targetId = AuditId(it.id),
                    objectId = AuditId(it.childId),
                )
            }
            .id
    }

    @GetMapping
    fun getAbsenceApplications(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestParam childId: ChildId,
    ): List<AbsenceApplicationSummaryCitizen> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.READ_ABSENCE_APPLICATIONS,
                        childId,
                    )
                    val applications = tx.selectAbsenceApplications(childId = childId)
                    val actions =
                        accessControl.getPermittedActions<
                            AbsenceApplicationId,
                            Action.Citizen.AbsenceApplication,
                        >(
                            tx,
                            user,
                            clock,
                            applications.map { it.id },
                        )
                    applications.map {
                        AbsenceApplicationSummaryCitizen(it, actions[it.id] ?: emptySet())
                    }
                }
            }
            .also { Audit.AbsenceApplicationRead.log(meta = mapOf("childId" to AuditId(childId))) }
    }

    @DeleteMapping("/{id}")
    fun deleteAbsenceApplication(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: AbsenceApplicationId,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.AbsenceApplication.DELETE,
                        id,
                    )
                    val application =
                        tx.selectAbsenceApplication(id, forUpdate = true) ?: throw NotFound()
                    if (application.status != AbsenceApplicationStatus.WAITING_DECISION) {
                        throw BadRequest(
                            "Absence application ${application.id} is not waiting decision"
                        )
                    }
                    tx.deleteAbsenceApplication(application.id)
                }
            }
            .also {
                Audit.AbsenceApplicationDelete.log(
                    targetId = AuditId(it.id),
                    objectId = AuditId(it.childId),
                )
            }
    }

    @GetMapping("/application-possible")
    fun getAbsenceApplicationPossibleDateRanges(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestParam childId: ChildId,
    ): DateSet {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.READ_PLACEMENT,
                        childId,
                    )
                    tx.getAbsenceApplicationDateRanges(childId, clock.today())
                }
            }
            .also {
                Audit.AbsenceApplicationPossibleRead.log(
                    meta = mapOf("childId" to AuditId(childId))
                )
            }
    }
}

private fun isMaxWeek(
    tx: Database.Read,
    application: AbsenceApplicationSummary,
    terms: List<PreschoolTerm>,
) = isMaxWeek(tx, application.child.id, application.startDate, application.endDate, terms)

private fun isMaxWeek(tx: Database.Read, application: AbsenceApplication) =
    isMaxWeek(
        tx,
        application.childId,
        application.startDate,
        application.endDate,
        tx.getPreschoolTerms(),
    )

private fun isMaxWeek(
    tx: Database.Read,
    childId: ChildId,
    startDate: LocalDate,
    endDate: LocalDate,
    terms: List<PreschoolTerm>,
) =
    tx.getPreschoolOperationalDatesForChild(FiniteDateRange(startDate, endDate), childId, terms)
        .size <= 5
