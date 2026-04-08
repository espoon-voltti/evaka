// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.serviceneed.application

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.absence.generateAbsencesFromIrregularDailyServiceTimes
import evaka.core.placement.PlacementSource
import evaka.core.placement.createPlacement
import evaka.core.placement.deleteFutureReservationsAndAbsencesOutsideValidPlacements
import evaka.core.serviceneed.ShiftCareType
import evaka.core.serviceneed.createServiceNeed
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.FeatureConfig
import evaka.core.shared.ServiceApplicationId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.NotFound
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/service-applications")
class ServiceApplicationController(
    private val accessControl: AccessControl,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val featureConfig: FeatureConfig,
) {
    data class EmployeeServiceApplication(
        val data: ServiceApplication,
        val permittedActions: Set<Action.ServiceApplication>,
    )

    @GetMapping
    fun getChildServiceApplications(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam childId: ChildId,
    ): List<EmployeeServiceApplication> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.READ_SERVICE_APPLICATIONS,
                        childId,
                    )
                    val applications = tx.getServiceApplicationsOfChild(childId)
                    val permittedActions =
                        accessControl.getPermittedActions<
                            ServiceApplicationId,
                            Action.ServiceApplication,
                        >(
                            tx,
                            user,
                            clock,
                            applications.map { it.id },
                        )
                    applications.map { application ->
                        EmployeeServiceApplication(
                            data = application,
                            permittedActions = permittedActions[application.id] ?: emptySet(),
                        )
                    }
                }
            }
            .also { Audit.ChildServiceApplicationsRead.log(targetId = AuditId(childId)) }
    }

    @GetMapping("/undecided")
    fun getUndecidedServiceApplications(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam unitId: DaycareId,
    ): List<UndecidedServiceApplicationSummary> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_SERVICE_APPLICATIONS,
                        unitId,
                    )
                    tx.getUndecidedServiceApplicationsByUnit(unitId)
                }
            }
            .also { Audit.UnitServiceApplicationsRead.log(targetId = AuditId(unitId)) }
    }

    data class AcceptServiceApplicationBody(val shiftCareType: ShiftCareType, val partWeek: Boolean)

    @PutMapping("/{id}/accept")
    fun acceptServiceApplication(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: ServiceApplicationId,
        @RequestBody body: AcceptServiceApplicationBody,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ServiceApplication.ACCEPT,
                        id,
                    )
                    val application = tx.getServiceApplication(id) ?: throw NotFound()

                    if (application.decision != null) {
                        throw Conflict("Application already decided")
                    }

                    if (!application.serviceNeedOption.validity.includes(application.startDate)) {
                        throw BadRequest(
                            "Selected service need is not valid on requested start date"
                        )
                    }

                    if (
                        application.serviceNeedOption.partWeek != null &&
                            application.serviceNeedOption.partWeek != body.partWeek
                    ) {
                        throw BadRequest("Conflicting part week value")
                    }

                    val placement =
                        application.currentPlacement
                            ?: throw BadRequest(
                                "Child no longer has placement on requested start date"
                            )

                    if (
                        !isPlacementTypeChangeAllowed(
                            placement.type,
                            application.serviceNeedOption.validPlacementType,
                        )
                    ) {
                        throw BadRequest(
                            "Selected service need is not valid with child's placement type"
                        )
                    }

                    val now = clock.now()
                    val optionValidityEnd = application.serviceNeedOption.validity.end
                    val endDate =
                        if (optionValidityEnd == null || optionValidityEnd >= placement.endDate) {
                            placement.endDate
                        } else {
                            optionValidityEnd
                        }
                    val range = FiniteDateRange(application.startDate, endDate)

                    val placementId =
                        if (placement.type != application.serviceNeedOption.validPlacementType) {
                            // needs a new placement
                            createPlacement(
                                    tx,
                                    childId = application.childId,
                                    unitId = placement.unitId,
                                    period = range,
                                    type = application.serviceNeedOption.validPlacementType,
                                    useFiveYearsOldDaycare =
                                        featureConfig.fiveYearsOldDaycareEnabled,
                                    placeGuarantee = false,
                                    now = clock.now(),
                                    userId = user.evakaUserId,
                                    source = PlacementSource.SERVICE_APPLICATION,
                                    sourceServiceApplicationId = id,
                                )
                                .also {
                                    tx.deleteFutureReservationsAndAbsencesOutsideValidPlacements(
                                        application.childId,
                                        now.toLocalDate(),
                                    )
                                    generateAbsencesFromIrregularDailyServiceTimes(
                                        tx,
                                        now,
                                        application.childId,
                                    )
                                }
                                .first {
                                    FiniteDateRange(it.startDate, it.endDate)
                                        .includes(application.startDate)
                                }
                                .id
                        } else placement.id

                    val serviceNeedId =
                        createServiceNeed(
                            tx = tx,
                            user = user,
                            placementId = placementId,
                            startDate = range.start,
                            endDate = range.end,
                            optionId = application.serviceNeedOption.id,
                            shiftCare = body.shiftCareType,
                            partWeek = body.partWeek,
                            confirmedAt = now,
                        )

                    tx.setServiceApplicationAccepted(id, now, user)

                    asyncJobRunner.plan(
                        tx,
                        listOf(
                            AsyncJob.GenerateFinanceDecisions.forChild(
                                application.childId,
                                range.asDateRange(),
                            ),
                            AsyncJob.SendServiceApplicationDecidedEmail(id),
                        ),
                        runAt = now,
                    )

                    Triple(application.childId, serviceNeedId, placementId)
                }
            }
            .also { (childId, serviceNeedId, placementId) ->
                Audit.ChildServiceApplicationAccept.log(
                    targetId = AuditId(id),
                    objectId = AuditId(serviceNeedId),
                    meta = mapOf("childId" to childId, "placementId" to placementId),
                )
            }
    }

    data class ServiceApplicationRejection(val reason: String)

    @PutMapping("/{id}/reject")
    fun rejectServiceApplication(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: ServiceApplicationId,
        @RequestBody body: ServiceApplicationRejection,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ServiceApplication.REJECT,
                        id,
                    )
                    val application = tx.getServiceApplication(id) ?: throw NotFound()
                    if (application.decision != null) {
                        throw Conflict("Application already decided")
                    }

                    tx.setServiceApplicationRejected(id, clock.now(), user, body.reason)

                    asyncJobRunner.plan(
                        tx,
                        listOf(AsyncJob.SendServiceApplicationDecidedEmail(id)),
                        runAt = clock.now(),
                    )

                    application.childId
                }
            }
            .also { childId ->
                Audit.ChildServiceApplicationReject.log(
                    targetId = AuditId(id),
                    objectId = AuditId(childId),
                )
            }
    }
}
