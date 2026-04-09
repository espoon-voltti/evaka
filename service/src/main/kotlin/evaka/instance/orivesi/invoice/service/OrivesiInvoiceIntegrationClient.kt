// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.orivesi.invoice.service

import evaka.core.invoicing.domain.InvoiceDetailed
import evaka.core.invoicing.integration.InvoiceIntegrationClient
import evaka.core.invoicing.integration.InvoiceIntegrationClient.SendResult
import evaka.trevaka.time.ClockService
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.YearMonth

private val logger = KotlinLogging.logger {}

class OrivesiInvoiceIntegrationClient(
    private val clockService: ClockService,
    private val s3Sender: S3Sender,
    private val invoiceGenerator: ProEInvoiceGenerator,
) : InvoiceIntegrationClient {
    override fun send(invoices: List<InvoiceDetailed>): SendResult {
        val now = clockService.clock().now()
        val failedList = mutableListOf<InvoiceDetailed>()

        val (zeroSumInvoices, nonZeroSumInvoices) =
            invoices.partition { invoice -> invoice.totalPrice == 0 }
        val previousMonth = YearMonth.of(now.year, now.month).minusMonths(1)
        val generatorResult = invoiceGenerator.generateInvoice(nonZeroSumInvoices, previousMonth)
        val proEinvoices = generatorResult.invoiceString
        val successList = generatorResult.sendResult.succeeded
        val manuallySentList = generatorResult.sendResult.manuallySent

        if (successList.isNotEmpty()) {
            s3Sender.send(proEinvoices, now)
            logger.info {
                "Successfully sent ${successList.size} invoices and created ${manuallySentList.size} manual invoice"
            }
        }

        return SendResult(successList + zeroSumInvoices, failedList, manuallySentList)
    }
}

fun interface StringInvoiceGenerator {
    data class InvoiceGeneratorResult(
        val sendResult: SendResult = SendResult(),
        val invoiceString: String = "",
    )

    fun generateInvoice(invoices: List<InvoiceDetailed>, period: YearMonth): InvoiceGeneratorResult
}
