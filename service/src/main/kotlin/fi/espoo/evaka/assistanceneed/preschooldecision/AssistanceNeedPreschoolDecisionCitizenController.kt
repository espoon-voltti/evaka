// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.assistanceneed.preschooldecision

import fi.espoo.evaka.Audit
import fi.espoo.evaka.assistanceneed.decision.UnreadAssistanceNeedDecisionItem
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen")
class AssistanceNeedPreschoolDecisionCitizenController(
    private val accessControl: AccessControl,
    private val assistanceNeedDecisionService: AssistanceNeedPreschoolDecisionService
) {
    @GetMapping("/assistance-need-preschool-decisions")
    fun getAssistanceNeedPreschoolDecisions(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock
    ): List<AssistanceNeedPreschoolDecisionCitizenListItem> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_ASSISTANCE_NEED_PRESCHOOL_DECISIONS,
                        user.id
                    )

                    tx.getAssistanceNeedPreschoolDecisionsForCitizen(clock.today(), user.id)
                }
            }
            .also { Audit.AssistanceNeedPreschoolDecisionsListCitizen.log(targetId = user.id) }
    }

    @GetMapping("/children/assistance-need-preschool-decisions/{id}")
    fun getAssistanceNeedPreschoolDecision(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: AssistanceNeedPreschoolDecisionId
    ): AssistanceNeedPreschoolDecision {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.AssistanceNeedPreschoolDecision.READ,
                        id
                    )
                    val decision = tx.getAssistanceNeedPreschoolDecisionById(id)

                    if (!decision.status.isDecided()) {
                        throw NotFound("Citizen can only view decided assistance need decisions")
                    }

                    decision
                }
            }
            .also { Audit.ChildAssistanceNeedPreschoolDecisionReadCitizen.log(targetId = id) }
    }

    @GetMapping("/children/assistance-need-preschool-decisions/{id}/pdf")
    fun getAssistanceNeedPreschoolDecisionPdf(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: AssistanceNeedPreschoolDecisionId
    ): ResponseEntity<Any> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Citizen.AssistanceNeedPreschoolDecision.DOWNLOAD,
                        id
                    )
                }
                assistanceNeedDecisionService.getDecisionPdfResponse(dbc, id)
            }
            .also { Audit.ChildAssistanceNeedPreschoolDecisionDownloadCitizen.log(targetId = id) }
    }

    @PutMapping("/children/assistance-need-preschool-decisions/{id}/read")
    fun markAssistanceNeedPreschoolDecisionAsRead(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: AssistanceNeedPreschoolDecisionId
    ) {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.AssistanceNeedPreschoolDecision.MARK_AS_READ,
                        id
                    )
                    tx.markAssistanceNeedPreschoolDecisionAsReadByGuardian(id, user.id)
                }
            }
            .also { Audit.ChildAssistanceNeedPreschoolDecisionMarkReadCitizen.log(targetId = id) }
    }

    @GetMapping("/children/assistance-need-preschool-decisions/unread-counts")
    fun getAssistanceNeedPreschoolDecisionUnreadCount(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock
    ): List<UnreadAssistanceNeedDecisionItem> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_UNREAD_ASSISTANCE_NEED_DECISION_COUNT,
                        user.id
                    )
                    tx.getAssistanceNeedPreschoolDecisionsUnreadCountsForCitizen(
                        clock.today(),
                        user.id
                    )
                }
            }
            .also {
                Audit.ChildAssistanceNeedPreschoolDecisionGetUnreadCountCitizen.log(
                    targetId = user.id
                )
            }
    }
}
