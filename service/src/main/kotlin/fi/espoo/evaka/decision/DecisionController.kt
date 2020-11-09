// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.fetchApplicationDetails
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole.ADMIN
import fi.espoo.evaka.shared.auth.UserRole.FINANCE_ADMIN
import fi.espoo.evaka.shared.auth.UserRole.SERVICE_WORKER
import fi.espoo.evaka.shared.auth.UserRole.UNIT_SUPERVISOR
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.domain.Forbidden
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
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
    private val jdbi: Jdbi,
    private val acl: AccessControlList,
    private val decisionService: DecisionService,
    private val decisionDraftService: DecisionDraftService,
    private val personService: PersonService
) {
    @GetMapping("/by-guardian")
    fun getDecisionsByGuardian(
        user: AuthenticatedUser,
        @RequestParam("id") guardianId: UUID
    ): ResponseEntity<DecisionListResponse> {
        Audit.DecisionRead.log(targetId = guardianId)
        user.requireOneOfRoles(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR)
        val decisions = jdbi.transaction { getDecisionsByGuardian(it, guardianId, acl.getAuthorizedUnits(user)) }

        return ResponseEntity.ok(DecisionListResponse(withPublicDocumentUri(decisions)))
    }

    @GetMapping("/by-child")
    fun getDecisionsByChild(
        user: AuthenticatedUser,
        @RequestParam("id") childId: UUID
    ): ResponseEntity<DecisionListResponse> {
        Audit.DecisionRead.log(targetId = childId)
        user.requireOneOfRoles(ADMIN, SERVICE_WORKER, UNIT_SUPERVISOR)
        val decisions = jdbi.transaction { getDecisionsByChild(it, childId, acl.getAuthorizedUnits(user)) }

        return ResponseEntity.ok(DecisionListResponse(withPublicDocumentUri(decisions)))
    }

    @GetMapping("/by-application")
    fun getDecisionsByApplication(
        user: AuthenticatedUser,
        @RequestParam("id") applicationId: UUID
    ): ResponseEntity<DecisionListResponse> {
        Audit.DecisionRead.log(targetId = applicationId)
        user.requireOneOfRoles(ADMIN, SERVICE_WORKER, UNIT_SUPERVISOR)
        val decisions = jdbi.transaction { getDecisionsByApplication(it, applicationId, acl.getAuthorizedUnits(user)) }

        return ResponseEntity.ok(DecisionListResponse(withPublicDocumentUri(decisions)))
    }

    @GetMapping("/units")
    fun getDecisionUnits(user: AuthenticatedUser): ResponseEntity<List<DecisionUnit>> {
        Audit.UnitRead.log()
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER, Roles.UNIT_SUPERVISOR, Roles.FINANCE_ADMIN)
        val units = jdbi.transaction { decisionDraftService.getDecisionUnits(it) }
        return ResponseEntity.ok(units)
    }

    @GetMapping("/{id}/download", produces = [MediaType.APPLICATION_PDF_VALUE])
    fun downloadDecisionPdf(
        user: AuthenticatedUser,
        @PathVariable("id") decisionId: UUID
    ): ResponseEntity<ByteArray> {
        Audit.DecisionDownloadPdf.log(targetId = decisionId)

        val roles = acl.getRolesForDecision(user, decisionId)
        roles.requireOneOfRoles(Roles.SERVICE_WORKER, Roles.ADMIN, Roles.UNIT_SUPERVISOR, Roles.END_USER)

        return jdbi.transaction { h ->
            if (user.hasOneOfRoles(Roles.END_USER)) {
                if (!getDecisionsByGuardian(h, user.id, AclAuthorization.All).any { it.id == decisionId }) {
                    throw Forbidden("Access denied")
                }
                return@transaction getDecisionPdf(h, decisionId)
            }

            val decision = getDecision(h, decisionId) ?: error("Cannot find decision for decision id '$decisionId'")
            val application = fetchApplicationDetails(h, decision.applicationId)
                ?: error("Cannot find application for decision id '$decisionId'")

            val child = personService.getUpToDatePerson(
                h,
                user,
                application.childId
            ) ?: error("Cannot find user for child id '${application.childId}'")

            val guardian = personService.getUpToDatePerson(
                h,
                user,
                application.guardianId
            ) ?: error("Cannot find user for guardian id '${application.guardianId}'")

            if ((child.restrictedDetailsEnabled || guardian.restrictedDetailsEnabled) && !user.isAdmin())
                throw Forbidden("Päätöksen alaisella henkilöllä on voimassa turvakielto. Osoitetietojen suojaamiseksi vain pääkäyttäjä voi ladata tämän päätöksen.")

            getDecisionPdf(h, decisionId)
        }
    }

    private fun getDecisionPdf(h: Handle, decisionId: UUID): ResponseEntity<ByteArray> {
        return decisionService.getDecisionPdf(h, decisionId)
            .let { document ->
                ResponseEntity.ok()
                    .header("Content-Disposition", "attachment;filename=${document.getName()}")
                    .body(document.getBytes())
            }
    }
}

fun withPublicDocumentUri(decisions: List<Decision>) = decisions.map { decision ->
    decision.copy(documentUri = "/api/internal/decisions2/${decision.id}/download") // do not expose direct s3 uri
}

data class DecisionListResponse(
    val decisions: List<Decision>
)
