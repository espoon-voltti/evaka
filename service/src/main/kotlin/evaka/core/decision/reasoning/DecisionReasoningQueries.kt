// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision.reasoning

import evaka.core.shared.DecisionGenericReasoningId
import evaka.core.shared.DecisionIndividualReasoningId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.HelsinkiDateTime

fun Database.Read.getGenericReasonings(
    collectionType: DecisionReasoningCollectionType
): List<DecisionGenericReasoning> =
    createQuery {
            sql(
                """
SELECT id, collection_type, valid_from, text_fi, text_sv, ready, created_at, modified_at
FROM decision_reasoning_generic
WHERE collection_type = ${bind(collectionType)}
ORDER BY valid_from DESC, created_at DESC
"""
            )
        }
        .toList<DecisionGenericReasoning>()

fun Database.Transaction.insertGenericReasoning(
    body: DecisionGenericReasoningBody,
    now: HelsinkiDateTime,
): DecisionGenericReasoningId =
    createUpdate {
            sql(
                """
INSERT INTO decision_reasoning_generic (collection_type, valid_from, text_fi, text_sv, ready, created_at, modified_at)
VALUES (${bind(body.collectionType)}, ${bind(body.validFrom)}, ${bind(body.textFi)}, ${bind(body.textSv)}, ${bind(body.ready)}, ${bind(now)}, ${bind(now)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<DecisionGenericReasoningId>()

fun Database.Transaction.updateGenericReasoning(
    id: DecisionGenericReasoningId,
    body: DecisionGenericReasoningBody,
    now: HelsinkiDateTime,
) {
    val updated =
        createUpdate {
                sql(
                    """
UPDATE decision_reasoning_generic
SET collection_type = ${bind(body.collectionType)},
    valid_from = ${bind(body.validFrom)},
    text_fi = ${bind(body.textFi)},
    text_sv = ${bind(body.textSv)},
    ready = ${bind(body.ready)},
    modified_at = ${bind(now)}
WHERE id = ${bind(id)} AND ready = false
"""
                )
            }
            .execute()
    if (updated == 0) {
        throw Conflict("Generic reasoning $id is already marked as ready")
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
        throw Conflict("Cannot delete generic reasoning $id because it is marked as ready")
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
    body: DecisionIndividualReasoningBody,
    now: HelsinkiDateTime,
): DecisionIndividualReasoningId =
    createUpdate {
            sql(
                """
INSERT INTO decision_reasoning_individual (collection_type, title_fi, title_sv, text_fi, text_sv, created_at, modified_at)
VALUES (${bind(body.collectionType)}, ${bind(body.titleFi)}, ${bind(body.titleSv)}, ${bind(body.textFi)}, ${bind(body.textSv)}, ${bind(now)}, ${bind(now)})
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
        throw Conflict("Individual reasoning $id is already removed")
    }
}
