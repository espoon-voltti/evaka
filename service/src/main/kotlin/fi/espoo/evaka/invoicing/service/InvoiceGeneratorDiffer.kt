// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class InvoiceGeneratorDiffer(private val draftInvoiceGenerator: DefaultDraftInvoiceGenerator, private val newDraftInvoiceGenerator: NewDraftInvoiceGenerator) {
    fun createInvoiceGeneratorDiff(tx: Database.Transaction, range: DateRange = DateRange(LocalDate.now(), LocalDate.now())): InvoiceGeneratorDiff {
        val invoiceData = InvoiceGenerator(draftInvoiceGenerator).calculateInvoiceData(tx, range)
        val currentInvoices = draftInvoiceGenerator.generateDraftInvoices(
            invoiceData.decisions,
            invoiceData.placements,
            invoiceData.period,
            invoiceData.daycareCodes,
            invoiceData.operationalDays,
            invoiceData.feeThresholds,
            invoiceData.absences,
            invoiceData.freeChildren,
            invoiceData.codebtors
        )
        val newInvoices = newDraftInvoiceGenerator.generateDraftInvoices(
            invoiceData.decisions,
            invoiceData.placements,
            invoiceData.period,
            invoiceData.daycareCodes,
            invoiceData.operationalDays,
            invoiceData.feeThresholds,
            invoiceData.absences,
            invoiceData.freeChildren,
            invoiceData.codebtors
        )

        val inBoth = currentInvoices.filter { currentInvoice -> newInvoices.any { invoiceId(it) == invoiceId(currentInvoice) } }
        val onlyInCurrentInvoices = currentInvoices.filter { currentInvoice -> newInvoices.none { invoiceId(it) == invoiceId(currentInvoice) } }
        val onlyInNewInvoices = newInvoices.filter { newInvoice -> currentInvoices.none { invoiceId(it) == invoiceId(newInvoice) } }
        val differentInvoices = inBoth.fold(emptyList()) { diffs: List<InvoiceDiff>, currentInvoice: Invoice ->
            val newInvoice = newInvoices.find { invoiceId(it) == invoiceId(currentInvoice) }!!
            if (invoicesDiffer(currentInvoice, newInvoice))
                diffs + InvoiceDiff(
                    invoiceId(currentInvoice),
                    currentInvoice,
                    newInvoice
                )
            else
                diffs
        }

        return InvoiceGeneratorDiff(
            usedRange = range,
            onlyInCurrentInvoices = onlyInCurrentInvoices,
            onlyInNewInvoices = onlyInNewInvoices,
            differentInvoices = differentInvoices
        )
    }

    fun invoiceId(invoice: Invoice): String {
        return "${invoice.headOfFamily}-${invoice.invoiceDate}"
    }

    fun invoicesDiffer(l: Invoice, r: Invoice): Boolean {
        return l.totalPrice != r.totalPrice ||
            l.rows.size != r.rows.size
    }
}

data class InvoiceGeneratorDiff(
    val usedRange: DateRange,
    val onlyInCurrentInvoices: List<Invoice>,
    val onlyInNewInvoices: List<Invoice>,
    val differentInvoices: List<InvoiceDiff>
)

data class InvoiceDiff(
    val invoiceId: String,
    val currentInvoice: Invoice,
    val newInvoice: Invoice
)
