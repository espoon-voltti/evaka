// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.invoicing.data.getInvoice
import fi.espoo.evaka.invoicing.data.getInvoiceIdsByDates
import fi.espoo.evaka.invoicing.data.getInvoicesByIds
import fi.espoo.evaka.invoicing.data.getMaxInvoiceNumber
import fi.espoo.evaka.invoicing.data.lockInvoices
import fi.espoo.evaka.invoicing.data.saveCostCenterFields
import fi.espoo.evaka.invoicing.data.setDraftsSent
import fi.espoo.evaka.invoicing.data.setDraftsWaitingForManualSending
import fi.espoo.evaka.invoicing.data.upsertInvoices
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.integration.InvoiceIntegrationClient
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.InvoiceRowId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Component

data class InvoiceDaycare(val id: DaycareId, val name: String, val costCenter: String?)

data class InvoiceCodes(val products: List<ProductWithName>, val units: List<InvoiceDaycare>)

@Component
class InvoiceService(
    private val integrationClient: InvoiceIntegrationClient,
    private val productProvider: InvoiceProductProvider,
    private val featureConfig: FeatureConfig,
) {
    fun sendInvoices(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        clock: EvakaClock,
        invoiceIds: List<InvoiceId>,
        invoiceDate: LocalDate?,
        dueDate: LocalDate?
    ) {
        val seriesStart = featureConfig.invoiceNumberSeriesStart
        tx.lockInvoices(invoiceIds)

        val invoices = tx.getInvoicesByIds(invoiceIds)
        if (invoices.isEmpty()) return

        val notDrafts = invoices.filterNot { it.status == InvoiceStatus.DRAFT }
        if (notDrafts.isNotEmpty()) {
            throw BadRequest("Some invoices were not drafts")
        }

        val maxInvoiceNumber =
            tx.getMaxInvoiceNumber().let { if (it >= seriesStart) it + 1 else seriesStart }
        val updatedInvoices =
            invoices.mapIndexed { index, invoice ->
                invoice.copy(
                    number = maxInvoiceNumber + index,
                    invoiceDate = invoiceDate ?: invoice.invoiceDate,
                    dueDate = dueDate ?: invoice.dueDate
                )
            }

        val sendResult = integrationClient.send(updatedInvoices)
        tx.setDraftsSent(clock, sendResult.succeeded, user.evakaUserId)
        tx.setDraftsWaitingForManualSending(sendResult.manuallySent)
        tx.saveCostCenterFields(
            sendResult.succeeded.map { it.id } + sendResult.manuallySent.map { it.id }
        )
        tx.markInvoicedCorrectionsAsComplete()
    }

    fun updateInvoice(tx: Database.Transaction, uuid: InvoiceId, invoice: Invoice) {
        val original =
            tx.getInvoice(uuid)
                ?: throw BadRequest("No original found for invoice with given ID ($uuid)")

        if (invoice.rows.any { it.amount <= 0 }) {
            throw BadRequest("Invoice rows amounts must be positive")
        }

        val updated =
            when (original.status) {
                InvoiceStatus.DRAFT ->
                    original.copy(
                        rows =
                            invoice.rows.map { row ->
                                if (row.id == null) row.copy(id = InvoiceRowId(UUID.randomUUID()))
                                else row
                            }
                    )
                else -> throw BadRequest("Only draft invoices can be updated")
            }

        tx.upsertInvoices(listOf(updated))
    }

    fun getInvoiceIds(
        tx: Database.Read,
        from: LocalDate,
        to: LocalDate,
        areas: List<String>
    ): List<InvoiceId> {
        return tx.getInvoiceIdsByDates(FiniteDateRange(from, to), areas)
    }

    fun getInvoiceCodes(tx: Database.Read): InvoiceCodes {
        val units =
            tx.createQuery(
                    """
        SELECT daycare.id, daycare.name, cost_center
        FROM daycare
        ORDER BY name
            """.trimIndent(
                    )
                )
                .mapTo<InvoiceDaycare>()
                .list()
        return InvoiceCodes(productProvider.products, units)
    }
}

fun Database.Transaction.markManuallySent(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    invoiceIds: List<InvoiceId>
) {
    val sql =
        """
        UPDATE invoice SET status = :status_sent::invoice_status, sent_at = :sent_at, sent_by = :sent_by
        WHERE id = ANY(:ids) AND status = :status_waiting::invoice_status
        RETURNING id
        """.trimIndent(
        )

    val updatedIds =
        createQuery(sql)
            .bind("status_sent", InvoiceStatus.SENT.toString())
            .bind("status_waiting", InvoiceStatus.WAITING_FOR_SENDING.toString())
            .bind("sent_at", now)
            .bind("sent_by", user.evakaUserId)
            .bind("ids", invoiceIds)
            .mapTo<InvoiceId>()
            .list()

    if (updatedIds.toSet() != invoiceIds.toSet())
        throw BadRequest("Some invoices have incorrect status")
}

fun Database.Transaction.markInvoicedCorrectionsAsComplete() {
    execute(
        """
WITH applied_corrections AS (
    SELECT c.id
    FROM invoice_correction c
    LEFT JOIN invoice_row r ON c.id = r.correction_id AND NOT c.applied_completely
    LEFT JOIN invoice i ON r.invoice_id = i.id AND i.status != 'DRAFT'
    GROUP BY c.id
    HAVING c.amount * c.unit_price = coalesce(sum(r.amount * r.unit_price) FILTER (WHERE i.id IS NOT NULL), 0)
)
UPDATE invoice_correction SET applied_completely = true
WHERE id IN (SELECT id FROM applied_corrections)
"""
    )
}
