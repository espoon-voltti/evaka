// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku.invoice.config

import com.jcraft.jsch.JSch
import fi.espoo.evaka.invoicing.domain.FeeAlterationType
import fi.espoo.evaka.invoicing.domain.IncomeCoefficient
import fi.espoo.evaka.invoicing.domain.IncomeType
import fi.espoo.evaka.invoicing.integration.InvoiceIntegrationClient
import fi.espoo.evaka.invoicing.service.DefaultInvoiceNumberProvider
import fi.espoo.evaka.invoicing.service.IncomeCoefficientMultiplierProvider
import fi.espoo.evaka.invoicing.service.IncomeTypesProvider
import fi.espoo.evaka.invoicing.service.InvoiceNumberProvider
import fi.espoo.evaka.invoicing.service.InvoiceProductProvider
import fi.espoo.evaka.invoicing.service.ProductKey
import fi.espoo.evaka.invoicing.service.ProductWithName
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.turku.TurkuProperties
import fi.espoo.evaka.turku.invoice.service.SapInvoiceGenerator
import fi.espoo.evaka.turku.invoice.service.SftpConnector
import fi.espoo.evaka.turku.invoice.service.SftpSender
import fi.espoo.evaka.turku.invoice.service.TurkuInvoiceClient
import java.math.BigDecimal
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("turku_evaka")
@Configuration
class InvoiceConfiguration {
    @Primary
    @Bean(name = ["evakaTurkuInvoiceIntegrationClient"])
    fun invoiceIntegrationClient(
        properties: TurkuProperties,
        invoiceGenerator: SapInvoiceGenerator,
        sftpConnector: SftpConnector,
    ): InvoiceIntegrationClient {
        val sftpSender = SftpSender(properties.sapInvoicing, sftpConnector)
        return TurkuInvoiceClient(sftpSender, invoiceGenerator)
    }

    @Bean fun jsch(): JSch = JSch()

    @Bean fun incomeTypesProvider(): IncomeTypesProvider = TurkuIncomeTypesProvider()

    @Bean
    fun incomeCoefficientMultiplierProvider(): IncomeCoefficientMultiplierProvider =
        TurkuIncomeCoefficientMultiplierProvider()

    @Bean fun invoiceProductProvider(): InvoiceProductProvider = TurkuInvoiceProductProvider()

    @Bean fun invoiceNumberProvider(): InvoiceNumberProvider = DefaultInvoiceNumberProvider(1)
}

class TurkuIncomeTypesProvider : IncomeTypesProvider {
    override fun get(): Map<String, IncomeType> =
        linkedMapOf(
            "MAIN_INCOME" to IncomeType("Palkkatulo", 1, true, false),
            "HOLIDAY_BONUS" to IncomeType("Lomaraha", 1, true, false),
            "PERKS" to IncomeType("Luontaisetu", 1, true, false),
            "DAILY_ALLOWANCE" to IncomeType("Päiväraha", 1, true, false),
            "HOME_CARE_ALLOWANCE" to IncomeType("Kotihoidontuki", 1, true, false),
            "PENSION" to IncomeType("Eläke", 1, true, false),
            "RELATIVE_CARE_SUPPORT" to IncomeType("Omaishoidontuki", 1, true, false),
            "STUDENT_INCOME" to IncomeType("Opiskelijan tulot", 1, true, false),
            "GRANT" to IncomeType("Apuraha", 1, true, false),
            "STARTUP_GRANT" to IncomeType("Starttiraha", 1, true, false),
            "BUSINESS_INCOME" to IncomeType("Yritystoiminnan tulo", 1, true, false),
            "CAPITAL_INCOME" to IncomeType("Pääomatulo", 1, true, false),
            "RENTAL_INCOME" to IncomeType("Vuokratulot", 1, true, false),
            "PAID_ALIMONY" to IncomeType("Maksetut elatusavut", -1, true, false),
            "ALIMONY" to IncomeType("Saadut elatusavut", 1, true, false),
            "OTHER_INCOME" to IncomeType("Muu tulo", 1, true, false),
            "ADJUSTED_DAILY_ALLOWANCE" to IncomeType("Soviteltu päiväraha", 1, true, false),
        )
}

