// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.createInvoiceFixture
import fi.espoo.evaka.invoicing.createInvoiceRowFixture
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.service.getInvoicedHeadsOfFamily
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.domain.Period
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAreaCode
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InvoiceQueriesTest : PureJdbiTest() {
    private val testInvoices = listOf(
        createInvoiceFixture(
            status = InvoiceStatus.DRAFT,
            headOfFamilyId = testAdult_1.id,
            agreementType = testAreaCode,
            rows = listOf(createInvoiceRowFixture(childId = testChild_1.id))
        ),
        createInvoiceFixture(
            status = InvoiceStatus.SENT,
            headOfFamilyId = testAdult_1.id,
            agreementType = testAreaCode,
            number = 5000000001L,
            rows = listOf(createInvoiceRowFixture(childId = testChild_2.id))
        ),
        createInvoiceFixture(
            status = InvoiceStatus.DRAFT,
            headOfFamilyId = testAdult_1.id,
            agreementType = testAreaCode,
            period = Period(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 31)),
            rows = listOf(createInvoiceRowFixture(childId = testChild_2.id))
        )
    )

    @BeforeEach
    fun beforeEach() {
        jdbi.handle(::insertGeneralTestFixtures)
    }

    @AfterEach
    fun afterEach() {
        jdbi.handle(::resetDatabase)
    }

    @Test
    fun `search without params`() {
        jdbi.handle { h ->
            upsertInvoices(h, testInvoices)

            val result = searchInvoices(h)
            assertEquals(3, result.size)
        }
    }

    @Test
    fun `search drafts`() {
        jdbi.handle { h ->
            upsertInvoices(h, testInvoices)

            val result = searchInvoices(h, listOf(InvoiceStatus.DRAFT))
            assertEquals(2, result.size)
        }
    }

    @Test
    fun `search canceled`() {
        jdbi.handle { h ->
            upsertInvoices(h, testInvoices)

            val result = searchInvoices(h, listOf(InvoiceStatus.CANCELED))
            assertEquals(0, result.size)
        }
    }

    @Test
    fun `search sent`() {
        jdbi.handle { h ->
            upsertInvoices(h, testInvoices)

            val result = searchInvoices(h, listOf(InvoiceStatus.SENT))
            assertEquals(1, result.size)
        }
    }

    @Test
    fun `get by id`() {
        jdbi.handle { h ->
            upsertInvoices(h, testInvoices)

            val result = getInvoice(h, testInvoices[0].id)
            assertEquals(testInvoices[0], result)
        }
    }

    @Test
    fun `invoice row has child and fee`() {
        jdbi.handle { h ->
            upsertInvoices(h, testInvoices)

            val result = searchInvoices(h, listOf(InvoiceStatus.SENT))
            val invoice = result.get(0)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { row ->
                assertEquals(testChild_2.id, row.child.id)
                assertEquals(28900, row.price())
            }
        }
    }

    @Test
    fun `getMaxInvoiceNumber works with one invoice`() {
        jdbi.handle { h ->
            upsertInvoices(
                h,
                listOf(
                    createInvoiceFixture(
                        status = InvoiceStatus.SENT,
                        headOfFamilyId = testAdult_1.id,
                        agreementType = testAreaCode,
                        number = 5000000123L,
                        rows = listOf(createInvoiceRowFixture(testChild_1.id))
                    )
                )
            )

            val maxNumber = getMaxInvoiceNumber(h)

            assertEquals(5000000123L, maxNumber)
        }
    }

    @Test
    fun `getMaxInvoiceNumber works with several invoices`() {
        jdbi.handle { h ->
            listOf(5000000200L, 5000000300L)
                .map {
                    createInvoiceFixture(
                        status = InvoiceStatus.SENT,
                        headOfFamilyId = testAdult_1.id,
                        agreementType = testAreaCode,
                        number = it,
                        rows = listOf(createInvoiceRowFixture(testChild_1.id))
                    )
                }
                .let { invoices -> upsertInvoices(h, invoices) }

            val maxNumber = getMaxInvoiceNumber(h)

            assertEquals(5000000300L, maxNumber)
        }
    }

    @Test
    fun `get invoiced heads of family`() {
        jdbi.handle { h ->
            upsertInvoices(h, testInvoices)

            val result = getInvoicedHeadsOfFamily(h, Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31)))
            assertEquals(1, result.size)
            assertEquals(listOf(testInvoices[0].headOfFamily.id), result)
        }
    }

    @Test
    fun `get invoiced heads of family period with no invoices`() {
        jdbi.handle { h ->
            upsertInvoices(h, testInvoices)

            val result = getInvoicedHeadsOfFamily(h, Period(LocalDate.of(2019, 6, 1), LocalDate.of(2019, 6, 30)))
            assertEquals(0, result.size)
        }
    }

    @Test
    fun `delete drafts basic case`() {
        jdbi.handle { h ->
            upsertInvoices(h, testInvoices)
            val draft = testInvoices[0]
            deleteDraftInvoices(h, listOf(draft.id))

            val result = getInvoice(h, draft.id)
            assertNull(result)
        }
    }

    @Test
    fun `delete drafts does not delete sent invoices`() {
        jdbi.handle { h ->
            upsertInvoices(h, testInvoices)
            val sent = testInvoices[1]
            deleteDraftInvoices(h, listOf(sent.id))

            val result = getInvoice(h, sent.id)
            assertEquals(sent, result)
        }
    }

    @Test
    fun `get head of family's invoices`() {
        jdbi.handle { h ->
            upsertInvoices(
                h,
                testInvoices.plus(
                    createInvoiceFixture(
                        status = InvoiceStatus.DRAFT,
                        headOfFamilyId = testAdult_2.id,
                        agreementType = testAreaCode,
                        rows = listOf(createInvoiceRowFixture(childId = testChild_1.id))
                    )
                )
            )

            assertEquals(testInvoices.size, getHeadOfFamilyInvoices(h, testAdult_1.id).size)
            assertEquals(1, getHeadOfFamilyInvoices(h, testAdult_2.id).size)
        }
    }
}
