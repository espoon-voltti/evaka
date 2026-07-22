// SPDX-FileCopyrightText: 2023-2025 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu.payment.service

import evaka.core.invoicing.domain.Payment
import evaka.core.invoicing.domain.PaymentIntegrationClient
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.sftp.SftpClient
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}

private val fileNameFormat = DateTimeFormatter.ofPattern("'proe-'yyyyMMdd-hhmmss'.txt'")

class OuluPaymentIntegrationClient(
    private val paymentGenerator: ProEPaymentGenerator,
    private val sftpClient: SftpClient,
    private val clock: EvakaClock,
) : PaymentIntegrationClient {
    override fun send(
        payments: List<Payment>,
        tx: Database.Read,
    ): PaymentIntegrationClient.SendResult {
        var failedList: MutableList<Payment> = mutableListOf()

        logger.info { "OuluPaymentIntegrationClient.send() called with ${payments.size} payments" }
        val generatorResult = paymentGenerator.generatePayments(payments)
        var successList = generatorResult.sendResult.succeeded
        failedList.addAll(generatorResult.sendResult.failed)

        if (!successList.isEmpty()) {
            try {
                sftpClient.put(
                    generatorResult.paymentString.byteInputStream(Charsets.ISO_8859_1),
                    clock.now().toLocalDateTime().format(fileNameFormat),
                )
                logger.info { "Successfully sent ${successList.size} payments" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to send ${successList.size} payments" }
                failedList.addAll(successList)
                successList = listOf()
            }
        }

        logger.info { "OuluPaymentIntegrationClient.send() returning" }
        return PaymentIntegrationClient.SendResult(successList, failedList)
    }
}
