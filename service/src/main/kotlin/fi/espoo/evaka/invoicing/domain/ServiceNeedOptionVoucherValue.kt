// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.ServiceNeedOptionVoucherValueId
import fi.espoo.evaka.shared.domain.DateRange
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
