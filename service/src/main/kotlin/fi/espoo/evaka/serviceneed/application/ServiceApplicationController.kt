// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed.application

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.AuditId.Companion.invoke
import fi.espoo.evaka.placement.getPlacementsForChildDuring
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ServiceApplicationId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/service-applications")
class ServiceApplicationController(private val accessControl: AccessControl) {
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

    @PutMapping("/{id}/accept")
    fun acceptServiceApplication(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: ServiceApplicationId,
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
                    val application =
                        tx.getServiceApplication(id)?.also {
                            validateApplicationForAccepting(tx, it)
                        } ?: throw NotFound()
                    tx.setServiceApplicationAccepted(id, clock.now(), user)
                    application.childId
                }
            }
            .also { childId ->
                Audit.ChildServiceApplicationAccept.log(
                    targetId = AuditId(id),
                    objectId = AuditId(childId),
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

private fun validateApplicationForAccepting(tx: Database.Read, application: ServiceApplication) {
    if (application.decision != null) {
        throw Conflict("Application already decided")
    }

    if (!application.serviceNeedOption.validity.includes(application.startDate)) {
        throw BadRequest("Selected service need is not valid on requested start date")
    }

    val placement =
        tx.getPlacementsForChildDuring(
                application.childId,
                application.startDate,
                application.startDate,
            )
            .firstOrNull()
            ?: throw BadRequest("Child no longer has placement on requested start date")

    if (placement.type != application.serviceNeedOption.validPlacementType) {
        throw BadRequest("Selected service need is not valid with child's placement type")
    }
}
