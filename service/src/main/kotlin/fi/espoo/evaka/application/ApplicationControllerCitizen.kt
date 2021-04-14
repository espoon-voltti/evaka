// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.getNextPreschoolTerm
import fi.espoo.evaka.decision.Decision
import fi.espoo.evaka.decision.DecisionService
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.decision.getDecisionsByApplication
import fi.espoo.evaka.decision.getDecisionsByGuardian
import fi.espoo.evaka.decision.getOwnDecisions
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.pis.service.getGuardianChildIds
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
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
import java.util.UUID

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
        db: Database.Connection,
        user: AuthenticatedUser
    ): ResponseEntity<List<ApplicationsOfChild>> {
        Audit.ApplicationRead.log(targetId = user.id)
        user.requireOneOfRoles(UserRole.END_USER)
        return ResponseEntity.ok(
            db.read { tx ->
                val existingApplicationsByChild = fetchApplicationSummariesForCitizen(tx.handle, user.id)
                    .groupBy { it.childId }
                    .map { ApplicationsOfChild(it.key, it.value.first().childName ?: "", it.value) }

                // Some children might not have applications, so add 0 application children
                getCitizenChildren(tx.handle, user.id).map { citizenChild ->
                    val childApplications = existingApplicationsByChild.findLast { it.childId == citizenChild.childId }?.applicationSummaries
                        ?: emptyList()
                    ApplicationsOfChild(
                        childId = citizenChild.childId,
                        childName = citizenChild.childName,
                        applicationSummaries = childApplications
                    )
                }
            }
        )
    }

    @GetMapping("/applications/{applicationId}")
    fun getApplication(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID
    ): ResponseEntity<ApplicationDetails> {
        Audit.ApplicationRead.log(targetId = user.id, objectId = applicationId)
        user.requireOneOfRoles(UserRole.END_USER)

        val application = db.transaction { tx ->
            fetchApplicationDetailsWithCurrentOtherGuardianInfoAndFilteredAttachments(user, tx, personService, applicationId)
        }

        return if (application?.guardianId == user.id && !application.hideFromGuardian)
            ResponseEntity.ok(application)
        else
            throw NotFound("Application not found")
    }

    @PostMapping("/applications")
    fun createApplication(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: CreateApplicationBody
    ): ResponseEntity<UUID> {
        Audit.ApplicationCreate.log(targetId = user.id, objectId = body)
        user.requireOneOfRoles(UserRole.END_USER)

        return db.transaction { tx ->
            if (
                body.type != ApplicationType.CLUB &&
                duplicateApplicationExists(tx.handle, guardianId = user.id, childId = body.childId, type = body.type)
            ) {
                throw BadRequest("Duplicate application")
            }

            val guardian = personService.getUpToDatePerson(tx, user, user.id)
                ?: throw IllegalStateException("Guardian not found")

            if (tx.getGuardianChildIds(user.id).none { it == body.childId }) {
                throw IllegalStateException("User is not child's guardian")
            }

            val child = personService.getUpToDatePerson(tx, user, body.childId)
                ?: throw IllegalStateException("Child not found")

            val applicationId = insertApplication(
                h = tx.handle,
                guardianId = user.id,
                childId = body.childId,
                origin = ApplicationOrigin.ELECTRONIC
            )
            val form = ApplicationForm.initForm(
                type = body.type,
                guardian = guardian,
                child = child
            ).let {
                if (body.type == ApplicationType.PRESCHOOL) {
                    it.copy(
                        preferences = it.preferences.copy(
                            preferredStartDate = tx.getNextPreschoolTerm()?.finnishPreschool?.start
                        )
                    )
                } else it
            }
            updateForm(
                tx.handle,
                applicationId,
                form,
                body.type,
                child.restrictedDetailsEnabled,
                guardian.restrictedDetailsEnabled
            )

            ResponseEntity.ok(applicationId)
        }
    }

    @GetMapping("/applications/duplicates/{childId}")
    fun getChildDuplicateApplications(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ): ResponseEntity<Map<ApplicationType, Boolean>> {
        Audit.ApplicationReadDuplicates.log(targetId = user.id, objectId = childId)
        user.requireOneOfRoles(UserRole.END_USER)

        return db.read { tx ->
            ApplicationType.values()
                .map { type ->
                    type to (
                        type != ApplicationType.CLUB &&
                            duplicateApplicationExists(tx.handle, guardianId = user.id, childId = childId, type = type)
                        )
                }
                .toMap()
                .let { ResponseEntity.ok(it) }
        }
    }

    @GetMapping("/applications/active-placements/{childId}")
    fun getChildPlacementStatusByApplicationType(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ): ResponseEntity<Map<ApplicationType, Boolean>> {
        Audit.ApplicationReadActivePlacementsByType.log(targetId = user.id, objectId = childId)
        user.requireOneOfRoles(UserRole.END_USER)

        return db.read { tx ->
            ApplicationType.values()
                .map { type ->
                    type to activePlacementExists(tx, childId = childId, type = type)
                }
                .toMap()
                .let { ResponseEntity.ok(it) }
        }
    }

    @PutMapping("/applications/{applicationId}")
    fun updateApplication(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID,
        @RequestBody applicationForm: ApplicationFormUpdate
    ): ResponseEntity<Unit> {
        Audit.ApplicationUpdate.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.END_USER)

        db.transaction { applicationStateService.updateOwnApplicationContentsCitizen(it, user, applicationId, applicationForm) }
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/applications/{applicationId}/draft")
    fun saveApplicationAsDraft(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID,
        @RequestBody applicationForm: ApplicationFormUpdate
    ): ResponseEntity<Unit> {
        Audit.ApplicationUpdate.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.END_USER)

        db.transaction { applicationStateService.updateOwnApplicationContentsCitizen(it, user, applicationId, applicationForm, asDraft = true) }
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/applications/{applicationId}")
    fun deleteUnprocessedApplication(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID
    ): ResponseEntity<Unit> {
        Audit.ApplicationDelete.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.END_USER)

        db.transaction { tx ->
            val application = fetchApplicationDetails(tx.handle, applicationId)
                ?.takeIf { it.guardianId == user.id }
                ?: throw NotFound("Application $applicationId of guardian ${user.id} not found")

            if (application.status != ApplicationStatus.CREATED && application.status != ApplicationStatus.SENT)
                throw BadRequest("Only applications which are not yet being processed can be deleted")

            deleteApplication(tx.handle, applicationId)
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/applications/{applicationId}/actions/send-application")
    fun sendApplication(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID
    ): ResponseEntity<Unit> {
        db.transaction { applicationStateService.sendApplication(it, user, applicationId, isEnduser = true) }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/decisions")
    fun getDecisions(db: Database.Connection, user: AuthenticatedUser): ResponseEntity<List<ApplicationDecisions>> {
        Audit.DecisionRead.log(targetId = user.id)
        user.requireOneOfRoles(UserRole.END_USER)
        return ResponseEntity.ok(db.read { getOwnDecisions(it, user.id) })
    }

    @GetMapping("/applications/{applicationId}/decisions")
    fun getApplicationDecisions(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID
    ): ResponseEntity<List<Decision>> {
        Audit.DecisionReadByApplication.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.END_USER)

        return db.read { tx ->
            fetchApplicationDetails(tx.handle, applicationId)
                ?.let { if (it.guardianId != user.id) throw Forbidden("Application not owned") }
                ?: throw NotFound("Application not found")

            getDecisionsByApplication(tx.handle, applicationId, AclAuthorization.All)
        }.let { ResponseEntity.ok(it) }
    }

    @PostMapping("/applications/{applicationId}/actions/accept-decision")
    fun acceptDecision(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID,
        @RequestBody body: AcceptDecisionRequest
    ): ResponseEntity<Unit> {
        // note: applicationStateService handles logging and authorization
        db.transaction {
            applicationStateService.acceptDecision(
                it,
                user,
                applicationId,
                body.decisionId,
                body.requestedStartDate,
                isEnduser = true
            )
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/applications/{applicationId}/actions/reject-decision")
    fun rejectDecision(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID,
        @RequestBody body: RejectDecisionRequest
    ): ResponseEntity<Unit> {
        // note: applicationStateService handles logging and authorization
        db.transaction {
            applicationStateService.rejectDecision(it, user, applicationId, body.decisionId, isEnduser = true)
        }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/decisions/{id}/download", produces = [MediaType.APPLICATION_PDF_VALUE])
    fun downloadDecisionPdf(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID
    ): ResponseEntity<ByteArray> {
        Audit.DecisionDownloadPdf.log(targetId = id)
        user.requireOneOfRoles(UserRole.END_USER)

        return db.transaction { tx ->
            if (!getDecisionsByGuardian(tx.handle, user.id, AclAuthorization.All).any { it.id == id }) {
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

data class ApplicationsOfChild(
    val childId: UUID,
    val childName: String,
    val applicationSummaries: List<CitizenApplicationSummary>
)

data class CreateApplicationBody(
    val childId: UUID,
    val type: ApplicationType
)

data class ApplicationDecisions(
    val applicationId: UUID,
    val childName: String,
    val decisions: List<DecisionSummary>
)

data class DecisionSummary(
    val decisionId: UUID,
    val type: DecisionType,
    val status: DecisionStatus,
    val sentDate: LocalDate,
    val resolved: LocalDate?
)
