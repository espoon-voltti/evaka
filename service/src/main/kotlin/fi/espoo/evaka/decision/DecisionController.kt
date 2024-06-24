// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.application.fetchApplicationDetails
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
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
@RequestMapping(
    "/decisions2", // deprecated
    "/employee/decisions"
)
class DecisionController(
    private val decisionService: DecisionService,
    private val accessControl: AccessControl
) {
    @GetMapping("/by-guardian")
    fun getDecisionsByGuardian(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam id: PersonId
    ): DecisionListResponse {
        val decisions =
            db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Person.READ_DECISIONS,
                        id
                    )
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.Decision.READ
                        )
                    it.getDecisionsByGuardian(id, filter)
                }
            }
        Audit.DecisionRead.log(targetId = AuditId(id), meta = mapOf("count" to decisions.size))
        return DecisionListResponse(decisions)
    }

    @GetMapping("/by-child")
    fun getDecisionsByChild(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam id: ChildId
    ): DecisionListResponse {
        val decisions =
            db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Child.READ_DECISIONS,
                        id
                    )
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.Decision.READ
                        )
                    it.getDecisionsByChild(id, filter)
                }
            }
        Audit.DecisionRead.log(targetId = AuditId(id), meta = mapOf("count" to decisions.size))
        return DecisionListResponse(decisions)
    }

    @GetMapping("/by-application")
    fun getDecisionsByApplication(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam id: ApplicationId
    ): DecisionListResponse {
        val decisions =
            db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Application.READ_DECISIONS,
                        id
                    )
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.Decision.READ
                        )
                    it.getDecisionsByApplication(id, filter)
                }
            }
        Audit.DecisionReadByApplication.log(targetId = AuditId(id))

        return DecisionListResponse(decisions)
    }

    @GetMapping("/units")
    fun getDecisionUnits(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock
    ): List<DecisionUnit> =
        db
            .connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_DECISION_UNITS
                    )
                    getDecisionUnits(it)
                }
            }.also { Audit.UnitRead.log(meta = mapOf("count" to it.size)) }

    @GetMapping("/{id}/download", produces = [MediaType.APPLICATION_PDF_VALUE])
    fun downloadDecisionPdf(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: DecisionId
    ): ResponseEntity<Any> =
        db
            .connect { dbc ->
                val decision =
                    dbc.transaction { tx ->
                        accessControl.requirePermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Decision.DOWNLOAD_PDF,
                            id
                        )

                        val decision =
                            tx.getDecision(id)
                                ?: error("Cannot find decision for decision id '$id'")
                        val application =
                            tx.fetchApplicationDetails(decision.applicationId)
                                ?: error("Cannot find application for decision id '$id'")

                        val child =
                            tx.getPersonById(application.childId)
                                ?: error("Cannot find user for child id '${application.childId}'")

                        val guardian =
                            tx.getPersonById(application.guardianId)
                                ?: error(
                                    "Cannot find user for guardian id '${application.guardianId}'"
                                )

                        if (
                            (child.restrictedDetailsEnabled || guardian.restrictedDetailsEnabled) &&
                            decision.documentContainsContactInfo &&
                            !user.isAdmin
                        ) {
                            throw Forbidden(
                                "Päätöksen alaisella henkilöllä on voimassa turvakielto. Osoitetietojen suojaamiseksi vain pääkäyttäjä voi ladata tämän päätöksen."
                            )
                        }

                        decision
                    }
                decisionService.getDecisionPdf(dbc, decision)
            }.also { Audit.DecisionDownloadPdf.log(targetId = AuditId(id)) }
}

data class DecisionListResponse(
    val decisions: List<Decision>
)
