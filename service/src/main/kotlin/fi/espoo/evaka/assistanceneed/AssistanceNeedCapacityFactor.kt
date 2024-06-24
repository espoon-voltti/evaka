// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed

import fi.espoo.evaka.shared.domain.DateRange
import java.math.BigDecimal

data class AssistanceNeedCapacityFactor(
    val dateRange: DateRange,
    val capacityFactor: BigDecimal
)
