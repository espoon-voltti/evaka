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
import fi.espoo.evaka.shared.db.withSpringHandle
import fi.espoo.evaka.shared.db.withSpringTx
import fi.espoo.evaka.shared.domain.BadRequest
import org.jdbi.v3.core.Handle
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

data class DaycareCodes(val areaCode: Int?, val costCenter: String?, val subCostCenter: String?)

data class InvoiceCodes(
    val products: List<Product>,
    val agreementTypes: List<Int>,
    val subCostCenters: List<String>,
    val costCenters: List<String>
)

@Component
class InvoiceService(
    private val integrationClient: InvoiceIntegrationClient,
    private val dataSource: DataSource,
    private val txManager: PlatformTransactionManager
) {
    fun sendInvoices(user: AuthenticatedUser, invoiceIds: List<UUID>, invoiceDate: LocalDate?, dueDate: LocalDate?) {
        val invoices = withSpringTx(txManager) {
            withSpringHandle(dataSource) { h ->
                getInvoicesByIds(h, invoiceIds)
            }
        }
        if (invoices.isEmpty()) return

        val notDrafts = invoices.filterNot { it.status == InvoiceStatus.DRAFT }
        if (notDrafts.isNotEmpty()) {
            throw BadRequest("Some invoices were not drafts")
        }

        val (withSSNs, withoutSSNs) = invoices
            .partition { invoice -> invoice.headOfFamily.ssn != null }

        val maxInvoiceNumber = withSpringTx(txManager) {
            withSpringHandle(dataSource) { h ->
                getMaxInvoiceNumber(h).let { if (it >= 5000000000) it + 1 else 5000000000 }
            }
        }
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

        withSpringTx(txManager) {
            withSpringHandle(dataSource) { h ->
                if (invoiceDate != null && dueDate != null) {
                    updateInvoiceDates(h, invoices.map { it.id }, invoiceDate, dueDate)
                }
                setDraftsSent(h, succeeded.map { (_, invoice) -> invoice.id to invoice.number!! }, user.id)
                updateToWaitingForSending(h, withoutSSNs.map { it.id })
            }
        }
    }

    fun updateInvoice(uuid: UUID, invoice: Invoice) {
        withSpringTx(txManager) {
            withSpringHandle(dataSource) { h ->
                val original = getInvoice(h, uuid)
                    ?: throw BadRequest("No original found for invoice with given ID ($uuid)")

                val updated = when (original.status) {
                    InvoiceStatus.DRAFT -> original.copy(
                        rows = invoice.rows.map { row -> if (row.id == null) row.copy(id = UUID.randomUUID()) else row }
                    )
                    else -> throw BadRequest("Only draft invoices can be updated")
                }

                upsertInvoices(h, listOf(updated))
            }
        }
    }

    fun getInvoiceIds(from: LocalDate, to: LocalDate, areas: List<String>): List<UUID> {
        return withSpringHandle(dataSource) { h ->
            getInvoiceIdsByDates(h, from, to, areas)
        }
    }
}

fun markManuallySent(user: AuthenticatedUser, invoiceIds: List<UUID>): (Handle) -> Unit = { h: Handle ->
    val sql =
        """
        UPDATE invoice SET status = :status_sent, sent_at = :sent_at, sent_by = :sent_by
        WHERE id IN (<ids>) AND status = :status_waiting
        RETURNING id
        """.trimIndent()

    val updatedIds = h.createQuery(sql)
        .bind("status_sent", InvoiceStatus.SENT.toString())
        .bind("status_waiting", InvoiceStatus.WAITING_FOR_SENDING.toString())
        .bind("sent_at", Instant.now())
        .bind("sent_by", user.id)
        .bindList("ids", invoiceIds)
        .mapTo(UUID::class.java)
        .list()

    if (updatedIds.toSet() != invoiceIds.toSet()) throw BadRequest("Some invoices have incorrect status")
}

fun getInvoiceCodes(h: Handle): InvoiceCodes {
    val daycareCodes = getDaycareCodes(h)

    val specialAreaCode = 255
    val specialSubCostCenter = "06"

    return InvoiceCodes(
        Product.values().toList(),
        daycareCodes.values.mapNotNull { it.areaCode }.plus(specialAreaCode).distinct().sorted().toList(),
        daycareCodes.values.mapNotNull { it.subCostCenter }.plus(specialSubCostCenter).distinct().sorted().toList(),
        daycareCodes.values.mapNotNull { it.costCenter }.distinct().sorted()
    )
}
