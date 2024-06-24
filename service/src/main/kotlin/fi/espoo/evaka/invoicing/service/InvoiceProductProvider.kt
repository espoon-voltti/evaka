// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import fi.espoo.evaka.invoicing.domain.FeeAlterationType
import fi.espoo.evaka.placement.PlacementType

interface InvoiceProductProvider {
    val products: List<ProductWithName>
    val dailyRefund: ProductKey
    val partMonthSickLeave: ProductKey
    val fullMonthSickLeave: ProductKey
    val fullMonthAbsence: ProductKey
    val contractSurplusDay: ProductKey

    fun mapToProduct(placementType: PlacementType): ProductKey

    fun mapToFeeAlterationProduct(
        productKey: ProductKey,
        feeAlterationType: FeeAlterationType
    ): ProductKey
}

@JsonSerialize(converter = ProductKey.ToJson::class)
@JsonDeserialize(converter = ProductKey.FromJson::class)
data class ProductKey(
    val value: String
) {
    class FromJson : StdConverter<String, ProductKey>() {
        override fun convert(value: String): ProductKey = ProductKey(value)
    }

    class ToJson : StdConverter<ProductKey, String>() {
        override fun convert(value: ProductKey): String = value.value
    }
}

data class ProductWithName(
    val key: ProductKey,
    val nameFi: String
)
