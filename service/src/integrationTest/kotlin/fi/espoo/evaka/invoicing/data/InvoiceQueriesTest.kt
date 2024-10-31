// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.service.getInvoicedHeadsOfFamily
import fi.espoo.evaka.shared.dev.DevInvoice
import fi.espoo.evaka.shared.dev.DevInvoiceRow
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InvoiceQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val testInvoices =
        listOf(
            DevInvoice(
                status = InvoiceStatus.DRAFT,
                headOfFamilyId = testAdult_1.id,
                areaId = testArea.id,
                rows = listOf(DevInvoiceRow(childId = testChild_1.id, unitId = testDaycare.id)),
            ),
            DevInvoice(
                status = InvoiceStatus.SENT,
                headOfFamilyId = testAdult_1.id,
                areaId = testArea.id,
                number = 5000000001L,
                rows = listOf(DevInvoiceRow(childId = testChild_2.id, unitId = testDaycare.id)),
            ),
            DevInvoice(
                status = InvoiceStatus.DRAFT,
                headOfFamilyId = testAdult_1.id,
                areaId = testArea.id,
                periodStart = LocalDate.of(2018, 1, 1),
                periodEnd = LocalDate.of(2018, 1, 31),
                rows = listOf(DevInvoiceRow(childId = testChild_2.id, unitId = testDaycare.id)),
            ),
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(testDaycare)
            listOf(testAdult_1, testAdult_2).forEach { tx.insert(it, DevPersonType.ADULT) }
            listOf(testChild_1, testChild_2).forEach { tx.insert(it, DevPersonType.CHILD) }
        }
    }

    @Test
    fun `search without params`() {
        db.transaction { tx ->
            tx.insert(testInvoices)

            val result = tx.searchInvoices()
            assertEquals(3, result.size)
        }
    }

    @Test
    fun `search drafts`() {
        db.transaction { tx ->
            tx.insert(testInvoices)

            val result = tx.searchInvoices(InvoiceStatus.DRAFT)
            assertEquals(2, result.size)
        }
    }

    @Test
    fun `search canceled`() {
        db.transaction { tx ->
            tx.insert(testInvoices)

            val result = tx.searchInvoices(InvoiceStatus.CANCELED)
            assertEquals(0, result.size)
        }
    }

    @Test
    fun `search sent`() {
        db.transaction { tx ->
            tx.insert(testInvoices)

            val result = tx.searchInvoices(InvoiceStatus.SENT)
            assertEquals(1, result.size)
        }
    }

    @Test
    fun `get by id`() {
        db.transaction { tx ->
            val ids = tx.insert(testInvoices)

            val result = tx.getInvoice(ids[0])
            assertEquals(testInvoices[0].id, result?.id)
        }
    }

    @Test
    fun `invoice row has child and fee`() {
        db.transaction { tx ->
            tx.insert(testInvoices)

            val result = tx.searchInvoices(InvoiceStatus.SENT)
            val invoice = result[0]
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { row ->
                assertEquals(testChild_2.id, row.child.id)
                assertEquals(28900, row.price)
            }
        }
    }

    @Test
    fun `getMaxInvoiceNumber works with one invoice`() {
        db.transaction { tx ->
            tx.insert(
                listOf(
                    DevInvoice(
                        status = InvoiceStatus.SENT,
                        headOfFamilyId = testAdult_1.id,
                        areaId = testArea.id,
                        number = 5000000123L,
                        rows =
                            listOf(DevInvoiceRow(childId = testChild_1.id, unitId = testDaycare.id)),
                    )
                )
            )

            val maxNumber = tx.getMaxInvoiceNumber()

            assertEquals(5000000123L, maxNumber)
        }
    }

    @Test
    fun `getMaxInvoiceNumber works with several invoices`() {
        db.transaction { tx ->
            listOf(5000000200L, 5000000300L)
                .map {
                    DevInvoice(
                        status = InvoiceStatus.SENT,
                        headOfFamilyId = testAdult_1.id,
                        areaId = testArea.id,
                        number = it,
                        rows =
                            listOf(DevInvoiceRow(childId = testChild_1.id, unitId = testDaycare.id)),
                    )
                }
                .let { invoices -> tx.insert(invoices) }

            val maxNumber = tx.getMaxInvoiceNumber()

            assertEquals(5000000300L, maxNumber)
        }
    }

    @Test
    fun `get invoiced heads of family`() {
        db.transaction { tx ->
            tx.insert(testInvoices)

            val result =
                tx.getInvoicedHeadsOfFamily(
                    FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
                )
            assertEquals(1, result.size)
            assertEquals(setOf(testInvoices[0].headOfFamilyId), result)
        }
    }

    @Test
    fun `get invoiced heads of family period with no invoices`() {
        db.transaction { tx ->
            tx.insert(testInvoices)

            val result =
                tx.getInvoicedHeadsOfFamily(
                    FiniteDateRange(LocalDate.of(2019, 6, 1), LocalDate.of(2019, 6, 30))
                )
            assertEquals(0, result.size)
        }
    }

    @Test
    fun `delete drafts basic case`() {
        db.transaction { tx ->
            tx.insert(testInvoices)
            val draft = testInvoices[0]
            tx.deleteDraftInvoices(listOf(draft.id))

            val result = tx.getInvoice(draft.id)
            assertNull(result)
        }
    }

    @Test
    fun `delete drafts does not delete sent invoices`() {
        db.transaction { tx ->
            val sentIds = tx.insert(testInvoices)
            val sent = testInvoices[1]
            tx.deleteDraftInvoices(sentIds)

            val result = tx.getInvoice(sent.id)
            assertEquals(sent.id, result?.id)
        }
    }

    @Test
    fun `get head of family's invoices`() {
        db.transaction { tx ->
            tx.insert(
                testInvoices.plus(
                    DevInvoice(
                        status = InvoiceStatus.DRAFT,
                        headOfFamilyId = testAdult_2.id,
                        areaId = testArea.id,
                        rows =
                            listOf(DevInvoiceRow(childId = testChild_1.id, unitId = testDaycare.id)),
                    )
                )
            )

            assertEquals(testInvoices.size, tx.getHeadOfFamilyInvoices(testAdult_1.id).size)
            assertEquals(1, tx.getHeadOfFamilyInvoices(testAdult_2.id).size)
        }
    }
}
