// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import fi.espoo.evaka.invoicing.service.ProductKey
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.InvoiceRowId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.mapper.Nested
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
    @Nested
    val child: ChildId,
    val amount: Int,
    val unitPrice: Int,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val product: ProductKey,
    val unitId: DaycareId,
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
    val product: ProductKey,
    val unitId: DaycareId,
    val costCenter: String,
    val subCostCenter: String?,
    val savedCostCenter: String?,
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

fun getDueDate(periodEnd: LocalDate): LocalDate {
    val lastDayOfMonth = periodEnd.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth())
    return when (lastDayOfMonth.dayOfWeek) {
        DayOfWeek.SUNDAY -> lastDayOfMonth.minusDays(2)
        DayOfWeek.SATURDAY -> lastDayOfMonth.minusDays(1)
        else -> lastDayOfMonth
    }
}

fun invoiceRowTotal(rows: List<RowWithPrice>): Int = rows.sumOf { it.price }
