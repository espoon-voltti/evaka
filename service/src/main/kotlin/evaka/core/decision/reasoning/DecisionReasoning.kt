// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision.reasoning

import evaka.core.shared.DecisionGenericReasoningId
import evaka.core.shared.DecisionIndividualReasoningId
import evaka.core.shared.db.DatabaseEnum
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate

enum class DecisionReasoningCollectionType : DatabaseEnum {
    DAYCARE,
    PRESCHOOL;

    override val sqlType = "decision_reasoning_collection_type"
}

data class DecisionGenericReasoningRequest(
    val collectionType: DecisionReasoningCollectionType,
    val validFrom: LocalDate,
    val textFi: String,
    val textSv: String,
    val ready: Boolean,
)

data class DecisionGenericReasoning(
    val id: DecisionGenericReasoningId,
    val collectionType: DecisionReasoningCollectionType,
    val validFrom: LocalDate,
    val textFi: String,
    val textSv: String,
    val ready: Boolean,
    val createdAt: HelsinkiDateTime,
    val modifiedAt: HelsinkiDateTime,
    val endDate: LocalDate?,
    val outdated: Boolean,
)

data class DecisionIndividualReasoningRequest(
    val collectionType: DecisionReasoningCollectionType,
    val titleFi: String,
    val titleSv: String,
    val textFi: String,
    val textSv: String,
)

data class DecisionIndividualReasoning(
    val id: DecisionIndividualReasoningId,
    val collectionType: DecisionReasoningCollectionType,
    val titleFi: String,
    val titleSv: String,
    val textFi: String,
    val textSv: String,
    val removedAt: HelsinkiDateTime?,
    val createdAt: HelsinkiDateTime,
    val modifiedAt: HelsinkiDateTime,
)
