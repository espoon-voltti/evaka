// SPDX-FileCopyrightText: 2021 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.invoice.service

import evaka.core.invoicing.domain.InvoiceDetailed
import evaka.core.invoicing.domain.InvoiceRowDetailed
import evaka.core.invoicing.domain.PersonDetailed
import evaka.core.invoicing.integration.InvoiceIntegrationClient
import evaka.core.invoicing.integration.InvoiceIntegrationClient.SendResult
import evaka.instance.tampere.InvoiceProperties
import evaka.instance.tampere.invoice.config.findProduct
import evaka.trevaka.jaxb.localDateToXMLGregorianCalendar
import fi.tampere.messages.ipaas.commontypes.v1.FaultType
import fi.tampere.messages.ipaas.commontypes.v1.SimpleAcknowledgementResponseType
import fi.tampere.messages.sapsd.salesorder.v11.Address
import fi.tampere.messages.sapsd.salesorder.v11.Header
import fi.tampere.messages.sapsd.salesorder.v11.Item
import fi.tampere.messages.sapsd.salesorder.v11.Items
import fi.tampere.messages.sapsd.salesorder.v11.P1PartnerType
import fi.tampere.messages.sapsd.salesorder.v11.Person
import fi.tampere.messages.sapsd.salesorder.v11.PersonName
import fi.tampere.messages.sapsd.salesorder.v11.SalesOrder
import fi.tampere.messages.sapsd.salesorder.v11.Text
import fi.tampere.services.sapsd.salesorder.v1.SendSalesOrderRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.xml.bind.JAXBIntrospector
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import org.springframework.ws.client.core.WebServiceTemplate
import org.springframework.ws.soap.client.SoapFaultClientException
import org.springframework.ws.soap.client.core.SoapActionCallback

private val logger = KotlinLogging.logger {}
private val restrictedAddress =
    Address().apply {
        street = "Turvakielto"
        postCode = "00000"
        town = "TUNTEMATON"
    }
private const val MAX_NAME_LENGTH = 35
private const val MAX_STREET_LENGTH = 30
private const val MAX_TOWN_LENGTH = 25
private const val MAX_ITEM_DESCRIPTION_LENGTH = 40
private const val MAX_ITEM_PROFIT_CENTER_LENGTH = 10
private const val MAX_TEXT_ROW_LENGTH = 70

