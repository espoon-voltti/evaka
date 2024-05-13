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
import fi.espoo.evaka.invoicing.data.updateInvoiceRows
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
    private val featureConfig: FeatureConfig
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

    fun updateDraftInvoiceRows(tx: Database.Transaction, uuid: InvoiceId, invoice: Invoice) {
        if (invoice.rows.any { it.amount <= 0 }) {
            throw BadRequest("Invoice rows amounts must be positive")
        }

        tx.getInvoice(uuid)?.also {
            if (it.status != InvoiceStatus.DRAFT) {
                throw BadRequest("Only draft invoices can be updated")
            }
        } ?: throw BadRequest("No original found for invoice with given ID ($uuid)")

        val rows =
            invoice.rows.map { row ->
                if (row.id == null) row.copy(id = InvoiceRowId(UUID.randomUUID())) else row
            }
        tx.updateInvoiceRows(uuid, rows)
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
            tx.createQuery {
                    sql(
                        """
                        SELECT daycare.id, daycare.name, cost_center
                        FROM daycare
                        WHERE cost_center IS NOT NULL
                        ORDER BY name
                        """
                    )
                }
                .toList<InvoiceDaycare>()
        return InvoiceCodes(productProvider.products, units)
    }
}

fun Database.Transaction.markManuallySent(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    invoiceIds: List<InvoiceId>
) {

    val updatedIds =
        createQuery {
                sql(
                    """
UPDATE invoice SET status = ${bind(InvoiceStatus.SENT)}, sent_at = ${bind(now)}, sent_by = ${bind(user.evakaUserId)}
WHERE id = ANY(${bind(invoiceIds)}) AND status = ${bind(InvoiceStatus.WAITING_FOR_SENDING)}
RETURNING id
"""
                )
            }
            .toList<InvoiceId>()

    if (updatedIds.toSet() != invoiceIds.toSet())
        throw BadRequest("Some invoices have incorrect status")
}

fun Database.Transaction.markInvoicedCorrectionsAsComplete() {
    execute {
        sql(
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
}
