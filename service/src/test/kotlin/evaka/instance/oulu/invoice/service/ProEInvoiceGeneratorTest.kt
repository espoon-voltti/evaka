// SPDX-FileCopyrightText: 2023-2025 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu.invoice.service

import evaka.core.shared.domain.MockEvakaClock
import evaka.instance.oulu.util.FieldType
import evaka.instance.oulu.util.FinanceDateProvider
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ProEInvoiceGeneratorTest {
    val financeDateProvider = FinanceDateProvider(MockEvakaClock(2022, 5, 5, 12, 34, 56))
    val proEInvoiceGenerator = ProEInvoiceGenerator(financeDateProvider)

    @Test
    fun `should return successfully created invoices in success list`() {
        val invoice = validInvoice()
        val invoiceList = listOf(invoice, invoice)

        val generationResult = proEInvoiceGenerator.generateInvoice(invoiceList)
        assertEquals(invoiceList, generationResult.sendResult.succeeded)
        assertEquals(listOf(), generationResult.sendResult.manuallySent)
        assertEquals(listOf(), generationResult.sendResult.failed)
    }

    @Test
    fun `should return manually sent invoices in manually list`() {
        val restrictedInvoice = validInvoice().copy(headOfFamily = personWithRestrictedDetails())
        val invoiceWithoutSsn = validInvoice().copy(headOfFamily = personWithoutSSN())
        val invoiceList = listOf(restrictedInvoice, invoiceWithoutSsn)

        val generationResult = proEInvoiceGenerator.generateInvoice(invoiceList)
        assertEquals(listOf(), generationResult.sendResult.succeeded)
        assertEquals(invoiceList, generationResult.sendResult.manuallySent)
        assertEquals(listOf(), generationResult.sendResult.failed)
    }

    @Test
    fun `uses the invoicing address and the invoice recipient name of the head of family`() {
        val invoice =
            validInvoice()
                .copy(
                    headOfFamily =
                        validPerson()
                            .copy(
                                invoiceRecipientName = "Laskunsaaja Liisa",
                                invoicingStreetAddress = "Laskutuskatu 1",
                                invoicingPostalCode = "90200",
                                invoicingPostOffice = "OULU",
                            )
                )

        val header = proEInvoiceGenerator.gatherInvoiceData(invoice).invoiceHeader

        assertEquals("Laskunsaaja Liisa", header[InvoiceFieldName.CLIENT_NAME1])
        assertEquals("Laskutuskatu 1", header[InvoiceFieldName.STREET_ADDRESS])
        assertEquals("90200 OULU", header[InvoiceFieldName.POSTAL_ADDRESS])
    }

    @Test
    fun `uses the address of the head of family when the invoicing address is incomplete`() {
        val invoice =
            validInvoice()
                .copy(headOfFamily = validPerson().copy(invoicingStreetAddress = "Laskutuskatu 1"))

        val header = proEInvoiceGenerator.gatherInvoiceData(invoice).invoiceHeader

        assertEquals("Meikäläinen Matti", header[InvoiceFieldName.CLIENT_NAME1])
        assertEquals("Meikäläisenkuja 6 B 7", header[InvoiceFieldName.STREET_ADDRESS])
        assertEquals("90100 OULU", header[InvoiceFieldName.POSTAL_ADDRESS])
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

        val invoiceDataMap = mutableMapOf<InvoiceFieldName, String>()

        invoiceDataMap[InvoiceFieldName.INVOICE_IDENTIFIER] = "121212A121A"
        invoiceDataMap[InvoiceFieldName.CLIENT_NAME1] = "Jokunen Jaska"
        invoiceDataMap[InvoiceFieldName.INCLUDED_LATE_PAYMENT_INTEREST] = "42"

        val result = proEInvoiceGenerator.generateRow(format, invoiceDataMap).toString()

        assertEquals("121212A121AJokunen Jaska                 00004200\n", result)
    }

    @Test
    fun `should check that invoice format is a proper one also with invoice function number`() {
        val invoice = validInvoice()
        val longNamedInvoice = validInvoice().copy(headOfFamily = personWithLongName())
        val invoiceList = listOf(invoice, longNamedInvoice)

        val generationResult = proEInvoiceGenerator.generateInvoice(invoiceList)

        val correctInvoice =
            object {}
                .javaClass
                .getResource("/oulu/invoice-client/CorrectProEInvoice.txt")
                ?.readText()

        assertEquals(correctInvoice, generationResult.invoiceString)
    }
}
