// SPDX-FileCopyrightText: 2023-2025 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu.payment.service

import com.jcraft.jsch.SftpException
import evaka.core.daycare.CareType
import evaka.core.invoicing.domain.Payment
import evaka.core.invoicing.domain.PaymentIntegrationClient
import evaka.core.invoicing.domain.PaymentUnit
import evaka.core.shared.DaycareId
import evaka.core.shared.db.Database
import evaka.instance.oulu.invoice.service.SftpSender
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension

@ExtendWith(OutputCaptureExtension::class)
internal class OuluPaymentIntegrationClientTest {
    val paymentGenerator = mock<ProEPaymentGenerator>()
    val sftpSender = mock<SftpSender>()
    val paymentClient = OuluPaymentIntegrationClient(paymentGenerator, sftpSender)
    val tx = mock<Database.Transaction>()
    val fileName: String = SimpleDateFormat("'proe-'yyyyMMdd-hhmmss'.txt'").format(Date())

    @Test
    fun `should pass payments to the payment generator`() {
        val validPayment = validPayment()
        val paymentList = listOf(validPayment)
        val proEPayment1 = ""
        val paymentGeneratorResult =
            ProEPaymentGenerator.Result(PaymentIntegrationClient.SendResult(), proEPayment1)
        whenever(paymentGenerator.generatePayments(paymentList)).thenReturn(paymentGeneratorResult)

        paymentClient.send(paymentList, tx)

        verify(paymentGenerator).generatePayments(paymentList)
    }

    @Test
    fun `should send generated payments`() {
        val validPayment = validPayment()
        val paymentList = listOf(validPayment)
        val proEPayment1 = ""
        val paymentGeneratorResult =
            ProEPaymentGenerator.Result(
                PaymentIntegrationClient.SendResult(paymentList, listOf()),
                proEPayment1,
            )
        whenever(paymentGenerator.generatePayments(paymentList)).thenReturn(paymentGeneratorResult)

        paymentClient.send(paymentList, tx)

        verify(sftpSender).send(proEPayment1, fileName)
    }

    @Test
    fun `should not send anything if there are no sendable payments`() {
        val paymentList = listOf<Payment>()
        val paymentGeneratorResult =
            ProEPaymentGenerator.Result(PaymentIntegrationClient.SendResult(), "")
        whenever(paymentGenerator.generatePayments(paymentList)).thenReturn(paymentGeneratorResult)

        paymentClient.send(paymentList, tx)

        verify(sftpSender, never()).send("", fileName)
    }

    @Test
    fun `should return successfully sent payments in success list`() {
        val validPayment = validPayment()
        val paymentList = listOf(validPayment)
        val proEPayment1 = ""
        val paymentGeneratorResult =
            ProEPaymentGenerator.Result(
                PaymentIntegrationClient.SendResult(listOf(validPayment), listOf()),
                proEPayment1,
            )
        whenever(paymentGenerator.generatePayments(paymentList)).thenReturn(paymentGeneratorResult)

        val sendResult = paymentClient.send(paymentList, tx)

        Assertions.assertThat(sendResult.succeeded).hasSize(1)
    }

    @Test
    fun `should return unsuccessfully generated payments in failed list`() {
        val validPayment = validPayment()
        val unitWithoutIban =
            PaymentUnit(
                DaycareId(UUID.randomUUID()),
                "No IBAN",
                "1234567-8",
                null,
                "PROVIDERID",
                "PARTNERCODE",
                setOf(CareType.CENTRE),
                "1234",
            )
        val paymentWithoutIban = validPayment().copy(unit = unitWithoutIban)
        val paymentList = listOf(validPayment, paymentWithoutIban)
        val proEPayment1 = ""
        val paymentGeneratorResult =
            ProEPaymentGenerator.Result(
                PaymentIntegrationClient.SendResult(
                    listOf(validPayment),
                    listOf(paymentWithoutIban),
                ),
                proEPayment1,
            )
        whenever(paymentGenerator.generatePayments(paymentList)).thenReturn(paymentGeneratorResult)
        // whenever(sftpSender.send(proEPayment1)).thenThrow(SftpException::class.java)

        val sendResult = paymentClient.send(paymentList, tx)

        Assertions.assertThat(sendResult.failed).hasSize(1)
    }

    @Test
    fun `should return unsuccessfully sent payments in failed list`() {
        val validPayment = validPayment()
        val paymentList = listOf(validPayment, validPayment)
        val proEPayment1 = ""
        val paymentGeneratorResult =
            ProEPaymentGenerator.Result(
                PaymentIntegrationClient.SendResult(paymentList, listOf()),
                proEPayment1,
            )
        whenever(paymentGenerator.generatePayments(paymentList)).thenReturn(paymentGeneratorResult)
        whenever(sftpSender.send(proEPayment1, fileName)).thenThrow(SftpException::class.java)

        val sendResult = paymentClient.send(paymentList, tx)

        Assertions.assertThat(sendResult.failed).hasSize(2)
    }

    @Test
    fun `should send multiple payments at once`() {
        val validPayment = validPayment()
        val paymentList = listOf(validPayment, validPayment)
        val proEPayment1 = "xx"
        whenever(paymentGenerator.generatePayments(paymentList))
            .thenReturn(
                ProEPaymentGenerator.Result(
                    PaymentIntegrationClient.SendResult(paymentList, listOf()),
                    "xx",
                )
            )
        val sendResult = paymentClient.send(paymentList, tx)

        Assertions.assertThat(sendResult.succeeded).hasSize(2)
        verify(sftpSender).send(proEPayment1, fileName)
    }

    @Test
    fun `should log successful payments`(output: CapturedOutput) {
        val validPayment = validPayment()
        val paymentList = listOf(validPayment, validPayment)
        whenever(paymentGenerator.generatePayments(paymentList))
            .thenReturn(
                ProEPaymentGenerator.Result(
                    PaymentIntegrationClient.SendResult(listOf(validPayment), listOf()),
                    "x",
                )
            )

        paymentClient.send(paymentList, tx)

        Assertions.assertThat(output).contains("Successfully sent 1 payments")
    }

    @Test
    fun `should log failed payment sends`(output: CapturedOutput) {
        val validPayment = validPayment()
        val paymentList = listOf(validPayment, validPayment)
        val proEPayment1 = ""
        whenever(paymentGenerator.generatePayments(paymentList))
            .thenReturn(
                ProEPaymentGenerator.Result(
                    PaymentIntegrationClient.SendResult(paymentList, listOf()),
                    proEPayment1,
                )
            )

        whenever(sftpSender.send(proEPayment1, fileName)).thenThrow(SftpException::class.java)
        paymentClient.send(paymentList, tx)

        Assertions.assertThat(output).contains("Failed to send 2 payments")
    }
}
