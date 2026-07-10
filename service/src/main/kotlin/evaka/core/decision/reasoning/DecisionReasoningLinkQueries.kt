// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision.reasoning

import evaka.core.decision.DecisionType
import evaka.core.decision.getDecisionLanguage
import evaka.core.shared.ApplicationId
import evaka.core.shared.DecisionGenericReasoningId
import evaka.core.shared.DecisionId
import evaka.core.shared.DecisionIndividualReasoningId
import evaka.core.shared.EvakaUserId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.OfficialLanguage
import java.time.LocalDate

const val DECISION_REASONING_NOT_FINALIZED = "DECISION_REASONING_NOT_FINALIZED"

fun DecisionType.applicableReasoningCollectionType(): DecisionReasoningCollectionType =
    when (this) {
        DecisionType.DAYCARE,
        DecisionType.DAYCARE_PART_TIME -> DecisionReasoningCollectionType.DAYCARE
        DecisionType.PRESCHOOL -> DecisionReasoningCollectionType.PRESCHOOL
        DecisionType.PRESCHOOL_DAYCARE -> DecisionReasoningCollectionType.DAYCARE
        DecisionType.PREPARATORY_EDUCATION -> DecisionReasoningCollectionType.PRESCHOOL
        DecisionType.CLUB,
        DecisionType.PRESCHOOL_CLUB -> DecisionReasoningCollectionType.CLUB
    }

data class DecisionReasoningStats(val individualReasoningCount: Int, val reasoningWarningCount: Int)

fun Database.Read.getApplicationDecisionReasoningStats(
    applicationIds: Set<ApplicationId>
): Map<ApplicationId, DecisionReasoningStats> {
    if (applicationIds.isEmpty()) return emptyMap()

    val decisionTypes = DecisionType.entries.toList()
    val collectionTypes = decisionTypes.map { it.applicableReasoningCollectionType() }

    data class Row(
        val applicationId: ApplicationId,
        val individualReasoningCount: Int,
        val reasoningWarningCount: Int,
    )

    return createQuery {
            sql(
                """
SELECT
    d.application_id,
    coalesce(sum(sel.cnt), 0)::int AS individual_reasoning_count,
    count(*) FILTER (WHERE NOT coalesce(gen.ready, false))::int AS reasoning_warning_count
FROM decision d
JOIN unnest(${bind(decisionTypes)}, ${bind(collectionTypes)})
    AS type_collection(decision_type, collection_type)
    ON type_collection.decision_type = d.type
LEFT JOIN LATERAL (
    SELECT count(*) AS cnt
    FROM decision_reasoning_individual_selection s
    WHERE s.decision_id = d.id
) sel ON true
LEFT JOIN LATERAL (
    SELECT g.ready
    FROM decision_reasoning_generic g
    WHERE g.removed_at IS NULL
      AND g.collection_type = type_collection.collection_type
      AND g.valid_from <= d.start_date
    ORDER BY g.valid_from DESC, g.created_at DESC
    LIMIT 1
) gen ON true
WHERE d.application_id = ANY(${bind(applicationIds)}) AND d.sent_date IS NULL AND d.planned
GROUP BY d.application_id
"""
            )
        }
        .toList<Row>()
        .associate {
            it.applicationId to
                DecisionReasoningStats(it.individualReasoningCount, it.reasoningWarningCount)
        }
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

fun validateResolvedGenericReasoning(
    tx: Database.Read,
    decisionId: DecisionId,
    decisionType: DecisionType,
    startDate: LocalDate,
): DecisionGenericReasoning {
    val genericReasoning =
        resolveApplicableGenericReasoning(tx, decisionType, startDate)?.takeIf { it.ready }
            ?: throw Conflict(
                "No ready generic reasoning found for decision $decisionId (type: $decisionType, start date: $startDate)",
                errorCode = DECISION_REASONING_NOT_FINALIZED,
            )

    if (tx.getDecisionLanguage(decisionId) == OfficialLanguage.SV) {
        if (genericReasoning.textSv.isBlank()) {
            throw Conflict(
                "Cannot use generic reasoning ${genericReasoning.id} for Swedish decision $decisionId: empty Swedish text",
                errorCode = DECISION_REASONING_NOT_FINALIZED,
            )
        }
        tx.getIndividualReasoningSelectionsForDecision(decisionId).forEach { individual ->
            if (individual.textSv.isBlank()) {
                throw Conflict(
                    "Cannot use individual reasoning ${individual.id} for Swedish decision $decisionId: empty Swedish text",
                    errorCode = DECISION_REASONING_NOT_FINALIZED,
                )
            }
        }
    }
    return genericReasoning
}

fun Database.Read.hasLinkedGenericReasoning(decisionId: DecisionId): Boolean =
    createQuery {
            sql(
                "SELECT generic_reasoning_id IS NOT NULL FROM decision WHERE id = ${bind(decisionId)}"
            )
        }
        .exactlyOne<Boolean>()

data class PlannedUnsentDecision(
    val id: DecisionId,
    val type: DecisionType,
    val startDate: LocalDate,
)

fun Database.Read.getPlannedUnsentDecisions(
    applicationId: ApplicationId
): List<PlannedUnsentDecision> =
    createQuery {
            sql(
                """
SELECT id, type, start_date
FROM decision
WHERE application_id = ${bind(applicationId)} AND sent_date IS NULL AND planned
"""
            )
        }
        .toList<PlannedUnsentDecision>()

fun Database.Transaction.clearGenericReasoningFromUnsentDecisions(applicationId: ApplicationId) {
    execute {
        sql(
            """
UPDATE decision SET generic_reasoning_id = NULL
WHERE application_id = ${bind(applicationId)} AND sent_date IS NULL
"""
        )
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

data class GenericReasoningText(val textFi: String, val textSv: String)

fun Database.Read.getDecisionLinkedGenericReasoningText(
    decisionId: DecisionId
): GenericReasoningText? =
    createQuery {
            sql(
                """
SELECT g.text_fi, g.text_sv
FROM decision d
JOIN decision_reasoning_generic g ON g.id = d.generic_reasoning_id
WHERE d.id = ${bind(decisionId)}
"""
            )
        }
        .exactlyOneOrNull<GenericReasoningText>()

data class DecisionPdfReasoningSource(
    val generic: GenericReasoningText?,
    val individual: List<DecisionIndividualReasoning>,
)

fun Database.Read.getDecisionPdfReasoningSource(
    decisionId: DecisionId
): DecisionPdfReasoningSource =
    DecisionPdfReasoningSource(
        generic = getDecisionLinkedGenericReasoningText(decisionId),
        individual = getIndividualReasoningSelectionsForDecision(decisionId),
    )
