// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.assistanceneed.decision

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen")
class AssistanceNeedDecisionCitizenController(
    private val accessControl: AccessControl,
    private val assistanceNeedDecisionService: AssistanceNeedDecisionService,
) {
    @GetMapping("/assistance-need-decisions")
    fun getAssistanceNeedDecisions(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): List<AssistanceNeedDecisionCitizenListItem> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_ASSISTANCE_NEED_DECISIONS,
                        user.id,
                    )

                    tx.getAssistanceNeedDecisionsForCitizen(clock.today(), user.id)
                }
            }
            .also { Audit.AssistanceNeedDecisionsListCitizen.log(targetId = AuditId(user.id)) }
    }

    @GetMapping("/children/assistance-need-decision/{id}")
    fun getAssistanceNeedDecision(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: AssistanceNeedDecisionId,
    ): AssistanceNeedDecision {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.AssistanceNeedDecision.READ,
                        id,
                    )
                    val decision = tx.getAssistanceNeedDecisionById(id)

                    if (
                        decision.status != AssistanceNeedDecisionStatus.ACCEPTED &&
                            decision.status != AssistanceNeedDecisionStatus.REJECTED &&
                            decision.status != AssistanceNeedDecisionStatus.ANNULLED
                    ) {
                        throw NotFound(
                            "Citizen can only view accepted and rejected assistance need decisions"
                        )
                    }

                    decision
                }
            }
            .also { Audit.ChildAssistanceNeedDecisionReadCitizen.log(targetId = AuditId(id)) }
    }

    @GetMapping("/children/assistance-need-decision/{id}/pdf")
    fun getAssistanceNeedDecisionPdf(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: AssistanceNeedDecisionId,
    ): ResponseEntity<Any> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Citizen.AssistanceNeedDecision.DOWNLOAD,
                        id,
                    )
                }
                assistanceNeedDecisionService.getDecisionPdfResponse(dbc, id)
            }
            .also { Audit.ChildAssistanceNeedDecisionDownloadCitizen.log(targetId = AuditId(id)) }
    }

    @PostMapping("/children/assistance-need-decision/{id}/read")
    fun markAssistanceNeedDecisionAsRead(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: AssistanceNeedDecisionId,
    ) {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.AssistanceNeedDecision.MARK_AS_READ,
                        id,
                    )
                    tx.markAssistanceNeedDecisionAsReadByGuardian(id, user.id)
                }
            }
            .also { Audit.ChildAssistanceNeedDecisionMarkReadCitizen.log(targetId = AuditId(id)) }
    }

    @GetMapping("/children/assistance-need-decisions/unread-counts")
    fun getAssistanceNeedDecisionUnreadCount(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): List<UnreadAssistanceNeedDecisionItem> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_UNREAD_ASSISTANCE_NEED_DECISION_COUNT,
                        user.id,
                    )
                    tx.getAssistanceNeedDecisionsUnreadCountsForCitizen(clock.today(), user.id)
                }
            }
            .also {
                Audit.ChildAssistanceNeedDecisionGetUnreadCountCitizen.log(
                    targetId = AuditId(user.id)
                )
            }
    }
}
