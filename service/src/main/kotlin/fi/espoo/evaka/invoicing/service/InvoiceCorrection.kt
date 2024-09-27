// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.invoicing.domain.InvoiceDetailed
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.InvoiceCorrectionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.YearMonth

data class InvoiceCorrection(
    val id: InvoiceCorrectionId,
    val targetMonth: YearMonth,
    val headOfFamilyId: PersonId,
    val childId: ChildId,
    val unitId: DaycareId,
    val product: ProductKey,
    val period: FiniteDateRange,
    val amount: Int,
    val unitPrice: Int,
    val description: String,
    val note: String,
) {
    fun toInsert() =
        InvoiceCorrectionInsert(
            targetMonth = targetMonth,
            headOfFamilyId = headOfFamilyId,
            childId = childId,
            unitId = unitId,
            product = product,
            period = period,
            amount = amount,
            unitPrice = unitPrice,
            description = description,
            note = note,
        )
}

fun updateCorrectionsOfInvoices(tx: Database.Transaction, sentInvoices: List<InvoiceDetailed>) {
    val rows =
        sentInvoices.flatMap { invoice -> invoice.rows.map { row -> invoice.targetMonth() to row } }
    val corrections =
        tx.getInvoiceCorrectionsByIds(rows.mapNotNull { (_, row) -> row.correctionId }.toSet())
            .associateBy { it.id }

    val (updatedCorrections, newCorrections) =
        rows
            .mapNotNull { (targetMonth, row) ->
                if (row.correctionId == null) return@mapNotNull null
                val correction = corrections[row.correctionId] ?: return@mapNotNull null

                val total = correction.amount * correction.unitPrice
                val appliedTotal = row.amount * row.unitPrice
                val outstandingTotal = total - appliedTotal

                if (outstandingTotal == 0) {
                    // Correction is fully applied
                    return@mapNotNull null
                }

                val (amount, unitPrice) =
                    if (outstandingTotal % correction.unitPrice == 0) {
                        outstandingTotal / correction.unitPrice to correction.unitPrice
                    } else {
                        1 to outstandingTotal
                    }

                val assignedInvoiceCorrection =
                    InvoiceCorrectionUpdate(
                        id = correction.id,
                        amount = row.amount,
                        unitPrice = row.unitPrice,
                    )

                val nextMonth = targetMonth.plusMonths(1)
                val newCorrection =
                    correction
                        .copy(targetMonth = nextMonth, amount = amount, unitPrice = unitPrice)
                        .toInsert()

                assignedInvoiceCorrection to newCorrection
            }
            .unzip()

    tx.updateInvoiceCorrections(updatedCorrections)
    tx.insertInvoiceCorrections(newCorrections)
}
