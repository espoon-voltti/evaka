package fi.espoo.evaka.application

import fi.espoo.evaka.Audit
import fi.espoo.evaka.decision.DecisionService
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.decision.getDecisionsByGuardian
import fi.espoo.evaka.decision.getOwnDecisions
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/citizen")
class ApplicationControllerCitizen(val decisionService: DecisionService) {
    @GetMapping("/decisions")
    fun getDecisions(db: Database.Connection, user: AuthenticatedUser): ResponseEntity<List<ApplicationDecisions>> {
        Audit.DecisionRead.log(targetId = user.id)
        user.requireOneOfRoles(UserRole.END_USER)
        return ResponseEntity.ok(db.read { getOwnDecisions(it, user.id) })
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
