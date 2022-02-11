// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed

import fi.espoo.evaka.shared.AssistanceNeedId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.security.Action
import java.math.BigDecimal
import java.time.LocalDate

data class AssistanceNeed(
    val id: AssistanceNeedId,
    val childId: ChildId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val capacityFactor: Double,
    val bases: Set<String>
)

data class AssistanceNeedRequest(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val capacityFactor: Double,
    val bases: Set<String> = emptySet(),
)

data class AssistanceNeedResponse(
    val need: AssistanceNeed,
    val permittedActions: Set<Action.AssistanceNeed>
)

data class AssistanceBasisOption(
    val value: String,
    val nameFi: String,
    val descriptionFi: String?
)

data class AssistanceNeedChildRange(
    val childId: ChildId,
    val dateRange: DateRange
)

data class AssistanceNeedCapacityFactor(
    val dateRange: DateRange,
    val capacityFactor: BigDecimal,
)
