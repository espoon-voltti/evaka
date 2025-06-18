// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceaction

import fi.espoo.evaka.ConstList
import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.user.EvakaUser
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested

data class AssistanceAction(
    val id: AssistanceActionId,
    val childId: ChildId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val actions: Set<String>,
    val otherAction: String,
    val modifiedAt: HelsinkiDateTime,
    @Nested("modified_by") val modifiedBy: EvakaUser,
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

@ConstList("assistanceActionOptionCategories")
enum class AssistanceActionOptionCategory : DatabaseEnum {
    DAYCARE,
    PRESCHOOL;

    override val sqlType: String = "assistance_action_option_category"
}

data class AssistanceActionOption(
    val value: String,
    val nameFi: String,
    val descriptionFi: String?,
    val category: AssistanceActionOptionCategory,
    val displayOrder: Int?,
    val validFrom: LocalDate?,
    val validTo: LocalDate?,
)
