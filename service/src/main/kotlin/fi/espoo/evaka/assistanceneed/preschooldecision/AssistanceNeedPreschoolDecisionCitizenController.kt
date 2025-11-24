// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.assistanceneed.preschooldecision

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.assistanceneed.decision.UnreadAssistanceNeedDecisionItem
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
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
    private val assistanceNeedDecisionService: AssistanceNeedPreschoolDecisionService,
) {
    @GetMapping("/assistance-need-preschool-decisions")
    fun getAssistanceNeedPreschoolDecisions(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): List<AssistanceNeedPreschoolDecisionCitizenListItem> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_ASSISTANCE_NEED_PRESCHOOL_DECISIONS,
                        user.id,
                    )

                    emptyList<AssistanceNeedPreschoolDecisionCitizenListItem>()
                }
            }
            .also {
                Audit.AssistanceNeedPreschoolDecisionsListCitizen.log(targetId = AuditId(user.id))
            }
    }

    @GetMapping("/children/assistance-need-preschool-decisions/{id}")
    fun getAssistanceNeedPreschoolDecision(
        @PathVariable id: AssistanceNeedPreschoolDecisionId
    ): AssistanceNeedPreschoolDecision {
        throw BadRequest(
            "Preschool assistance need decisions have been migrated to MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION documents"
        )
    }

    @GetMapping("/children/assistance-need-preschool-decisions/{id}/pdf")
    fun getAssistanceNeedPreschoolDecisionPdf(
        @PathVariable id: AssistanceNeedPreschoolDecisionId
    ): ResponseEntity<Any> {
        throw BadRequest(
            "Preschool assistance need decisions have been migrated to MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION documents"
        )
    }

    @PutMapping("/children/assistance-need-preschool-decisions/{id}/read")
    fun markAssistanceNeedPreschoolDecisionAsRead(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: AssistanceNeedPreschoolDecisionId,
    ) {
        throw BadRequest(
            "Preschool assistance need decisions have been migrated to MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION documents"
        )
    }

    @GetMapping("/children/assistance-need-preschool-decisions/unread-counts")
    fun getAssistanceNeedPreschoolDecisionUnreadCount(
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
                        Action.Citizen.Person.READ_UNREAD_ASSISTANCE_NEED_PRESCHOOL_DECISION_COUNT,
                        user.id,
                    )
                    emptyList<UnreadAssistanceNeedDecisionItem>()
                }
            }
            .also {
                Audit.ChildAssistanceNeedPreschoolDecisionGetUnreadCountCitizen.log(
                    targetId = AuditId(user.id)
                )
            }
    }
}
