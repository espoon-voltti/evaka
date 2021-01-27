// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

interface RowWithPrice {
    fun price(): Int
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Invoice(
    val id: UUID,
    val status: InvoiceStatus,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val dueDate: LocalDate = getDueDate(periodEnd),
    val invoiceDate: LocalDate = dueDate.minusWeeks(2),
    val agreementType: Int,
    val headOfFamily: PersonData.JustId,
    val rows: List<InvoiceRow>,
    val number: Long? = null,
    val sentBy: UUID? = null,
    val sentAt: Instant? = null
) {
    @JsonProperty("totalPrice")
    fun totalPrice(): Int = invoiceRowTotal(rows)
}

enum class InvoiceStatus {
    DRAFT,
    WAITING_FOR_SENDING,
    SENT,
    CANCELED
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class InvoiceRow(
    val id: UUID?,
    val child: PersonData.WithDateOfBirth,
    val amount: Int,
    val unitPrice: Int,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val product: Product,
    val costCenter: String,
    val subCostCenter: String?,
    val description: String = ""
) : RowWithPrice {
    @JsonProperty("price")
    override fun price(): Int = amount * unitPrice
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class InvoiceDetailed(
    val id: UUID,
    val status: InvoiceStatus,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val dueDate: LocalDate,
    val invoiceDate: LocalDate,
    val agreementType: Int,
    val headOfFamily: PersonData.Detailed,
    val rows: List<InvoiceRowDetailed>,
    val number: Long?,
    val sentBy: UUID?,
    val sentAt: Instant?
) {
    val account: Int = 3295

    @JsonProperty("totalPrice")
    fun totalPrice(): Int = invoiceRowTotal(rows)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class InvoiceRowDetailed(
    val id: UUID,
    val child: PersonData.Detailed,
    val amount: Int,
    val unitPrice: Int,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val product: Product,
    val costCenter: String,
    val subCostCenter: String?,
    val description: String = ""
) : RowWithPrice {
    @JsonProperty("price")
    override fun price(): Int = amount * unitPrice
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class InvoiceSummary(
    val id: UUID,
    val status: InvoiceStatus,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val headOfFamily: PersonData.Detailed,
    val rows: List<InvoiceRowSummary>,
    val sentBy: UUID?,
    val sentAt: Instant?
) {
    val account: Int = 3295

    @JsonProperty("totalPrice")
    fun totalPrice(): Int = invoiceRowTotal(rows)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class InvoiceRowSummary(
    val id: UUID,
    val child: PersonData.Basic,
    val amount: Int,
    val unitPrice: Int
) : RowWithPrice {
    @JsonProperty("price")
    override fun price(): Int = amount * unitPrice
}

enum class Product(val code: String) {
    DAYCARE("01001"),
    DAYCARE_DISCOUNT("01001"),
    DAYCARE_INCREASE("01001"),
    PRESCHOOL_WITH_DAYCARE("01002"),
    PRESCHOOL_WITH_DAYCARE_DISCOUNT("01002"),
    PRESCHOOL_WITH_DAYCARE_INCREASE("01002"),
    TEMPORARY_CARE("01005"),
    SICK_LEAVE_100("01101"),
    SICK_LEAVE_50("01102"),
    ABSENCE("01103"),
    FREE_OF_CHARGE("01103")
}

fun getDueDate(periodEnd: LocalDate): LocalDate {
    val lastDayOfMonth = periodEnd.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth())
    return when (lastDayOfMonth.dayOfWeek) {
        DayOfWeek.SUNDAY -> lastDayOfMonth.minusDays(2)
        DayOfWeek.SATURDAY -> lastDayOfMonth.minusDays(1)
        else -> lastDayOfMonth
    }
}

fun calculatePriceForTemporary(partDay: Boolean, siblingOrdinal: Int): Int {
    val basePrice = BigDecimal(2900)

    val siblingDiscountMultiplier =
        if (siblingOrdinal == 1) BigDecimal(1)
        else BigDecimal("0.5")
    val afterSiblingDiscount = (basePrice * siblingDiscountMultiplier)
        .divide(BigDecimal(100), 0, RoundingMode.HALF_UP)
        .multiply(BigDecimal(100))

    val serviceNeedMultiplier =
        if (partDay) BigDecimal("0.5")
        else BigDecimal(1)
    return roundToEuros(afterSiblingDiscount * serviceNeedMultiplier).toInt()
}

fun getProductFromActivity(placementType: PlacementType): Product {
    return when (placementType) {
        PlacementType.DAYCARE -> Product.DAYCARE
        PlacementType.PRESCHOOL_WITH_DAYCARE -> Product.PRESCHOOL_WITH_DAYCARE
        PlacementType.PREPARATORY_WITH_DAYCARE -> Product.PRESCHOOL_WITH_DAYCARE
        PlacementType.FIVE_YEARS_OLD_DAYCARE -> Product.DAYCARE
        PlacementType.PRESCHOOL, PlacementType.PREPARATORY ->
            error("Preschool and preparatory without daycare shouldn't be invoiced.")
    }
}

fun getFeeAlterationProduct(product: Product, feeAlterationType: FeeAlteration.Type): Product {
    return when (product to feeAlterationType) {
        Product.DAYCARE to FeeAlteration.Type.DISCOUNT,
        Product.DAYCARE to FeeAlteration.Type.RELIEF -> Product.DAYCARE_DISCOUNT
        Product.DAYCARE to FeeAlteration.Type.INCREASE -> Product.DAYCARE_INCREASE
        Product.PRESCHOOL_WITH_DAYCARE to FeeAlteration.Type.DISCOUNT,
        Product.PRESCHOOL_WITH_DAYCARE to FeeAlteration.Type.RELIEF -> Product.PRESCHOOL_WITH_DAYCARE_DISCOUNT
        Product.PRESCHOOL_WITH_DAYCARE to FeeAlteration.Type.INCREASE -> Product.PRESCHOOL_WITH_DAYCARE_INCREASE
        else -> error("Unknown product + fee alteration type combo ($product + $feeAlterationType)")
    }
}

fun invoiceRowTotal(rows: List<RowWithPrice>): Int = rows.fold(0) { sum, row -> sum + row.price() }
