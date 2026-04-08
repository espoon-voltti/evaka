// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision

import evaka.core.shared.DaycareId
import evaka.core.shared.DecisionId
import java.time.LocalDate

data class DecisionDraft(
    val id: DecisionId,
    val unitId: DaycareId,
    val type: DecisionType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val planned: Boolean,
)
