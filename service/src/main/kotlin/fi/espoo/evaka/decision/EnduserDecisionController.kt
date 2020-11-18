// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class EnduserDecisionController {
    @GetMapping("/enduser/decisions")
    fun getDecisions(db: Database.Connection, user: AuthenticatedUser): ResponseEntity<EnduserDecisionsResponse> {
        Audit.DecisionRead.log(targetId = user.id)
        user.requireOneOfRoles(Roles.END_USER)
        val decisions = db.read { getDecisionsByGuardian(it.handle, user.id, AclAuthorization.All) }
            .map {
                val unit = when (it.type) {
                    DecisionType.CLUB -> it.unit.name
                    DecisionType.PREPARATORY_EDUCATION, DecisionType.PRESCHOOL_DAYCARE, DecisionType.PRESCHOOL -> it.unit.preschoolDecisionName
                    DecisionType.DAYCARE, DecisionType.DAYCARE_PART_TIME -> it.unit.daycareDecisionName
                }
                EnduserDecisionJson(
                    id = it.id,
                    type = it.type,
                    startDate = it.startDate,
                    endDate = it.endDate,
                    unit = unit,
                    applicationId = it.applicationId,
                    childId = it.childId,
                    decisionNumber = it.decisionNumber,
                    sentDate = it.sentDate,
                    status = it.status,
                    requestedStartDate = it.requestedStartDate,
                    resolved = it.resolved
                )
            }
        return ResponseEntity.ok(EnduserDecisionsResponse(decisions))
    }
}

data class EnduserAcceptDecisionRequest(val requestedStartDate: LocalDate)

data class EnduserDecisionsResponse(val decisions: List<EnduserDecisionJson>)

data class EnduserDecisionJson(
    val id: UUID,
    val type: DecisionType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val unit: String,
    val applicationId: UUID,
    val childId: UUID,
    val decisionNumber: Long,
    val sentDate: LocalDate,
    val status: DecisionStatus,
    val requestedStartDate: LocalDate?,
    val resolved: LocalDate?
)
