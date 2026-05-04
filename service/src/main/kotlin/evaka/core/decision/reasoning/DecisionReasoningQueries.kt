// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision.reasoning

import evaka.core.shared.DecisionGenericReasoningId
import evaka.core.shared.DecisionIndividualReasoningId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.NotFound
import java.time.LocalDate

private data class DecisionGenericReasoningRow(
    val id: DecisionGenericReasoningId,
    val collectionType: DecisionReasoningCollectionType,
    val validFrom: LocalDate,
    val textFi: String,
    val textSv: String,
    val ready: Boolean,
    val createdAt: HelsinkiDateTime,
    val modifiedAt: HelsinkiDateTime,
)

fun Database.Read.getGenericReasonings(
    collectionType: DecisionReasoningCollectionType,
    today: LocalDate,
): List<DecisionGenericReasoning> {
    val rows =
        createQuery {
                sql(
                    """
SELECT id, collection_type, valid_from, text_fi, text_sv, ready, created_at, modified_at
FROM decision_reasoning_generic
WHERE collection_type = ${bind(collectionType)} AND removed_at IS NULL
ORDER BY valid_from DESC, created_at DESC
"""
                )
            }
            .toList<DecisionGenericReasoningRow>()
    val sortedUniqueValidFrom = rows.map { it.validFrom }.distinct().sorted()
    return rows.map { row ->
        val idx = sortedUniqueValidFrom.indexOf(row.validFrom)
        val endDate =
            if (idx < 0 || idx == sortedUniqueValidFrom.lastIndex) null
            else sortedUniqueValidFrom[idx + 1].minusDays(1)
        val supersededBy = rows.any {
            it.id != row.id && it.validFrom == row.validFrom && it.createdAt > row.createdAt
        }
        DecisionGenericReasoning(
            id = row.id,
            collectionType = row.collectionType,
            validFrom = row.validFrom,
            textFi = row.textFi,
            textSv = row.textSv,
            ready = row.ready,
            createdAt = row.createdAt,
            modifiedAt = row.modifiedAt,
            endDate = endDate,
            outdated = (endDate != null && endDate.isBefore(today)) || supersededBy,
        )
    }
}

fun Database.Transaction.insertGenericReasoning(
    request: DecisionGenericReasoningRequest,
    now: HelsinkiDateTime,
): DecisionGenericReasoningId =
    createUpdate {
            sql(
                """
INSERT INTO decision_reasoning_generic (collection_type, valid_from, text_fi, text_sv, ready, created_at, modified_at)
VALUES (${bind(request.collectionType)}, ${bind(request.validFrom)}, ${bind(request.textFi)}, ${bind(request.textSv)}, ${bind(request.ready)}, ${bind(now)}, ${bind(now)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<DecisionGenericReasoningId>()

fun Database.Transaction.updateGenericReasoning(
    id: DecisionGenericReasoningId,
    request: DecisionGenericReasoningRequest,
    now: HelsinkiDateTime,
) {
    val updated =
        createUpdate {
                sql(
                    """
UPDATE decision_reasoning_generic
SET valid_from = ${bind(request.validFrom)},
    text_fi = ${bind(request.textFi)},
    text_sv = ${bind(request.textSv)},
    ready = ${bind(request.ready)},
    modified_at = ${bind(now)}
WHERE id = ${bind(id)} AND ready = false
"""
                )
            }
            .execute()
    if (updated == 0) {
        throw NotFound("Generic reasoning $id not found in expected state (not ready)")
    }
}

fun Database.Transaction.deleteGenericReasoning(id: DecisionGenericReasoningId) {
    val deleted =
        createUpdate {
                sql(
                    """
DELETE FROM decision_reasoning_generic
WHERE id = ${bind(id)} AND ready = false
"""
                )
            }
            .execute()
    if (deleted == 0) {
        throw NotFound("Generic reasoning $id not found in expected state (not ready)")
    }
}

fun Database.Read.getIndividualReasonings(
    collectionType: DecisionReasoningCollectionType
): List<DecisionIndividualReasoning> =
    createQuery {
            sql(
                """
SELECT id, collection_type, title_fi, title_sv, text_fi, text_sv, removed_at, created_at, modified_at
FROM decision_reasoning_individual
WHERE collection_type = ${bind(collectionType)}
ORDER BY created_at DESC
"""
            )
        }
        .toList<DecisionIndividualReasoning>()

fun Database.Transaction.insertIndividualReasoning(
    request: DecisionIndividualReasoningRequest,
    now: HelsinkiDateTime,
): DecisionIndividualReasoningId =
    createUpdate {
            sql(
                """
INSERT INTO decision_reasoning_individual (collection_type, title_fi, title_sv, text_fi, text_sv, created_at, modified_at)
VALUES (${bind(request.collectionType)}, ${bind(request.titleFi)}, ${bind(request.titleSv)}, ${bind(request.textFi)}, ${bind(request.textSv)}, ${bind(now)}, ${bind(now)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<DecisionIndividualReasoningId>()

fun Database.Transaction.removeIndividualReasoning(
    id: DecisionIndividualReasoningId,
    now: HelsinkiDateTime,
) {
    val updated =
        createUpdate {
                sql(
                    """
UPDATE decision_reasoning_individual
SET removed_at = ${bind(now)}, modified_at = ${bind(now)}
WHERE id = ${bind(id)} AND removed_at IS NULL
"""
                )
            }
            .execute()
    if (updated == 0) {
        throw NotFound("Individual reasoning $id not found")
    }
}
