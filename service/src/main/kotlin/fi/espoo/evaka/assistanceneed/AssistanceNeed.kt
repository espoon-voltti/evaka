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
    val bases: Set<String>,
    val otherBasis: String
)

data class AssistanceNeedRequest(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val capacityFactor: Double,
    val description: String = "",
    val bases: Set<String> = emptySet(),
    val otherBasis: String = ""
)

data class AssistanceBasisOption(
    val value: String,
    val nameFi: String,
    val descriptionFi: String?
)
