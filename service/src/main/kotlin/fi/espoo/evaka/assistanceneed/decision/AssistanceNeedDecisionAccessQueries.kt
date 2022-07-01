// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.decision

import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.mapTo

fun Database.Read.filterPermittedAssistanceNeedDecisionsForDecisionMaker(user: AuthenticatedUser.Employee, ids: Collection<AssistanceNeedDecisionId>): Set<AssistanceNeedDecisionId> =
    this.createQuery(
        """
SELECT id
FROM assistance_need_decision
WHERE decision_maker_employee_id = :employeeId
  AND id = ANY(:ids)
  AND sent_for_decision IS NOT NULL
        """.trimIndent()
    )
        .bind("employeeId", user.id)
        .bind("ids", ids.toTypedArray())
        .mapTo<AssistanceNeedDecisionId>()
        .toSet()

fun Database.Read.filterSentAssistanceNeedDecisions(ids: Collection<AssistanceNeedDecisionId>): Set<AssistanceNeedDecisionId> =
    this.createQuery(
        """
SELECT id
FROM assistance_need_decision
WHERE id = ANY(:ids)
  AND sent_for_decision IS NOT NULL
        """.trimIndent()
    )
        .bind("ids", ids.toTypedArray())
        .mapTo<AssistanceNeedDecisionId>()
        .toSet()
