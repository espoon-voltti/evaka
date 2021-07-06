// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.fetchApplicationDetails
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Forbidden
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/decisions2")
class DecisionController(
    private val acl: AccessControlList,
    private val decisionService: DecisionService,
    private val decisionDraftService: DecisionDraftService,
    private val personService: PersonService
) {
    @GetMapping("/by-guardian")
    fun getDecisionsByGuardian(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam("id") guardianId: UUID
    ): ResponseEntity<DecisionListResponse> {
        Audit.DecisionRead.log(targetId = guardianId)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR)
        val decisions = db.read { it.getDecisionsByGuardian(guardianId, acl.getAuthorizedUnits(user)) }

        return ResponseEntity.ok(DecisionListResponse(decisions))
    }

    @GetMapping("/by-child")
    fun getDecisionsByChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam("id") childId: UUID
    ): ResponseEntity<DecisionListResponse> {
        Audit.DecisionRead.log(targetId = childId)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR)
        val decisions = db.read { it.getDecisionsByChild(childId, acl.getAuthorizedUnits(user)) }

        return ResponseEntity.ok(DecisionListResponse(decisions))
    }

    @GetMapping("/by-application")
    fun getDecisionsByApplication(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam("id") applicationId: ApplicationId
    ): ResponseEntity<DecisionListResponse> {
        Audit.DecisionRead.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR)
        val decisions = db.read { it.getDecisionsByApplication(applicationId, acl.getAuthorizedUnits(user)) }

        return ResponseEntity.ok(DecisionListResponse(decisions))
    }

    @GetMapping("/units")
    fun getDecisionUnits(db: Database.Connection, user: AuthenticatedUser): ResponseEntity<List<DecisionUnit>> {
        Audit.UnitRead.log()
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR)
        val units = db.read { decisionDraftService.getDecisionUnits(it) }
        return ResponseEntity.ok(units)
    }

    @GetMapping("/{id}/download", produces = [MediaType.APPLICATION_PDF_VALUE])
    fun downloadDecisionPdf(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("id") decisionId: DecisionId
    ): ResponseEntity<ByteArray> {
        Audit.DecisionDownloadPdf.log(targetId = decisionId)

        val roles = acl.getRolesForDecision(user, decisionId)
        roles.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)

        return db.transaction { tx ->
            val decision = tx.getDecision(decisionId) ?: error("Cannot find decision for decision id '$decisionId'")
            val application = tx.fetchApplicationDetails(decision.applicationId)
                ?: error("Cannot find application for decision id '$decisionId'")

            val child = personService.getUpToDatePerson(
                tx,
                user,
                application.childId
            ) ?: error("Cannot find user for child id '${application.childId}'")

            val guardian = personService.getUpToDatePerson(
                tx,
                user,
                application.guardianId
            ) ?: error("Cannot find user for guardian id '${application.guardianId}'")

            if ((child.restrictedDetailsEnabled || guardian.restrictedDetailsEnabled) && !user.isAdmin)
                throw Forbidden("Päätöksen alaisella henkilöllä on voimassa turvakielto. Osoitetietojen suojaamiseksi vain pääkäyttäjä voi ladata tämän päätöksen.")

            decisionService.getDecisionPdf(tx, decisionId)
        }.let { document ->
            ResponseEntity.ok()
                .header("Content-Disposition", "attachment;filename=${document.getName()}")
                .body(document.getBytes())
        }
    }
}

data class DecisionListResponse(
    val decisions: List<Decision>
)
