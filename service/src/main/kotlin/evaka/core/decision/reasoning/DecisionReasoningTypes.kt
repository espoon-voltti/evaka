// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision.reasoning

import evaka.core.shared.DecisionGenericReasoningId
import evaka.core.shared.DecisionIndividualReasoningId
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested

data class DecisionGenericReasoningBody(
    val collectionType: DecisionReasoningCollectionType,
    val validFrom: LocalDate,
    val textFi: String,
    val textSv: String,
    val ready: Boolean,
)

data class DecisionGenericReasoning(
    val id: DecisionGenericReasoningId,
    @Nested("") val body: DecisionGenericReasoningBody,
    val createdAt: HelsinkiDateTime,
    val modifiedAt: HelsinkiDateTime,
)

data class DecisionIndividualReasoningBody(
    val collectionType: DecisionReasoningCollectionType,
    val titleFi: String,
    val titleSv: String,
    val textFi: String,
    val textSv: String,
)

data class DecisionIndividualReasoning(
    val id: DecisionIndividualReasoningId,
    @Nested("") val body: DecisionIndividualReasoningBody,
    val removedAt: HelsinkiDateTime?,
    val createdAt: HelsinkiDateTime,
    val modifiedAt: HelsinkiDateTime,
)
