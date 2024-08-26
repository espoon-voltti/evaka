// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceaction

import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate

data class AssistanceAction(
    val id: AssistanceActionId,
    val childId: ChildId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val actions: Set<String>,
    val otherAction: String,
)

data class AssistanceActionRequest(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val actions: Set<String> = emptySet(),
    val otherAction: String = "",
)

data class AssistanceActionResponse(
    val action: AssistanceAction,
    val permittedActions: Set<Action.AssistanceAction>,
)

data class AssistanceActionOption(
    val value: String,
    val nameFi: String,
    val descriptionFi: String?,
)
