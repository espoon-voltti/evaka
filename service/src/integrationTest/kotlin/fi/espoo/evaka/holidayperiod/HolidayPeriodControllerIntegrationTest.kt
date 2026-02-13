// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.absence.FullDayAbsenseUpsert
import fi.espoo.evaka.absence.upsertFullDayAbsences
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
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

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val employee = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val adult = DevPerson()
    private val child = DevPerson()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = today,
                    endDate = today.plusYears(1),
                )
            )
        }
    }

    @Test
    fun `existing reservations and absences are deleted when a holiday period is created`() {
        db.transaction { tx ->
            tx.upsertFullDayAbsences(
                adult.evakaUserId(),
                now,
                listOf(
                    // Outside holiday period
                    FullDayAbsenseUpsert(
                        child.id,
                        holidayPeriodStart.minusDays(1),
                        AbsenceType.OTHER_ABSENCE,
                        AbsenceType.OTHER_ABSENCE,
                    ),
                    // Inside holiday period
                    FullDayAbsenseUpsert(
                        child.id,
                        holidayPeriodStart,
                        AbsenceType.OTHER_ABSENCE,
                        AbsenceType.OTHER_ABSENCE,
                    ),
                ),
            )
            tx.upsertFullDayAbsences(
                employee.evakaUserId,
                now,
                listOf(
                    // Inside holiday period, but marked by employee
                    FullDayAbsenseUpsert(
                        child.id,
                        holidayPeriodStart.plusDays(1),
                        AbsenceType.OTHER_ABSENCE,
                        AbsenceType.OTHER_ABSENCE,
                    )
                ),
            )

            listOf(
                    // Inside holiday period
                    DevReservation(
                        childId = child.id,
                        date = holidayPeriodStart,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = adult.evakaUserId(),
                    ),
                    // Inside holiday period, created by staff
                    DevReservation(
                        childId = child.id,
                        date = holidayPeriodEnd,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = employee.evakaUserId,
                    ),
                    // Outside holiday period
                    DevReservation(
                        childId = child.id,
                        date = holidayPeriodEnd.plusDays(1),
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = adult.evakaUserId(),
                    ),
                )
                .forEach { tx.insert(it) }
        }

        holidayPeriodController.createHolidayPeriod(
            dbInstance(),
            employee.user,
            MockEvakaClock(now),
            HolidayPeriodCreate(
                period = FiniteDateRange(holidayPeriodStart, holidayPeriodEnd),
                reservationsOpenOn = holidayPeriodDeadline,
                reservationDeadline = holidayPeriodDeadline,
            ),
        )

        val holidayPeriods =
            holidayPeriodController.getHolidayPeriods(
                dbInstance(),
                employee.user,
                MockEvakaClock(now),
            )
        assertEquals(
            listOf(
                HolidayPeriod(
                    id = holidayPeriods[0].id,
                    period = FiniteDateRange(holidayPeriodStart, holidayPeriodEnd),
                    reservationsOpenOn = holidayPeriodDeadline,
                    reservationDeadline = holidayPeriodDeadline,
                )
            ),
            holidayPeriods,
        )

        val absences =
            db.read {
                it.createQuery { sql("SELECT date FROM absence ORDER BY date") }.toList<LocalDate>()
            }
        assertEquals(
            listOf(holidayPeriodStart.minusDays(1), holidayPeriodStart.plusDays(1)),
            absences,
        )

        val reservations =
            db.read {
                it.createQuery { sql("SELECT date FROM attendance_reservation ORDER BY date") }
                    .toList<LocalDate>()
            }
        assertEquals(listOf(holidayPeriodEnd, holidayPeriodEnd.plusDays(1)), reservations)
    }
}
