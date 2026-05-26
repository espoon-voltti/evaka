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
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.HelsinkiDateTime
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

data class ResolvedGenericReasoning(
    val collectionType: DecisionReasoningCollectionType,
    val reasoning: DecisionGenericReasoning?,
    val validUntil: LocalDate?,
)

fun Database.Read.resolveApplicableGenericReasoning(
    decisionType: DecisionType,
    startDate: LocalDate,
): ResolvedGenericReasoning {
    val collectionType = decisionType.applicableReasoningCollectionType()
    val reasoning = resolveLatestApplicableGenericReasoning(collectionType, startDate)
    return ResolvedGenericReasoning(
        collectionType = collectionType,
        reasoning = reasoning,
        validUntil = reasoning?.let { findValidUntil(collectionType, it.validFrom) },
    )
}

private fun Database.Read.findValidUntil(
    collectionType: DecisionReasoningCollectionType,
    validFrom: LocalDate,
): LocalDate? =
    createQuery {
            sql(
                """
SELECT min(valid_from)
FROM decision_reasoning_generic
WHERE collection_type = ${bind(collectionType)}
  AND valid_from > ${bind(validFrom)}
  AND removed_at IS NULL
"""
            )
        }
        .exactlyOneOrNull<LocalDate?>()
        ?.minusDays(1)

private fun Database.Read.resolveLatestApplicableGenericReasoning(
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

fun Database.Transaction.freezeGenericReasoningLinks(
    decisionId: DecisionId,
    decisionType: DecisionType,
    startDate: LocalDate,
    now: HelsinkiDateTime,
    createdBy: EvakaUserId,
): List<DecisionGenericReasoningId> {
    val existing = getDecisionGenericReasoningIds(decisionId)
    if (existing.isNotEmpty()) return existing

    val reasoningIds =
        listOfNotNull(resolveApplicableGenericReasoning(decisionType, startDate).reasoning?.id)
    insertDecisionGenericReasoningLinks(decisionId, reasoningIds, now, createdBy)
    return reasoningIds
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
): Boolean {
    val inserted =
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
    return inserted > 0
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

fun Database.Read.getDraftReasoningPreview(decisionId: DecisionId): DraftReasoningPreview {
    val decision =
        createQuery {
                sql(
                    """
SELECT id, type, start_date, sent_date
FROM decision
WHERE id = ${bind(decisionId)}
"""
                )
            }
            .exactlyOne<DecisionStatusRow>()

    if (decision.sentDate != null) {
        throw BadRequest(
            "Decision $decisionId has already been sent; reasoning preview is for drafts only"
        )
    }

    val generic = resolveApplicableGenericReasoning(decision.type, decision.startDate)

    val individual =
        createQuery {
                sql(
                    """
SELECT r.id, r.collection_type, r.title_fi, r.title_sv, r.text_fi, r.text_sv,
       r.removed_at, r.created_at, r.modified_at
FROM decision_individual_reasoning link
JOIN decision_reasoning_individual r ON r.id = link.reasoning_id
WHERE link.decision_id = ${bind(decisionId)}
ORDER BY link.created_at
"""
                )
            }
            .toList<DecisionIndividualReasoning>()

    return DraftReasoningPreview(genericReasoning = generic, individualReasonings = individual)
}

private data class DecisionStatusRow(
    val id: DecisionId,
    val type: DecisionType,
    val startDate: LocalDate,
    val sentDate: LocalDate?,
)

data class DraftReasoningPreview(
    val genericReasoning: ResolvedGenericReasoning,
    val individualReasonings: List<DecisionIndividualReasoning>,
)
