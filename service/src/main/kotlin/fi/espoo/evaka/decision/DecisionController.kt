// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.fetchApplicationDetails
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/decisions2")
class DecisionController(
    private val acl: AccessControlList,
    private val decisionService: DecisionService,
    private val decisionDraftService: DecisionDraftService,
    private val accessControl: AccessControl
) {
    @GetMapping("/by-guardian")
    fun getDecisionsByGuardian(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam("id") guardianId: PersonId
    ): DecisionListResponse {
        Audit.DecisionRead.log(targetId = guardianId)
        accessControl.requirePermissionFor(user, Action.Person.READ_DECISIONS, guardianId)
        val decisions = db.connect { dbc -> dbc.read { it.getDecisionsByGuardian(guardianId, acl.getAuthorizedUnits(user)) } }
        return DecisionListResponse(decisions)
    }

    @GetMapping("/by-child")
    fun getDecisionsByChild(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam("id") childId: ChildId
    ): DecisionListResponse {
        Audit.DecisionRead.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_DECISIONS, childId)
        val decisions = db.connect { dbc -> dbc.read { it.getDecisionsByChild(childId, acl.getAuthorizedUnits(user)) } }
        return DecisionListResponse(decisions)
    }

    @GetMapping("/by-application")
    fun getDecisionsByApplication(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam("id") applicationId: ApplicationId
    ): DecisionListResponse {
        Audit.DecisionRead.log(targetId = applicationId)
        accessControl.requirePermissionFor(user, Action.Application.READ_DECISIONS, applicationId)
        val decisions = db.connect { dbc -> dbc.read { it.getDecisionsByApplication(applicationId, acl.getAuthorizedUnits(user)) } }

        return DecisionListResponse(decisions)
    }

    @GetMapping("/units")
    fun getDecisionUnits(db: Database, user: AuthenticatedUser): List<DecisionUnit> {
        Audit.UnitRead.log()
        accessControl.requirePermissionFor(user, Action.Global.READ_DECISION_UNITS)
        return db.connect { dbc -> dbc.read { decisionDraftService.getDecisionUnits(it) } }
    }

    @GetMapping("/{id}/download", produces = [MediaType.APPLICATION_PDF_VALUE])
    fun downloadDecisionPdf(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable("id") decisionId: DecisionId
    ): ResponseEntity<ByteArray> {
        Audit.DecisionDownloadPdf.log(targetId = decisionId)
        accessControl.requirePermissionFor(user, Action.Decision.DOWNLOAD_PDF, decisionId)

        return db.connect { dbc ->
            dbc.transaction { tx ->
                val decision = tx.getDecision(decisionId) ?: error("Cannot find decision for decision id '$decisionId'")
                val application = tx.fetchApplicationDetails(decision.applicationId)
                    ?: error("Cannot find application for decision id '$decisionId'")

                val child = tx.getPersonById(application.childId)
                    ?: error("Cannot find user for child id '${application.childId}'")

                val guardian = tx.getPersonById(application.guardianId)
                    ?: error("Cannot find user for guardian id '${application.guardianId}'")

                if ((child.restrictedDetailsEnabled || guardian.restrictedDetailsEnabled) && !user.isAdmin)
                    throw Forbidden("Päätöksen alaisella henkilöllä on voimassa turvakielto. Osoitetietojen suojaamiseksi vain pääkäyttäjä voi ladata tämän päätöksen.")

                decisionService.getDecisionPdf(tx, decisionId)
            }
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
