// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.HolidayPeriodId
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import kotlin.test.assertContains
import kotlin.test.assertEquals

class HolidayPeriodIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {

    private val christmasRange =
        FiniteDateRange(start = LocalDate.of(2021, 12, 18), end = LocalDate.of(2022, 1, 6))
    private val christmasPeriod =
        summerPeriod.copy(period = christmasRange)

    @Test
    fun `holiday periods can be created, updated and deleted`() {
        val summer = createHolidayPeriod(summerPeriod)
        val christmas = createHolidayPeriod(christmasPeriod)

        assertEquals(summer, getHolidayPeriod(summer.id))

        assertEquals(listOf(summer.id, christmas.id), getHolidayPeriods().map { p -> p.id })

        updateHolidayPeriod(christmas.id, christmasPeriod.copy(reservationDeadline = null))
        assertEquals(listOf(summer.reservationDeadline, null), getHolidayPeriods().map { p -> p.reservationDeadline })

        deleteHolidayPeriod(summer.id)
        assertEquals(listOf(christmas.id), getHolidayPeriods().map { p -> p.id })
    }

    @Test
    fun `cannot create overlapping holiday periods`() {
        val summer = createHolidayPeriod(summerPeriod)
        createHolidayPeriod(christmasPeriod)

        assertConstraintViolation { createHolidayPeriod(summerPeriod) }
        assertConstraintViolation { updateHolidayPeriod(summer.id, summerPeriod.copy(period = christmasRange)) }
    }

    private fun assertConstraintViolation(executable: () -> Any) {
        assertThrows<UnableToExecuteStatementException> { executable() }
            .also { assertContains(it.message ?: "", "violates exclusion constraint") }
    }

    private fun getHolidayPeriod(id: HolidayPeriodId) = db.read { it.getHolidayPeriod(id) }
    private fun getHolidayPeriods(): List<HolidayPeriod> = db.read { it.getHolidayPeriods() }
    private fun createHolidayPeriod(period: HolidayPeriodBody) = db.transaction { it.createHolidayPeriod(period) }
    private fun updateHolidayPeriod(id: HolidayPeriodId, period: HolidayPeriodBody) = db.transaction { it.updateHolidayPeriod(id, period) }
    private fun deleteHolidayPeriod(id: HolidayPeriodId) = db.transaction { it.deleteHolidayPeriod(id) }
}
