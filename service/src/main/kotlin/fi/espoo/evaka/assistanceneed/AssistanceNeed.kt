// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed

import fi.espoo.evaka.shared.AssistanceNeedId
import java.time.LocalDate
import java.util.UUID

data class AssistanceNeed(
    val id: AssistanceNeedId,
    val childId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val capacityFactor: Double,
    val description: String,
    val bases: Set<AssistanceBasis>,
    val otherBasis: String
)

data class AssistanceNeedRequest(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val capacityFactor: Double,
    val description: String = "",
    val bases: Set<AssistanceBasis> = emptySet(),
    val otherBasis: String = ""
)

enum class AssistanceBasis {
    AUTISM,
    DEVELOPMENTAL_DISABILITY_1,
    DEVELOPMENTAL_DISABILITY_2,
    FOCUS_CHALLENGE,
    LINGUISTIC_CHALLENGE,
    DEVELOPMENT_MONITORING,
    DEVELOPMENT_MONITORING_PENDING,
    MULTI_DISABILITY,
    LONG_TERM_CONDITION,
    REGULATION_SKILL_CHALLENGE,
    DISABILITY,
    OTHER
}
