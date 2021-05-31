// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import fi.espoo.evaka.placement.PlacementType
import java.math.BigDecimal
import java.util.UUID

sealed class Placement(open val unit: UUID)

data class PermanentPlacement(
    override val unit: UUID,
    val type: PlacementType
) : Placement(unit)

data class TemporaryPlacement(override val unit: UUID, val partDay: Boolean) : Placement(unit)

sealed class UnitData {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class JustId(val id: UUID) : UnitData()

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class InvoicedByMunicipality(val id: UUID, val invoicedByMunicipality: Boolean) : UnitData()

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Detailed(val id: UUID, val name: String, val areaId: UUID, val areaName: String, val language: String) : UnitData()
}

data class PlacementWithServiceNeed(
    val unitId: UUID,
    val type: PlacementType,
    val serviceNeed: ServiceNeedValue
)

data class ServiceNeedValue(
    val id: UUID,
    val feeCoefficient: BigDecimal,
    val voucherValueCoefficient: BigDecimal,
    val feeDescriptionFi: String,
    val feeDescriptionSv: String,
    val voucherValueDescriptionFi: String,
    val voucherValueDescriptionSv: String
)
