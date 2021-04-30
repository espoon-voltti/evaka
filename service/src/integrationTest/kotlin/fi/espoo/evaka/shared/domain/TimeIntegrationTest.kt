// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.testAreaId
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.util.UUID

class TimeIntegrationTest : PureJdbiTest() {
    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            insertGeneralTestFixtures(tx.handle)
        }
    }

    @AfterEach
    fun afterEach() {
        db.transaction { tx ->
            tx.resetDatabase()
        }
    }

    @Test
    fun `operational days with no holidays or round the clock units in database`() {
        val result = db.transaction { operationalDays(it.handle, 2020, Month.JANUARY) }

        assertEquals(january2020Weekdays, result.generalCase)
        assertEquals(january2020Weekdays, result.forUnit(testDaycare.id))
    }

    @Test
    fun `operational days with no holidays and a round the clock unit in database`() {
        db.transaction { tx ->
            tx.createUpdate("UPDATE daycare SET operation_days = '{1, 2, 3, 4, 5, 6, 7}' WHERE id = :id")
                .bind("id", testDaycare.id)
                .execute()
        }

        val result = db.transaction { operationalDays(it.handle, 2020, Month.JANUARY) }

        assertEquals(january2020Weekdays, result.generalCase)
        assertEquals(january2020Weekdays, result.forUnit(testDaycare2.id))
        assertEquals(january2020Days, result.forUnit(testDaycare.id))
    }

    @Test
    fun `operational days with no holidays and multiple round the clock units in database`() {
        var secondUnitId: UUID? = null
        var thirdUnitId: UUID? = null
        db.transaction { tx ->
            secondUnitId = tx.handle.insertTestDaycare(DevDaycare(areaId = testAreaId, name = "second round the clock unit"))
            thirdUnitId = tx.handle.insertTestDaycare(DevDaycare(areaId = testAreaId, name = "third round the clock unit"))
            tx.createUpdate("UPDATE daycare SET operation_days = '{1, 2, 3, 4, 5, 6, 7}' WHERE id = ANY(:ids)")
                .bind("ids", arrayOf(secondUnitId, thirdUnitId))
                .execute()
        }

        val result = db.transaction { operationalDays(it.handle, 2020, Month.JANUARY) }

        assertEquals(january2020Weekdays, result.generalCase)
        assertEquals(january2020Weekdays, result.forUnit(testDaycare.id))
        assertEquals(january2020Days, result.forUnit(secondUnitId!!))
        assertEquals(january2020Days, result.forUnit(thirdUnitId!!))
    }

    @Test
    fun `operational days with a weekday holiday and no round the clock units in database`() {
        val newYear = LocalDate.of(2020, 1, 1)
        db.transaction { tx ->
            tx.createUpdate("INSERT INTO holiday (date) VALUES (:date)")
                .bind("date", newYear)
                .execute()
        }

        val result = db.transaction { operationalDays(it.handle, 2020, Month.JANUARY) }

        val weekdaysWithoutHolidays = january2020Weekdays.filter { it != newYear }
        assertEquals(weekdaysWithoutHolidays, result.generalCase)
        assertEquals(weekdaysWithoutHolidays, result.forUnit(testDaycare.id))
    }

    @Test
    fun `operational days with multiple weekday holidays and no round the clock units in database`() {
        val newYear = LocalDate.of(2020, 1, 1)
        val epiphany = LocalDate.of(2020, 1, 6)
        db.transaction { tx ->
            tx.createUpdate("INSERT INTO holiday (date) VALUES (:date1), (:date2)")
                .bind("date1", newYear)
                .bind("date2", epiphany)
                .execute()
        }

        val result = db.transaction { operationalDays(it.handle, 2020, Month.JANUARY) }

        val weekdaysWithoutHolidays = january2020Weekdays.filter { it != newYear && it != epiphany }
        assertEquals(weekdaysWithoutHolidays, result.generalCase)
        assertEquals(weekdaysWithoutHolidays, result.forUnit(testDaycare.id))
    }

    @Test
    fun `operational days with a weekend holiday and no round the clock units in database`() {
        val someHoliday = LocalDate.of(2020, 1, 4)
        db.transaction { tx ->
            tx.createUpdate("INSERT INTO holiday (date) VALUES (:date)")
                .bind("date", someHoliday)
                .execute()
        }

        val result = db.transaction { operationalDays(it.handle, 2020, Month.JANUARY) }

        assertEquals(january2020Weekdays, result.generalCase)
        assertEquals(january2020Weekdays, result.forUnit(testDaycare.id))
    }

    @Test
    fun `operational days with multiple weekday holidays and a round the clock unit in database`() {
        val newYear = LocalDate.of(2020, 1, 1)
        val epiphany = LocalDate.of(2020, 1, 6)
        db.transaction { tx ->
            tx.createUpdate("INSERT INTO holiday (date) VALUES (:date1), (:date2)")
                .bind("date1", newYear)
                .bind("date2", epiphany)
                .execute()

            tx.createUpdate("UPDATE daycare SET operation_days = '{1, 2, 3, 4, 5, 6, 7}' WHERE id = :id")
                .bind("id", testDaycare.id)
                .execute()
        }

        val result = db.transaction { operationalDays(it.handle, 2020, Month.JANUARY) }

        val weekdaysWithoutHolidays = january2020Weekdays.filter { it != newYear && it != epiphany }
        assertEquals(weekdaysWithoutHolidays, result.generalCase)
        assertEquals(weekdaysWithoutHolidays, result.forUnit(testDaycare2.id))
        assertEquals(january2020Days, result.forUnit(testDaycare.id))
    }

    private val january2020Days = listOf(
        LocalDate.of(2020, 1, 1),
        LocalDate.of(2020, 1, 2),
        LocalDate.of(2020, 1, 3),
        LocalDate.of(2020, 1, 4),
        LocalDate.of(2020, 1, 5),
        LocalDate.of(2020, 1, 6),
        LocalDate.of(2020, 1, 7),
        LocalDate.of(2020, 1, 8),
        LocalDate.of(2020, 1, 9),
        LocalDate.of(2020, 1, 10),
        LocalDate.of(2020, 1, 11),
        LocalDate.of(2020, 1, 12),
        LocalDate.of(2020, 1, 13),
        LocalDate.of(2020, 1, 14),
        LocalDate.of(2020, 1, 15),
        LocalDate.of(2020, 1, 16),
        LocalDate.of(2020, 1, 17),
        LocalDate.of(2020, 1, 18),
        LocalDate.of(2020, 1, 19),
        LocalDate.of(2020, 1, 20),
        LocalDate.of(2020, 1, 21),
        LocalDate.of(2020, 1, 22),
        LocalDate.of(2020, 1, 23),
        LocalDate.of(2020, 1, 24),
        LocalDate.of(2020, 1, 25),
        LocalDate.of(2020, 1, 26),
        LocalDate.of(2020, 1, 27),
        LocalDate.of(2020, 1, 28),
        LocalDate.of(2020, 1, 29),
        LocalDate.of(2020, 1, 30),
        LocalDate.of(2020, 1, 31)
    )

    private val january2020Weekdays = listOf(
        LocalDate.of(2020, 1, 1),
        LocalDate.of(2020, 1, 2),
        LocalDate.of(2020, 1, 3),
        LocalDate.of(2020, 1, 6),
        LocalDate.of(2020, 1, 7),
        LocalDate.of(2020, 1, 8),
        LocalDate.of(2020, 1, 9),
        LocalDate.of(2020, 1, 10),
        LocalDate.of(2020, 1, 13),
        LocalDate.of(2020, 1, 14),
        LocalDate.of(2020, 1, 15),
        LocalDate.of(2020, 1, 16),
        LocalDate.of(2020, 1, 17),
        LocalDate.of(2020, 1, 20),
        LocalDate.of(2020, 1, 21),
        LocalDate.of(2020, 1, 22),
        LocalDate.of(2020, 1, 23),
        LocalDate.of(2020, 1, 24),
        LocalDate.of(2020, 1, 27),
        LocalDate.of(2020, 1, 28),
        LocalDate.of(2020, 1, 29),
        LocalDate.of(2020, 1, 30),
        LocalDate.of(2020, 1, 31)
    )
}
