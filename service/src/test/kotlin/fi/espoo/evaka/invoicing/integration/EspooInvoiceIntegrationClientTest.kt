// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.integration

import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.invoicing.domain.InvoiceDetailed
import fi.espoo.evaka.invoicing.domain.InvoiceRowDetailed
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.domain.PersonDetailed
import fi.espoo.evaka.invoicing.service.EspooInvoiceProducts
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.InvoiceRowId
import fi.espoo.evaka.shared.PersonId
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.jupiter.api.Test

class EspooInvoiceIntegrationClientTest {
    private val agreementType = 100

    @Test
    fun `sending an invoice uses the recipient's actual address if it's valid`() {
        val testInvoice = testInvoice()
        val batch =
            EspooInvoiceIntegrationClient.createBatchExports(
                listOf(testInvoice),
                agreementType,
                true
            )
        assertEquals(1, batch.invoices.size)
        batch.invoices.first().let { invoice ->
            assertEquals(testInvoice.headOfFamily.streetAddress, invoice.client.street)
            assertEquals(testInvoice.headOfFamily.postalCode, invoice.client.postalCode)
            assertEquals(testInvoice.headOfFamily.postOffice, invoice.client.post)
            assertEquals(testInvoice.headOfFamily.streetAddress, invoice.recipient.street)
            assertEquals(testInvoice.headOfFamily.postalCode, invoice.recipient.postalCode)
            assertEquals(testInvoice.headOfFamily.postOffice, invoice.recipient.post)
        }
    }

    @Test
    fun `sending an invoice trims whitespace from recipient's address`() {
        val postalCodeWithWhiteSpace = " 12345 "
        val testInvoice =
            testInvoice(headOfFamily = testPerson(postalCode = postalCodeWithWhiteSpace))
        val batch =
            EspooInvoiceIntegrationClient.createBatchExports(
                listOf(testInvoice),
                agreementType,
                true
            )
        assertEquals(1, batch.invoices.size)
        batch.invoices.first().let { invoice ->
            assertEquals(testInvoice.headOfFamily.streetAddress, invoice.client.street)
            assertNotEquals(postalCodeWithWhiteSpace, invoice.client.postalCode)
            assertEquals(postalCodeWithWhiteSpace.trim(), invoice.client.postalCode)
            assertEquals(testInvoice.headOfFamily.postOffice, invoice.client.post)
            assertEquals(testInvoice.headOfFamily.streetAddress, invoice.recipient.street)
            assertNotEquals(postalCodeWithWhiteSpace, invoice.recipient.postalCode)
            assertEquals(postalCodeWithWhiteSpace.trim(), invoice.recipient.postalCode)
            assertEquals(testInvoice.headOfFamily.postOffice, invoice.recipient.post)
        }
    }

    @Test
    fun `sending an invoice uses a fallback address when the recipient's actual address is partially incomplete`() {
        val testInvoice = testInvoice(headOfFamily = testPerson(streetAddress = ""))
        val batch =
            EspooInvoiceIntegrationClient.createBatchExports(
                listOf(testInvoice),
                agreementType,
                true
            )
        assertEquals(1, batch.invoices.size)
        batch.invoices.first().let { invoice ->
            assertEquals(null, invoice.client.street)
            assertEquals(null, invoice.client.postalCode)
            assertEquals(null, invoice.client.post)
            assertEquals(
                EspooInvoiceIntegrationClient.fallbackStreetAddress,
                invoice.recipient.street
            )
            assertEquals(
                EspooInvoiceIntegrationClient.fallbackPostalCode,
                invoice.recipient.postalCode
            )
            assertEquals(EspooInvoiceIntegrationClient.fallbackPostOffice, invoice.recipient.post)
        }
    }

    @Test
    fun `sending an invoice uses the recipient's invoicing address when it's complete`() {
        val invoicingStreetAddress = "Testikatu 1"
        val invoicingPostalCode = "00100"
        val invoicingPostOffice = "Helsinki"
        val testInvoice =
            testInvoice(
                headOfFamily =
                    testPerson(
                        invoicingStreetAddress = invoicingStreetAddress,
                        invoicingPostalCode = invoicingPostalCode,
                        invoicingPostOffice = invoicingPostOffice
                    )
            )
        val batch =
            EspooInvoiceIntegrationClient.createBatchExports(
                listOf(testInvoice),
                agreementType,
                true
            )
        assertEquals(1, batch.invoices.size)
        batch.invoices.first().let { invoice ->
            assertEquals(testInvoice.headOfFamily.streetAddress, invoice.client.street)
            assertEquals(testInvoice.headOfFamily.postalCode, invoice.client.postalCode)
            assertEquals(testInvoice.headOfFamily.postOffice, invoice.client.post)
            assertEquals(invoicingStreetAddress, invoice.recipient.street)
            assertEquals(invoicingPostalCode, invoice.recipient.postalCode)
            assertEquals(invoicingPostOffice, invoice.recipient.post)
        }
    }

