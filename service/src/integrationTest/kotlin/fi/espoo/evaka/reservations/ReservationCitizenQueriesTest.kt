package fi.espoo.evaka.reservations

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.daycare.service.AbsenceCareType
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestHoliday
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
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
                it, testAdult_1.id,
                listOf(
                    Reservation(
                        childId = testChild_1.id,
                        startTime = HelsinkiDateTime.of(monday, startTime),
                        endTime = HelsinkiDateTime.of(monday, endTime),
                    ),
                    Reservation(
                        childId = testChild_1.id,
                        startTime = HelsinkiDateTime.of(monday.plusDays(1), startTime),
                        endTime = HelsinkiDateTime.of(monday.plusDays(1), endTime),
                    )
                )
            )
        }

        // then 2 reservations are added
        val reservations = db.read { it.getReservations(testAdult_1.id, queryRange) }.flatMap { it.reservations }
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
                it, testAdult_1.id,
                listOf(
                    Reservation(
                        childId = testChild_1.id,
                        startTime = HelsinkiDateTime.of(monday, startTime),
                        endTime = HelsinkiDateTime.of(monday, endTime),
                    ),
                    Reservation(
                        childId = testChild_1.id,
                        startTime = HelsinkiDateTime.of(monday.plusDays(1), startTime),
                        endTime = HelsinkiDateTime.of(monday.plusDays(1), endTime),
                    )
                )
            )
        }

        // then only 1 reservation is added
        val reservations = db.read { it.getReservations(testAdult_1.id, queryRange) }.flatMap { it.reservations }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first().startTime.toLocalDate())
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
                it, testAdult_1.id,
                listOf(
                    Reservation(
                        childId = testChild_1.id,
                        startTime = HelsinkiDateTime.of(monday, startTime),
                        endTime = HelsinkiDateTime.of(monday, endTime),
                    ),
                    Reservation(
                        childId = testChild_1.id,
                        startTime = HelsinkiDateTime.of(monday.plusDays(1), startTime),
                        endTime = HelsinkiDateTime.of(monday.plusDays(1), endTime),
                    )
                )
            )
        }

        // then no reservation are added
        val reservations = db.read { it.getReservations(testAdult_1.id, queryRange) }.flatMap { it.reservations }
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
                it, testAdult_1.id,
                listOf(
                    Reservation(
                        childId = testChild_1.id,
                        startTime = HelsinkiDateTime.of(monday.minusDays(1), startTime),
                        endTime = HelsinkiDateTime.of(monday.minusDays(1), endTime),
                    ),
                    Reservation(
                        childId = testChild_1.id,
                        startTime = HelsinkiDateTime.of(monday, startTime),
                        endTime = HelsinkiDateTime.of(monday, endTime),
                    )
                )
            )
        }

        // then only 1 reservation is added
        val reservations = db.read { it.getReservations(testAdult_1.id, queryRange) }.flatMap { it.reservations }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first().startTime.toLocalDate())
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
                it, testAdult_1.id,
                listOf(
                    Reservation(
                        childId = testChild_1.id,
                        startTime = HelsinkiDateTime.of(monday, startTime),
                        endTime = HelsinkiDateTime.of(monday, endTime),
                    ),
                    Reservation(
                        childId = testChild_1.id,
                        startTime = HelsinkiDateTime.of(monday.plusDays(1), startTime),
                        endTime = HelsinkiDateTime.of(monday.plusDays(1), endTime),
                    )
                )
            )
        }

        // then only 1 reservation is added
        val reservations = db.read { it.getReservations(testAdult_1.id, queryRange) }.flatMap { it.reservations }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first().startTime.toLocalDate())
    }

    @Test
    fun `reservation is not added if employee has added absence`() {
        // given
        db.transaction {
            it.insertTestPlacement(childId = testChild_1.id, unitId = testDaycare.id, startDate = monday, endDate = monday.plusDays(1))
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertTestAbsence(childId = testChild_1.id, date = monday.plusDays(1), careType = AbsenceCareType.DAYCARE)
        }

        // when
        db.transaction {
            createReservations(
                it, testAdult_1.id,
                listOf(
                    Reservation(
                        childId = testChild_1.id,
                        startTime = HelsinkiDateTime.of(monday, startTime),
                        endTime = HelsinkiDateTime.of(monday, endTime),
                    ),
                    Reservation(
                        childId = testChild_1.id,
                        startTime = HelsinkiDateTime.of(monday.plusDays(1), startTime),
                        endTime = HelsinkiDateTime.of(monday.plusDays(1), endTime),
                    )
                )
            )
        }

        // then only 1 reservation is added
        val reservations = db.read { it.getReservations(testAdult_1.id, queryRange) }.flatMap { it.reservations }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first().startTime.toLocalDate())
    }

    @Test
    fun `previous reservation is overwritten`() {
        // given
        db.transaction {
            it.insertTestPlacement(childId = testChild_1.id, unitId = testDaycare.id, startDate = monday, endDate = monday.plusDays(1))
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            createReservations(
                it, testAdult_1.id,
                listOf(
                    Reservation(
                        childId = testChild_1.id,
                        startTime = HelsinkiDateTime.of(monday, startTime),
                        endTime = HelsinkiDateTime.of(monday, endTime),
                    ),
                    Reservation(
                        childId = testChild_1.id,
                        startTime = HelsinkiDateTime.of(monday.plusDays(1), startTime),
                        endTime = HelsinkiDateTime.of(monday.plusDays(1), endTime),
                    )
                )
            )
        }

        // when
        db.transaction {
            createReservations(
                it, testAdult_1.id,
                listOf(
                    Reservation(
                        childId = testChild_1.id,
                        startTime = HelsinkiDateTime.of(monday, LocalTime.of(12, 0)),
                        endTime = HelsinkiDateTime.of(monday, endTime),
                    )
                )
            )
        }

        // then 1 reservation is changed
        val reservations = db.read { it.getReservations(testAdult_1.id, queryRange) }.flatMap { it.reservations }
        assertEquals(2, reservations.size)
        assertEquals(monday, reservations[0].startTime.toLocalDate())
        assertEquals(12, reservations[0].startTime.toLocalTime().hour)
        assertEquals(monday.plusDays(1), reservations[1].startTime.toLocalDate())
        assertEquals(9, reservations[1].startTime.toLocalTime().hour)
    }

    @Test
    fun `reservable days query date is Monday`() {
        val result = getReservableDays(LocalDate.of(2021, 7, 5))
        assertEquals(
            FiniteDateRange(LocalDate.of(2021, 7, 12), LocalDate.of(2021, 7, 31)),
            result
        )
    }

    @Test
    fun `reservable days query date is Tuesday`() {
        val result = getReservableDays(LocalDate.of(2021, 7, 6))
        assertEquals(
            FiniteDateRange(LocalDate.of(2021, 7, 19), LocalDate.of(2021, 7, 31)),
            result
        )
    }

    @Test
    fun `reservable days is at least one full week`() {
        val result = getReservableDays(LocalDate.of(2021, 7, 14))
        assertEquals(
            FiniteDateRange(LocalDate.of(2021, 7, 26), LocalDate.of(2022, 7, 31)),
            result
        )
    }
}
