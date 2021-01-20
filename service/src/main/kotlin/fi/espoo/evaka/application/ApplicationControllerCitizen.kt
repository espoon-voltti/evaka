package fi.espoo.evaka.application

import fi.espoo.evaka.Audit
import fi.espoo.evaka.decision.Decision
import fi.espoo.evaka.decision.DecisionService
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.decision.getDecisionsByApplication
import fi.espoo.evaka.decision.getDecisionsByGuardian
import fi.espoo.evaka.decision.getOwnDecisions
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
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
    private val decisionService: DecisionService
) {

    @GetMapping("/applications/by-guardian")
    fun getGuardianApplications(
        db: Database.Connection,
        user: AuthenticatedUser
    ): ResponseEntity<List<GuardianApplications>> {
        Audit.ApplicationRead.log(targetId = user.id)
        user.requireOneOfRoles(UserRole.END_USER)
        return ResponseEntity.ok(
            db.read { tx ->
                val existingApplicationsByChild = fetchApplicationSummariesForCitizen(tx.handle, user.id)
                    .groupBy { it.childId }
                    .map { GuardianApplications(it.key, it.value.first().childName ?: "", it.value) }

                // Some children might not have applications, so add 0 application children
                getCitizenChildren(tx.handle, user.id).map { citizenChild ->
                    val childApplications = existingApplicationsByChild.findLast { it.childId == citizenChild.childId }
                        ?.let { it.applicationSummaries } ?: emptyList()
                    GuardianApplications(
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
        val application = db.read { fetchApplicationDetails(it.handle, applicationId) }
        return if (application?.guardianId == user.id && !application.hideFromGuardian)
            ResponseEntity.ok(application)
        else
            throw NotFound("Application not found")
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

data class GuardianApplications(
    val childId: UUID,
    val childName: String,
    val applicationSummaries: List<CitizenApplicationSummary>
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
