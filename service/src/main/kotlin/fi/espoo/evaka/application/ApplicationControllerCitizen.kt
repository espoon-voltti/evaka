// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.Audit
import fi.espoo.evaka.decision.Decision
import fi.espoo.evaka.decision.DecisionService
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.decision.getDecision
import fi.espoo.evaka.decision.getDecisionsByApplication
import fi.espoo.evaka.decision.getOwnDecisions
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen")
class ApplicationControllerCitizen(
    private val accessControl: AccessControl,
    private val featureConfig: FeatureConfig,
    private val applicationStateService: ApplicationStateService,
    private val decisionService: DecisionService,
    private val personService: PersonService
) {

    @GetMapping("/applications/by-guardian")
    fun getGuardianApplications(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock
    ): List<ApplicationsOfChild> {
        Audit.ApplicationRead.log(targetId = user.id)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Citizen.Person.READ_APPLICATIONS,
            user.id
        )
        return db.connect { dbc ->
            dbc.read { tx ->
                val existingApplicationsByChild =
                    tx.fetchApplicationSummariesForCitizen(user.id)
                        .groupBy { it.childId }
                        .map {
                            ApplicationsOfChild(it.key, it.value.first().childName ?: "", it.value)
                        }

                // Some children might not have applications, so add 0 application children
                tx.getCitizenChildren(user.id).map { citizenChild ->
                    val childApplications =
                        existingApplicationsByChild
                            .findLast { it.childId == citizenChild.childId }
                            ?.applicationSummaries
                            ?: emptyList()
                    ApplicationsOfChild(
                        childId = citizenChild.childId,
                        childName = citizenChild.childName,
                        applicationSummaries = childApplications
                    )
                }
            }
        }
    }

    @GetMapping("/applications/{applicationId}")
    fun getApplication(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId
    ): ApplicationDetails {
        Audit.ApplicationRead.log(targetId = user.id, objectId = applicationId)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Citizen.Application.READ,
            applicationId
        )

        val application =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    fetchApplicationDetailsWithCurrentOtherGuardianInfoAndFilteredAttachments(
                        user,
                        tx,
                        personService,
                        applicationId
                    )
                }
            }

        return if (application?.guardianId == user.id && !application.hideFromGuardian) application
        else throw NotFound("Application not found")
    }

    @PostMapping("/applications")
    fun createApplication(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: CreateApplicationBody
    ): ApplicationId {
        Audit.ApplicationCreate.log(targetId = user.id, objectId = body)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Citizen.Child.CREATE_APPLICATION,
            body.childId
        )

        return db.connect { dbc ->
            dbc.transaction { tx ->
                if (
                    body.type != ApplicationType.CLUB &&
                        tx.duplicateApplicationExists(
                            guardianId = user.id,
                            childId = body.childId,
                            type = body.type
                        )
                ) {
                    throw BadRequest("Duplicate application")
                }

                val guardian =
                    tx.getPersonById(user.id) ?: throw IllegalStateException("Guardian not found")

                val child =
                    tx.getPersonById(body.childId) ?: throw IllegalStateException("Child not found")

                tx.insertApplication(
                        type = body.type,
                        guardianId = user.id,
                        childId = body.childId,
                        origin = ApplicationOrigin.ELECTRONIC
                    )
                    .also {
                        applicationStateService.initializeApplicationForm(
                            tx,
                            user,
                            clock.today(),
                            it,
                            body.type,
                            guardian,
                            child
                        )
                    }
            }
        }
    }

    @GetMapping("/applications/duplicates/{childId}")
    fun getChildDuplicateApplications(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId
    ): Map<ApplicationType, Boolean> {
        Audit.ApplicationReadDuplicates.log(targetId = user.id, objectId = childId)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Citizen.Child.READ_DUPLICATE_APPLICATIONS,
            childId
        )

        return db.connect { dbc ->
            dbc.read { tx ->
                ApplicationType.values()
                    .map { type ->
                        type to
                            (type != ApplicationType.CLUB &&
                                tx.duplicateApplicationExists(
                                    guardianId = user.id,
                                    childId = childId,
                                    type = type
                                ))
                    }
                    .toMap()
            }
        }
    }

    @GetMapping("/applications/active-placements/{childId}")
    fun getChildPlacementStatusByApplicationType(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId
    ): Map<ApplicationType, Boolean> {
        Audit.ApplicationReadActivePlacementsByType.log(targetId = user.id, objectId = childId)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Citizen.Child.READ_PLACEMENT_STATUS_BY_APPLICATION_TYPE,
            childId
        )

        return db.connect { dbc ->
            dbc.read { tx ->
                ApplicationType.values()
                    .map { type ->
                        type to
                            tx.activePlacementExists(
                                childId = childId,
                                type = type,
                                today = clock.today()
                            )
                    }
                    .toMap()
            }
        }
    }

    @PutMapping("/applications/{applicationId}")
    fun updateApplication(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @RequestBody applicationForm: ApplicationFormUpdate
    ) {
        Audit.ApplicationUpdate.log(targetId = applicationId)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Citizen.Application.UPDATE,
            applicationId
        )

        db.connect { dbc ->
            dbc.transaction {
                applicationStateService.updateOwnApplicationContentsCitizen(
                    it,
                    user,
                    applicationId,
                    applicationForm,
                    clock.today()
                )
            }
        }
    }

    @PutMapping("/applications/{applicationId}/draft")
    fun saveApplicationAsDraft(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @RequestBody applicationForm: ApplicationFormUpdate
    ) {
        Audit.ApplicationUpdate.log(targetId = applicationId)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Citizen.Application.UPDATE,
            applicationId
        )

        db.connect { dbc ->
            dbc.transaction {
                applicationStateService.updateOwnApplicationContentsCitizen(
                    it,
                    user,
                    applicationId,
                    applicationForm,
                    clock.today(),
                    asDraft = true
                )
            }
        }
    }

    @DeleteMapping("/applications/{applicationId}")
    fun deleteUnprocessedApplication(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId
    ) {
        Audit.ApplicationDelete.log(targetId = applicationId)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Citizen.Application.DELETE,
            applicationId
        )

        db.connect { dbc ->
            dbc.transaction { tx ->
                val application =
                    tx.fetchApplicationDetails(applicationId)
                        ?: throw NotFound(
                            "Application $applicationId of guardian ${user.id} not found"
                        )

                if (
                    application.status != ApplicationStatus.CREATED &&
                        application.status != ApplicationStatus.SENT
                )
                    throw BadRequest(
                        "Only applications which are not yet being processed can be deleted"
                    )

                tx.deleteApplication(applicationId)
            }
        }
    }

    @PostMapping("/applications/{applicationId}/actions/send-application")
    fun sendApplication(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId
    ) {
        db.connect { dbc ->
            dbc.transaction {
                applicationStateService.sendApplication(it, user, clock, applicationId)
            }
        }
    }

    @GetMapping("/decisions")
    fun getDecisions(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock
    ): List<ApplicationDecisions> {
        Audit.DecisionRead.log(targetId = user.id)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Citizen.Person.READ_DECISIONS,
            user.id
        )
        return db.connect { dbc -> dbc.read { it.getOwnDecisions(user.id) } }
    }

    data class DecisionWithValidStartDatePeriod(
        val decision: Decision,
        val validRequestedStartDatePeriod: FiniteDateRange
    )

    @GetMapping("/applications/{applicationId}/decisions")
    fun getApplicationDecisions(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId
    ): List<DecisionWithValidStartDatePeriod> {
        Audit.DecisionReadByApplication.log(targetId = applicationId)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Citizen.Application.READ_DECISIONS,
            applicationId
        )

        return db.connect { dbc ->
            dbc.read { tx ->
                tx.fetchApplicationDetails(applicationId) ?: throw NotFound("Application not found")
                tx.getDecisionsByApplication(applicationId, AclAuthorization.All).map {
                    DecisionWithValidStartDatePeriod(
                        it,
                        it.validRequestedStartDatePeriod(featureConfig)
                    )
                }
            }
        }
    }

    @PostMapping("/applications/{applicationId}/actions/accept-decision")
    fun acceptDecision(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @RequestBody body: AcceptDecisionRequest
    ) {
        // note: applicationStateService handles logging and authorization
        db.connect { dbc ->
            dbc.transaction {
                applicationStateService.acceptDecision(
                    it,
                    user,
                    clock,
                    applicationId,
                    body.decisionId,
                    body.requestedStartDate,
                )
            }
        }
    }

    @PostMapping("/applications/{applicationId}/actions/reject-decision")
    fun rejectDecision(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @RequestBody body: RejectDecisionRequest
    ) {
        // note: applicationStateService handles logging and authorization
        db.connect { dbc ->
            dbc.transaction {
                applicationStateService.rejectDecision(
                    it,
                    user,
                    clock,
                    applicationId,
                    body.decisionId
                )
            }
        }
    }

    @GetMapping("/decisions/{id}/download", produces = [MediaType.APPLICATION_PDF_VALUE])
    fun downloadDecisionPdf(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: DecisionId
    ): ResponseEntity<Any> {
        Audit.DecisionDownloadPdf.log(targetId = id)
        accessControl.requirePermissionFor(user, clock, Action.Citizen.Decision.DOWNLOAD_PDF, id)

        return db.connect { dbc ->
            val decision =
                dbc.transaction { tx -> tx.getDecision(id) }
                    ?: throw NotFound("Decision $id does not exist")
            decisionService.getDecisionPdf(dbc, decision)
        }
    }

    @GetMapping("/applications/by-guardian/notifications")
    fun getGuardianApplicationNotifications(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock
    ): Int {
        Audit.ApplicationReadNotifications.log(targetId = user.id)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Citizen.Person.READ_APPLICATION_NOTIFICATIONS,
            user.id
        )
        return db.connect { dbc ->
            dbc.read { tx -> tx.fetchApplicationNotificationCountForCitizen(user.id) }
        }
    }
}

data class ApplicationsOfChild(
    val childId: ChildId,
    val childName: String,
    val applicationSummaries: List<CitizenApplicationSummary>
)

data class CreateApplicationBody(val childId: ChildId, val type: ApplicationType)

data class ApplicationDecisions(
    val applicationId: ApplicationId,
    val childId: ChildId,
    val childName: String,
    val decisions: List<DecisionSummary>
)

data class DecisionSummary(
    val id: DecisionId,
    val type: DecisionType,
    val status: DecisionStatus,
    val sentDate: LocalDate,
    val resolved: LocalDate?
)
