// SPDX-FileCopyrightText: 2023-2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.payment

import evaka.core.daycare.CareType
import evaka.core.invoicing.domain.Payment
import evaka.core.invoicing.domain.PaymentStatus
import evaka.core.invoicing.domain.PaymentUnit
import evaka.core.shared.DaycareId
import evaka.core.shared.PaymentId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.instance.tampere.BiExportProperties
import evaka.instance.tampere.BucketProperties
import evaka.instance.tampere.InvoiceProperties
import evaka.instance.tampere.PaymentProperties
import evaka.instance.tampere.SummertimeAbsenceProperties
import evaka.instance.tampere.TampereConfig
import evaka.instance.tampere.TampereProperties
import evaka.trevaka.addClientInterceptors
import evaka.trevaka.newPayloadValidatingInterceptor
import java.time.LocalDate
import java.time.Month
import java.util.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.core.io.ClassPathResource
import org.springframework.ws.test.client.MockWebServiceServer
import org.springframework.ws.test.client.RequestMatchers.connectionTo
import org.springframework.ws.test.client.RequestMatchers.payload
import org.springframework.ws.test.client.ResponseCreators.withPayload

class TamperePaymentClientTest {

    private lateinit var client: TamperePaymentClient
    private lateinit var server: MockWebServiceServer
    private lateinit var tx: Database.Read

    @BeforeEach
    fun setup() {
        val properties =
            TampereProperties(
                InvoiceProperties(""),
                PaymentProperties("http://localhost:8080/payableAccounting"),
                SummertimeAbsenceProperties(),
                BucketProperties(export = "trevaka-export-it"),
                BiExportProperties(prefix = "bi"),
                "finance-api-key-123",
            )
        val configuration = TampereConfig()
        client = configuration.paymentIntegrationClient(properties) as TamperePaymentClient
        addClientInterceptors(
            client.webServiceTemplate,
            newPayloadValidatingInterceptor(
                "iPaaS_Common_Types_v1_0.xsd",
                "PayableAccounting_v08.xsd",
                "SAPFICO_Ostoreskontra_v1_0_InlineSchema1.xsd",
            ),
        )
        server = MockWebServiceServer.createServer(client.webServiceTemplate)
        tx = mock {}
    }

    @Test
    fun `send with multiple payments`() {
        val payment1 = testPayment
        val payment2 =
            testPayment.copy(unit = testPayment.unit.copy(partnerCode = "  ", costCenter = " "))
        val payment3 =
            testPayment.copy(
                unit = testPayment.unit.copy(partnerCode = " 5810  ", costCenter = " 131000  ")
            )
        server
            .expect(connectionTo("http://localhost:8080/payableAccounting"))
            .andExpect(payload(ClassPathResource("payment-client/payable-accounting.xml")))
            .andRespond(
                withPayload(ClassPathResource("payment-client/payable-accounting-response-ok.xml"))
            )

        val result = client.send(listOf(payment1, payment2, payment3), tx)

        assertThat(result)
            .returns(listOf(payment1, payment2, payment3)) { it.succeeded }
            .returns(emptyList()) { it.failed }
    }

    @Test
    fun `send with zero payments`() {
        val result = client.send(emptyList(), tx)

        assertThat(result).returns(emptyList()) { it.succeeded }.returns(emptyList()) { it.failed }
        server.verify()
    }
}

internal val testPayment =
    Payment(
        id = PaymentId(UUID.randomUUID()),
        created = HelsinkiDateTime.now(),
        updated = HelsinkiDateTime.now(),
        unit =
            PaymentUnit(
                id = DaycareId(UUID.randomUUID()),
                name = "Esimerkkiyksikkö",
                businessId = "1234567-9",
                iban = "FIxx xxxx xxxx xx",
                providerId = "81591",
                partnerCode = null,
                careType = setOf(CareType.CENTRE),
                costCenter = null,
            ),
        number = 134910,
        period = DateRange.ofMonth(2024, Month.JULY),
        amount = 35000,
        status = PaymentStatus.CONFIRMED,
        paymentDate = LocalDate.of(2024, 7, 27),
        dueDate = LocalDate.of(2024, 8, 31),
        sentAt = null,
        sentBy = null,
    )
