// SPDX-FileCopyrightText: 2023-2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.orivesi.invoice.service

import evaka.core.invoicing.integration.InvoiceIntegrationClient
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.instance.orivesi.AbstractOrivesiIntegrationTest
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired

class OrivesiInvoiceIntegrationClientTest : AbstractOrivesiIntegrationTest() {
    @Autowired private lateinit var invoiceIntegrationClient: InvoiceIntegrationClient

    @Test
    fun `valid invoice with invoice number`() {
        whenever(clockService.clock())
            .thenReturn(
                MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2021, 2, 1), LocalTime.of(12, 34)))
            )
        val invoices = listOf(validInvoice().copy(number = 1))

        val result = invoiceIntegrationClient.send(invoices)

        assertThat(result)
            .returns(invoices) { it.succeeded }
            .returns(emptyList()) { it.failed }
            .returns(emptyList()) { it.manuallySent }
        val data =
            getS3Object(properties.bucket.export, "invoices/562_30_202102011234.dat").use {
                it.readAllBytes().toString(StandardCharsets.ISO_8859_1)
            }
        assertEquals(
            """310382-956DL  Bengtsson-Henriksson Tes Matilda Josefina                                                           Meikäläisenkuja 6 B 7         90100 OULU                                                                                                                              1   K202102042021030620220505                    1                            1000         30   Varhaiskasvatus 01.2021                                                                                                                                                                                                                                                                                      
310382-956D3Eskarilainen Essi                                                                                                     
310382-956D301.01.2021 - 15.01.2021                                                                                               
310382-956D1Varhaiskasvatus                          000002430000KPL 00000001000000                                                                                                                         325730010003202          2627                               00000024300
310382-956D3alkukuukausi vakaa                                                                                                    
310382-956D316.01.2021 - 31.01.2021                                                                                               
310382-956D1Esiopetusta täydentävä varhaiskasvatus   000002430000KPL 00000001000000                                                                                                                         325730010003203          2627                               00000024300
310382-956D3loppukuukausi eskaria                                                                                                 
310382-956D3Meikäläinen Maiju                                                                                                     
310382-956D301.01.2021 - 31.01.2021                                                                                               
310382-956D1Esiopetusta täydentävä varhaiskasvatus   000004820000KPL 00000001000000                                                                                                                         325730010003203          2627                               00000048200
310382-956D3kuvaus2                                                                                                               
310382-956D3Meikäläinen Matti                                                                                                     
310382-956D301.01.2021 - 31.01.2021                                                                                               
310382-956D1Varhaiskasvatus                          000002430000KPL 00000001000000                                                                                                                         325730010003202          2627                               00000024300
310382-956D3kuvaus1                                                                                                               
310382-956D301.01.2021 - 31.01.2021                                                                                               
310382-956D1Varhaiskasvatus                          000002500000KPL 00000001000000                                                                                                                         325730010003204          2627                               00000025000
310382-956D301.01.2021 - 31.01.2021                                                                                               
310382-956D1Hyvityspäivä                             000002500000KPL-00000001000000                                                                                                                         325730010003204          2627                              -00000025000
310382-956D3kuvaus4                                                                                                               
310382-956D301.01.2021 - 31.01.2021                                                                                               
310382-956D1Hyvityspäivä                             000002500000KPL-00000001000000                                                                                                                         325730010003202          2627                              -00000025000
310382-956D3kuvaus5                                                                                                               
""",
            data,
        )
    }
}
