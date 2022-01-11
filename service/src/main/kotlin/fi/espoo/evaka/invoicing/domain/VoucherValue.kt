// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import fi.espoo.evaka.shared.VoucherValueId
import fi.espoo.evaka.shared.domain.DateRange
import java.math.BigDecimal

data class VoucherValue(
    val id: VoucherValueId,
    val validity: DateRange,
    val baseValue: Int,
    val ageUnderThreeCoefficient: BigDecimal,
)
