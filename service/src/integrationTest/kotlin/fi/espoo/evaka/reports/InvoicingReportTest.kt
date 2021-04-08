// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.createInvoiceFixture
import fi.espoo.evaka.invoicing.createInvoiceRowFixture
import fi.espoo.evaka.invoicing.data.upsertInvoices
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testArea2Code
import fi.espoo.evaka.testAreaCode
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class InvoicingReportTest : FullApplicationTest() {
    @BeforeEach
    fun beforeEach() {
        jdbi.handle(::insertGeneralTestFixtures)
    }

    @AfterEach
    fun afterEach() {
        jdbi.handle(::resetDatabase)
    }

    @Test
    fun `simple case of three invoices`() {
        val date = LocalDate.of(2019, 1, 1)
        insertInvoices(date)

        getAndAssert(
            date,
            InvoiceReport(
                reportRows = listOf(
                    InvoiceReportRow(
                        areaCode = testAreaCode,
                        amountOfInvoices = 1,
                        totalSumCents = 28900,
                        amountWithoutSSN = 0,
                        amountWithoutAddress = 0,
                        amountWithZeroPrice = 0
                    ),
                    InvoiceReportRow(
                        areaCode = testArea2Code,
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

    private val testUser = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.DIRECTOR))

    private fun getAndAssert(date: LocalDate, expected: InvoiceReport) {
        val (_, response, result) = http.get(
            "/reports/invoices",
            listOf("date" to date.format(DateTimeFormatter.ISO_DATE))
        )
            .asUser(testUser)
            .responseObject<InvoiceReport>(objectMapper)

        assertEquals(200, response.statusCode)
        assertEquals(expected, result.get())
    }

    private val testInvoices = listOf(
        createInvoiceFixture(
            status = InvoiceStatus.SENT,
            headOfFamilyId = testAdult_1.id,
            agreementType = testAreaCode,
            rows = listOf(createInvoiceRowFixture(childId = testChild_1.id))
        ),
        createInvoiceFixture(
            status = InvoiceStatus.SENT,
            headOfFamilyId = testAdult_2.id,
            agreementType = testArea2Code,
            rows = listOf(createInvoiceRowFixture(childId = testChild_2.id))
        ),
        createInvoiceFixture(
            status = InvoiceStatus.SENT,
            headOfFamilyId = testAdult_2.id,
            agreementType = testArea2Code,
            rows = listOf()
        )
    )

    private fun insertInvoices(date: LocalDate) {
        jdbi.handle { h ->
            upsertInvoices(h, testInvoices)
            h.createUpdate(
                """
                UPDATE invoice SET sent_at = :sentAt
                """.trimIndent()
            )
                .bind("sentAt", date)
                .execute()
        }
    }
}
