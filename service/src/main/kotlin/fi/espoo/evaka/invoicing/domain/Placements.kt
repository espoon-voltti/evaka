// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import java.math.BigDecimal

data class UnitData(
    val id: DaycareId,
    val name: String,
    val areaId: AreaId,
    val areaName: String,
    val language: String,
)

data class PlacementWithServiceNeed(
    val unitId: DaycareId,
    val type: PlacementType,
    val serviceNeed: ServiceNeedValue,
    val missingServiceNeed: Boolean,
)

data class ServiceNeedValue(
    val optionId: ServiceNeedOptionId,
    val feeCoefficient: BigDecimal,
    val contractDaysPerMonth: Int?,
    val feeDescriptionFi: String,
    val feeDescriptionSv: String,
    val voucherValueDescriptionFi: String,
    val voucherValueDescriptionSv: String,
)
