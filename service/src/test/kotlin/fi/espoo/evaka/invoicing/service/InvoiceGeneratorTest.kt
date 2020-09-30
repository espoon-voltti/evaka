// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.testDaycareCodes
import fi.espoo.evaka.invoicing.testDecision1
import fi.espoo.evaka.invoicing.testDecision2
import fi.espoo.evaka.shared.domain.OperationalDays
import fi.espoo.evaka.shared.domain.Period
import fi.espoo.evaka.shared.domain.isWeekday
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

val splittedDecision1: List<FeeDecision> = listOf(
    testDecision1.copy(
        validFrom = LocalDate.of(2019, 5, 1),
        validTo = LocalDate.of(2019, 5, 15),
        decisionNumber = 2,
        status = FeeDecisionStatus.SENT
    ),
    testDecision1.copy(
        validFrom = LocalDate.of(2019, 5, 16),
        validTo = LocalDate.of(2019, 5, 31),
        decisionNumber = 22,
        status = FeeDecisionStatus.SENT
    )
)

val splittedDecision2: List<FeeDecision> = listOf(
    testDecision2.copy(
        validFrom = LocalDate.of(2019, 5, 1),
        validTo = LocalDate.of(2019, 5, 15),
        decisionNumber = 3,
        status = FeeDecisionStatus.SENT
    ),
    testDecision2.copy(
        validFrom = LocalDate.of(2019, 5, 16),
        validTo = LocalDate.of(2019, 5, 31),
        decisionNumber = 33,
        status = FeeDecisionStatus.SENT
    )
)

val may2019operationalDays = generateSequence(LocalDate.of(2019, 5, 2)) { it.plusDays(1) }
    .takeWhile { it < LocalDate.of(2019, 6, 1) }
    .filter(isWeekday)
    .filterNot { it == LocalDate.of(2019, 5, 30) } // ascension thursday
    .toList()

class InvoiceGeneratorTest {
    @Test
    fun `a set of sent decisions leads to one invoice for each head of family`() {
        val invoices = generateDraftInvoices(
            (splittedDecision1 + splittedDecision2).groupBy { it.headOfFamily.id },
            mapOf(),
            Period(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31)),
            testDaycareCodes,
            OperationalDays(may2019operationalDays, mapOf())
        )

        assertEquals(2, invoices.size)
        assertEquals(2, invoices.distinctBy { it.headOfFamily.id }.size)
    }

    @Test
    fun `invoices have rows for each child with rows with same placements and prices merged`() {
        val invoices = generateDraftInvoices(
            (splittedDecision1 + splittedDecision2).groupBy { it.headOfFamily.id },
            mapOf(),
            Period(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31)),
            testDaycareCodes,
            OperationalDays(may2019operationalDays, mapOf())
        )

        with(invoices.first()) {
            assertEquals(2, rows.size)
            rows.forEach {
                assertEquals(LocalDate.of(2019, 5, 1), it.periodStart)
                assertEquals(LocalDate.of(2019, 5, 31), it.periodEnd)
            }
        }
        with(invoices.last()) {
            assertEquals(1, rows.size)
            rows.forEach {
                assertEquals(LocalDate.of(2019, 5, 1), it.periodStart)
                assertEquals(LocalDate.of(2019, 5, 31), it.periodEnd)
            }
        }
    }

    @Test
    fun `invoices don't lose children`() {
        val decisions = (splittedDecision1 + splittedDecision2)
        val invoices = generateDraftInvoices(
            decisions.groupBy { it.headOfFamily.id },
            mapOf(),
            Period(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31)),
            testDaycareCodes,
            OperationalDays(may2019operationalDays, mapOf())
        )

        val expectedChildren =
            decisions.filter { it.status == FeeDecisionStatus.SENT }.flatMap { it.parts.map { part -> part.child.id } }
        val foundChildren = invoices.flatMap { it.rows.map { row -> row.child.id } }

        assertEquals(expectedChildren.toSet(), foundChildren.toSet())
    }

    @Test
    fun `calculateDailyPriceForInvoiceRow works as expected`() {
        val result = calculateDailyPriceForInvoiceRow(28900, may2019operationalDays)

        assertEquals(1376, result)
    }
}
