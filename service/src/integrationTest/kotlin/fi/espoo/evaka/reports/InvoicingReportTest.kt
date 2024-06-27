// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.createInvoiceFixture
import fi.espoo.evaka.invoicing.createInvoiceRowFixture
import fi.espoo.evaka.invoicing.data.insertInvoices
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testArea2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InvoicingReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testArea2)
            tx.insert(testDaycare2)
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
                            areaCode = testArea.areaCode!!,
                            amountOfInvoices = 1,
                            totalSumCents = 28900,
                            amountWithoutSSN = 0,
                            amountWithoutAddress = 0,
                            amountWithZeroPrice = 0
                        ),
                        InvoiceReportRow(
                            areaCode = testArea2.areaCode!!,
                            amountOfInvoices = 2,
                            totalSumCents = 28900,
                            amountWithoutSSN = 0,
                            amountWithoutAddress = 0,
                            amountWithZeroPrice = 1
                        )
                    )
            )
        )
    }

    private val testUser =
        AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.FINANCE_ADMIN))

    private fun getAndAssert(date: LocalDate, expected: InvoiceReport) {
        val (_, response, result) =
            http
                .get("/reports/invoices", listOf("date" to date.format(DateTimeFormatter.ISO_DATE)))
                .asUser(testUser)
                .responseObject<InvoiceReport>(jsonMapper)

        assertEquals(200, response.statusCode)
        assertEquals(expected, result.get())
    }

    private val testInvoices =
        listOf(
            createInvoiceFixture(
                status = InvoiceStatus.SENT,
                headOfFamilyId = testAdult_1.id,
                areaId = testArea.id,
                rows =
                    listOf(
                        createInvoiceRowFixture(childId = testChild_1.id, unitId = testDaycare.id)
                    )
            ),
            createInvoiceFixture(
                status = InvoiceStatus.SENT,
                headOfFamilyId = testAdult_2.id,
                areaId = testArea2.id,
                rows =
                    listOf(
                        createInvoiceRowFixture(childId = testChild_2.id, unitId = testDaycare2.id)
                    )
            ),
            createInvoiceFixture(
                status = InvoiceStatus.SENT,
                headOfFamilyId = testAdult_2.id,
                areaId = testArea2.id,
                rows = listOf()
            )
        )

    private fun insertInvoices(date: LocalDate) {
        db.transaction {
            it.insertInvoices(testInvoices)
            @Suppress("DEPRECATION")
            it.createUpdate(
                    """
                UPDATE invoice SET sent_at = :sentAt
                """
                        .trimIndent()
                )
                .bind("sentAt", date)
                .execute()
        }
    }
}
