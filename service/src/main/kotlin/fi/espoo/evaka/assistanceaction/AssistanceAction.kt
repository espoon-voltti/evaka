// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceaction

import fi.espoo.evaka.shared.AssistanceActionId
import java.time.LocalDate
import java.util.UUID

data class AssistanceAction(
    val id: AssistanceActionId,
    val childId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val actions: Set<String>,
    val otherAction: String,
    val measures: Set<AssistanceMeasure>
)

data class AssistanceActionRequest(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val actions: Set<String> = emptySet(),
    val otherAction: String = "",
    val measures: Set<AssistanceMeasure> = emptySet()
)

data class AssistanceActionOption(
    val value: String,
    val nameFi: String
)

enum class AssistanceMeasure {
    SPECIAL_ASSISTANCE_DECISION,
    INTENSIFIED_ASSISTANCE,
    EXTENDED_COMPULSORY_EDUCATION,
    CHILD_SERVICE,
    CHILD_ACCULTURATION_SUPPORT,
    TRANSPORT_BENEFIT
}
