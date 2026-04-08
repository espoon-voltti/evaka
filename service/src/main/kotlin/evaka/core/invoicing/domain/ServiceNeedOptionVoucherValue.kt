// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.domain

import evaka.core.shared.ServiceNeedOptionId
import evaka.core.shared.ServiceNeedOptionVoucherValueId
import evaka.core.shared.domain.DateRange
import java.math.BigDecimal

data class ServiceNeedOptionVoucherValue(
    val id: ServiceNeedOptionVoucherValueId,
    val serviceNeedOptionId: ServiceNeedOptionId,
    val validity: DateRange,
    val baseValue: Int,
    val coefficient: BigDecimal,
    val value: Int,
    val baseValueUnder3y: Int,
    val coefficientUnder3y: BigDecimal,
    val valueUnder3y: Int,
)
