// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.HolidayPeriodId
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Translatable
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import kotlin.test.assertContains
import kotlin.test.assertEquals

class HolidayPeriodIntegrationTest : PureJdbiTest() {

    private val emptyTranslatable = Translatable("", "", "")
    private val summerRange = FiniteDateRange(start = LocalDate.of(2021, 6, 1), end = LocalDate.of(2021, 8, 31))
    private val summerPeriod = HolidayPeriodBody(
        period = summerRange,
        description = Translatable("Varaathan \n 'quote' \"double\" loma-aikasi", "", ""),
        descriptionLink = emptyTranslatable,
        showReservationBannerFrom = summerRange.start.minusWeeks(6),
        reservationDeadline = summerRange.start.minusWeeks(3),
        freePeriod = null
    )
    private val christmasRange =
        FiniteDateRange(start = LocalDate.of(2021, 12, 18), end = LocalDate.of(2022, 1, 6))
    private val christmasPeriod =
        summerPeriod.copy(period = christmasRange, showReservationBannerFrom = christmasRange.start.minusMonths(1))

    @BeforeEach
    private fun prepare() {
        db.transaction { it.resetDatabase() }
    }

    @Test
    fun `holiday periods can be created, updated and deleted`() {
        val summer = createHolidayPeriod(summerPeriod)
        val christmas = createHolidayPeriod(christmasPeriod)

        assertEquals(summer, getHolidayPeriod(summer.id))

        assertEquals(listOf(summer.id, christmas.id), getHolidayPeriods().map { p -> p.id })

        val newLinks = Translatable(
            fi = "https://www.example.com",
            sv = "https://www.example.com/sv",
            en = "https://www.example.com/en",
        )
        updateHolidayPeriod(christmas.id, christmasPeriod.copy(descriptionLink = newLinks))
        val periods = getHolidayPeriods()
        assertEquals(listOf(emptyTranslatable, newLinks), periods.map { p -> p.descriptionLink })
        assertEquals(listOf(summerPeriod.freePeriod, summerPeriod.freePeriod), periods.map { p -> p.freePeriod })

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

    @Test
    fun `holiday period description serialization works as expected`() {
        val descriptionsWithSpecialCharacters = Translatable(
            fi = "Syötä loma-ajat ja katso https://www.example.com",
            sv = """'single quotes' "double quotes" \backslash
            new lines \n \r
        """,
            en = ""
        )
        val created = createHolidayPeriod(summerPeriod.copy(description = descriptionsWithSpecialCharacters))

        assertEquals(descriptionsWithSpecialCharacters, created.description)
    }

    @Test
    fun `holiday periods can contain a free period`() {
        val period = summerPeriod.copy(
            freePeriod = FreeAbsencePeriod(
                deadline = LocalDate.of(2022, 6, 1),
                periodOptions = listOf(
                    FiniteDateRange(LocalDate.of(2022, 7, 1), LocalDate.of(2022, 7, 7)),
                    FiniteDateRange(LocalDate.of(2022, 7, 8), LocalDate.of(2022, 7, 14)),
                ),
                periodOptionLabel = emptyTranslatable,
                questionLabel = emptyTranslatable
            )
        )
        val created = createHolidayPeriod(period)

        assertEquals(period.freePeriod, created.freePeriod)
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
