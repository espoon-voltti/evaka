// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import java.time.LocalDate

data class DecisionDraft(
    val id: DecisionId,
    val unitId: DaycareId,
    val type: DecisionType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val planned: Boolean,
)
