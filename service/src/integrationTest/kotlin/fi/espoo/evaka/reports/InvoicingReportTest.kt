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
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalTime
import java.time.YearMonth
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
    private val adult1 =
        DevPerson(
            ssn = "010180-1232",
            streetAddress = "Testikatu 1",
            postalCode = "02770",
            postOffice = "Espoo",
        )
    private val adult2 =
        DevPerson(
            ssn = "010180-2357",
            streetAddress = "Testikatu 2",
            postalCode = "02770",
            postOffice = "Espoo",
        )
    private val child1 = DevPerson()
    private val child2 = DevPerson()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area1)
            tx.insert(area2)
            tx.insert(daycare1)
            tx.insert(daycare2)
            listOf(adult1, adult2).forEach { tx.insert(it, DevPersonType.ADULT) }
            listOf(child1, child2).forEach { tx.insert(it, DevPersonType.CHILD) }
        }
    }

    @Test
    fun `simple case of three invoices`() {
        val yearMonth = YearMonth.of(2018, 12)
        insertInvoices(yearMonth)

        getAndAssert(
            yearMonth,
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

    private fun getAndAssert(yearMonth: YearMonth, expected: InvoiceReport) {
        val result =
            invoiceReportController.getInvoiceReport(
                dbInstance(),
                testUser,
                MockEvakaClock(2024, 10, 31, 12, 0, 0),
                yearMonth,
            )
        assertEquals(expected, result)
    }

    private val testInvoices =
        listOf(
            DevInvoice(
                status = InvoiceStatus.SENT,
                headOfFamilyId = adult1.id,
                areaId = area1.id,
                rows = listOf(DevInvoiceRow(childId = child1.id, unitId = daycare1.id)),
            ),
            DevInvoice(
                status = InvoiceStatus.SENT,
                headOfFamilyId = adult2.id,
                areaId = area2.id,
                rows = listOf(DevInvoiceRow(childId = child2.id, unitId = daycare2.id)),
            ),
            DevInvoice(
                status = InvoiceStatus.SENT,
                headOfFamilyId = adult2.id,
                areaId = area2.id,
                rows = listOf(),
            ),
        )

    private fun insertInvoices(period: YearMonth) {
        val range = FiniteDateRange.ofMonth(period)
        val invoiceDate = range.start.plusMonths(1)
        db.transaction {
            it.insert(
                testInvoices.map { invoice ->
                    invoice.copy(
                        invoiceDate = invoiceDate,
                        dueDate = invoiceDate.plusWeeks(2),
                        periodStart = range.start,
                        periodEnd = range.end,
                        sentAt = HelsinkiDateTime.of(invoiceDate, LocalTime.of(11, 12)),
                    )
                }
            )
        }
    }
}
