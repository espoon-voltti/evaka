// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision.reasoning

import evaka.core.decision.DecisionType
import evaka.core.shared.DecisionGenericReasoningId
import evaka.core.shared.DecisionId
import evaka.core.shared.DecisionIndividualReasoningId
import evaka.core.shared.EvakaUserId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate

/**
 * For each decision type, the set of [DecisionReasoningCollectionType] slots that apply.
 *
 * Source: the existing admin UI labels in
 * `frontend/src/lib-customizations/defaults/employee/i18n/fi.tsx`
 * (`decisionReasonings.placementTypes`).
 */
fun DecisionType.applicableReasoningCollectionTypes(): Set<DecisionReasoningCollectionType> =
    when (this) {
        DecisionType.DAYCARE,
        DecisionType.DAYCARE_PART_TIME -> setOf(DecisionReasoningCollectionType.DAYCARE)
        DecisionType.PRESCHOOL -> setOf(DecisionReasoningCollectionType.PRESCHOOL)
        DecisionType.PRESCHOOL_DAYCARE -> setOf(DecisionReasoningCollectionType.DAYCARE)
        DecisionType.PREPARATORY_EDUCATION -> setOf(DecisionReasoningCollectionType.PRESCHOOL)
        DecisionType.CLUB,
        DecisionType.PRESCHOOL_CLUB -> setOf(DecisionReasoningCollectionType.DAYCARE)
    }

/**
 * One entry per applicable [DecisionReasoningCollectionType] slot for a given decision type.
 * [reasoning] is null when no ready, non-removed generic exists with `valid_from <= startDate` for
 * that slot.
 */
data class ResolvedGenericReasoning(
    val collectionType: DecisionReasoningCollectionType,
    val reasoning: DecisionGenericReasoning?,
)

fun Database.Read.resolveApplicableGenericReasonings(
    decisionType: DecisionType,
    startDate: LocalDate,
): List<ResolvedGenericReasoning> =
    decisionType.applicableReasoningCollectionTypes().sorted().map { collectionType ->
        ResolvedGenericReasoning(
            collectionType = collectionType,
            reasoning = resolveLatestReadyGenericReasoning(collectionType, startDate),
        )
    }

private fun Database.Read.resolveLatestReadyGenericReasoning(
    collectionType: DecisionReasoningCollectionType,
    startDate: LocalDate,
): DecisionGenericReasoning? {
    val row =
        createQuery {
                sql(
                    """
SELECT id, collection_type, valid_from, text_fi, text_sv, ready, created_at, modified_at
FROM decision_reasoning_generic
WHERE collection_type = ${bind(collectionType)}
  AND valid_from <= ${bind(startDate)}
  AND ready = true
  AND removed_at IS NULL
ORDER BY valid_from DESC, created_at DESC
LIMIT 1
"""
                )
            }
            .exactlyOneOrNull<DecisionGenericReasoningRow>() ?: return null
    return DecisionGenericReasoning(
        id = row.id,
        collectionType = row.collectionType,
        validFrom = row.validFrom,
        textFi = row.textFi,
        textSv = row.textSv,
        ready = row.ready,
        createdAt = row.createdAt,
        modifiedAt = row.modifiedAt,
        // The slot resolver does not need endDate / outdated semantics;
        // those are admin-page concerns. We return safe defaults.
        endDate = null,
        outdated = false,
    )
}

fun Database.Transaction.insertDecisionGenericReasoningLinks(
    decisionId: DecisionId,
    reasoningIds: List<DecisionGenericReasoningId>,
    createdAt: HelsinkiDateTime,
    createdBy: EvakaUserId,
) {
    if (reasoningIds.isEmpty()) return
    executeBatch(reasoningIds) {
        sql(
            """
INSERT INTO decision_generic_reasoning (decision_id, reasoning_id, created_at, created_by)
VALUES (${bind(decisionId)}, ${bind { it }}, ${bind(createdAt)}, ${bind(createdBy)})
"""
        )
    }
}

fun Database.Read.getDecisionGenericReasoningIds(
    decisionId: DecisionId
): List<DecisionGenericReasoningId> =
    createQuery {
            sql(
                """
SELECT reasoning_id
FROM decision_generic_reasoning
WHERE decision_id = ${bind(decisionId)}
ORDER BY created_at
"""
            )
        }
        .toList<DecisionGenericReasoningId>()

fun Database.Transaction.insertDecisionIndividualReasoningLink(
    decisionId: DecisionId,
    reasoningId: DecisionIndividualReasoningId,
    createdAt: HelsinkiDateTime,
    createdBy: EvakaUserId,
) {
    createUpdate {
            sql(
                """
INSERT INTO decision_individual_reasoning (decision_id, reasoning_id, created_at, created_by)
VALUES (${bind(decisionId)}, ${bind(reasoningId)}, ${bind(createdAt)}, ${bind(createdBy)})
ON CONFLICT (decision_id, reasoning_id) DO NOTHING
"""
            )
        }
        .execute()
}

fun Database.Transaction.deleteDecisionIndividualReasoningLink(
    decisionId: DecisionId,
    reasoningId: DecisionIndividualReasoningId,
): Boolean {
    val deleted =
        createUpdate {
                sql(
                    """
DELETE FROM decision_individual_reasoning
WHERE decision_id = ${bind(decisionId)} AND reasoning_id = ${bind(reasoningId)}
"""
                )
            }
            .execute()
    return deleted > 0
}

fun Database.Read.getDecisionIndividualReasoningIds(
    decisionId: DecisionId
): List<DecisionIndividualReasoningId> =
    createQuery {
            sql(
                """
SELECT reasoning_id
FROM decision_individual_reasoning
WHERE decision_id = ${bind(decisionId)}
ORDER BY created_at
"""
            )
        }
        .toList<DecisionIndividualReasoningId>()
