// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevInvoice
import fi.espoo.evaka.shared.dev.DevInvoiceRow
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class InvoicingReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var invoiceReportController: InvoiceReportController

    private val area1 = DevCareArea(areaCode = 100, name = "Area 1", shortName = "area1")
    private val area2 = DevCareArea(areaCode = 200, name = "Area 2", shortName = "area2")
    private val daycare1 = DevDaycare(areaId = area1.id)
    private val daycare2 = DevDaycare(areaId = area2.id)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area1)
            tx.insert(area2)
            tx.insert(daycare1)
            tx.insert(daycare2)
            listOf(testAdult_1, testAdult_2).forEach { tx.insert(it, DevPersonType.ADULT) }
            listOf(testChild_1, testChild_2).forEach { tx.insert(it, DevPersonType.CHILD) }
        }
    }

    @Test
    fun `simple case of three invoices`() {
        val date = LocalDate.of(2019, 1, 1)
        insertInvoices(date)

        getAndAssert(
            date,
            InvoiceReport(
                reportRows =
                    listOf(
                        InvoiceReportRow(
                            areaCode = area1.areaCode!!,
                            amountOfInvoices = 1,
                            totalSumCents = 28900,
                            amountWithoutSSN = 0,
                            amountWithoutAddress = 0,
                            amountWithZeroPrice = 0,
                        ),
                        InvoiceReportRow(
                            areaCode = area2.areaCode!!,
                            amountOfInvoices = 2,
                            totalSumCents = 28900,
                            amountWithoutSSN = 0,
                            amountWithoutAddress = 0,
                            amountWithZeroPrice = 1,
                        ),
                    )
            ),
        )
    }

    private val testUser =
        AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.FINANCE_ADMIN))

    private fun getAndAssert(date: LocalDate, expected: InvoiceReport) {
        val result =
            invoiceReportController.getInvoiceReport(
                dbInstance(),
                testUser,
                MockEvakaClock(2024, 10, 31, 12, 0, 0),
                date,
            )
        assertEquals(expected, result)
    }

    private val testInvoices =
        listOf(
            DevInvoice(
                status = InvoiceStatus.SENT,
                headOfFamilyId = testAdult_1.id,
                areaId = area1.id,
                rows = listOf(DevInvoiceRow(childId = testChild_1.id, unitId = daycare1.id)),
            ),
            DevInvoice(
                status = InvoiceStatus.SENT,
                headOfFamilyId = testAdult_2.id,
                areaId = area2.id,
                rows = listOf(DevInvoiceRow(childId = testChild_2.id, unitId = daycare2.id)),
            ),
            DevInvoice(
                status = InvoiceStatus.SENT,
                headOfFamilyId = testAdult_2.id,
                areaId = area2.id,
                rows = listOf(),
            ),
        )

    private fun insertInvoices(date: LocalDate) {
        db.transaction {
            it.insert(testInvoices)
            it.execute { sql("UPDATE invoice SET sent_at = ${bind(date)}") }
        }
    }
}
