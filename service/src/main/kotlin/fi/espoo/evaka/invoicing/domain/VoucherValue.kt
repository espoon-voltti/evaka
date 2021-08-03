// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import fi.espoo.evaka.shared.domain.DateRange
import java.math.BigDecimal
import java.util.UUID

data class VoucherValue(
    val id: UUID,
    val validity: DateRange,
    val baseValue: Int,
    val ageUnderThreeCoefficient: BigDecimal,
)
