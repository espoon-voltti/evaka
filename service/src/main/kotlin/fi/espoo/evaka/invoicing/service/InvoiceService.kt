// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.invoicing.data.getInvoice
import fi.espoo.evaka.invoicing.data.getInvoiceIdsByDates
import fi.espoo.evaka.invoicing.data.getInvoicesByIds
import fi.espoo.evaka.invoicing.data.getMaxInvoiceNumber
import fi.espoo.evaka.invoicing.data.setDraftsSent
import fi.espoo.evaka.invoicing.data.updateInvoiceDates
import fi.espoo.evaka.invoicing.data.updateToWaitingForSending
import fi.espoo.evaka.invoicing.data.upsertInvoices
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.integration.InvoiceIntegrationClient
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.InvoiceRowId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class DaycareCodes(val areaId: AreaId, val costCenter: String?, val subCostCenter: String?)

data class InvoiceCodes(
    val products: List<ProductWithName>,
    val subCostCenters: List<String>,
    val costCenters: List<String>
)

@Component
class InvoiceService(
    private val integrationClient: InvoiceIntegrationClient,
    private val productProvider: InvoiceProductProvider
) {
    fun sendInvoices(tx: Database.Transaction, user: AuthenticatedUser, invoiceIds: List<InvoiceId>, invoiceDate: LocalDate?, dueDate: LocalDate?) {
        val invoices = tx.getInvoicesByIds(invoiceIds)
        if (invoices.isEmpty()) return

        val notDrafts = invoices.filterNot { it.status == InvoiceStatus.DRAFT }
        if (notDrafts.isNotEmpty()) {
            throw BadRequest("Some invoices were not drafts")
        }

        val (withSSNs, withoutSSNs) = invoices
            .partition { invoice -> invoice.headOfFamily.ssn != null }

        val maxInvoiceNumber = tx.getMaxInvoiceNumber().let { if (it >= 5000000000) it + 1 else 5000000000 }
        val updatedInvoices = withSSNs.mapIndexed { index, invoice ->
            invoice.copy(
                number = maxInvoiceNumber + index,
                invoiceDate = invoiceDate ?: invoice.invoiceDate,
                dueDate = dueDate ?: invoice.dueDate
            )
        }

        val (succeeded, _) = integrationClient.send(updatedInvoices)

        if (invoiceDate != null && dueDate != null) {
            tx.updateInvoiceDates(succeeded.map { it.id }, invoiceDate, dueDate)
        }
        tx.setDraftsSent(succeeded.map { it.id to it.number!! }, user.evakaUserId)
        tx.updateToWaitingForSending(withoutSSNs.map { it.id })
    }

    fun updateInvoice(tx: Database.Transaction, uuid: InvoiceId, invoice: Invoice) {
        val original = tx.getInvoice(uuid)
            ?: throw BadRequest("No original found for invoice with given ID ($uuid)")

        val updated = when (original.status) {
            InvoiceStatus.DRAFT -> original.copy(
                rows = invoice.rows.map { row -> if (row.id == null) row.copy(id = InvoiceRowId(UUID.randomUUID())) else row }
            )
            else -> throw BadRequest("Only draft invoices can be updated")
        }

        tx.upsertInvoices(listOf(updated))
    }

    fun getInvoiceIds(tx: Database.Read, from: LocalDate, to: LocalDate, areas: List<String>): List<InvoiceId> {
        return tx.getInvoiceIdsByDates(FiniteDateRange(from, to), areas)
    }

    fun getInvoiceCodes(tx: Database.Read): InvoiceCodes {
        val daycareCodes = tx.getDaycareCodes()

        val specialSubCostCenter = "06"

        return InvoiceCodes(
            productProvider.products,
            daycareCodes.values.mapNotNull { it.subCostCenter }.plus(specialSubCostCenter).distinct().sorted().toList(),
            daycareCodes.values.mapNotNull { it.costCenter }.distinct().sorted()
        )
    }
}

fun Database.Transaction.markManuallySent(user: AuthenticatedUser, invoiceIds: List<InvoiceId>) {
    val sql =
        """
        UPDATE invoice SET status = :status_sent::invoice_status, sent_at = :sent_at, sent_by = :sent_by
        WHERE id = ANY(:ids) AND status = :status_waiting::invoice_status
        RETURNING id
        """.trimIndent()

    val updatedIds = createQuery(sql)
        .bind("status_sent", InvoiceStatus.SENT.toString())
        .bind("status_waiting", InvoiceStatus.WAITING_FOR_SENDING.toString())
        .bind("sent_at", Instant.now())
        .bind("sent_by", user.id)
        .bind("ids", invoiceIds.toTypedArray())
        .mapTo<InvoiceId>()
        .list()

    if (updatedIds.toSet() != invoiceIds.toSet()) throw BadRequest("Some invoices have incorrect status")
}
