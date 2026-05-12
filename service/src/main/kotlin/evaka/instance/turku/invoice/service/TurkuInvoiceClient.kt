// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku.invoice.service

import evaka.core.invoicing.domain.InvoiceDetailed
import evaka.core.invoicing.integration.InvoiceIntegrationClient
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.sftp.SftpClient
import io.github.oshai.kotlinlogging.KotlinLogging
import java.text.SimpleDateFormat
import java.util.Date

private val logger = KotlinLogging.logger {}

class TurkuInvoiceClient(
    private val sftpClient: SftpClient,
    private val remotePath: String,
    private val invoiceGenerator: SapInvoiceGenerator,
) : InvoiceIntegrationClient {
    override fun send(
        now: HelsinkiDateTime,
        invoices: List<InvoiceDetailed>,
    ): InvoiceIntegrationClient.SendResult {
        val failedList = mutableListOf<InvoiceDetailed>()

        val generatorResult = invoiceGenerator.generateInvoice(invoices)
        val invoiceString = generatorResult.invoiceString
        var successList = generatorResult.sendResult.succeeded
        var manuallySentList = generatorResult.sendResult.manuallySent

        if (successList.isNotEmpty()) {
            try {
                val filename = SimpleDateFormat("'LAVAK_1002'yyMMdd-hhmmss'.xml'").format(Date())
                sftpClient.put(
                    invoiceString.byteInputStream(Charsets.UTF_8),
                    "$remotePath/$filename",
                )
                logger.info {
                    "Successfully sent ${successList.size} invoices and created ${manuallySentList.size} manual invoice"
                }
            } catch (e: Exception) {
                failedList.addAll(successList)
                failedList.addAll(manuallySentList)
                successList = listOf()
                manuallySentList = listOf()
                logger.error(e) { "Failed to send ${failedList.size} invoices" }
            }
        }

        return InvoiceIntegrationClient.SendResult(successList, failedList, manuallySentList)
    }
}

interface StringInvoiceGenerator {
    data class InvoiceGeneratorResult(
        val sendResult: InvoiceIntegrationClient.SendResult = InvoiceIntegrationClient.SendResult(),
        val invoiceString: String = "",
    )

    fun generateInvoice(invoices: List<InvoiceDetailed>): InvoiceGeneratorResult
}
