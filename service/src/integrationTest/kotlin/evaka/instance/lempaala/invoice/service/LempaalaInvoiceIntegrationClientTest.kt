// SPDX-FileCopyrightText: 2023-2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.lempaala.invoice.service

import evaka.core.invoicing.integration.InvoiceIntegrationClient
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.instance.lempaala.AbstractLempaalaIntegrationTest
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class LempaalaInvoiceIntegrationClientTest : AbstractLempaalaIntegrationTest() {
    @Autowired private lateinit var invoiceIntegrationClient: InvoiceIntegrationClient

    @Test
    fun `valid invoice with invoice number`() {
        val mockNow = HelsinkiDateTime.of(LocalDate.of(2021, 2, 1), LocalTime.of(12, 34))
        val invoices = listOf(validInvoice().copy(number = 1))

        val result = invoiceIntegrationClient.send(mockNow, invoices)

        assertThat(result)
            .returns(invoices) { it.succeeded }
            .returns(emptyList()) { it.failed }
            .returns(emptyList()) { it.manuallySent }
        val data =
            getS3Object(properties.bucket.export, "invoices/193_54_202102011234.dat").use {
                it.readAllBytes().toString(StandardCharsets.ISO_8859_1)
            }
        assertEquals(
            """310382-956DL  Meikäläinen Matti                                                                                   Meikäläisenkuja 6 B 7         90100 OULU                                                                                                                              1   K202205052021030620210204                                                 1000         54   Varhaiskasvatus 01.2021                                                                                                                                                                                                                                                                                      
310382-956D3Meikäläinen Maiju                                                                                                     
310382-956D301.01.2021 - 31.01.2021                                                                                               
310382-956D1Esiopetusta täydentävä varhaiskasvatus   000004820000KPL 00000001000000                                                                                                                         325730010002627                                             00000048200
310382-956D3kuvaus2                                                                                                               
310382-956D3Meikäläinen Matti                                                                                                     
310382-956D301.01.2021 - 31.01.2021                                                                                               
310382-956D1Varhaiskasvatus                          000002430000KPL 00000001000000                                                                                                                         325730010002627                                             00000024300
310382-956D3kuvaus1                                                                                                               
310382-956D301.01.2021 - 31.01.2021                                                                                               
310382-956D1Varhaiskasvatus                          000002500000KPL 00000001000000                                                                                                                         325730010002627                                             00000025000
310382-956D3kuvaus3                                                                                                               
310382-956D301.01.2021 - 31.01.2021                                                                                               
310382-956D1Hyvityspäivä                             000002500000KPL-00000001000000                                                                                                                         325730010002627                                            -00000025000
310382-956D3kuvaus4                                                                                                               
""",
            data,
        )
    }
}
