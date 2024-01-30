// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.invoicing.domain.FeeAlterationType
import fi.espoo.evaka.placement.PlacementType

object EspooInvoiceProducts {
    enum class Product(
        val nameFi: String,
        val code: String,
        val nameOnInvoiceFi: String,
        val nameOnInvoiceSv: String
    ) {
        DAYCARE("Varhaiskasvatus", "01001", "Varhaiskasvatus", "Småbarnspedagogik"),
        DAYCARE_DISCOUNT("Alennus (maksup.)", "01001", "Alennus", "Avdrag"),
        DAYCARE_INCREASE("Korotus (maksup.)", "01001", "Lisä", "Lisä"),
        PRESCHOOL_WITH_DAYCARE(
            "Varhaiskasvatus + Esiopetus",
            "01002",
            "Varhaiskasvatus + esiopetus",
            "Småbarnspedagogik + FKL"
        ),
        PRESCHOOL_WITH_DAYCARE_DISCOUNT("Alennus (maksup.)", "01002", "Alennus", "Avdrag"),
        PRESCHOOL_WITH_DAYCARE_INCREASE("Korotus (maksup.)", "01002", "Lisä", "Lisä"),
        TEMPORARY_CARE(
            "Tilapäinen varhaiskasvatus",
            "01005",
            "Tilapäinen varhaiskasvatus",
            "Temporär småbarnspedagogik"
        ),
        SICK_LEAVE_100(
            "Laskuun vaikuttava poissaolo 100%",
            "01101",
            "Laskuun vaikuttava poissaolo 100%",
            "Frånvaro som påverkar faktureringen 100%"
        ),
        SICK_LEAVE_50(
            "Laskuun vaikuttava poissaolo 50%",
            "01102",
            "Laskuun vaikuttava poissaolo 50%",
            "Frånvaro som påverkar faktureringen 50%"
        ),
        ABSENCE("Poissaolovähennys", "01103", "Poissaolovähennys", "Avdrag annan frånvaro");

        val key = ProductKey(this.name)
    }

    fun findProduct(key: ProductKey) =
        Product.entries.find { it.key == key } ?: error("Product with key $key not found")

    class Provider : InvoiceProductProvider {
        override val products = Product.entries.map { ProductWithName(it.key, it.nameFi) }
        override val dailyRefund = Product.ABSENCE.key
        override val partMonthSickLeave = Product.SICK_LEAVE_50.key
        override val fullMonthSickLeave = Product.SICK_LEAVE_100.key
        override val fullMonthAbsence = Product.ABSENCE.key

        override val contractSurplusDay
            get() = error("Contract days not used in Espoo")

        override fun mapToProduct(placementType: PlacementType): ProductKey {
            val product =
                when (placementType) {
                    PlacementType.DAYCARE,
                    PlacementType.DAYCARE_PART_TIME,
                    PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                    PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS -> Product.DAYCARE
                    PlacementType.PRESCHOOL_DAYCARE -> Product.PRESCHOOL_WITH_DAYCARE
                    PlacementType.PREPARATORY_DAYCARE -> Product.PRESCHOOL_WITH_DAYCARE
                    PlacementType.TEMPORARY_DAYCARE,
                    PlacementType.TEMPORARY_DAYCARE_PART_DAY -> Product.TEMPORARY_CARE
                    PlacementType.PRESCHOOL,
                    PlacementType.PRESCHOOL_DAYCARE_ONLY,
                    PlacementType.PRESCHOOL_CLUB,
                    PlacementType.PREPARATORY,
                    PlacementType.PREPARATORY_DAYCARE_ONLY,
                    PlacementType.CLUB,
                    PlacementType.SCHOOL_SHIFT_CARE ->
                        error("No product mapping found for placement type $placementType")
                }
            return product.key
        }

        override fun mapToFeeAlterationProduct(
            productKey: ProductKey,
            feeAlterationType: FeeAlterationType
        ): ProductKey {
            val product =
                when (findProduct(productKey) to feeAlterationType) {
                    Product.DAYCARE to FeeAlterationType.DISCOUNT,
                    Product.DAYCARE to FeeAlterationType.RELIEF -> Product.DAYCARE_DISCOUNT
                    Product.DAYCARE to FeeAlterationType.INCREASE -> Product.DAYCARE_INCREASE
                    Product.PRESCHOOL_WITH_DAYCARE to FeeAlterationType.DISCOUNT,
                    Product.PRESCHOOL_WITH_DAYCARE to FeeAlterationType.RELIEF ->
                        Product.PRESCHOOL_WITH_DAYCARE_DISCOUNT
                    Product.PRESCHOOL_WITH_DAYCARE to FeeAlterationType.INCREASE ->
                        Product.PRESCHOOL_WITH_DAYCARE_INCREASE
                    else ->
                        error(
                            "No product mapping found for product + fee alteration type combo ($productKey + $feeAlterationType)"
                        )
                }
            return product.key
        }
    }
}
