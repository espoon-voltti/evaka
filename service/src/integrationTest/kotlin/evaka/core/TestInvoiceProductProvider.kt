// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core

import evaka.core.invoicing.domain.FeeAlterationType
import evaka.core.invoicing.service.InvoiceProductProvider
import evaka.core.invoicing.service.ProductKey
import evaka.core.invoicing.service.ProductWithName
import evaka.core.placement.PlacementType

class TestInvoiceProductProvider : InvoiceProductProvider {
    override val dailyRefund = ProductKey("DAILY_REFUND")
    override val partMonthSickLeave = ProductKey("PART_MONTH_SICK_LEAVE")
    override val fullMonthSickLeave = ProductKey("FULL_MONTH_SICK_LEAVE")
    override val fullMonthAbsence = ProductKey("FULL_MONTH_ABSENCE")
    override val contractSurplusDay = ProductKey("SURPLUS_DAY")

    override val products: List<ProductWithName> =
        PlacementType.entries
            .flatMap { placementType ->
                val placementTypeProduct = mapToProduct(placementType)
                FeeAlterationType.entries
                    .map { feeAlterationType ->
                        mapToFeeAlterationProduct(placementTypeProduct, feeAlterationType)
                    }
                    .plus(placementTypeProduct)
            }
            .plus(this.dailyRefund)
            .plus(this.partMonthSickLeave)
            .plus(this.fullMonthSickLeave)
            .plus(this.fullMonthAbsence)
            .map { ProductWithName(it, it.value) }

    override fun mapToProduct(placementType: PlacementType) = ProductKey(placementType.name)

    override fun mapToFeeAlterationProduct(
        productKey: ProductKey,
        feeAlterationType: FeeAlterationType,
    ) = ProductKey("${productKey.value}_${feeAlterationType.name}")
}
