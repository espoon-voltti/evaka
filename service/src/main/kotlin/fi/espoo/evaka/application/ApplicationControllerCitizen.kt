// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.Audit
import fi.espoo.evaka.decision.Decision
import fi.espoo.evaka.decision.DecisionService
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.decision.getDecisionsByApplication
import fi.espoo.evaka.decision.getDecisionsByGuardian
import fi.espoo.evaka.decision.getOwnDecisions
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.pis.service.getGuardianChildIds
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
import mu.KotlinLogging
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
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/citizen")
class ApplicationControllerCitizen(
    private val applicationStateService: ApplicationStateService,
    private val decisionService: DecisionService,
    private val personService: PersonService
) {

    @GetMapping("/applications/by-guardian")
    fun getGuardianApplications(
        db: Database,
        user: AuthenticatedUser
    ): List<ApplicationsOfChild> {
        Audit.ApplicationRead.log(targetId = user.id)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.END_USER)
        return db.connect { dbc ->
            dbc.read { tx ->
                val existingApplicationsByChild = tx.fetchApplicationSummariesForCitizen(PersonId(user.id))
                    .groupBy { it.childId }
                    .map { ApplicationsOfChild(it.key, it.value.first().childName ?: "", it.value) }

                // Some children might not have applications, so add 0 application children
                tx.getCitizenChildren(PersonId(user.id)).map { citizenChild ->
                    val childApplications =
                        existingApplicationsByChild.findLast { it.childId == citizenChild.childId }?.applicationSummaries
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
        user: AuthenticatedUser,
        @PathVariable applicationId: ApplicationId
    ): ApplicationDetails {
        Audit.ApplicationRead.log(targetId = user.id, objectId = applicationId)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.END_USER)

        val application = db.connect { dbc ->
            dbc.transaction { tx ->
                fetchApplicationDetailsWithCurrentOtherGuardianInfoAndFilteredAttachments(user, tx, personService, applicationId)
            }
        }

        return if (application?.guardianId?.raw == user.id && !application.hideFromGuardian)
            application
        else
            throw NotFound("Application not found")
    }

    @PostMapping("/applications")
    fun createApplication(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @RequestBody body: CreateApplicationBody
    ): ApplicationId {
        Audit.ApplicationCreate.log(targetId = user.id, objectId = body)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.END_USER)

        return db.connect { dbc ->
            dbc.transaction { tx ->
                if (
                    body.type != ApplicationType.CLUB &&
                    tx.duplicateApplicationExists(guardianId = PersonId(user.id), childId = body.childId, type = body.type)
                ) {
                    throw BadRequest("Duplicate application")
                }

                val guardian = tx.getPersonById(PersonId(user.id))
                    ?: throw IllegalStateException("Guardian not found")

                if (tx.getGuardianChildIds(PersonId(user.id)).none { it == body.childId }) {
                    throw IllegalStateException("User is not child's guardian")
                }

                val child = tx.getPersonById(body.childId)
                    ?: throw IllegalStateException("Child not found")

                tx.insertApplication(
                    guardianId = PersonId(user.id),
                    childId = body.childId,
                    origin = ApplicationOrigin.ELECTRONIC
                ).also {
                    applicationStateService.initializeApplicationForm(tx, user, evakaClock.today(), it, body.type, guardian, child)
                }
            }
        }
    }

    @GetMapping("/applications/duplicates/{childId}")
    fun getChildDuplicateApplications(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId
    ): Map<ApplicationType, Boolean> {
        Audit.ApplicationReadDuplicates.log(targetId = user.id, objectId = childId)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.END_USER)

        return db.connect { dbc ->
            dbc.read { tx ->
                ApplicationType.values()
                    .map { type ->
                        type to (
                            type != ApplicationType.CLUB &&
                                tx.duplicateApplicationExists(guardianId = PersonId(user.id), childId = childId, type = type)
                            )
                    }
                    .toMap()
            }
        }
    }

    @GetMapping("/applications/active-placements/{childId}")
    fun getChildPlacementStatusByApplicationType(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @PathVariable childId: ChildId
    ): Map<ApplicationType, Boolean> {
        Audit.ApplicationReadActivePlacementsByType.log(targetId = user.id, objectId = childId)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.END_USER)

        return db.connect { dbc ->
            dbc.read { tx ->
                ApplicationType.values()
                    .map { type ->
                        type to tx.activePlacementExists(childId = childId, type = type, today = evakaClock.today())
                    }
                    .toMap()
            }
        }
    }

    @PutMapping("/applications/{applicationId}")
    fun updateApplication(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @RequestBody applicationForm: ApplicationFormUpdate
    ) {
        Audit.ApplicationUpdate.log(targetId = applicationId)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.END_USER)

        db.connect { dbc ->
            dbc.transaction {
                applicationStateService.updateOwnApplicationContentsCitizen(
                    it,
                    user,
                    applicationId,
                    applicationForm,
                    evakaClock.today()
                )
            }
        }
    }

    @PutMapping("/applications/{applicationId}/draft")
    fun saveApplicationAsDraft(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @RequestBody applicationForm: ApplicationFormUpdate
    ) {
        Audit.ApplicationUpdate.log(targetId = applicationId)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.END_USER)

        db.connect { dbc ->
            dbc.transaction {
                applicationStateService.updateOwnApplicationContentsCitizen(
                    it,
                    user,
                    applicationId,
                    applicationForm,
                    evakaClock.today(),
                    asDraft = true
                )
            }
        }
    }

    @DeleteMapping("/applications/{applicationId}")
    fun deleteUnprocessedApplication(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: ApplicationId
    ) {
        Audit.ApplicationDelete.log(targetId = applicationId)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.END_USER)

        db.connect { dbc ->
            dbc.transaction { tx ->
                val application = tx.fetchApplicationDetails(applicationId)
                    ?.takeIf { it.guardianId.raw == user.id }
                    ?: throw NotFound("Application $applicationId of guardian ${user.id} not found")

                if (application.status != ApplicationStatus.CREATED && application.status != ApplicationStatus.SENT)
                    throw BadRequest("Only applications which are not yet being processed can be deleted")

                tx.deleteApplication(applicationId)
            }
        }
    }

    @PostMapping("/applications/{applicationId}/actions/send-application")
    fun sendApplication(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @PathVariable applicationId: ApplicationId
    ) {
        db.connect { dbc -> dbc.transaction { applicationStateService.sendApplication(it, user, applicationId, evakaClock.today(), isEnduser = true) } }
    }

    @GetMapping("/decisions")
    fun getDecisions(db: Database, user: AuthenticatedUser): List<ApplicationDecisions> {
        Audit.DecisionRead.log(targetId = user.id)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.END_USER)
        return db.connect { dbc -> dbc.read { it.getOwnDecisions(PersonId(user.id)) } }
    }

    @GetMapping("/applications/{applicationId}/decisions")
    fun getApplicationDecisions(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: ApplicationId
    ): List<Decision> {
        Audit.DecisionReadByApplication.log(targetId = applicationId)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.END_USER)

        return db.connect { dbc ->
            dbc.read { tx ->
                tx.fetchApplicationDetails(applicationId)
                    ?.let { if (it.guardianId.raw != user.id) throw Forbidden("Application not owned") }
                    ?: throw NotFound("Application not found")

                tx.getDecisionsByApplication(applicationId, AclAuthorization.All)
            }
        }
    }

    @PostMapping("/applications/{applicationId}/actions/accept-decision")
    fun acceptDecision(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: ApplicationId,
        @RequestBody body: AcceptDecisionRequest
    ) {
        // note: applicationStateService handles logging and authorization
        db.connect { dbc ->
            dbc.transaction {
                applicationStateService.acceptDecision(
                    it,
                    user,
                    applicationId,
                    body.decisionId,
                    body.requestedStartDate,
                    isEnduser = true
                )
            }
        }
    }

    @PostMapping("/applications/{applicationId}/actions/reject-decision")
    fun rejectDecision(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: ApplicationId,
        @RequestBody body: RejectDecisionRequest
    ) {
        // note: applicationStateService handles logging and authorization
        db.connect { dbc ->
            dbc.transaction {
                applicationStateService.rejectDecision(it, user, applicationId, body.decisionId, isEnduser = true)
            }
        }
    }

    @GetMapping("/decisions/{id}/download", produces = [MediaType.APPLICATION_PDF_VALUE])
    fun downloadDecisionPdf(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: DecisionId
    ): ResponseEntity<ByteArray> {
        Audit.DecisionDownloadPdf.log(targetId = id)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.END_USER)

        return db.connect { dbc ->
            dbc.transaction { tx ->
                if (!tx.getDecisionsByGuardian(PersonId(user.id), AclAuthorization.All).any { it.id == id }) {
                    logger.warn { "Citizen ${user.id} tried to download decision $id" }
                    throw NotFound("Decision not found")
                }

                decisionService.getDecisionPdf(tx, id)
                    .let { document ->
                        ResponseEntity.ok()
                            .header("Content-Disposition", "attachment;filename=${document.getName()}")
                            .body(document.getBytes())
                    }
            }
        }
    }
}

data class ApplicationsOfChild(
    val childId: ChildId,
    val childName: String,
    val applicationSummaries: List<CitizenApplicationSummary>
)

data class CreateApplicationBody(
    val childId: ChildId,
    val type: ApplicationType
)

data class ApplicationDecisions(
    val applicationId: ApplicationId,
    val childName: String,
    val decisions: List<DecisionSummary>
)

data class DecisionSummary(
    val decisionId: DecisionId,
    val type: DecisionType,
    val status: DecisionStatus,
    val sentDate: LocalDate,
    val resolved: LocalDate?
)
