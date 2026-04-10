// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.nokia.invoice

import evaka.core.invoicing.domain.InvoiceDetailed
import evaka.core.invoicing.integration.InvoiceIntegrationClient
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.sftp.SftpClient
import evaka.instance.nokia.AbstractNokiaIntegrationTest
import evaka.instance.nokia.invoice.service.validInvoice
import java.time.LocalDate
import java.time.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource

class NokiaInvoiceClientIT : AbstractNokiaIntegrationTest() {
    @Autowired private lateinit var invoiceIntegrationClient: InvoiceIntegrationClient
    private lateinit var sftpClient: SftpClient

    @BeforeEach
    fun init() {
        sftpClient = SftpClient(properties.invoice.sftp.toSftpEnv())
    }

    @Test
    fun `valid invoice with invoice number`() {
        val mockNow = HelsinkiDateTime.of(LocalDate.of(2021, 2, 1), LocalTime.of(12, 34))
        val invoices = listOf(validInvoice().copy(number = 1))

        val result = invoiceIntegrationClient.send(mockNow, invoices)

        assertEquals(invoices, result.succeeded)
        assertEquals(emptyList<InvoiceDetailed>(), result.failed)
        assertEquals(emptyList<InvoiceDetailed>(), result.manuallySent)
        val data = sftpClient.getAsString("upload/605_56_202102011234.dat", Charsets.ISO_8859_1)
        assertEquals(
            ClassPathResource("invoice-client/nokia-invoice-2026.dat")
                .getContentAsString(Charsets.ISO_8859_1),
            data,
        )
    }

    @Test
    fun `invoice without ssn is marked as manually sent`() {
        val mockNow = HelsinkiDateTime.of(LocalDate.of(2021, 2, 1), LocalTime.of(12, 34))
        val invoices =
            listOf(validInvoice().copy(headOfFamily = validInvoice().headOfFamily.copy(ssn = null)))

        val result = invoiceIntegrationClient.send(mockNow, invoices)

        assertEquals(emptyList<InvoiceDetailed>(), result.succeeded)
        assertEquals(emptyList<InvoiceDetailed>(), result.failed)
        assertEquals(invoices, result.manuallySent)
    }
}
