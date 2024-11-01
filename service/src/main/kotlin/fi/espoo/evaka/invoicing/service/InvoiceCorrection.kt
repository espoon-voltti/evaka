// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.invoicing.domain.DraftInvoiceRow
import fi.espoo.evaka.invoicing.domain.InvoiceDetailed
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.InvoiceCorrectionId
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.YearMonth
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull

data class InvoiceWithCorrection(@PropagateNull val id: InvoiceId, val status: InvoiceStatus)

data class InvoiceCorrection(
    val id: InvoiceCorrectionId,
    val targetMonth: YearMonth?,
    val headOfFamilyId: PersonId,
    val childId: ChildId,
    val unitId: DaycareId,
    val product: ProductKey,
    val period: FiniteDateRange,
    val amount: Int,
    val unitPrice: Int,
    val description: String,
    val note: String,
    @Nested("invoice") val invoice: InvoiceWithCorrection?, // should later be a list?
) {
    fun toDraftInvoiceRow() =
        DraftInvoiceRow(
            childId = childId,
            amount = amount,
            unitPrice = unitPrice,
            periodStart = period.start,
            periodEnd = period.end,
            product = product,
            unitId = unitId,
            description = description,
            correctionId = id,
        )

    fun toInsert() =
        InvoiceCorrectionInsert(
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
    val correctionIds =
        sentInvoices.flatMap { invoice -> invoice.rows.mapNotNull { it.correctionId } }.toSet()
    val corrections = tx.getInvoiceCorrectionsByIds(correctionIds)

    val (updatedCorrections, newCorrections) =
        generateInvoiceCorrectionChanges(corrections, sentInvoices)

    tx.updateInvoiceCorrections(updatedCorrections)
    tx.insertInvoiceCorrections(newCorrections)
}

fun generateInvoiceCorrectionChanges(
    corrections: List<InvoiceCorrection>,
    sentInvoices: List<InvoiceDetailed>,
): Pair<List<InvoiceCorrectionUpdate>, List<InvoiceCorrectionInsert>> {
    val correctionsById = corrections.associateBy { it.id }
    val rows =
        sentInvoices.flatMap { invoice -> invoice.rows.map { row -> invoice.targetMonth() to row } }

    return rows
        .mapNotNull { (targetMonth, row) ->
            if (row.correctionId == null) return@mapNotNull null
            val correction = correctionsById[row.correctionId] ?: return@mapNotNull null

            val total = correction.amount * correction.unitPrice
            val appliedTotal = row.amount * row.unitPrice
            val outstandingTotal = total - appliedTotal

            val assignedInvoiceCorrection =
                InvoiceCorrectionUpdate(
                    id = correction.id,
                    targetMonth = targetMonth,
                    amount = row.amount,
                    unitPrice = row.unitPrice,
                )

            if (outstandingTotal == 0) {
                // Correction is fully applied
                return@mapNotNull assignedInvoiceCorrection to null
            }
            val (remainingAmount, remainingUnitPrice) =
                if (
                    row.unitPrice == correction.unitPrice &&
                        outstandingTotal % correction.unitPrice == 0
                ) {
                    (correction.amount - row.amount) to correction.unitPrice
                } else {
                    // Keep amount intact, apply partial unit price
                    correction.amount to (outstandingTotal / correction.amount)
                }
            val remainingCorrection =
                correction.copy(amount = remainingAmount, unitPrice = remainingUnitPrice).toInsert()

            assignedInvoiceCorrection to remainingCorrection
        }
        .unzip()
        .let { (updates, inserts) -> updates to inserts.filterNotNull() }
}