    @Test
    fun `invoice sent to invoicing system has the rows grouped by child`() {
        val firstChild =
            testPerson(
                firstName = "First",
                lastName = "Child",
                dateOfBirth = LocalDate.now().minusDays(1)
            )
        val secondChild =
            testPerson(firstName = "Second", lastName = "Child", dateOfBirth = LocalDate.now())
        val testInvoice =
            testInvoice(
                rows =
                    listOf(
                        testInvoiceRow(child = firstChild),
                        testInvoiceRow(child = secondChild),
                        testInvoiceRow(child = firstChild)
                    )
            )
        val batch =
            EspooInvoiceIntegrationClient.createBatchExports(
                listOf(testInvoice),
                agreementType,
                true
            )
        assertEquals(1, batch.invoices.size)
        batch.invoices.first().let { invoice ->
            assertEquals(7, invoice.rows.size)
            assertEquals(
                "${secondChild.lastName} ${secondChild.firstName}",
                invoice.rows[0].description
            )
            assertEquals("Varhaiskasvatus", invoice.rows[1].description)
            assertEquals("", invoice.rows[2].description)
            assertEquals(
                "${firstChild.lastName} ${firstChild.firstName}",
                invoice.rows[3].description
            )
            assertEquals("Varhaiskasvatus", invoice.rows[4].description)
            assertEquals("Varhaiskasvatus", invoice.rows[5].description)
            assertEquals("", invoice.rows[6].description)
        }
    }

    private fun testInvoice(
        headOfFamily: PersonDetailed = testPerson(),
        codebtor: PersonDetailed? = null,
        rows: List<InvoiceRowDetailed> = listOf(testInvoiceRow())
    ) =
        InvoiceDetailed(
            id = InvoiceId(UUID.randomUUID()),
            status = InvoiceStatus.DRAFT,
            periodStart = LocalDate.of(2020, 1, 1),
            periodEnd = LocalDate.of(2020, 1, 31),
            dueDate = LocalDate.of(2020, 2, 28),
            invoiceDate = LocalDate.of(2020, 2, 14),
            agreementType = agreementType,
            areaId = AreaId(UUID.randomUUID()),
            headOfFamily = headOfFamily,
            codebtor = codebtor,
            number = 1L,
            sentBy = null,
            sentAt = null,
            rows = rows,
            relatedFeeDecisions = emptyList()
        )

    private fun testInvoiceRow(child: PersonDetailed = testPerson()) =
        InvoiceRowDetailed(
            id = InvoiceRowId(UUID.randomUUID()),
            child = child,
            amount = 1,
            unitPrice = 10000,
            periodStart = LocalDate.of(2020, 1, 1),
            periodEnd = LocalDate.of(2020, 1, 31),
            product = EspooInvoiceProducts.Product.DAYCARE.key,
            unitId = DaycareId(UUID.randomUUID()),
            unitName = "Satunnainen päivähoitopaikka",
            unitProviderType = ProviderType.MUNICIPAL,
            daycareType = setOf(CareType.CENTRE),
            costCenter = "12345",
            subCostCenter = "01",
            savedCostCenter = "12345",
            description = "",
            correctionId = null,
            note = null
        )

    private fun testPerson(
        dateOfBirth: LocalDate = LocalDate.of(2000, 1, 1),
        firstName: String = "First name",
        lastName: String = "Last name",
        streetAddress: String = "Address 123",
        postalCode: String = "02100",
        postOffice: String = "Espoo",
        invoicingStreetAddress: String = "",
        invoicingPostalCode: String = "",
        invoicingPostOffice: String = ""
    ) =
        PersonDetailed(
            id = PersonId(UUID.randomUUID()),
            dateOfBirth = dateOfBirth,
            dateOfDeath = null,
            firstName = firstName,
            lastName = lastName,
            ssn = "ssn",
            streetAddress = streetAddress,
            postalCode = postalCode,
            postOffice = postOffice,
            residenceCode = "address_123",
            email = "email@evaka.test",
            phone = "123456",
            language = "fi",
            invoiceRecipientName = "",
            invoicingStreetAddress = invoicingStreetAddress,
            invoicingPostalCode = invoicingPostalCode,
            invoicingPostOffice = invoicingPostOffice,
            restrictedDetailsEnabled = false,
            forceManualFeeDecisions = false
        )
}