class TampereInvoiceClient(
    val webServiceTemplate: WebServiceTemplate,
    private val properties: InvoiceProperties,
) : InvoiceIntegrationClient {

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.of("fi"))

    private fun priceInEuros(priceInCents: Int?): BigDecimal? =
        if (priceInCents != null) {
            BigDecimal(priceInCents).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        } else {
            null
        }

    override fun send(invoices: List<InvoiceDetailed>): SendResult {
        logger.info { "Invoice batch started" }
        val (zeroSumInvoices, nonZeroSumInvoices) =
            invoices.partition { invoice -> invoice.totalPrice == 0 }
        val (withSSN, withoutSSN) =
            nonZeroSumInvoices.partition { invoice -> invoice.headOfFamily.ssn != null }

        if (withSSN.isNotEmpty()) {
            try {
                val request = toRequest(withSSN)
                val response =
                    webServiceTemplate.marshalSendAndReceive(
                        properties.url,
                        request,
                        SoapActionCallback(
                            "http://www.tampere.fi/services/sapsd/salesorder/v1.0/SendSalesOrder"
                        ),
                    )
                when (val value = JAXBIntrospector.getValue(response)) {
                    is SimpleAcknowledgementResponseType ->
                        logger.info { "Invoice batch ended with status ${value.statusMessage}" }
                    else -> logger.warn { "Unknown response in invoice: $value" }
                }
            } catch (e: SoapFaultClientException) {
                when (val faultDetail = unmarshalFaultDetail(e)) {
                    is FaultType ->
                        logger.error {
                            "Fault in invoice: ${faultDetail.errorCode}. Message: ${faultDetail.errorMessage}. Details: ${faultDetail.detailMessage}"
                        }
                    else -> logger.error(e) { "Unknown fault in invoice: $faultDetail" }
                }
                throw e
            }
        }
        return SendResult(manuallySent = withoutSSN, succeeded = withSSN + zeroSumInvoices)
    }

    private fun toRequest(invoices: List<InvoiceDetailed>): SendSalesOrderRequest =
        SendSalesOrderRequest().apply {
            salesOrder =
                SalesOrder().apply {
                    header.addAll(invoices.map(this@TampereInvoiceClient::toHeader))
                }
        }

    private fun toHeader(invoice: InvoiceDetailed): Header {
        val headOfFamily = toInvoicePerson(invoice.headOfFamily)
        val codebtor = invoice.codebtor?.let { toInvoicePerson(it) }

        return Header().apply {
            customer =
                P1PartnerType().apply {
                    person =
                        Person().apply {
                            ssn = headOfFamily.ssn
                            personName =
                                PersonName().apply {
                                    firstNames = headOfFamily.firstName.take(MAX_NAME_LENGTH)
                                    surName = headOfFamily.lastName.take(MAX_NAME_LENGTH)
                                }
                        }
                    address = headOfFamily.address()
                }
            alternativePayer =
                when (codebtor != null) {
                    true ->
                        P1PartnerType().apply {
                            person =
                                Person().apply {
                                    ssn = headOfFamily.ssn
                                    personName =
                                        PersonName().apply {
                                            firstNames = headOfFamily.name().take(MAX_NAME_LENGTH)
                                            surName = codebtor.name().take(MAX_NAME_LENGTH)
                                        }
                                }
                            address = headOfFamily.address()
                        }

                    false -> null
                }
            paymentTerm = properties.paymentTerm
            dueDate = localDateToXMLGregorianCalendar(invoice.dueDate)
            billingDate = localDateToXMLGregorianCalendar(invoice.invoiceDate)
            salesOrganisation = properties.salesOrganisation
            distributionChannel = properties.distributionChannel
            division = properties.division
            salesOrderType = properties.salesOrderType
            interfaceID = properties.interfaceID
            items = toItems(invoice.rows)
        }
    }

    private fun toItems(rows: List<InvoiceRowDetailed>): Items =
        Items().apply { item.addAll(rows.map(this@TampereInvoiceClient::toItem)) }

    private fun toItem(it: InvoiceRowDetailed): Item {
        val product = findProduct(it.product)
        return Item().apply {
            description = it.description.take(MAX_ITEM_DESCRIPTION_LENGTH)
            profitCenter = it.costCenter.take(MAX_ITEM_PROFIT_CENTER_LENGTH)
            internalOrder = product.internalOrder
            material = product.code
            unitPrice = priceInEuros(it.unitPrice)
            quantity = it.amount.toFloat().toString()
            plant = properties.plant
            text.addAll(
                listOf(
                    Text().apply {
                        textRow.addAll(
                            listOf(
                                "${it.child.lastName} ${it.child.firstName}"
                                    .take(MAX_TEXT_ROW_LENGTH),
                                "${it.periodStart.format(dateFormatter)} - ${it.periodEnd.format(dateFormatter)}",
                            )
                        )
                    }
                )
            )
        }
    }

    private fun unmarshalFaultDetail(exception: SoapFaultClientException): Any? =
        try {
            val detailEntries = exception.soapFault?.faultDetail?.detailEntries
            when (detailEntries?.hasNext()) {
                true -> webServiceTemplate.unmarshaller!!.unmarshal(detailEntries.next().source)
                else -> null
            }
        } catch (e: Exception) {
            logger.error(e) { "Unable to unmarshal fault detail" }
            null
        }
}

internal fun toInvoicePerson(person: PersonDetailed): InvoicePerson {
    val ssn = person.ssn
    val (lastName, firstName) =
        if (person.invoiceRecipientName.isNotBlank()) {
            person.invoiceRecipientName.trim() to ""
        } else {
            person.lastName.trim() to person.firstName.trim()
        }
    val restrictedDetailsEnabled = person.restrictedDetailsEnabled
    val (streetName, postalCode, postOffice) =
        if (hasInvoicingAddress(person)) {
            Triple(
                person.invoicingStreetAddress.trim(),
                person.invoicingPostalCode.trim(),
                person.invoicingPostOffice.trim(),
            )
        } else {
            Triple(person.streetAddress.trim(), person.postalCode.trim(), person.postOffice.trim())
        }
    return InvoicePerson(
        ssn!!,
        lastName,
        firstName,
        restrictedDetailsEnabled,
        streetName,
        postalCode,
        postOffice,
    )
}

internal fun hasInvoicingAddress(person: PersonDetailed): Boolean =
    person.invoicingStreetAddress.isNotBlank() &&
        person.invoicingPostalCode.isNotBlank() &&
        person.invoicingPostOffice.isNotBlank()

internal data class InvoicePerson(
    val ssn: String,
    val lastName: String,
    val firstName: String,
    val restrictedDetailsEnabled: Boolean,
    val streetName: String,
    val postalCode: String,
    val postOffice: String,
) {
    fun name(): String = "$lastName $firstName".trim()

    fun address(): Address =
        when (restrictedDetailsEnabled) {
            true -> restrictedAddress

            false ->
                Address().apply {
                    street = streetName.take(MAX_STREET_LENGTH)
                    town = postOffice.take(MAX_TOWN_LENGTH)
                    postCode = postalCode
                }
        }
}
