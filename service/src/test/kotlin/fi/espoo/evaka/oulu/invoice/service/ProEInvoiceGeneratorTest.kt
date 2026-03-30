// SPDX-FileCopyrightText: 2023-2025 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.invoice.service

import fi.espoo.evaka.invoicing.domain.InvoiceDetailed
import fi.espoo.evaka.oulu.util.FieldType
import fi.espoo.evaka.oulu.util.FinanceDateProvider
import fi.espoo.evaka.shared.domain.MockEvakaClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ProEInvoiceGeneratorTest {
    val financeDateProvider = FinanceDateProvider(MockEvakaClock(2022, 5, 5, 12, 34, 56))
    val proEInvoiceGenerator = ProEInvoiceGenerator(InvoiceChecker(), financeDateProvider)

    @Test
    fun `should return successfully created invoices in success list`() {
        val invoice = validInvoice()
        val invoiceList = listOf(invoice, invoice)

        val generationResult = proEInvoiceGenerator.generateInvoice(invoiceList)
        assertEquals(generationResult.sendResult.succeeded, invoiceList)
        assertEquals(generationResult.sendResult.manuallySent, listOf<InvoiceDetailed>())
        assertEquals(generationResult.sendResult.failed, listOf<InvoiceDetailed>())
    }

    @Test
    fun `should return manually sent invoices in manually list`() {
        val restrictedInvoice = validInvoice().copy(headOfFamily = personWithRestrictedDetails())
        val invoiceWithoutSsn = validInvoice().copy(headOfFamily = personWithoutSSN())
        val invoiceList = listOf(restrictedInvoice, invoiceWithoutSsn)

        val generationResult = proEInvoiceGenerator.generateInvoice(invoiceList)
        assertEquals(generationResult.sendResult.succeeded, listOf<InvoiceDetailed>())
        assertEquals(generationResult.sendResult.manuallySent, invoiceList)
        assertEquals(generationResult.sendResult.failed, listOf<InvoiceDetailed>())
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

        assertEquals(result, "121212A121AJokunen Jaska                 00004200\n")
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
