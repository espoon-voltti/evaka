// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.assistanceneed.decision

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import kotlin.collections.emptyList
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

                    emptyList<AssistanceNeedDecisionCitizenListItem>()
                }
            }
            .also { Audit.AssistanceNeedDecisionsListCitizen.log(targetId = AuditId(user.id)) }
    }

    @GetMapping("/children/assistance-need-decision/{id}")
    fun getAssistanceNeedDecision(
        user: AuthenticatedUser.Citizen,
        @PathVariable id: AssistanceNeedDecisionId,
    ): AssistanceNeedDecision {
        throw BadRequest(
            "Assistance need decisions have been migrated to MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION documents"
        )
    }

    @GetMapping("/children/assistance-need-decision/{id}/pdf")
    fun getAssistanceNeedDecisionPdf(
        user: AuthenticatedUser.Citizen,
        @PathVariable id: AssistanceNeedDecisionId,
    ): ResponseEntity<Any> {
        throw BadRequest(
            "Assistance need decisions have been migrated to MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION documents"
        )
    }

    @PostMapping("/children/assistance-need-decision/{id}/read")
    fun markAssistanceNeedDecisionAsRead(
        user: AuthenticatedUser.Citizen,
        @PathVariable id: AssistanceNeedDecisionId,
    ) {

        throw BadRequest(
            "Assistance need decisions have been migrated to MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION documents"
        )
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
                    emptyList<UnreadAssistanceNeedDecisionItem>()
                }
            }
            .also {
                Audit.ChildAssistanceNeedDecisionGetUnreadCountCitizen.log(
                    targetId = AuditId(user.id)
                )
            }
    }
}
