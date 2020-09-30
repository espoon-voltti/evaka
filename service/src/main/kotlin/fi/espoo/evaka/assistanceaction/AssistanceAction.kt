// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceaction

import java.time.LocalDate
import java.util.UUID

data class AssistanceAction(
    val id: UUID,
    val childId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val actions: Set<AssistanceActionType>,
    val otherAction: String,
    val measures: Set<AssistanceMeasure>
)

data class AssistanceActionRequest(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val actions: Set<AssistanceActionType> = emptySet(),
    val otherAction: String = "",
    val measures: Set<AssistanceMeasure> = emptySet()
)

enum class AssistanceActionType {
    ASSISTANCE_SERVICE_CHILD,
    ASSISTANCE_SERVICE_UNIT,
    SMALLER_GROUP,
    SPECIAL_GROUP,
    PERVASIVE_VEO_SUPPORT,
    RESOURCE_PERSON,
    RATIO_DECREASE,
    PERIODICAL_VEO_SUPPORT,
    OTHER
}

enum class AssistanceMeasure {
    SPECIAL_ASSISTANCE_DECISION,
    INTENSIFIED_ASSISTANCE,
    EXTENDED_COMPULSORY_EDUCATION,
    CHILD_SERVICE,
    CHILD_ACCULTURATION_SUPPORT,
    TRANSPORT_BENEFIT
}
