// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.getAbsencesByChildByRange
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestHoliday
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals

class ReservationCitizenQueriesTest : PureJdbiTest() {

    val monday = LocalDate.of(2021, 8, 23)
    val startTime = LocalTime.of(9, 0)
    val endTime = LocalTime.of(17, 0)
    val queryRange = FiniteDateRange(monday.minusDays(10), monday.plusDays(10))

    @BeforeEach
    fun before() {
        db.transaction {
            it.resetDatabase()
            it.insertGeneralTestFixtures()
        }
    }

    @Test
    fun `adding two reservations works in a basic case`() {
        // given
        db.transaction {
            it.insertTestPlacement(childId = testChild_1.id, unitId = testDaycare.id, startDate = monday, endDate = monday.plusDays(1))
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
        }

        // when
        db.transaction {
            createReservations(
                it, testAdult_1.id.raw,
                listOf(
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday,
                        listOf(TimeRange(startTime, endTime)),
                    ),
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday.plusDays(1),
                        listOf(TimeRange(startTime, endTime)),
                    )
                )
            )
        }

        // then 2 reservations are added
        val reservations = db.read { it.getReservationsCitizen(testAdult_1.id, queryRange, false) }
            .flatMap { it.children.mapNotNull { child -> child.reservations.takeIf { it.isNotEmpty() } } }
        assertEquals(2, reservations.size)
    }

    @Test
    fun `reservation is not added outside placement`() {
        // given
        db.transaction {
            it.insertTestPlacement(childId = testChild_1.id, unitId = testDaycare.id, startDate = monday, endDate = monday)
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
        }

        // when
        db.transaction {
            createReservations(
                it, testAdult_1.id.raw,
                listOf(
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday,
                        listOf(TimeRange(startTime, endTime)),
                    ),
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday.plusDays(1),
                        listOf(TimeRange(startTime, endTime))
                    )
                )
            )
        }

        // then only 1 reservation is added
        val reservations = db.read { it.getReservationsCitizen(testAdult_1.id, queryRange, false) }
            .mapNotNull { dailyData -> dailyData.date.takeIf { dailyData.children.any { it.reservations.isNotEmpty() } } }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first())
    }

    @Test
    fun `reservation is not added if user is not guardian of the child`() {
        // given
        db.transaction {
            it.insertTestPlacement(childId = testChild_1.id, unitId = testDaycare.id, startDate = monday, endDate = monday.plusDays(1))
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_2.id)
        }

        // when
        db.transaction {
            createReservations(
                it, testAdult_1.id.raw,
                listOf(
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday,
                        listOf(TimeRange(startTime, endTime))
                    ),
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday.plusDays(1),
                        listOf(TimeRange(startTime, endTime))
                    )
                )
            )
        }

        // then no reservation are added
        val reservations = db.read { it.getReservationsCitizen(testAdult_1.id, queryRange, false) }
            .mapNotNull { dailyData -> dailyData.date.takeIf { dailyData.children.any { it.reservations.isNotEmpty() } } }
        assertEquals(0, reservations.size)
    }

    @Test
    fun `reservation is not added outside operating days`() {
        // given
        db.transaction {
            it.insertTestPlacement(childId = testChild_1.id, unitId = testDaycare.id, startDate = monday.minusDays(1), endDate = monday)
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
        }

        // when
        db.transaction {
            createReservations(
                it, testAdult_1.id.raw,
                listOf(
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday.minusDays(1),
                        listOf(TimeRange(startTime, endTime))
                    ),
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday,
                        listOf(TimeRange(startTime, endTime))
                    )
                )
            )
        }

        // then only 1 reservation is added
        val reservations = db.read { it.getReservationsCitizen(testAdult_1.id, queryRange, false) }
            .mapNotNull { dailyData -> dailyData.date.takeIf { dailyData.children.any { it.reservations.isNotEmpty() } } }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first())
    }

    @Test
    fun `reservation is not added on holiday`() {
        // given
        db.transaction {
            it.insertTestPlacement(childId = testChild_1.id, unitId = testDaycare.id, startDate = monday, endDate = monday.plusDays(1))
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertTestHoliday(monday.plusDays(1))
        }

        // when
        db.transaction {
            createReservations(
                it, testAdult_1.id.raw,
                listOf(
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday,
                        listOf(TimeRange(startTime, endTime))
                    ),
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday.plusDays(1),
                        listOf(TimeRange(startTime, endTime))
                    )
                )
            )
        }

        // then only 1 reservation is added
        val reservations = db.read { it.getReservationsCitizen(testAdult_1.id, queryRange, false) }
            .mapNotNull { dailyData -> dailyData.date.takeIf { dailyData.children.any { it.reservations.isNotEmpty() } } }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first())
    }

    @Test
    fun `absences are removed from days with reservation`() {
        // given
        db.transaction {
            it.insertTestPlacement(childId = testChild_1.id, unitId = testDaycare.id, startDate = monday, endDate = monday.plusDays(1))
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertTestAbsence(childId = testChild_1.id, date = monday, category = AbsenceCategory.BILLABLE)
            it.insertTestAbsence(childId = testChild_1.id, date = monday.plusDays(1), category = AbsenceCategory.BILLABLE)
        }

        // when
        db.transaction {
            createReservations(
                it, testAdult_1.id.raw,
                listOf(
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday,
                        listOf(TimeRange(startTime, endTime))
                    ),
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday.plusDays(1),
                        reservations = null
                    )
                )
            )
        }

        // then 1 reservation is added
        val reservations = db.read { it.getReservationsCitizen(testAdult_1.id, queryRange, false) }
            .mapNotNull { dailyData -> dailyData.date.takeIf { dailyData.children.any { it.reservations.isNotEmpty() } } }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first())

        // and 1st absence has been removed
        val absences = db.read { it.getAbsencesByChildByRange(testChild_1.id, FiniteDateRange(monday, monday.plusDays(1))) }
        assertEquals(1, absences.size)
        assertEquals(monday.plusDays(1), absences.first().date)
    }

    @Test
    fun `free absences are not overwritten`() {
        // given
        val tuesday = monday.plusDays(1)
        val wednesday = monday.plusDays(2)
        db.transaction {
            it.insertTestPlacement(childId = testChild_1.id, unitId = testDaycare.id, startDate = monday, endDate = tuesday)
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertTestAbsence(childId = testChild_1.id, date = monday, category = AbsenceCategory.BILLABLE)
            it.insertTestAbsence(childId = testChild_1.id, date = tuesday, category = AbsenceCategory.BILLABLE, absenceType = AbsenceType.FREE_ABSENCE)
            it.insertTestAbsence(childId = testChild_1.id, date = wednesday, category = AbsenceCategory.BILLABLE, absenceType = AbsenceType.FREE_ABSENCE)
        }

        // when
        db.transaction {
            createReservations(
                it, testAdult_1.id.raw,
                listOf(
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday,
                        reservations = listOf(TimeRange(startTime, endTime))
                    ),
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = tuesday,
                        reservations = listOf(TimeRange(startTime, endTime))
                    ),
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = wednesday,
                        reservations = null
                    )
                )
            )
        }

        // then 1 reservation is added
        val reservations = db.read { it.getReservationsCitizen(testAdult_1.id, queryRange, false) }
            .mapNotNull { dailyData -> dailyData.date.takeIf { dailyData.children.any { it.reservations.isNotEmpty() } } }
        assertEquals(monday, reservations.first())
        assertEquals(1, reservations.size)

        // and 1st absence has been removed
        val absences = db.read { it.getAbsencesByChildByRange(testChild_1.id, FiniteDateRange(monday, wednesday)) }
        assertEquals(listOf(tuesday, wednesday), absences.map { it.date })
        assertEquals(listOf(AbsenceType.FREE_ABSENCE, AbsenceType.FREE_ABSENCE), absences.map { it.absenceType })
    }

    @Test
    fun `previous reservation is overwritten`() {
        // given
        db.transaction {
            it.insertTestPlacement(childId = testChild_1.id, unitId = testDaycare.id, startDate = monday, endDate = monday.plusDays(1))
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            createReservations(
                it, testAdult_1.id.raw,
                listOf(
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday,
                        listOf(TimeRange(startTime, endTime))
                    ),
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday.plusDays(1),
                        listOf(TimeRange(startTime, endTime))
                    )
                )
            )
        }

        // when
        db.transaction {
            createReservations(
                it, testAdult_1.id.raw,
                listOf(
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday,
                        listOf(TimeRange(LocalTime.of(12, 0), endTime))
                    )
                )
            )
        }

        // then 1 reservation is changed
        val reservations = db.read { it.getReservationsCitizen(testAdult_1.id, queryRange, false) }
            .flatMap { dailyData -> dailyData.children.map { child -> dailyData.date to child.reservations } }
        assertEquals(2, reservations.size)
        assertEquals(monday, reservations[0].first)
        assertEquals(LocalTime.of(12, 0), reservations[0].second[0].startTime)
        assertEquals(monday.plusDays(1), reservations[1].first)
        assertEquals(LocalTime.of(9, 0), reservations[1].second[0].startTime)
    }
}
