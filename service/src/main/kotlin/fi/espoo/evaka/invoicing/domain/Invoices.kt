// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.PlacementType.CLUB
import fi.espoo.evaka.placement.PlacementType.DAYCARE
import fi.espoo.evaka.placement.PlacementType.DAYCARE_FIVE_YEAR_OLDS
import fi.espoo.evaka.placement.PlacementType.DAYCARE_PART_TIME
import fi.espoo.evaka.placement.PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS
import fi.espoo.evaka.placement.PlacementType.PREPARATORY
import fi.espoo.evaka.placement.PlacementType.PREPARATORY_DAYCARE
import fi.espoo.evaka.placement.PlacementType.PRESCHOOL
import fi.espoo.evaka.placement.PlacementType.PRESCHOOL_DAYCARE
import fi.espoo.evaka.placement.PlacementType.SCHOOL_SHIFT_CARE
import fi.espoo.evaka.placement.PlacementType.TEMPORARY_DAYCARE
import fi.espoo.evaka.placement.PlacementType.TEMPORARY_DAYCARE_PART_DAY
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.InvoiceRowId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

interface RowWithPrice {
    val price: Int
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Invoice(
    val id: InvoiceId,
    val status: InvoiceStatus,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val dueDate: LocalDate = getDueDate(periodEnd),
    val invoiceDate: LocalDate = dueDate.minusWeeks(2),
    val areaId: AreaId,
    val headOfFamily: PersonId,
    val codebtor: PersonId?,
    val rows: List<InvoiceRow>,
    val number: Long? = null,
    val sentBy: EvakaUserId? = null,
    val sentAt: HelsinkiDateTime? = null
) {
    val totalPrice
        get() = invoiceRowTotal(rows)
}

enum class InvoiceStatus {
    DRAFT,
    WAITING_FOR_SENDING,
    SENT,
    CANCELED
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class InvoiceRow(
    val id: InvoiceRowId?,
    val child: ChildWithDateOfBirth,
    val amount: Int,
    val unitPrice: Int,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val product: Product,
    val costCenter: String,
    val subCostCenter: String?,
    val description: String = ""
) : RowWithPrice {
    override val price
        get() = amount * unitPrice
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class InvoiceDetailed(
    val id: InvoiceId,
    val status: InvoiceStatus,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val dueDate: LocalDate,
    val invoiceDate: LocalDate,
    val agreementType: Int?,
    val areaId: AreaId,
    val headOfFamily: PersonDetailed,
    val codebtor: PersonDetailed?,
    val rows: List<InvoiceRowDetailed>,
    val number: Long?,
    val sentBy: EvakaUserId?,
    val sentAt: HelsinkiDateTime?
) {
    val account: Int = 3295
    val totalPrice
        get() = invoiceRowTotal(rows)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class InvoiceRowDetailed(
    val id: InvoiceRowId,
    val child: PersonDetailed,
    val amount: Int,
    val unitPrice: Int,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val product: Product,
    val costCenter: String,
    val subCostCenter: String?,
    val description: String = ""
) : RowWithPrice {
    override val price
        get() = amount * unitPrice
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class InvoiceSummary(
    val id: InvoiceId,
    val status: InvoiceStatus,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val headOfFamily: PersonDetailed,
    val codebtor: PersonDetailed?,
    val rows: List<InvoiceRowSummary>,
    val sentBy: EvakaUserId?,
    val sentAt: HelsinkiDateTime?,
    val createdAt: HelsinkiDateTime? = null
) {
    val account: Int = 3295
    val totalPrice
        get() = invoiceRowTotal(rows)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class InvoiceRowSummary(
    val id: InvoiceRowId,
    val child: PersonBasic,
    val amount: Int,
    val unitPrice: Int
) : RowWithPrice {
    override val price
        get() = amount * unitPrice
}

enum class Product(val code: String) {
    DAYCARE("01001"),
    DAYCARE_DISCOUNT("01001"),
    DAYCARE_INCREASE("01001"),
    PRESCHOOL_WITH_DAYCARE("01002"),
    PRESCHOOL_WITH_DAYCARE_DISCOUNT("01002"),
    PRESCHOOL_WITH_DAYCARE_INCREASE("01002"),
    TEMPORARY_CARE("01005"),
    SCHOOL_SHIFT_CARE("unsupported"),
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

fun getProductFromActivity(placementType: PlacementType): Product {
    return when (placementType) {
        DAYCARE, DAYCARE_PART_TIME, DAYCARE_FIVE_YEAR_OLDS, DAYCARE_PART_TIME_FIVE_YEAR_OLDS -> Product.DAYCARE
        PRESCHOOL_DAYCARE -> Product.PRESCHOOL_WITH_DAYCARE
        PREPARATORY_DAYCARE -> Product.PRESCHOOL_WITH_DAYCARE
        SCHOOL_SHIFT_CARE -> Product.SCHOOL_SHIFT_CARE
        TEMPORARY_DAYCARE, TEMPORARY_DAYCARE_PART_DAY -> Product.TEMPORARY_CARE
        PRESCHOOL, PREPARATORY, CLUB ->
            error("Club and preschool or preparatory without daycare shouldn't be invoiced.")
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

fun invoiceRowTotal(rows: List<RowWithPrice>): Int = rows.fold(0) { sum, row -> sum + row.price }
