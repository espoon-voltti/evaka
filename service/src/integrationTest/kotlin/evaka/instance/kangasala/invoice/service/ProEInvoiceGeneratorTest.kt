// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.kangasala.invoice.service

import evaka.core.invoicing.domain.InvoiceDetailed
import evaka.instance.kangasala.AbstractKangasalaIntegrationTest
import evaka.instance.kangasala.util.FieldType
import java.time.YearMonth
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

private val restrictedStreetAddress = "Sivistyskeskus, Varhaiskasvatus- ja esiopetuspalvelut, PL 50"
private val restrictedPostCode = "36201"
private val restrictedPostOffice = "Kangasala"

internal class ProEInvoiceGeneratorTest : AbstractKangasalaIntegrationTest() {
    @Autowired private lateinit var proEInvoiceGenerator: ProEInvoiceGenerator

    @Test
    fun `should return successfully created invoices in success list`() {
        val invoice = validInvoice()
        val invoiceList = listOf(invoice, invoice)

        val generationResult =
            proEInvoiceGenerator.generateInvoice(invoiceList, YearMonth.of(2024, 11))
        assertEquals(generationResult.sendResult.succeeded, invoiceList)
        assertEquals(generationResult.sendResult.manuallySent, listOf<InvoiceDetailed>())
        assertEquals(generationResult.sendResult.failed, listOf<InvoiceDetailed>())
    }

    @Test
    fun `should return manually sent invoices in manually list`() {
        val restrictedInvoice = validInvoice().copy(headOfFamily = personWithRestrictedDetails())
        val invoiceWithoutSsn = validInvoice().copy(headOfFamily = personWithoutSSN())
        val invoiceList = listOf(restrictedInvoice, invoiceWithoutSsn)

        val generationResult =
            proEInvoiceGenerator.generateInvoice(invoiceList, YearMonth.of(2024, 11))
        assertEquals(generationResult.sendResult.succeeded.size, 1)
        assertEquals(generationResult.sendResult.manuallySent, listOf(invoiceWithoutSsn))
        assertEquals(generationResult.sendResult.failed, listOf<InvoiceDetailed>())
    }

    @Test
    fun `should return invoices with hidden address if head of family has restricted details`() {
        val restrictedHeadOfFamily =
            validInvoice().copy(headOfFamily = personWithRestrictedDetails())
        val invoiceList = listOf(restrictedHeadOfFamily)

        val generationResult =
            proEInvoiceGenerator.generateInvoice(invoiceList, YearMonth.of(2024, 11))
        val result = generationResult.sendResult.succeeded[0]
        assertEquals(result.headOfFamily.streetAddress, restrictedStreetAddress)
        assertEquals(result.headOfFamily.postalCode, restrictedPostCode)
        assertEquals(result.headOfFamily.postOffice, restrictedPostOffice)
    }

    @Test
    fun `should return invoices with hidden address if codebtor has restricted details`() {
        val restrictedCodebtor = validInvoice().copy(codebtor = personWithRestrictedDetails())
        val invoiceList = listOf(restrictedCodebtor)

        val generationResult =
            proEInvoiceGenerator.generateInvoice(invoiceList, YearMonth.of(2024, 11))
        val result = generationResult.sendResult.succeeded[0]
        assertEquals(result.codebtor?.streetAddress, restrictedStreetAddress)
        assertEquals(result.codebtor?.postalCode, restrictedPostCode)
        assertEquals(result.codebtor?.postOffice, restrictedPostOffice)
    }

    @Test
    fun `should format invoice rows according to data and formatting`() {
        val format =
            listOf(
                InvoiceField(InvoiceFieldName.INVOICE_IDENTIFIER, FieldType.ALPHANUMERIC, 1, 11),
                InvoiceField(InvoiceFieldName.CLIENT_NAME1, FieldType.ALPHANUMERIC, 12, 30),
                InvoiceField(
                    InvoiceFieldName.INCLUDED_LATE_PAYMENT_INTEREST,
                    FieldType.NUMERIC,
                    42,
                    6,
                    2,
                ),
            )
        val invoiceData = InvoiceData()

        invoiceData.setAlphanumericValue(InvoiceFieldName.INVOICE_IDENTIFIER, "121212A121A")
        invoiceData.setAlphanumericValue(InvoiceFieldName.CLIENT_NAME1, "Jokunen Jaska")
        invoiceData.setNumericValue(InvoiceFieldName.INCLUDED_LATE_PAYMENT_INTEREST, 42)

        val result = proEInvoiceGenerator.generateRow(format, invoiceData)

        assertEquals(result, "121212A121AJokunen Jaska                 00004200\n")
    }
}
