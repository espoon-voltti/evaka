// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HolidayPeriodIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val christmasRange =
        FiniteDateRange(LocalDate.of(2021, 12, 18), LocalDate.of(2022, 1, 6))
    private val christmasDeadline = christmasRange.start.minusWeeks(4)

    @Test
    fun `holiday periods can be created, updated and deleted`() {
        val summer = db.transaction { it.insertHolidayPeriod(summerRange, summerDeadline) }
        val christmas = db.transaction { it.insertHolidayPeriod(christmasRange, christmasDeadline) }

        assertEquals(summer, db.read { it.getHolidayPeriod(summer.id) })

        assertEquals(
            listOf(summer.id, christmas.id),
            db.read { it.getHolidayPeriods() }.map { p -> p.id }
        )

        val newDeadline = christmasDeadline.minusWeeks(1)
        db.transaction { it.updateHolidayPeriod(christmas.id, christmasRange, newDeadline) }
        assertEquals(
            listOf(summer.reservationDeadline, newDeadline),
            db.read { it.getHolidayPeriods() }.map { p -> p.reservationDeadline }
        )

        db.transaction { it.deleteHolidayPeriod(summer.id) }
        assertEquals(listOf(christmas.id), db.read { it.getHolidayPeriods() }.map { p -> p.id })
    }

    @Test
    fun `cannot create overlapping holiday periods`() {
        val summer =
            db.transaction {
                it.insertHolidayPeriod(christmasRange, christmasDeadline)
                it.insertHolidayPeriod(summerRange, summerDeadline)
            }

        assertConstraintViolation {
            db.transaction { it.insertHolidayPeriod(summerRange, summerDeadline) }
        }
        assertConstraintViolation {
            db.transaction { it.updateHolidayPeriod(summer.id, christmasRange, summerDeadline) }
        }
    }

    private fun assertConstraintViolation(executable: () -> Any) {
        assertThrows<UnableToExecuteStatementException> { executable() }
            .also { assertContains(it.message ?: "", "violates exclusion constraint") }
    }
}
