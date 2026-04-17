// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.hameenkyro.invoice.service

import evaka.core.invoicing.integration.InvoiceIntegrationClient
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.instance.hameenkyro.AbstractHameenkyroIntegrationTest
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class HameenkyroInvoiceIntegrationClientTest : AbstractHameenkyroIntegrationTest() {
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
            getS3Object(properties.bucket.export, "invoices/vaka20210201.txt").use {
                it.readAllBytes().toString(StandardCharsets.ISO_8859_1)
            }
        assertEquals(
            """310382-956DL10Meikäläinen Matti                                                                                   Meikäläisenkuja 6 B 7         90100 OULU                                                                                                                             01 N0K20210204202103062022050500000000            1                           NN              000Varhaiskasvatus 01.2021                                                                                                                                                                                                                                                                       
310382-956D3Meikäläinen Maiju                                                                                                                                                                  
310382-956D301.01.2021 - 31.01.2021                                                                                                                                                            
310382-956D1Esiopetusta täydentävä varhaiskasvatus   000004820000kpl 00000001000000                                                            01731                                                        3257 2627                                                              
310382-956D3Meikäläinen Matti                                                                                                                                                                  
310382-956D301.01.2021 - 31.01.2021                                                                                                                                                            
310382-956D1Varhaiskasvatus                          000002430000kpl 00000001000000                                                            01731                                                        3257 2627                                                              
310382-956D301.01.2021 - 31.01.2021                                                                                                                                                            
310382-956D1Varhaiskasvatus                          000002500000kpl 00000001000000                                                            01731                                                        3257 2627                                                              
""",
            data,
        )
    }
}
