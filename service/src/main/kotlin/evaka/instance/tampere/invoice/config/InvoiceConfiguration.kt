// SPDX-FileCopyrightText: 2021 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.invoice.config

import evaka.core.invoicing.domain.FeeAlterationType
import evaka.core.invoicing.domain.IncomeCoefficient
import evaka.core.invoicing.domain.IncomeType
import evaka.core.invoicing.integration.InvoiceIntegrationClient
import evaka.core.invoicing.service.DefaultInvoiceNumberProvider
import evaka.core.invoicing.service.IncomeCoefficientMultiplierProvider
import evaka.core.invoicing.service.IncomeTypesProvider
import evaka.core.invoicing.service.InvoiceGenerationLogicChooser
import evaka.core.invoicing.service.InvoiceNumberProvider
import evaka.core.invoicing.service.InvoiceProductProvider
import evaka.core.invoicing.service.ProductKey
import evaka.core.invoicing.service.ProductWithName
import evaka.core.placement.PlacementType
import evaka.core.shared.ChildId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.getHolidays
import evaka.instance.tampere.SummertimeAbsenceProperties
import evaka.instance.tampere.TampereProperties
import evaka.instance.tampere.invoice.service.TampereInvoiceClient
import evaka.trevaka.frends.newFrendsHttpClient
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import org.springframework.ws.client.core.WebServiceTemplate
import org.springframework.ws.soap.SoapVersion
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory
import org.springframework.ws.transport.http.SimpleHttpComponents5MessageSender

internal val SOAP_PACKAGES =
    arrayOf(
        "fi.tampere.messages.ipaas.commontypes.v1",
        "fi.tampere.messages.sapsd.salesorder.v11",
        "fi.tampere.services.sapsd.salesorder.v1",
    )

@Configuration
class InvoiceConfiguration {
    @Primary
    @Bean
    fun invoiceIntegrationClient(properties: TampereProperties): InvoiceIntegrationClient {
        val httpClient = newFrendsHttpClient(properties.financeApiKey)
        val messageFactory =
            SaajSoapMessageFactory().apply {
                setSoapVersion(SoapVersion.SOAP_12)
                afterPropertiesSet()
            }
        val marshaller =
            Jaxb2Marshaller().apply {
                setPackagesToScan(*SOAP_PACKAGES)
                afterPropertiesSet()
            }
        val webServiceTemplate =
            WebServiceTemplate(messageFactory).apply {
                this.marshaller = marshaller
                unmarshaller = marshaller
                setMessageSender(SimpleHttpComponents5MessageSender(httpClient))
                afterPropertiesSet()
            }
        return TampereInvoiceClient(webServiceTemplate, properties.invoice)
    }

    @Bean fun incomeTypesProvider(): IncomeTypesProvider = TampereIncomeTypesProvider()

    @Bean
    fun incomeCoefficientMultiplierProvider(): IncomeCoefficientMultiplierProvider =
        TampereIncomeCoefficientMultiplierProvider()

    @Bean fun invoiceProductProvider(): InvoiceProductProvider = TampereInvoiceProductProvider()

    @Bean
    fun invoiceGenerationLogicChooser(
        properties: TampereProperties
    ): InvoiceGenerationLogicChooser =
        TampereInvoiceGeneratorLogicChooser(properties.summertimeAbsence)

    @Bean
    fun invoiceNumberProvider(): InvoiceNumberProvider = DefaultInvoiceNumberProvider(5000000000)
}

class TampereIncomeTypesProvider : IncomeTypesProvider {
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

class TampereIncomeCoefficientMultiplierProvider : IncomeCoefficientMultiplierProvider {
    override fun multiplier(coefficient: IncomeCoefficient): BigDecimal =
        when (coefficient) {
            IncomeCoefficient.MONTHLY_WITH_HOLIDAY_BONUS -> BigDecimal("1.0417")
            IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS -> BigDecimal("1.0000")
            IncomeCoefficient.BI_WEEKLY_WITH_HOLIDAY_BONUS -> BigDecimal("2.2323")
            IncomeCoefficient.BI_WEEKLY_NO_HOLIDAY_BONUS -> BigDecimal("2.1429")
            IncomeCoefficient.DAILY_ALLOWANCE_21_5 -> BigDecimal("21.5")
            IncomeCoefficient.DAILY_ALLOWANCE_25 -> BigDecimal("25")
            IncomeCoefficient.YEARLY -> BigDecimal("0.0833")
        }
}

class TampereInvoiceProductProvider : InvoiceProductProvider {

    override val products = Product.entries.map { ProductWithName(it.key, it.nameFi) }
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
                PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS -> Product.DAYCARE

                PlacementType.PRESCHOOL_DAYCARE,
                PlacementType.PRESCHOOL_DAYCARE_ONLY -> Product.PRESCHOOL_WITH_DAYCARE

                PlacementType.PRESCHOOL_CLUB -> Product.PRESCHOOL_WITH_CLUB

                PlacementType.PREPARATORY_DAYCARE,
                PlacementType.PREPARATORY_DAYCARE_ONLY -> Product.PRESCHOOL_WITH_DAYCARE

                PlacementType.TEMPORARY_DAYCARE -> Product.TEMPORARY_CARE

