// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.orivesi.invoice.config

import evaka.core.invoicing.domain.FeeAlterationType
import evaka.core.invoicing.domain.IncomeCoefficient
import evaka.core.invoicing.domain.IncomeType
import evaka.core.invoicing.integration.InvoiceIntegrationClient
import evaka.core.invoicing.service.DefaultInvoiceGenerationLogic
import evaka.core.invoicing.service.IncomeCoefficientMultiplierProvider
import evaka.core.invoicing.service.IncomeTypesProvider
import evaka.core.invoicing.service.InvoiceNumberProvider
import evaka.core.invoicing.service.InvoiceProductProvider
import evaka.core.invoicing.service.ProductKey
import evaka.core.invoicing.service.ProductWithName
import evaka.core.placement.PlacementType
import evaka.core.shared.db.Database
import evaka.instance.orivesi.OrivesiProperties
import evaka.instance.orivesi.invoice.service.OrivesiInvoiceIntegrationClient
import evaka.instance.orivesi.invoice.service.ProEInvoiceGenerator
import evaka.instance.orivesi.invoice.service.S3Sender
import evaka.trevaka.time.ClockService
import java.math.BigDecimal
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import software.amazon.awssdk.services.s3.S3Client

@Configuration
class InvoiceConfiguration {
    @Primary
    @Bean(name = ["orivesiInvoiceIntegrationClient"])
    fun invoiceIntegrationClient(
        clockService: ClockService,
        properties: OrivesiProperties,
        invoiceGenerator: ProEInvoiceGenerator,
        s3Client: S3Client,
    ): InvoiceIntegrationClient {
        val s3Sender = S3Sender(s3Client, properties)
        return OrivesiInvoiceIntegrationClient(clockService, s3Sender, invoiceGenerator)
    }

    @Bean fun incomeTypesProvider(): IncomeTypesProvider = OrivesiIncomeTypesProvider()

    @Bean
    fun incomeCoefficientMultiplierProvider(): IncomeCoefficientMultiplierProvider =
        OrivesiIncomeCoefficientMultiplierProvider()

    @Bean fun invoiceProductProvider(): InvoiceProductProvider = OrivesiInvoiceProductProvider()

    @Bean fun invoiceGenerationLogicChooser() = DefaultInvoiceGenerationLogic

    @Bean fun invoiceNumberProvider(): InvoiceNumberProvider = OrivesiInvoiceNumberProvider()
}

class OrivesiIncomeTypesProvider : IncomeTypesProvider {
    override fun get(): Map<String, IncomeType> =
        linkedMapOf(
            "MAIN_INCOME" to IncomeType("Palkkatulo", 1, false, false),
            "HOLIDAY_BONUS" to IncomeType("Lomaraha", 1, false, false),
            "PERKS" to IncomeType("Luontaisetu", 1, false, false),
            "DAILY_ALLOWANCE" to IncomeType("Päiväraha", 1, true, false),
            "HOME_CARE_ALLOWANCE" to IncomeType("Kotihoidontuki", 1, false, false),
            "PENSION" to IncomeType("Eläke", 1, false, false),
            "RELATIVE_CARE_SUPPORT" to IncomeType("Omaishoidontuki", 1, false, false),
            "STUDENT_INCOME" to IncomeType("Opiskelijan tulot", 1, false, false),
            "GRANT" to IncomeType("Apuraha", 1, false, false),
            "STARTUP_GRANT" to IncomeType("Starttiraha", 1, true, false),
            "BUSINESS_INCOME" to IncomeType("Yritystoiminnan tulo", 1, false, false),
            "CAPITAL_INCOME" to IncomeType("Pääomatulo", 1, false, false),
            "RENTAL_INCOME" to IncomeType("Vuokratulot", 1, false, false),
            "PAID_ALIMONY" to IncomeType("Maksetut elatusavut", -1, false, false),
            "ALIMONY" to IncomeType("Saadut elatusavut", 1, false, false),
            "OTHER_INCOME" to IncomeType("Muu tulo", 1, true, false),
            "ADJUSTED_DAILY_ALLOWANCE" to IncomeType("Soviteltu päiväraha", 1, true, false),
        )
}

class OrivesiIncomeCoefficientMultiplierProvider : IncomeCoefficientMultiplierProvider {
    override fun multiplier(coefficient: IncomeCoefficient): BigDecimal =
        when (coefficient) {
            IncomeCoefficient.MONTHLY_WITH_HOLIDAY_BONUS -> BigDecimal("1.05")
            IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS -> BigDecimal("1.0000")
            IncomeCoefficient.BI_WEEKLY_WITH_HOLIDAY_BONUS -> BigDecimal("2.23125")
            IncomeCoefficient.BI_WEEKLY_NO_HOLIDAY_BONUS -> BigDecimal("2.125")
            IncomeCoefficient.DAILY_ALLOWANCE_21_5 -> BigDecimal("21.5")
            IncomeCoefficient.DAILY_ALLOWANCE_25 -> BigDecimal("25")
            IncomeCoefficient.YEARLY -> BigDecimal("0.0833333")
        }
}

