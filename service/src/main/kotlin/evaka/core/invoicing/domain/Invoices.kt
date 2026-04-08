// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import evaka.core.ConstList
import evaka.core.attachment.Attachment
import evaka.core.daycare.CareType
import evaka.core.daycare.domain.ProviderType
import evaka.core.invoicing.service.ProductKey
import evaka.core.shared.AreaId
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.EvakaUserId
import evaka.core.shared.FeeDecisionId
import evaka.core.shared.InvoiceCorrectionId
import evaka.core.shared.InvoiceId
import evaka.core.shared.InvoiceRowId
import evaka.core.shared.PersonId
import evaka.core.shared.db.DatabaseEnum
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.user.EvakaUser
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.json.Json

interface RowWithPrice {
    val price: Int
}

enum class InvoiceStatus : DatabaseEnum {
    DRAFT,
    WAITING_FOR_SENDING,
    SENT,
    REPLACEMENT_DRAFT,
    REPLACED;

    override val sqlType: String = "invoice_status"
}

data class RelatedFeeDecision(val id: FeeDecisionId, val decisionNumber: Long)

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
    @Nested("head") val headOfFamily: PersonDetailed,
    @Nested("codebtor") val codebtor: PersonDetailed?,
    @Json val rows: List<InvoiceRowDetailed>,
    val number: Long?,
    @Nested("sent_by") val sentBy: EvakaUser?,
    val sentAt: HelsinkiDateTime?,
    @Json val relatedFeeDecisions: List<RelatedFeeDecision>,
    val revisionNumber: Int,
    val replacedInvoiceId: InvoiceId?,
    val replacementReason: InvoiceReplacementReason?,
    val replacementNotes: String?,
    @Json val attachments: List<Attachment>,
) {
    val account: Int = 3295
    val totalPrice
        get() = invoiceRowTotal(rows)

    fun targetMonth(): YearMonth = YearMonth.of(periodStart.year, periodStart.month)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class InvoiceRowDetailed(
    val id: InvoiceRowId,
    @Json val child: PersonDetailed,
    val amount: Int,
    val unitPrice: Int,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val product: ProductKey,
    val unitId: DaycareId,
    val unitName: String,
    val unitProviderType: ProviderType,
    val daycareType: Set<CareType>,
    val costCenter: String,
    val subCostCenter: String?,
    val savedCostCenter: String?,
    val description: String,
    val correctionId: InvoiceCorrectionId?,
    val note: String?,
) : RowWithPrice {
    override val price
        get() = amount * unitPrice
}

data class InvoiceSummary(
    val id: InvoiceId,
    val status: InvoiceStatus,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    @Json val headOfFamily: PersonDetailed,
    @Json val children: List<PersonBasic>,
    val totalPrice: Int,
    val sentBy: EvakaUserId?,
    val sentAt: HelsinkiDateTime?,
    val createdAt: HelsinkiDateTime?,
    val revisionNumber: Int,
)

fun getDueDate(periodEnd: LocalDate): LocalDate {
    val lastDayOfMonth = periodEnd.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth())
    return when (lastDayOfMonth.dayOfWeek) {
        DayOfWeek.SUNDAY -> lastDayOfMonth.minusDays(2)
        DayOfWeek.SATURDAY -> lastDayOfMonth.minusDays(1)
        else -> lastDayOfMonth
    }
}

fun invoiceRowTotal(rows: List<RowWithPrice>): Int = rows.sumOf { it.price }

data class DraftInvoice(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val dueDate: LocalDate = getDueDate(periodEnd),
    val invoiceDate: LocalDate = dueDate.minusWeeks(2),
    val areaId: AreaId,
    val headOfFamily: PersonId,
    val codebtor: PersonId?,
    val rows: List<DraftInvoiceRow>,
    val revisionNumber: Int = 0,
    val replacedInvoiceId: InvoiceId? = null,
) {
    val totalPrice
        get() = invoiceRowTotal(rows)
}

data class DraftInvoiceRow(
    val childId: ChildId,
    val amount: Int,
    val unitPrice: Int,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val product: ProductKey,
    val unitId: DaycareId,
    val description: String = "",
    val correctionId: InvoiceCorrectionId? = null,
) : RowWithPrice {
    override val price: Int
        get() = amount * unitPrice
}

@ConstList("invoiceReplacementReasons")
enum class InvoiceReplacementReason : DatabaseEnum {
    SERVICE_NEED,
    ABSENCE,
    INCOME,
    FAMILY_SIZE,
    RELIEF_RETROACTIVE,
    OTHER;

    override val sqlType = "invoice_replacement_reason"
}
