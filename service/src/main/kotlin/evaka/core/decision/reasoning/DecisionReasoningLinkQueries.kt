// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision.reasoning

import evaka.core.decision.DecisionType
import evaka.core.decision.getDecision
import evaka.core.shared.DecisionGenericReasoningId
import evaka.core.shared.DecisionId
import evaka.core.shared.DecisionIndividualReasoningId
import evaka.core.shared.EvakaUserId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.NotFound
import java.time.LocalDate

fun DecisionType.applicableReasoningCollectionType(): DecisionReasoningCollectionType =
    when (this) {
        DecisionType.DAYCARE,
        DecisionType.DAYCARE_PART_TIME -> DecisionReasoningCollectionType.DAYCARE
        DecisionType.PRESCHOOL -> DecisionReasoningCollectionType.PRESCHOOL
        DecisionType.PRESCHOOL_DAYCARE -> DecisionReasoningCollectionType.DAYCARE
        DecisionType.PREPARATORY_EDUCATION -> DecisionReasoningCollectionType.PRESCHOOL
        DecisionType.CLUB,
        DecisionType.PRESCHOOL_CLUB -> DecisionReasoningCollectionType.DAYCARE
    }

fun resolveApplicableGenericReasoning(
    tx: Database.Read,
    decisionType: DecisionType,
    startDate: LocalDate,
): DecisionGenericReasoning? {
    val reasonings =
        tx.getGenericReasonings(decisionType.applicableReasoningCollectionType(), startDate)
    return reasonings.firstOrNull {
        it.validFrom <= startDate && (it.endDate == null || it.endDate >= startDate) && !it.outdated
    }
}

fun Database.Transaction.updateGenericReasoningToDecision(
    decisionId: DecisionId,
    reasoningId: DecisionGenericReasoningId,
) =
    createUpdate {
            sql(
                """UPDATE decision
                    SET generic_reasoning_id = ${bind(reasoningId)}
                    WHERE id = ${bind(decisionId)}
                    AND generic_reasoning_id IS NULL
                """
            )
        }
        .updateExactlyOne()

fun Database.Transaction.setDecisionReasoningIndividualSelections(
    decisionId: DecisionId,
    reasoningIds: Set<DecisionIndividualReasoningId>,
    createdAt: HelsinkiDateTime,
    createdBy: EvakaUserId,
) {
    execute {
        sql(
            """DELETE FROM decision_reasoning_individual_selection
    WHERE decision_id = ${bind(decisionId)}
    """
        )
    }

    if (reasoningIds.isNotEmpty()) {
        prepareBatch(reasoningIds) {
                sql(
                    """
                INSERT INTO decision_reasoning_individual_selection (decision_id, reasoning_id, created_at, created_by)
                VALUES (${bind(decisionId)}, ${bind{it}}, ${bind(createdAt)}, ${bind(createdBy)})
                """
                )
            }
            .execute()
    }
}

fun Database.Read.getDecisionIndividualReasoningIds(
    decisionId: DecisionId
): List<DecisionIndividualReasoningId> =
    createQuery {
            sql(
                """
SELECT reasoning_id
FROM decision_reasoning_individual_selection
WHERE decision_id = ${bind(decisionId)}
ORDER BY created_at
"""
            )
        }
        .toList<DecisionIndividualReasoningId>()

fun getDraftReasoningPreview(tx: Database.Read, decisionId: DecisionId): DraftReasoningPreview {
    val decision = tx.getDecision(decisionId) ?: throw NotFound("Decision $decisionId not found")

    if (decision.sentDate != null) {
        throw BadRequest(
            "Decision $decisionId has already been sent; reasoning preview is for drafts only"
        )
    }

    val generic = resolveApplicableGenericReasoning(tx, decision.type, decision.startDate)
    val individual = tx.getIndividualReasoningSelectionsForDecision(decisionId)

    return DraftReasoningPreview(
        genericReasoning = generic,
        individualReasoningSelections = individual,
    )
}

private data class DecisionStatusRow(
    val id: DecisionId,
    val type: DecisionType,
    val startDate: LocalDate,
    val sentDate: LocalDate?,
)

data class DraftReasoningPreview(
    val genericReasoning: DecisionGenericReasoning?,
    val individualReasoningSelections: List<DecisionIndividualReasoning>,
)
