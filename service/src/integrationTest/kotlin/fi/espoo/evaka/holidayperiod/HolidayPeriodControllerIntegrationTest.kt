// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.FullDayAbsenseUpsert
import fi.espoo.evaka.daycare.service.upsertFullDayAbsences
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class HolidayPeriodControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var holidayPeriodController: HolidayPeriodController

    val today = LocalDate.of(2023, 4, 1)
    val now = HelsinkiDateTime.of(today, LocalTime.of(12, 0))

    val holidayPeriodDeadline = LocalDate.of(2023, 5, 1)
    val holidayPeriodStart = LocalDate.of(2023, 6, 1)
    val holidayPeriodEnd = LocalDate.of(2023, 6, 30)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = today,
                endDate = today.plusYears(1)
            )
        }
    }

    @Test
    fun `existing reservations and absences are deleted when a holiday period is created`() {
        db.transaction { tx ->
            tx.upsertFullDayAbsences(
                EvakaUserId(testAdult_1.id.raw),
                listOf(
                    // Outside holiday period
                    FullDayAbsenseUpsert(
                        testChild_1.id,
                        holidayPeriodStart.minusDays(1),
                        AbsenceType.OTHER_ABSENCE
                    ),
                    // Inside holiday period
                    FullDayAbsenseUpsert(
                        testChild_1.id,
                        holidayPeriodStart,
                        AbsenceType.OTHER_ABSENCE
                    )
                )
            )
            tx.upsertFullDayAbsences(
                EvakaUserId(testDecisionMaker_1.id.raw),
                listOf(
                    // Inside holiday period, but marked by employee
                    FullDayAbsenseUpsert(
                        testChild_1.id,
                        holidayPeriodStart.plusDays(1),
                        AbsenceType.OTHER_ABSENCE
                    )
                )
            )

            listOf(
                    // Inside holiday period
                    DevReservation(
                        childId = testChild_1.id,
                        date = holidayPeriodStart,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = EvakaUserId(testAdult_1.id.raw),
                    ),
                    // Inside holiday period, created by staff
                    DevReservation(
                        childId = testChild_1.id,
                        date = holidayPeriodEnd,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = EvakaUserId(testDecisionMaker_1.id.raw),
                    ),
                    // Outside holiday period
                    DevReservation(
                        childId = testChild_1.id,
                        date = holidayPeriodEnd.plusDays(1),
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = EvakaUserId(testAdult_1.id.raw),
                    )
                )
                .forEach { tx.insert(it) }
        }

        holidayPeriodController.createHolidayPeriod(
            dbInstance(),
            AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN)),
            MockEvakaClock(now),
            HolidayPeriodBody(
                FiniteDateRange(holidayPeriodStart, holidayPeriodEnd),
                holidayPeriodDeadline
            )
        )

        val holidayPeriods =
            holidayPeriodController.getHolidayPeriods(
                dbInstance(),
                AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN)),
                MockEvakaClock(now)
            )
        assertEquals(
            listOf(
                HolidayPeriod(
                    holidayPeriods[0].id,
                    FiniteDateRange(holidayPeriodStart, holidayPeriodEnd),
                    holidayPeriodDeadline
                )
            ),
            holidayPeriods
        )

        val absences =
            db.read { it.createQuery("SELECT date FROM absence ORDER BY date").toList<LocalDate>() }
        assertEquals(
            listOf(holidayPeriodStart.minusDays(1), holidayPeriodStart.plusDays(1)),
            absences
        )

        val reservations =
            db.read {
                it.createQuery("SELECT date FROM attendance_reservation ORDER BY date")
                    .toList<LocalDate>()
            }
        assertEquals(listOf(holidayPeriodEnd, holidayPeriodEnd.plusDays(1)), reservations)
    }
}
