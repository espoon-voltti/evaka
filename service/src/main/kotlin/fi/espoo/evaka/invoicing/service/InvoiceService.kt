// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.invoicing.data.getInvoiceIdsByDates
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
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.YearMonth
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
        sentBy: EvakaUserId,
        now: HelsinkiDateTime,
        invoiceIds: List<InvoiceId>,
        invoiceDate: LocalDate?,
        dueDate: LocalDate?,
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
                    dueDate = dueDate ?: invoice.dueDate,
                )
            }

        val sendResult = integrationClient.send(updatedInvoices)
        tx.setDraftsSent(now, sendResult.succeeded, sentBy)
        tx.setDraftsWaitingForManualSending(sendResult.manuallySent)
        tx.saveCostCenterFields(
            sendResult.succeeded.map { it.id } + sendResult.manuallySent.map { it.id }
        )
        updateCorrectionsOfInvoices(tx, invoices)

        // If any corrections didn't "fit" to these invoices, move them to next month right away
        val invoiceMonth = YearMonth.from(invoices.maxOf { it.periodStart })
        tx.movePastUnappliedInvoiceCorrections(
            invoices.map { it.headOfFamily.id }.toSet(),
            invoiceMonth.plusMonths(1),
        )
    }

    fun getInvoiceIds(
        tx: Database.Read,
        from: LocalDate,
        to: LocalDate,
        areas: List<String>,
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