class OrivesiInvoiceProductProvider : InvoiceProductProvider {

    override val products = Product.entries.map { ProductWithName(it.key, it.nameFi) }
    override val dailyRefund = Product.FREE_OF_CHARGE.key
    override val partMonthSickLeave = Product.SICK_LEAVE_50.key
    override val fullMonthSickLeave = Product.SICK_LEAVE_100.key
    override val fullMonthAbsence = Product.ABSENCE.key
    override val contractSurplusDay
        get() = error("Contract days not used in Orivesi")

    override fun mapToProduct(placementType: PlacementType): ProductKey {
        val product =
            when (placementType) {
                PlacementType.DAYCARE,
                PlacementType.DAYCARE_PART_TIME,
                PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS -> Product.DAYCARE

                PlacementType.PRESCHOOL_DAYCARE,
                PlacementType.PRESCHOOL_DAYCARE_ONLY -> Product.PRESCHOOL_WITH_DAYCARE

                PlacementType.PREPARATORY_DAYCARE,
                PlacementType.PREPARATORY_DAYCARE_ONLY -> Product.PRESCHOOL_WITH_DAYCARE

                PlacementType.TEMPORARY_DAYCARE,
                PlacementType.TEMPORARY_DAYCARE_PART_DAY -> Product.TEMPORARY_CARE

                PlacementType.SCHOOL_SHIFT_CARE,
                PlacementType.PRESCHOOL_CLUB,
                PlacementType.PRESCHOOL,
                PlacementType.PREPARATORY,
                PlacementType.CLUB ->
                    error("No product mapping found for placement type $placementType")
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
                Product.DAYCARE to FeeAlterationType.RELIEF -> Product.DAYCARE_DISCOUNT

                Product.PRESCHOOL_WITH_DAYCARE to FeeAlterationType.DISCOUNT,
                Product.PRESCHOOL_WITH_DAYCARE to FeeAlterationType.RELIEF ->
                    Product.PRESCHOOL_WITH_DAYCARE_DISCOUNT

                Product.DAYCARE to FeeAlterationType.INCREASE -> Product.DAYCARE_CORRECTION

                Product.PRESCHOOL_WITH_DAYCARE to FeeAlterationType.INCREASE ->
                    Product.PRESCHOOL_WITH_DAYCARE_CORRECTION

                else ->
                    error(
                        "No product mapping found for product + fee alteration type combo ($productKey + $feeAlterationType)"
                    )
            }
        return product.key
    }
}

fun findProduct(key: ProductKey) =
    Product.entries.find { it.key == key } ?: error("Product with key $key not found")

enum class Product(val nameFi: String, val costCenter: String? = null) {
    DAYCARE("Varhaiskasvatus", "3202"),
    DAYCARE_DISCOUNT("Alennus - Varhaiskasvatus", "3202"),
    PRESCHOOL_WITH_DAYCARE("Esiopetusta täydentävä varhaiskasvatus", "3203"),
    PRESCHOOL_WITH_DAYCARE_DISCOUNT("Alennus - Esiopetusta täydentävä varhaiskasvatus", "3203"),
    TEMPORARY_CARE("Tilapäinen varhaiskasvatus", "3202"),
    SICK_LEAVE_50("Laskuun vaikuttava poissaolo 50%"),
    SICK_LEAVE_100("Laskuun vaikuttava poissaolo 100%"),
    ABSENCE("Poissaolovähennys 50%"),
    FREE_OF_CHARGE("Hyvityspäivä"),
    DAYCARE_CORRECTION("Oikaisu - Varhaiskasvatus", "3202"),
    PRESCHOOL_WITH_DAYCARE_CORRECTION("Oikaisu - Esiopetusta täydentävä varhaiskasvatus", "3203");

    val key = ProductKey(this.name)
}

class OrivesiInvoiceNumberProvider : InvoiceNumberProvider {
    override fun getNextInvoiceNumber(tx: Database.Read): Long =
        tx.createQuery {
                sql(
                    "SELECT max(number) FROM invoice WHERE number >= 30000001 AND number < 54000001"
                )
            }
            .exactlyOneOrNull<Long>()
            ?.let { it + 1 } ?: 30000001
}