                PlacementType.TEMPORARY_DAYCARE_PART_DAY -> Product.SUMMER_CLUB

                PlacementType.SCHOOL_SHIFT_CARE -> Product.SCHOOL_SHIFT_CARE

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

                Product.PRESCHOOL_WITH_CLUB to FeeAlterationType.DISCOUNT,
                Product.PRESCHOOL_WITH_CLUB to FeeAlterationType.RELIEF ->
                    Product.PRESCHOOL_WITH_CLUB_DISCOUNT

                Product.DAYCARE to FeeAlterationType.INCREASE,
                Product.PRESCHOOL_WITH_DAYCARE to FeeAlterationType.INCREASE,
                Product.PRESCHOOL_WITH_CLUB to FeeAlterationType.INCREASE -> Product.CORRECTION

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

enum class Product(val nameFi: String, val code: String, val internalOrder: String? = null) {
    DAYCARE("Varhaiskasvatus", "500218"),
    DAYCARE_DISCOUNT("Alennus - Varhaiskasvatus", "500687"),
    PRESCHOOL_WITH_DAYCARE("Esiopetusta täydentävä varhaiskasvatus", "500220"),
    PRESCHOOL_WITH_DAYCARE_DISCOUNT("Alennus - Esiopetusta täydentävä varhaiskasvatus", "509565"),
    PRESCHOOL_WITH_CLUB("Esiopetuksen kerho", "503745"),
    PRESCHOOL_WITH_CLUB_DISCOUNT("Alennus - Esiopetuksen kerhotoiminta", "509787"),
    TEMPORARY_CARE("Tilapäinen varhaiskasvatus", "500576"),
    SUMMER_CLUB("Kesäkerho", "500061", "23461"),
    SCHOOL_SHIFT_CARE("Koululaisen vuorohoito", "500949"),
    SICK_LEAVE_50("Laskuun vaikuttava poissaolo 50%", "500283"),
    SICK_LEAVE_100("Laskuun vaikuttava poissaolo 100%", "500248"),
    ABSENCE("Poissaolovähennys 50%", "500210"),
    FREE_OF_CHARGE("Hyvityspäivä", "503696"),
    CORRECTION("Oikaisu", "500177"),
    FREE_MONTH("Maksuton kuukausi", "500156"),
    OVER_CONTRACT("Sopimuksen ylitys", "500538"),
    UNANNOUNCED_ABSENCE("Ilmoittamaton päivystysajan poissaolo", "507292");

    val key = ProductKey(this.name)
}

class TampereInvoiceGeneratorLogicChooser(private val properties: SummertimeAbsenceProperties) :
    InvoiceGenerationLogicChooser {

    override fun getFreeChildren(
        tx: Database.Read,
        month: YearMonth,
        childIds: Set<ChildId>,
    ): Set<ChildId> {
        val holidays =
            getHolidays(
                FiniteDateRange(LocalDate.of(month.year, 1, 1), LocalDate.of(month.year, 12, 31))
            )
        return when {
            properties.freeMonth == month.month ->
                tx.freeSummerAbsenceChildren(month, childIds, holidays)
            else -> emptySet()
        }
    }

    private fun Database.Read.freeSummerAbsenceChildren(
        month: YearMonth,
        childIds: Set<ChildId>,
        holidays: Set<LocalDate>,
    ): Set<ChildId> {
        // language=SQL
        val sql =
            """
            WITH holidays AS (SELECT day FROM unnest(:holidays::date[]) day)
            SELECT candidate as child_id FROM unnest(:childIds::uuid[]) candidate
            WHERE EXISTS(
                SELECT 1
                FROM holiday_period_questionnaire hpq
                WHERE
                    hpq.absence_type = 'FREE_ABSENCE'
                  AND date_part('year', upper(hpq.active) - interval '1 day') = :year
                  AND EXISTS(
                    SELECT count(a.id), period
                    FROM
                        absence a
                            JOIN placement pl ON pl.child_id = a.child_id AND daterange(pl.start_date, pl.end_date, '[]') @> a.date
                            JOIN daycare dc ON dc.id = pl.unit_id,
                        unnest(hpq.period_options) period
                    WHERE
                        a.absence_type = 'FREE_ABSENCE'
                      AND a.child_id = candidate
                      AND period @> a.date
                      AND a.date NOT IN (SELECT day FROM holidays)
                      AND date_part('isodow', a.date) = ANY(dc.operation_days)
                    GROUP BY period
                    HAVING count(a.id) >= (
                        SELECT count(*)
                        FROM
                            placement pl
                                JOIN daycare dc ON dc.id = pl.unit_id,
                            generate_series(lower(period), upper(period) - interval '1 day', interval '1 day') period_date
                        WHERE
                            pl.child_id = candidate
                          AND period_date NOT IN (SELECT day FROM holidays)
                          AND daterange(pl.start_date, pl.end_date, '[]') @> period_date::date
                          AND date_part('isodow', period_date) = ANY(dc.operation_days)
                    )
                )
            )
            """
                .trimIndent()
        return createQuery { sql(sql) }
            .bind("year", month.year)
            .bind("childIds", childIds)
            .bind("holidays", holidays)
            .toSet<ChildId>()
    }
}
