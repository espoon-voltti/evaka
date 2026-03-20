// SPDX-FileCopyrightText: 2023-2025 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku.invoice.service

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SapInvoiceGeneratorTest {
    companion object {
        var invoiceXml: String = ""

        @BeforeAll
        @JvmStatic
        fun generateXml() {
            val invoiceGenerator = SapInvoiceGenerator()
            val invoiceList = listOf(validInvoice())
            invoiceXml = invoiceGenerator.generateInvoice(invoiceList).invoiceString
        }
    }

    fun assertTag(tag: String) {
        assert(invoiceXml.contains("<$tag>"))
        assert(invoiceXml.contains("</$tag>"))
    }

    fun assertExpectedString(expectedString: String) {
        val pattern = Regex(expectedString)
        assert(pattern.containsMatchIn(invoiceXml))
    }

    fun assertElement(element: String, value: String) {
        val expectedString = "<$element>$value</$element>"
        assertExpectedString(expectedString)
    }

    fun assertNotExpectedString(xml: String, expectedString: String) {
        val pattern = Regex(expectedString)
        assert(!pattern.containsMatchIn(xml))
    }

    fun assertEmptyElement(tag: String) {
        assertElement(tag, "")
    }

    @Test
    fun `should not return iDoc if invoice sum is zero`() {
        val invoiceGenerator = SapInvoiceGenerator()
        val invoiceList2 = mutableListOf(validInvoiceZeroSum())
        invoiceList2.add(validInvoice())
        var invoiceIdoc = invoiceGenerator.generateInvoice(invoiceList2).invoiceString
        assertNotExpectedString(invoiceIdoc, "<KRATE>0.00</KRATE>")
    }

    @Test
    fun `success list of invoices still contains zero value invoices`() {
        val invoiceGenerator = SapInvoiceGenerator()
        val invoiceList = mutableListOf(validInvoiceZeroSum())
        invoiceList.add(validInvoice())
        var invoiceCount = invoiceList.count()
        var successListCount =
            invoiceGenerator.generateInvoice(invoiceList).sendResult.succeeded.count()
        assert(successListCount == invoiceCount)
    }

    // EDIDC40 - Segment

    @Test
    fun `invoiceGenerator result should have ORDERS05 root element`() {
        assertTag("ORDERS05")
    }

    @Test
    fun `invoiceGenerator result should have IDOC with BEGIN element`() {
        assertExpectedString("<IDOC BEGIN=\"1\">")
    }

    @Test
    fun `TABNAM should have constant value of EDI_DC40`() {
        assertElement("TABNAM", "EDI_DC40")
    }

    @Test
    fun `DIRECT should have constant value of 2`() {
        assertElement("DIRECT", "2")
    }

    @Test
    fun `IDOCTYP should have constant value of ORDERS05`() {
        assertElement("IDOCTYP", "ORDERS05")
    }

    @Test
    fun `MESTYP should have constant value of ORDERS`() {
        assertElement("MESTYP", "ORDERS")
    }

    @Test
    fun `SNDPOR should be empty element`() {
        assertEmptyElement("SNDPOR")
    }

    @Test
    fun `SNDPRT should have constant value of LS`() {
        assertElement("SNDPRT", "LS")
    }

    @Test
    fun `SNDPRN should have constant value of VAK_1002`() {
        assertElement("SNDPRN", "VAK_1002")
    }

    @Test
    fun `RCVPOR should be empty element`() {
        assertEmptyElement("RCVPOR")
    }

    @Test
    fun `RCVPRT should be empty element`() {
        assertEmptyElement("RCVPRT")
    }

    @Test
    fun `RCVPRN should be empty element`() {
        assertEmptyElement("RCVPRN")
    }

    // E1EDK01 - Segment
    @Test
    fun `ZTERM should be empty element`() {
        assertEmptyElement("ZTERM")
    }

    @Test
    fun `AUGRU should be empty element`() {
        assertEmptyElement("AUGRU")
    }

    // E1EDK14 - Segment
    @Test
    fun `QUALF should have constant value of 006`() {
        assertElement("QUALF", "006")
    }

    @Test
    fun `ORGID should have constant value of 2E`() {
        assertElement("ORGID", "2E")
    }

    @Test
    fun `QUALF should have constant value of 007`() {
        assertElement("QUALF", "007")
    }

    @Test
    fun `ORGID should have constant value of 22`() {
        assertElement("ORGID", "22")
    }

    @Test
    fun `QUALF should have constant value of 008`() {
        assertElement("QUALF", "008")
    }

    @Test
    fun `ORGID should have constant value of 2100`() {
        assertElement("ORGID", "2100")
    }

    @Test
    fun `QUALF should have constant value of 012`() {
        assertElement("QUALF", "012")
    }

    @Test
    fun `ORGID should have constant value of ZMT`() {
        assertElement("ORGID", "ZMT")
    }

    @Test
    fun `QUALF should have constant value of 016`() {
        assertElement("QUALF", "016")
    }

    @Test
    fun `ORGID should have constant value of A010`() {
        assertElement("ORGID", "A010")
    }

    @Test
    fun `QUALF should have constant value of 019`() {
        assertElement("QUALF", "019")
    }

    @Test
    fun `ORGID should have constant value of VAK`() {
        assertElement("ORGID", "VAK")
    }

    // E1EDK03 - segment
    @Test
    fun `IDDAT should have constant value of 016`() {
        assertElement("IDDAT", "016")
    }

    @Test
    fun `DATUM should have constant format of yyyyMMdd`() {
        assertElement("DATUM", "\\d{8}")
    }

    @Test
    fun `IDDAT should have constant value of 024`() {
        assertElement("IDDAT", "024")
    }

    @Test
    fun `PARVW should have constant value of AG`() {
        assertElement("PARVW", "AG")
    }

    @Test
    fun `PARTN should have constant value of valid ssn (310382-956D)`() {
        assertElement("PARTN", "310382-956D")
    }

    @Test
    fun `PARVW should have constant value of RE`() {
        assertElement("PARVW", "RE")
    }

    @Test
    fun `PARVW should have constant value of RG`() {
        assertElement("PARVW", "RG")
    }

    @Test
    fun `PARVW should have constant value of WE`() {
        assertElement("PARVW", "WE")
    }

    @Test
    fun `PARVW should have constant value of Y1`() {
        assertElement("PARVW", "Y1")
    }

    @Test
    fun `PARTN should have constant value of codedebtor ssn (310384-956D)`() {
        assertElement("PARTN", "310384-956D")
    }

    // E1EDK02 - segment
    @Test
    fun `QUALF should have constant value of 001`() {
        assertElement("QUALF", "001")
    }

    @Test
    fun `BELNR should have YEAR+SalesAgency(A010)+InvoicenumberFromEVaka`() {
        assertElement("BELNR", "2021A01012345")
    }

    // E1EDKT1 - segment TODO: this is optional
    @Test
    fun `TDID should have constant value of Z002`() {
        assertElement("TDID", "Z002")
    }

    // E1EDKT2 - TODO: this is optional
    @Test
    fun `TDLINE should have constant value of Lisätietoja laskusta email and phone number`() {
        assertElement("TDLINE", "Lisätietoja laskusta varhaiskasvatusmaksut@turku.fi/ 02-2625609")
    }

    // E1EDP01 ROWS - segment
    @Test // TODO: invoicerow number
    fun `POSEX should have constant value of 000010`() {
        assertElement("POSEX", "000010")
    }

    @Test
    fun `MENGE should have constant value of 1,000`() {
        assertElement("MENGE", "1.000")
    }

    @Test
    fun `WERKS should have constant value of 102S`() {
        assertElement("WERKS", "102S")
    }

    // E1EDP02 ROWS - segment
    @Test
    fun `QUALF should have constant value of 048`() {
        assertElement("QUALF", "048")
    }

    @Test // TODO: same as invoice row number POSEX
    fun `ZEILE should have constant value of 000010`() {
        assertElement("ZEILE", "000010")
    }

    // E1EDP03 ROWS - segment
    @Test
    fun `IDDAT should have constant value of 002`() {
        assertElement("IDDAT", "002")
    }

    @Test
    fun `KSCHL should have constant value of ZPR0`() {
        assertElement("KSCHL", "ZPR0")
    }

    // E1EDP19 ROWS - segment
    @Test
    fun `QUALF should have constant value of 002`() {
        assertElement("QUALF", "002")
    }

    // TODO: IDTNR test for product

    // E1EDPT1 ROWS - segment
    @Test
    fun `TDID should have constant value of ZZ01`() {
        assertElement("TDID", "ZZ01")
    }

    @Test
    fun `TDID should have constant value of ZZ02`() {
        assertElement("TDID", "ZZ02")
    }

    @Test
    fun `TDLINE should have constant value of Matti Meikäläinen`() {
        assertElement("TDLINE", "Matti Meikäläinen")
    }

    @Test
    fun `TDID should have constant value of ZZ03`() {
        assertElement("TDID", "ZZ03")
    }

    @Test
    fun `TDLINE should have constant value of DateTime interval`() {
        assertElement("TDLINE", "01.01.2021-31.01.2021")
    }

    @Test
    fun `result should be equal to a known good format`() {
        val correctInvoice =
            object {}.javaClass.getResource("/invoice-client/CorrectSapInvoice.txt")?.readText()
        assert(invoiceXml.equals(correctInvoice))
    }
}
