package fi.espoo.evaka.reservations

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.daycare.service.AbsenceCareType
import fi.espoo.evaka.daycare.service.getAbsencesByChildByRange
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestHoliday
import fi.espoo.evaka.shared.dev.insertTestPlacement
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
            createReservationsAsCitizen(
                it, testAdult_1.id,
                listOf(
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday.plusDays(1),
                        TimeRange(startTime, endTime),
                    )
                )
            )
        }

        // then 2 reservations are added
        val reservations = db.read { it.getReservationsCitizen(testAdult_1.id, queryRange) }
            .flatMap { it.children.mapNotNull { child -> child.reservation } }
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
            createReservationsAsCitizen(
                it, testAdult_1.id,
                listOf(
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday.plusDays(1),
                        TimeRange(startTime, endTime)
                    )
                )
            )
        }

        // then only 1 reservation is added
        val reservations = db.read { it.getReservationsCitizen(testAdult_1.id, queryRange) }
            .flatMap { it.children.mapNotNull { child -> child.reservation } }
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
            createReservationsAsCitizen(
                it, testAdult_1.id,
                listOf(
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday,
                        TimeRange(startTime, endTime)
                    ),
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday.plusDays(1),
                        TimeRange(startTime, endTime)
                    )
                )
            )
        }

        // then no reservation are added
        val reservations = db.read { it.getReservationsCitizen(testAdult_1.id, queryRange) }
            .flatMap { it.children.mapNotNull { child -> child.reservation } }
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
            createReservationsAsCitizen(
                it, testAdult_1.id,
                listOf(
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday.minusDays(1),
                        TimeRange(startTime, endTime)
                    ),
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday,
                        TimeRange(startTime, endTime)
                    )
                )
            )
        }

        // then only 1 reservation is added
        val reservations = db.read { it.getReservationsCitizen(testAdult_1.id, queryRange) }
            .flatMap { it.children.mapNotNull { child -> child.reservation } }
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
            createReservationsAsCitizen(
                it, testAdult_1.id,
                listOf(
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday,
                        TimeRange(startTime, endTime)
                    ),
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday.plusDays(1),
                        TimeRange(startTime, endTime)
                    )
                )
            )
        }

        // then only 1 reservation is added
        val reservations = db.read { it.getReservationsCitizen(testAdult_1.id, queryRange) }
            .flatMap { it.children.mapNotNull { child -> child.reservation } }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first().startTime.toLocalDate())
    }

    @Test
    fun `absences are removed from days with reservation`() {
        // given
        db.transaction {
            it.insertTestPlacement(childId = testChild_1.id, unitId = testDaycare.id, startDate = monday, endDate = monday.plusDays(1))
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertTestAbsence(childId = testChild_1.id, date = monday, careType = AbsenceCareType.DAYCARE)
            it.insertTestAbsence(childId = testChild_1.id, date = monday.plusDays(1), careType = AbsenceCareType.DAYCARE)
        }

        // when
        db.transaction {
            createReservationsAsCitizen(
                it, testAdult_1.id,
                listOf(
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday,
                        TimeRange(startTime, endTime)
                    ),
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday.plusDays(1),
                        reservation = null
                    )
                )
            )
        }

        // then 1 reservation is added
        val reservations = db.read { it.getReservationsCitizen(testAdult_1.id, queryRange) }
            .flatMap { it.children.mapNotNull { child -> child.reservation } }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first().startTime.toLocalDate())

        // and 1st absence has been removed
        val absences = db.read { it.getAbsencesByChildByRange(testChild_1.id, FiniteDateRange(monday, monday.plusDays(1))) }
        assertEquals(1, absences.size)
        assertEquals(monday.plusDays(1), absences.first().date)
    }

    @Test
    fun `previous reservation is overwritten`() {
        // given
        db.transaction {
            it.insertTestPlacement(childId = testChild_1.id, unitId = testDaycare.id, startDate = monday, endDate = monday.plusDays(1))
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            createReservationsAsCitizen(
                it, testAdult_1.id,
                listOf(
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday,
                        TimeRange(startTime, endTime)
                    ),
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday.plusDays(1),
                        TimeRange(startTime, endTime)
                    )
                )
            )
        }

        // when
        db.transaction {
            createReservationsAsCitizen(
                it, testAdult_1.id,
                listOf(
                    DailyReservationRequest(
                        childId = testChild_1.id,
                        date = monday,
                        TimeRange(LocalTime.of(12, 0), endTime)
                    )
                )
            )
        }

        // then 1 reservation is changed
        val reservations = db.read { it.getReservationsCitizen(testAdult_1.id, queryRange) }
            .flatMap { it.children.mapNotNull { child -> child.reservation } }
        assertEquals(2, reservations.size)
        assertEquals(monday, reservations[0].startTime.toLocalDate())
        assertEquals(12, reservations[0].startTime.toLocalTime().hour)
        assertEquals(monday.plusDays(1), reservations[1].startTime.toLocalDate())
        assertEquals(9, reservations[1].startTime.toLocalTime().hour)
    }

    @Test
    fun `reservable days query date is Monday`() {
        val result = getReservableDays(LocalDate.of(2021, 6, 7))
        assertEquals(
            FiniteDateRange(LocalDate.of(2021, 6, 14), LocalDate.of(2021, 7, 31)),
            result
        )
    }

    @Test
    fun `reservable days query date is Tuesday`() {
        val result = getReservableDays(LocalDate.of(2021, 6, 8))
        assertEquals(
            FiniteDateRange(LocalDate.of(2021, 6, 21), LocalDate.of(2021, 7, 31)),
            result
        )
    }

    @Test
    fun `reservable days includes next year when start is in July`() {
        val result = getReservableDays(LocalDate.of(2021, 6, 28))
        assertEquals(
            FiniteDateRange(LocalDate.of(2021, 7, 5), LocalDate.of(2022, 7, 31)),
            result
        )
    }

    @Test
    fun `reservable days query date is in September`() {
        val result = getReservableDays(LocalDate.of(2021, 9, 1))
        assertEquals(
            FiniteDateRange(LocalDate.of(2021, 9, 13), LocalDate.of(2022, 7, 31)),
            result
        )
    }
}
