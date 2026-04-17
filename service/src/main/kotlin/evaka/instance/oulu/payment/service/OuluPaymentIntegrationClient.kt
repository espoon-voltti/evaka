// SPDX-FileCopyrightText: 2023-2025 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu.payment.service

import com.jcraft.jsch.SftpException
import evaka.core.invoicing.domain.Payment
import evaka.core.invoicing.domain.PaymentIntegrationClient
import evaka.core.shared.db.Database
import evaka.instance.oulu.invoice.service.SftpSender
import io.github.oshai.kotlinlogging.KotlinLogging
import java.text.SimpleDateFormat
import java.util.Date

private val logger = KotlinLogging.logger {}

class OuluPaymentIntegrationClient(
    private val paymentGenerator: ProEPaymentGenerator,
    private val sftpSender: SftpSender,
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
                sftpSender.send(
                    generatorResult.paymentString,
                    SimpleDateFormat("'proe-'yyyyMMdd-hhmmss'.txt'").format(Date()),
                )
                logger.info { "Successfully sent ${successList.size} payments" }
            } catch (e: SftpException) {
                logger.error { "Failed to send ${successList.size} payments" }
                failedList.addAll(successList)
                successList = listOf()
            }
        }

        logger.info { "OuluPaymentIntegrationClient.send() returning" }
        return PaymentIntegrationClient.SendResult(successList, failedList)
    }
}
