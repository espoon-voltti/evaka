// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import com.github.kittinunf.fuel.core.extensions.jsonBody
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.withMockedTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals

class HolidayPeriodControllerCitizenIntegrationTest : FullApplicationTest() {
    private val mockToday: LocalDate = summerPeriodWithFreePeriod.freePeriod!!.deadline.minusWeeks(2)

    private final val child = testChild_1
    private final val parent = testAdult_1
    private final val authenticatedParent = AuthenticatedUser.Citizen(parent.id.raw)

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()

            tx.insertGuardian(parent.id, child.id)
        }
    }

    @Test
    fun `holiday absences cannot be saved when no holiday period is active`() {
        reportFreeHoliday(
            holidayAbsence(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 5)
            ),
            400
        )
    }

    @Test
    fun `free absences cannot be saved outside of a free period`() {
        createHolidayPeriod(summerPeriodWithFreePeriod)
        reportFreeHoliday(
            holidayAbsence(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 5)
            ),
            400
        )
    }

    @Test
    fun `free absences cannot be saved after the deadline`() {
        createHolidayPeriod(summerPeriodWithFreePeriod)
        val firstOption = summerPeriodWithFreePeriod.freePeriod!!.periodOptions[0]
        reportFreeHoliday(
            holidayAbsence(firstOption.start, firstOption.end),
            400,
            summerPeriodWithFreePeriod.freePeriod!!.deadline.plusDays(1)
        )
    }

    @Test
    fun `can save free absence in a free period before the deadline`() {
        createHolidayPeriod(summerPeriodWithFreePeriod)
        val firstOption = summerPeriodWithFreePeriod.freePeriod!!.periodOptions[0]
        reportFreeHoliday(holidayAbsence(firstOption.start, firstOption.end), 200)
    }

    private fun reportFreeHoliday(
        absenceBody: HolidayAbsenceRequest,
        expectedStatus: Int = 200,
        mockedDay: LocalDate = mockToday
    ) {
        http.post("/citizen/holiday-period/holidays")
            .jsonBody(jsonMapper.writeValueAsString(absenceBody))
            .asUser(authenticatedParent)
            .withMockedTime(HelsinkiDateTime.of(mockedDay, LocalTime.of(0, 0)))
            .response()
            .also { assertEquals(expectedStatus, it.second.statusCode) }
    }

    private fun holidayAbsence(start: LocalDate, end: LocalDate) =
        HolidayAbsenceRequest(mapOf(child.id to FiniteDateRange(start, end)))

    private fun createHolidayPeriod(period: HolidayPeriodBody) = db.transaction { it.createHolidayPeriod(period) }
}
