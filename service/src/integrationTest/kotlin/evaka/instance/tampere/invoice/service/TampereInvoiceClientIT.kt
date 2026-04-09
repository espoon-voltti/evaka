// SPDX-FileCopyrightText: 2021 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.invoice.service

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToXml
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import evaka.core.invoicing.integration.InvoiceIntegrationClient
import evaka.instance.tampere.AbstractTampereIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.ws.soap.client.SoapFaultClientException

internal class TampereInvoiceClientIT : AbstractTampereIntegrationTest() {

    @Autowired private lateinit var client: InvoiceIntegrationClient

    @Test
    fun send() {
        val invoice1 = validInvoice()
        stubFor(
            post(urlEqualTo("/mock/frends/salesOrder"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/soap+xml")
                        .withBodyFile("invoice-client/sales-order-response-ok.xml")
                )
        )

        assertThat(client.send(listOf(invoice1)))
            .returns(listOf(invoice1)) { it.succeeded }
            .returns(listOf()) { it.failed }

        verify(
            postRequestedFor(urlEqualTo("/mock/frends/salesOrder"))
                .withoutHeader("Authorization")
                .withHeader("X-API-KEY", equalTo("finance-api-key-123"))
                .withHeader(
                    "Content-Type",
                    equalTo(
                        "application/soap+xml; charset=utf-8; action=\"http://www.tampere.fi/services/sapsd/salesorder/v1.0/SendSalesOrder\""
                    ),
                )
                .withoutHeader("SOAPAction")
                .withRequestBody(
                    equalToXml(
                        ClassPathResource("__files/invoice-client/sales-order-request.xml")
                            .getContentAsString(Charsets.UTF_8)
                    )
                )
        )
    }

    @Test
    fun sendWithApplicationFaultResponse() {
        val invoice1 = validInvoice()
        stubFor(
            post(urlEqualTo("/mock/frends/salesOrder"))
                .willReturn(
                    aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/soap+xml")
                        .withBodyFile("invoice-client/sales-order-response-application-fault.xml")
                )
        )

        val thrown = catchThrowable { client.send(listOf(invoice1)) }

        assertThat(thrown).isInstanceOf(SoapFaultClientException::class.java)
        verify(
            postRequestedFor(urlEqualTo("/mock/frends/salesOrder"))
                .withoutHeader("Authorization")
                .withHeader("X-API-KEY", equalTo("finance-api-key-123"))
                .withHeader(
                    "Content-Type",
                    equalTo(
                        "application/soap+xml; charset=utf-8; action=\"http://www.tampere.fi/services/sapsd/salesorder/v1.0/SendSalesOrder\""
                    ),
                )
                .withoutHeader("SOAPAction")
        )
    }

    @Test
    fun sendWithSystemFaultResponse() {
        val invoice1 = validInvoice()
        stubFor(
            post(urlEqualTo("/mock/frends/salesOrder"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/soap+xml")
                        .withBodyFile("invoice-client/sales-order-response-system-fault.xml")
                )
        )

        val thrown = catchThrowable { client.send(listOf(invoice1)) }

        assertThat(thrown).isInstanceOf(SoapFaultClientException::class.java)
        verify(
            postRequestedFor(urlEqualTo("/mock/frends/salesOrder"))
                .withoutHeader("Authorization")
                .withHeader("X-API-KEY", equalTo("finance-api-key-123"))
                .withHeader(
                    "Content-Type",
                    equalTo(
                        "application/soap+xml; charset=utf-8; action=\"http://www.tampere.fi/services/sapsd/salesorder/v1.0/SendSalesOrder\""
                    ),
                )
                .withoutHeader("SOAPAction")
        )
    }
}
