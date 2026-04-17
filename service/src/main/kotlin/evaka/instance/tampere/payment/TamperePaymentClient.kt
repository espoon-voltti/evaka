// SPDX-FileCopyrightText: 2023-2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.payment

import evaka.core.daycare.CareType
import evaka.core.invoicing.domain.Payment
import evaka.core.invoicing.domain.PaymentIntegrationClient
import evaka.core.shared.db.Database
import evaka.instance.tampere.PaymentProperties
import fi.tampere.messages.ipaas.commontypes.v1.SimpleAcknowledgementResponseType
import fi.tampere.messages.sapfico.payableaccounting.v08.Invoice
import fi.tampere.messages.sapfico.payableaccounting.v08.PayableAccounting
import fi.tampere.messages.sapfico.payableaccounting.v08.PayableAccountingHeader
import fi.tampere.messages.sapfico.payableaccounting.v08.PayableAccountingLine
import fi.tampere.services.sapfico.payableaccounting.v1.SendPayableAccountingRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.xml.bind.JAXBIntrospector
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.springframework.ws.client.core.WebServiceTemplate
import org.springframework.ws.soap.client.core.SoapActionCallback

private val logger = KotlinLogging.logger {}

class TamperePaymentClient(
    val webServiceTemplate: WebServiceTemplate,
    private val properties: PaymentProperties,
) : PaymentIntegrationClient {
    override fun send(
        payments: List<Payment>,
        tx: Database.Read,
    ): PaymentIntegrationClient.SendResult {
        if (payments.isNotEmpty()) {
            val request =
                SendPayableAccountingRequest().apply {
                    payableAccounting =
                        PayableAccounting().apply { invoice.addAll(payments.map(::toInvoice)) }
                }
            val response =
                webServiceTemplate.marshalSendAndReceive(
                    properties.url,
                    request,
                    SoapActionCallback(
                        "http://www.tampere.fi/services/sapfico/payableaccounting/v1.0/SendPayableAccounting"
                    ),
                )
            when (val value = JAXBIntrospector.getValue(response)) {
                is SimpleAcknowledgementResponseType ->
                    logger.info { "Payment batch ended with status ${value.statusMessage}" }
                else -> logger.warn { "Unknown response in payment: $value" }
            }
        }
        return PaymentIntegrationClient.SendResult(succeeded = payments)
    }

    private fun toInvoice(payment: Payment): Invoice {
        val value = payment.amount.centsToEuros()
        val costCenter = payment.unit.costCenter?.trim()?.ifEmpty { null }
        val number = payment.number?.toString()
        val partnerCode = payment.unit.partnerCode?.trim()?.ifEmpty { null }
        return Invoice().apply {
            payableAccountingHeader =
                PayableAccountingHeader().apply {
                    sapVendor = payment.unit.providerId
                    iban = payment.unit.iban?.filterNot { it.isWhitespace() }
                    customer = payment.unit.businessId
                    organisation = 1310.toBigInteger()
                    date = payment.period.end?.format()
                    receiptType = "6S"
                    debetKredit = "-"
                    reference = costCenter?.let { "${it}_$number" } ?: number
                    currency = "EUR"
                    description = "Varhaiskasvatus"
                    billingDate = payment.paymentDate?.format()
                    billNumber = payment.number?.toString()
                    billValue = value
                    basicDate = payment.dueDate?.format()
                    interfaceID = "383"
                }
            payableAccountingLine.add(
                PayableAccountingLine().apply {
                    account = "440100"
                    this.costCenter = "131111"
                    profitCenter = "131111"
                    internalOrder =
                        with(payment.unit.careType) {
                            when {
                                contains(CareType.CLUB) -> "25275"
                                else -> "20285"
                            }
                        }
                    taxCode = "29"
                    this.partnerCode = partnerCode?.toBigInteger()
                    debetKredit = "+"
                    this.value = value
                }
            )
        }
    }
}

private fun LocalDate.format(): String = format(DateTimeFormatter.ISO_LOCAL_DATE)

private fun Int.centsToEuros(): BigDecimal =
    BigDecimal(this).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
