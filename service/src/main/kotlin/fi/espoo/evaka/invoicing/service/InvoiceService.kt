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
import fi.espoo.evaka.invoicing.domain.Product
import fi.espoo.evaka.invoicing.integration.InvoiceIntegrationClient
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class DaycareCodes(val areaCode: Int?, val costCenter: String?, val subCostCenter: String?)

data class InvoiceCodes(
    val products: List<Product>,
    val agreementTypes: List<Int>,
    val subCostCenters: List<String>,
    val costCenters: List<String>
)

@Component
class InvoiceService(private val integrationClient: InvoiceIntegrationClient) {
    fun sendInvoices(tx: Database.Transaction, user: AuthenticatedUser, invoiceIds: List<UUID>, invoiceDate: LocalDate?, dueDate: LocalDate?) {
        val invoices = getInvoicesByIds(tx.handle, invoiceIds)
        if (invoices.isEmpty()) return

        val notDrafts = invoices.filterNot { it.status == InvoiceStatus.DRAFT }
        if (notDrafts.isNotEmpty()) {
            throw BadRequest("Some invoices were not drafts")
        }

        val (withSSNs, withoutSSNs) = invoices
            .partition { invoice -> invoice.headOfFamily.ssn != null }

        val maxInvoiceNumber = getMaxInvoiceNumber(tx.handle).let { if (it >= 5000000000) it + 1 else 5000000000 }
        val updatedInvoices = withSSNs.mapIndexed { index, invoice ->
            invoice.copy(
                number = maxInvoiceNumber + index,
                invoiceDate = invoiceDate ?: invoice.invoiceDate,
                dueDate = dueDate ?: invoice.dueDate
            )
        }

        val (succeeded, _) = updatedInvoices
            .groupBy { it.agreementType }
            .map { (agreementType, invoices) ->
                val wasSuccessful = integrationClient.sendBatch(invoices, agreementType)
                wasSuccessful to invoices
            }
            .flatMap { (wasSuccessful, invoices) ->
                invoices.map { invoice -> wasSuccessful to invoice }
            }
            .partition { (succeeded, _) -> succeeded }

        if (invoiceDate != null && dueDate != null) {
            updateInvoiceDates(tx.handle, invoices.map { it.id }, invoiceDate, dueDate)
        }
        setDraftsSent(tx.handle, succeeded.map { (_, invoice) -> invoice.id to invoice.number!! }, user.id)
        updateToWaitingForSending(tx.handle, withoutSSNs.map { it.id })
    }

    fun updateInvoice(tx: Database.Transaction, uuid: UUID, invoice: Invoice) {
        val original = getInvoice(tx.handle, uuid)
            ?: throw BadRequest("No original found for invoice with given ID ($uuid)")

        val updated = when (original.status) {
            InvoiceStatus.DRAFT -> original.copy(
                rows = invoice.rows.map { row -> if (row.id == null) row.copy(id = UUID.randomUUID()) else row }
            )
            else -> throw BadRequest("Only draft invoices can be updated")
        }

        upsertInvoices(tx.handle, listOf(updated))
    }

    fun getInvoiceIds(tx: Database.Read, from: LocalDate, to: LocalDate, areas: List<String>): List<UUID> {
        return getInvoiceIdsByDates(tx.handle, from, to, areas)
    }
}

fun Database.Transaction.markManuallySent(user: AuthenticatedUser, invoiceIds: List<UUID>) {
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
        .mapTo(UUID::class.java)
        .list()

    if (updatedIds.toSet() != invoiceIds.toSet()) throw BadRequest("Some invoices have incorrect status")
}

fun Database.Read.getInvoiceCodes(): InvoiceCodes {
    val daycareCodes = getDaycareCodes(handle)

    val specialAreaCode = 255
    val specialSubCostCenter = "06"

    return InvoiceCodes(
        Product.values().toList(),
        daycareCodes.values.mapNotNull { it.areaCode }.plus(specialAreaCode).distinct().sorted().toList(),
        daycareCodes.values.mapNotNull { it.subCostCenter }.plus(specialSubCostCenter).distinct().sorted().toList(),
        daycareCodes.values.mapNotNull { it.costCenter }.distinct().sorted()
    )
}
