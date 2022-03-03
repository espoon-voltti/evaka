// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import com.github.kittinunf.fuel.core.extensions.jsonBody
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.insertPlacement
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.withMockedTime
import org.jdbi.v3.core.kotlin.mapTo
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

            tx.insertPlacement(PlacementType.DAYCARE, child.id, testDaycare.id, mockToday.minusYears(2), mockToday.plusYears(1))
        }
    }

    @Test
    fun `holiday absences cannot be saved when no holiday period is active`() {
        reportFreePeriods(
            freeAbsence(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 5)
            ),
            400
        )
    }

    @Test
    fun `free absences cannot be saved outside of a free period`() {
        createHolidayPeriod(summerPeriodWithFreePeriod)
        reportFreePeriods(
            freeAbsence(
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
        reportFreePeriods(
            freeAbsence(firstOption.start, firstOption.end),
            400,
            summerPeriodWithFreePeriod.freePeriod!!.deadline.plusDays(1)
        )
    }

    @Test
    fun `can save free absence in a free period before the deadline`() {
        createHolidayPeriod(summerPeriodWithFreePeriod)
        val firstOption = summerPeriodWithFreePeriod.freePeriod!!.periodOptions[0]
        reportFreePeriods(freeAbsence(firstOption.start, firstOption.end), 200)

        val expected = firstOption.dates().map { Absence(child.id, it, AbsenceType.FREE_ABSENCE) }
        assertEquals(expected.toList(), db.read { it.getAllAbsences() })
    }

    private fun reportFreePeriods(
        body: FreePeriodsBody,
        expectedStatus: Int = 200,
        mockedDay: LocalDate = mockToday
    ) {
        http.post("/citizen/holiday-period/holidays/free-period")
            .jsonBody(jsonMapper.writeValueAsString(body))
            .asUser(authenticatedParent)
            .withMockedTime(HelsinkiDateTime.of(mockedDay, LocalTime.of(0, 0)))
            .response()
            .also { assertEquals(expectedStatus, it.second.statusCode) }
    }

    private fun freeAbsence(start: LocalDate, end: LocalDate) =
        FreePeriodsBody(mapOf(child.id to FiniteDateRange(start, end)))

    private fun createHolidayPeriod(period: HolidayPeriodBody) = db.transaction { it.createHolidayPeriod(period) }

    private data class Absence(val childId: ChildId, val date: LocalDate, val type: AbsenceType)
    private fun Database.Read.getAllAbsences(): List<Absence> =
        createQuery("SELECT a.child_id, a.date, a.absence_type as type FROM absence a ORDER BY date")
            .mapTo<Absence>()
            .list()
}
