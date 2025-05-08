// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.invoicing.data.getInvoiceIdsByDatesAndStatus
import fi.espoo.evaka.invoicing.data.getInvoicesByIds
import fi.espoo.evaka.invoicing.data.getMaxInvoiceNumber
import fi.espoo.evaka.invoicing.data.lockInvoices
import fi.espoo.evaka.invoicing.data.saveCostCenterFields
import fi.espoo.evaka.invoicing.data.setDraftsSent
import fi.espoo.evaka.invoicing.data.setDraftsWaitingForManualSending
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.integration.InvoiceIntegrationClient
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import org.springframework.stereotype.Component

data class InvoiceDaycare(val id: DaycareId, val name: String, val costCenter: String?)

data class InvoiceCodes(val products: List<ProductWithName>, val units: List<InvoiceDaycare>)

interface InvoiceNumberProvider {
    /**
     * The next invoice number to use
     *
     * The number of subsequent invoices is the previous number plus one.
     */
    fun getNextInvoiceNumber(tx: Database.Read): Long
}

class DefaultInvoiceNumberProvider(private val seriesStart: Long) : InvoiceNumberProvider {
    override fun getNextInvoiceNumber(tx: Database.Read): Long =
        tx.getMaxInvoiceNumber().let { if (it >= seriesStart) it + 1 else seriesStart }
}

@Component
class InvoiceService(
    private val integrationClient: InvoiceIntegrationClient,
    private val productProvider: InvoiceProductProvider,
    private val numberProvider: InvoiceNumberProvider,
) {
    fun sendInvoices(
        tx: Database.Transaction,
        sentBy: EvakaUserId,
        now: HelsinkiDateTime,
        invoiceIds: List<InvoiceId>,
        invoiceDate: LocalDate?,
        dueDate: LocalDate?,
    ) {
        tx.lockInvoices(invoiceIds)

        val invoices = tx.getInvoicesByIds(invoiceIds)
        if (invoices.isEmpty()) return

        val notDrafts = invoices.filterNot { it.status == InvoiceStatus.DRAFT }
        if (notDrafts.isNotEmpty()) {
            throw BadRequest("Some invoices were not drafts")
        }

        val nextInvoiceNumber = numberProvider.getNextInvoiceNumber(tx)
        val updatedInvoices =
            invoices.mapIndexed { index, invoice ->
                invoice.copy(
                    number = nextInvoiceNumber + index,
                    invoiceDate = invoiceDate ?: invoice.invoiceDate,
                    dueDate = dueDate ?: invoice.dueDate,
                )
            }

        val sendResult = integrationClient.send(updatedInvoices)
        tx.setDraftsSent(now, sendResult.succeeded, sentBy)
        tx.setDraftsWaitingForManualSending(sendResult.manuallySent)
        tx.saveCostCenterFields(
            sendResult.succeeded.map { it.id } + sendResult.manuallySent.map { it.id }
        )
        updateCorrectionsOfInvoices(tx, now, invoices)
    }

    fun resendInvoices(
        tx: Database.Transaction,
        sentBy: EvakaUserId,
        now: HelsinkiDateTime,
        invoiceIds: List<InvoiceId>,
    ) {
        tx.lockInvoices(invoiceIds)

        val invoices = tx.getInvoicesByIds(invoiceIds)
        if (invoices.isEmpty()) return

        val notSent = invoices.filterNot { it.status == InvoiceStatus.SENT }
        if (notSent.isNotEmpty()) {
            throw BadRequest("Some invoices were not sent before")
        }

        val sendResult = integrationClient.send(invoices)
        tx.setDraftsSent(now, sendResult.succeeded, sentBy)
    }

    fun getInvoiceIds(
        tx: Database.Read,
        from: LocalDate,
        to: LocalDate,
        areas: List<String>,
        status: InvoiceStatus = InvoiceStatus.DRAFT,
    ): List<InvoiceId> {
        return tx.getInvoiceIdsByDatesAndStatus(FiniteDateRange(from, to), areas, status)
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
    invoiceIds: List<InvoiceId>,
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
