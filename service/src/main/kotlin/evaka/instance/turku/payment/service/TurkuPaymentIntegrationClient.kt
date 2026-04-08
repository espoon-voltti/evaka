// SPDX-FileCopyrightText: 2023-2025 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku.payment.service

import evaka.core.invoicing.domain.Payment
import evaka.core.invoicing.domain.PaymentIntegrationClient
import evaka.core.shared.db.Database
import evaka.instance.turku.invoice.service.SftpSender
import io.github.oshai.kotlinlogging.KotlinLogging
import java.text.SimpleDateFormat
import java.util.Date

private val logger = KotlinLogging.logger {}

class TurkuPaymentIntegrationClient(
    private val paymentGenerator: SapPaymentGenerator,
    private val sftpSender: SftpSender,
) : PaymentIntegrationClient {
    override fun send(
        payments: List<Payment>,
        tx: Database.Read,
    ): PaymentIntegrationClient.SendResult {
        var failedList: MutableList<Payment> = mutableListOf()

        logger.info { "TurkuPaymentIntegrationClient.send() called with ${payments.size} payments" }
        val preschoolFetcher = PreschoolValuesFetcher(tx)
        val generatorResult = paymentGenerator.generatePayments(payments, preschoolFetcher)
        var successList = generatorResult.sendResult.succeeded
        failedList.addAll(generatorResult.sendResult.failed)

        if (successList.isNotEmpty()) {
            val contents =
                generatorResult.paymentStrings
                    .mapIndexed { index, content ->
                        val filename =
                            SimpleDateFormat("'OLVAK_1002_0000001_'yyMMdd-hhmmss").format(Date()) +
                                '-' +
                                (index + 1).toString() +
                                ".xml"
                        filename to content
                    }
                    .toMap()

            try {
                sftpSender.sendAll(contents)
                logger.info { "Successfully sent ${successList.size} payments" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to send ${successList.size} payments" }
                // TODO: only add payments whose sending failed to failedList
                failedList.addAll(successList)
                successList = listOf()
            }
        }

        logger.info { "TurkuPaymentIntegrationClient.send() returning" }
        return PaymentIntegrationClient.SendResult(successList, failedList)
    }
}
