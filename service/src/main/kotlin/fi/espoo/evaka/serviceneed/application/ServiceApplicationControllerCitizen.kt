// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed.application

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.AuditId.Companion.invoke
import fi.espoo.evaka.placement.getPlacementsForChildDuring
import fi.espoo.evaka.serviceneed.getServiceNeedOptions
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ServiceApplicationId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.utils.letIf
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
@RequestMapping("/citizen/service-applications")
class ServiceApplicationControllerCitizen(private val accessControl: AccessControl) {
    data class CitizenServiceApplication(
        val data: ServiceApplication,
        val permittedActions: Set<Action.Citizen.ServiceApplication>,
    )

    @GetMapping
    fun getChildServiceApplications(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestParam childId: ChildId,
    ): List<CitizenServiceApplication> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.READ_SERVICE_APPLICATIONS,
                        childId,
                    )
                    val applications = tx.getServiceApplicationsOfChild(childId)
                    val permittedActions =
                        accessControl.getPermittedActions<
                            ServiceApplicationId,
                            Action.Citizen.ServiceApplication,
                        >(
                            tx,
                            user,
                            clock,
                            applications.map { it.id },
                        )
                    applications.map { application ->
                        CitizenServiceApplication(
                            data = application,
                            permittedActions =
                                (permittedActions[application.id] ?: emptySet()).letIf(
                                    application.decision != null
                                ) {
                                    // already decided applications can not be deleted
                                    it - Action.Citizen.ServiceApplication.DELETE
                                },
                        )
                    }
                }
            }
            .also { Audit.CitizenChildServiceApplicationsRead.log(targetId = AuditId(childId)) }
    }

    @GetMapping("/options")
    fun getChildServiceNeedOptions(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestParam childId: ChildId,
        @RequestParam date: LocalDate,
    ): List<ServiceNeedOptionBasics> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.READ_PLACEMENT,
                        childId,
                    )
                    val placement =
                        tx.getPlacementsForChildDuring(childId, date, date).firstOrNull()
                            ?: return@read emptyList()

                    tx.getServiceNeedOptions()
                        .filter {
                            it.validPlacementType == placement.type &&
                                !it.defaultOption &&
                                DateRange(it.validFrom, it.validTo).includes(date)
                        }
                        .map {
                            ServiceNeedOptionBasics(
                                id = it.id,
                                nameFi = it.nameFi,
                                nameSv = it.nameSv,
                                nameEn = it.nameEn,
                                validPlacementType = it.validPlacementType,
                                partWeek = it.partWeek,
                            )
                        }
                }
            }
            .also { Audit.CitizenChildServiceNeedOptionsRead.log(targetId = AuditId(childId)) }
    }

    data class ServiceApplicationCreateRequest(
        val childId: ChildId,
        val startDate: LocalDate,
        val serviceNeedOptionId: ServiceNeedOptionId,
        val additionalInfo: String,
    )

    @PostMapping
    fun createServiceApplication(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: ServiceApplicationCreateRequest,
    ) {
        if (body.startDate.isBefore(clock.today()))
            throw BadRequest("Start date can not be in past")

        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.CREATE_SERVICE_APPLICATION,
                        body.childId,
                    )

                    val hasOpenApplication =
                        tx.getServiceApplicationsOfChild(body.childId).any { it.decision == null }
                    if (hasOpenApplication)
                        throw Conflict("Child already has an open service application")

                    val placementType =
                        tx.getPlacementsForChildDuring(
                                childId = body.childId,
                                start = body.startDate,
                                end = body.startDate,
                            )
                            .firstOrNull()
                            ?.type
                            ?: throw BadRequest("Child does not have placement on start date")

                    if (
                        tx.getServiceNeedOptions().none {
                            it.id == body.serviceNeedOptionId &&
                                !it.defaultOption &&
                                it.validPlacementType == placementType &&
                                DateRange(it.validFrom, it.validTo).includes(body.startDate)
                        }
                    )
                        throw BadRequest("Invalid service need option")

                    tx.insertServiceApplication(
                        sentAt = clock.now(),
                        personId = user.id,
                        childId = body.childId,
                        startDate = body.startDate,
                        serviceNeedOptionId = body.serviceNeedOptionId,
                        additionalInfo = body.additionalInfo,
                    )
                }
            }
            .also { serviceApplicationId ->
                Audit.CitizenChildServiceApplicationsCreate.log(
                    targetId = AuditId(body.childId),
                    objectId = AuditId(serviceApplicationId),
                )
            }
    }

    @DeleteMapping("/{id}")
    fun deleteServiceApplication(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: ServiceApplicationId,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.ServiceApplication.DELETE,
                        id,
                    )

                    val application =
                        tx.getServiceApplication(id)
                            ?: throw NotFound("Service application with id $id not found")
                    if (application.decision != null)
                        throw Conflict("Cannot delete already decided service application")

                    tx.deleteUndecidedServiceApplication(id)

                    application
                }
            }
            .also { application ->
                Audit.CitizenChildServiceApplicationsDelete.log(
                    targetId = AuditId(application.id),
                    objectId = AuditId(application.childId),
                )
            }
    }
}