class TurkuIncomeCoefficientMultiplierProvider : IncomeCoefficientMultiplierProvider {
    override fun multiplier(coefficient: IncomeCoefficient): BigDecimal =
        when (coefficient) {
            IncomeCoefficient.MONTHLY_WITH_HOLIDAY_BONUS -> BigDecimal("1.05")

            // = 12.5 / 12
            IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS -> BigDecimal("1.0000")

            // = 12 / 12
            IncomeCoefficient.BI_WEEKLY_WITH_HOLIDAY_BONUS -> BigDecimal("2.23125")

            // = ???
            IncomeCoefficient.BI_WEEKLY_NO_HOLIDAY_BONUS -> BigDecimal("2.125")

            // = ???
            IncomeCoefficient.DAILY_ALLOWANCE_21_5 -> BigDecimal("21.5")

            IncomeCoefficient.DAILY_ALLOWANCE_25 -> BigDecimal("25")

            IncomeCoefficient.YEARLY -> BigDecimal("0.0833333") // 1 / 12
        }
}

class TurkuInvoiceProductProvider : InvoiceProductProvider {
    override val products = Product.values().map { ProductWithName(it.key, it.nameFi) }
    override val dailyRefund = Product.FREE_OF_CHARGE.key
    override val partMonthSickLeave = Product.SICK_LEAVE_50.key
    override val fullMonthSickLeave = Product.SICK_LEAVE_100.key
    override val fullMonthAbsence = Product.ABSENCE.key
    override val contractSurplusDay = Product.OVER_CONTRACT.key

    override fun mapToProduct(placementType: PlacementType): ProductKey {
        val product =
            when (placementType) {
                PlacementType.DAYCARE,
                PlacementType.DAYCARE_PART_TIME,
                PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS -> {
                    Product.DAYCARE
                }

                PlacementType.PRESCHOOL_DAYCARE -> {
                    Product.PRESCHOOL_WITH_DAYCARE
                }

                PlacementType.PREPARATORY_DAYCARE -> {
                    Product.PRESCHOOL_WITH_DAYCARE
                }

                PlacementType.TEMPORARY_DAYCARE,
                PlacementType.TEMPORARY_DAYCARE_PART_DAY -> {
                    Product.TEMPORARY_CARE
                }

                PlacementType.PRESCHOOL,
                PlacementType.PREPARATORY,
                PlacementType.SCHOOL_SHIFT_CARE,
                PlacementType.CLUB,
                PlacementType.PRESCHOOL_CLUB,
                PlacementType.PRESCHOOL_DAYCARE_ONLY,
                PlacementType.PREPARATORY_DAYCARE_ONLY -> {
                    error("No product mapping found for placement type $placementType")
                }
            }
        return product.key
    }

    override fun mapToFeeAlterationProduct(
        productKey: ProductKey,
        feeAlterationType: FeeAlterationType,
    ): ProductKey {
        val product =
            when (findProduct(productKey) to feeAlterationType) {
                Product.DAYCARE to FeeAlterationType.DISCOUNT,
                Product.DAYCARE to FeeAlterationType.RELIEF,
                Product.PRESCHOOL_WITH_DAYCARE to FeeAlterationType.DISCOUNT,
                Product.PRESCHOOL_WITH_DAYCARE to FeeAlterationType.RELIEF ->
                    Product.DAYCARE_DISCOUNT

                Product.DAYCARE to FeeAlterationType.INCREASE -> Product.CORRECTION

                Product.PRESCHOOL_WITH_DAYCARE to FeeAlterationType.INCREASE ->
                    Product.PRESCHOOL_DAYCARE_CORRECTION

                else ->
                    error(
                        "No product mapping found for product + fee alteration type combo ($productKey + $feeAlterationType)"
                    )
            }
        return product.key
    }
}

fun findProduct(key: ProductKey) =
    Product.values().find { it.key == key } ?: error("Product with key $key not found")

enum class Product(val nameFi: String, val code: String) {
    DAYCARE("Varhaiskasvatus", "000000000000007246"),
    DAYCARE_DISCOUNT("Alennus", "000000000000009952"),
    PRESCHOOL_WITH_DAYCARE("Esiopetusta täydentävä varhaiskasvatus", "000000000000007251"),
    TEMPORARY_CARE("Tilapäinen varhaiskasvatus", "000000000000007250"),
    SICK_LEAVE_50("Sairaspoissaolovähennys 50 %", "000000000000007248"),
    SICK_LEAVE_100("Sairaspoissaolovähennys 100 %", "000000000000007247"),
    ABSENCE("Poissaolovähennys 50%", "000000000000007244"),
    FREE_OF_CHARGE("Maksuton päivä", "000000000000008575"),
    CORRECTION("Oikaisu", "000000000000007246"),
    FREE_MONTH("Poissaolovähennys 100 %", "000000000000007243"),
    OVER_CONTRACT("Sovittujen tuntien ylitys", "000000000000009955"),
    PRESCHOOL_DAYCARE_CORRECTION("Kokoaikainen varhaiskasvatus", "000000000000007246");

    val key = ProductKey(this.name)
}
