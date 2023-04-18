// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.getAbsencesOfChildByRange
import fi.espoo.evaka.holidayperiod.createHolidayPeriod
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestHoliday
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestReservation
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CreateReservationsAndAbsencesTest : PureJdbiTest(resetDbBeforeEach = true) {

    private val monday = LocalDate.of(2021, 8, 23)
    private val startTime = LocalTime.of(9, 0)
    private val endTime: LocalTime = LocalTime.of(17, 0)
    private val queryRange = FiniteDateRange(monday.minusDays(10), monday.plusDays(10))

    private val citizenUser = AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG)
    private val employeeUser = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf())

    @BeforeEach
    fun before() {
        db.transaction { it.insertGeneralTestFixtures() }
    }

    @Test
    fun `adding two reservations works in a basic case`() {
        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = monday.plusDays(1)
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                monday,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday,
                        Reservation.Times(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday.plusDays(1),
                        Reservation.Times(startTime, endTime),
                    )
                )
            )
        }

        // then 2 reservations are added
        val reservations =
            db.read { it.getReservationsCitizen(monday, testAdult_1.id, queryRange) }
                .flatMap {
                    it.children.mapNotNull { child ->
                        child.reservations.takeIf { it.isNotEmpty() }
                    }
                }
        assertEquals(2, reservations.size)
    }

    @Test
    fun `reservation is not added outside placement`() {
        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = monday
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                monday,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday,
                        Reservation.Times(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday.plusDays(1),
                        Reservation.Times(startTime, endTime),
                    )
                )
            )
        }

        // then only 1 reservation is added
        val reservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        queryRange,
                    )
                }
                .mapNotNull { dailyData ->
                    dailyData.date.takeIf {
                        dailyData.children.any { it.reservations.isNotEmpty() }
                    }
                }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first())
    }

    @Test
    fun `reservation is not added if user is not guardian of the child`() {
        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = monday.plusDays(1)
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_2.id)
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                monday,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday,
                        Reservation.Times(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday.plusDays(1),
                        Reservation.Times(startTime, endTime),
                    )
                )
            )
        }

        // then no reservation are added
        val reservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        queryRange,
                    )
                }
                .mapNotNull { dailyData ->
                    dailyData.date.takeIf {
                        dailyData.children.any { it.reservations.isNotEmpty() }
                    }
                }
        assertEquals(0, reservations.size)
    }

    @Test
    fun `reservation is not added outside operating days`() {
        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday.minusDays(1),
                endDate = monday
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                monday,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday.minusDays(1),
                        Reservation.Times(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday,
                        Reservation.Times(startTime, endTime),
                    )
                )
            )
        }

        // then only 1 reservation is added
        val reservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        queryRange,
                    )
                }
                .mapNotNull { dailyData ->
                    dailyData.date.takeIf {
                        dailyData.children.any { it.reservations.isNotEmpty() }
                    }
                }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first())
    }

    @Test
    fun `reservation is not added on holiday`() {
        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = monday.plusDays(1)
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertTestHoliday(monday.plusDays(1))
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                monday,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday,
                        Reservation.Times(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday.plusDays(1),
                        Reservation.Times(startTime, endTime),
                    )
                )
            )
        }

        // then only 1 reservation is added
        val reservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        queryRange,
                    )
                }
                .mapNotNull { dailyData ->
                    dailyData.date.takeIf {
                        dailyData.children.any { it.reservations.isNotEmpty() }
                    }
                }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first())
    }

    @Test
    fun `absences are removed from days with reservation`() {
        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = monday.plusDays(1)
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertTestAbsence(
                childId = testChild_1.id,
                date = monday,
                category = AbsenceCategory.BILLABLE,
                modifiedBy = EvakaUserId(testAdult_1.id.raw)
            )
            it.insertTestAbsence(
                childId = testChild_1.id,
                date = monday.plusDays(1),
                category = AbsenceCategory.BILLABLE,
                modifiedBy = EvakaUserId(testAdult_1.id.raw)
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                monday,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday,
                        Reservation.Times(startTime, endTime),
                    ),
                )
            )
        }

        // then 1 reservation is added
        val reservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        queryRange,
                    )
                }
                .mapNotNull { dailyData ->
                    dailyData.date.takeIf {
                        dailyData.children.any { it.reservations.isNotEmpty() }
                    }
                }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first())

        // and 1st absence has been removed
        val absences =
            db.read {
                it.getAbsencesOfChildByRange(testChild_1.id, DateRange(monday, monday.plusDays(1)))
            }
        assertEquals(1, absences.size)
        assertEquals(monday.plusDays(1), absences.first().date)
    }

    @Test
    fun `absences and reservations are removed from days with no absence or reservation`() {
        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = monday.plusDays(1)
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertTestAbsence(
                childId = testChild_1.id,
                date = monday,
                category = AbsenceCategory.BILLABLE,
                modifiedBy = EvakaUserId(testAdult_1.id.raw)
            )
            it.insertTestReservation(
                DevReservation(
                    childId = testChild_1.id,
                    date = monday.plusDays(1),
                    startTime = startTime,
                    endTime = endTime,
                    createdBy = EvakaUserId(testAdult_1.id.raw)
                )
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                monday,
                citizenUser,
                listOf(
                    DailyReservationRequest.Nothing(
                        childId = testChild_1.id,
                        date = monday,
                    ),
                    DailyReservationRequest.Nothing(
                        childId = testChild_1.id,
                        date = monday.plusDays(1),
                    )
                )
            )
        }

        // then no reservations exist
        val reservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        queryRange,
                    )
                }
                .flatMap { dailyData -> dailyData.children.flatMap { it.reservations } }
        assertEquals(listOf(), reservations)

        // and no absences exist
        val absences =
            db.read {
                it.getAbsencesOfChildByRange(testChild_1.id, DateRange(monday, monday.plusDays(1)))
            }
        assertEquals(listOf(), absences)
    }

    @Test
    fun `free absences are not overwritten`() {
        // given
        val tuesday = monday.plusDays(1)
        val wednesday = monday.plusDays(2)
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = tuesday
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertTestAbsence(
                childId = testChild_1.id,
                date = monday,
                category = AbsenceCategory.BILLABLE,
                modifiedBy = EvakaUserId(testAdult_1.id.raw)
            )
            it.insertTestAbsence(
                childId = testChild_1.id,
                date = tuesday,
                category = AbsenceCategory.BILLABLE,
                absenceType = AbsenceType.FREE_ABSENCE,
                modifiedBy = EvakaUserId(testAdult_1.id.raw)
            )
            it.insertTestAbsence(
                childId = testChild_1.id,
                date = wednesday,
                category = AbsenceCategory.BILLABLE,
                absenceType = AbsenceType.FREE_ABSENCE,
                modifiedBy = EvakaUserId(testAdult_1.id.raw)
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                monday,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday,
                        Reservation.Times(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = tuesday,
                        Reservation.Times(startTime, endTime),
                    ),
                    DailyReservationRequest.Nothing(
                        childId = testChild_1.id,
                        date = wednesday,
                    )
                )
            )
        }

        // then 1 reservation is added
        val reservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        queryRange,
                    )
                }
                .mapNotNull { dailyData ->
                    dailyData.date.takeIf {
                        dailyData.children.any { it.reservations.isNotEmpty() }
                    }
                }
        assertEquals(monday, reservations.first())
        assertEquals(1, reservations.size)

        // and 1st absence has been removed
        val absences =
            db.read { it.getAbsencesOfChildByRange(testChild_1.id, DateRange(monday, wednesday)) }
        assertEquals(listOf(tuesday, wednesday), absences.map { it.date })
        assertEquals(
            listOf(AbsenceType.FREE_ABSENCE, AbsenceType.FREE_ABSENCE),
            absences.map { it.absenceType }
        )
    }

    @Test
    fun `previous reservation is overwritten`() {
        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = monday.plusDays(1)
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            createReservationsAndAbsences(
                it,
                monday,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday,
                        Reservation.Times(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday.plusDays(1),
                        Reservation.Times(startTime, endTime),
                    )
                )
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                monday,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday,
                        Reservation.Times(LocalTime.of(12, 0), endTime),
                    )
                )
            )
        }

        // then 1 reservation is changed
        val reservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        queryRange,
                    )
                }
                .flatMap { dailyData ->
                    dailyData.children.map { child -> dailyData.date to child.reservations }
                }
        assertEquals(2, reservations.size)
        assertEquals(monday, reservations[0].first)
        assertEquals(
            LocalTime.of(12, 0),
            (reservations[0].second[0] as Reservation.Times).startTime
        )
        assertEquals(monday.plusDays(1), reservations[1].first)
        assertEquals(LocalTime.of(9, 0), (reservations[1].second[0] as Reservation.Times).startTime)
    }

    @Test
    fun `reservations without times can be added to open holiday period`() {
        val holidayPeriodStart = monday.plusMonths(1)
        val holidayPeriodEnd = holidayPeriodStart.plusWeeks(1).minusDays(1)
        val holidayPeriod = FiniteDateRange(holidayPeriodStart, holidayPeriodEnd)

        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = monday.plusYears(1)
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.createHolidayPeriod(holidayPeriod, monday)
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                monday,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = holidayPeriodStart,
                        Reservation.NoTimes,
                    )
                )
            )
        }

        // then
        val dailyReservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        holidayPeriod,
                    )
                }
                .flatMap { dailyData ->
                    dailyData.children.map { child -> dailyData.date to child.reservations }
                }
        assertEquals(1, dailyReservations.size)
        dailyReservations.first().let { (date, reservations) ->
            assertEquals(holidayPeriodStart, date)
            assertEquals(listOf(Reservation.NoTimes), reservations)
        }
    }

    @Test
    fun `reservations without times cannot be added to closed holiday period or outside holiday periods`() {
        val holidayPeriodStart = monday.plusMonths(1)
        val holidayPeriodEnd = holidayPeriodStart.plusWeeks(1).minusDays(1)
        val holidayPeriod = FiniteDateRange(holidayPeriodStart, holidayPeriodEnd)

        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = monday.plusYears(1)
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.createHolidayPeriod(holidayPeriod, monday.minusDays(1))
        }

        // when
        assertThrows<BadRequest> {
            // Closed holiday period
            db.transaction {
                createReservationsAndAbsences(
                    it,
                    monday,
                    citizenUser,
                    listOf(
                        DailyReservationRequest.Reservations(
                            childId = testChild_1.id,
                            date = holidayPeriodStart,
                            Reservation.NoTimes,
                        )
                    )
                )
            }
        }

        assertThrows<BadRequest> {
            // Outside holiday periods
            db.transaction {
                createReservationsAndAbsences(
                    it,
                    monday,
                    citizenUser,
                    listOf(
                        DailyReservationRequest.Reservations(
                            childId = testChild_1.id,
                            date = holidayPeriodEnd.plusDays(1),
                            Reservation.NoTimes,
                        )
                    )
                )
            }
        }
    }

    @Test
    fun `citizen cannot override absences in closed holiday periods`() {
        val holidayPeriodStart = monday.plusMonths(1)
        val holidayPeriodEnd = holidayPeriodStart.plusWeeks(1).minusDays(1)
        val holidayPeriod = FiniteDateRange(holidayPeriodStart, holidayPeriodEnd)

        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = monday.plusYears(1)
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.createHolidayPeriod(holidayPeriod, monday.minusDays(1))
            it.insertAbsences(
                citizenUser.evakaUserId,
                listOf(
                    AbsenceInsert(
                        childId = testChild_1.id,
                        date = holidayPeriodStart,
                        absenceType = AbsenceType.OTHER_ABSENCE,
                    )
                )
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                monday,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = holidayPeriodStart,
                        Reservation.Times(startTime, endTime),
                    )
                )
            )
        }

        // then
        val reservationData =
            db.read {
                it.getReservationsCitizen(
                    monday,
                    testAdult_1.id,
                    holidayPeriod,
                )
            }
        val allReservations =
            reservationData.flatMap { dailyData ->
                dailyData.children.flatMap { child -> child.reservations }
            }
        val absenceDates =
            reservationData.flatMap { dailyData ->
                if (dailyData.children.any { child -> child.absence != null })
                    listOf(dailyData.date)
                else emptyList()
            }
        assertEquals(0, allReservations.size)
        assertEquals(listOf(holidayPeriodStart), absenceDates)
    }

    @Test
    fun `employee can override absences in closed holiday periods`() {
        val holidayPeriodStart = monday.plusMonths(1)
        val holidayPeriodEnd = holidayPeriodStart.plusWeeks(1).minusDays(1)
        val holidayPeriod = FiniteDateRange(holidayPeriodStart, holidayPeriodEnd)

        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = monday.plusYears(1)
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.createHolidayPeriod(holidayPeriod, monday.minusDays(1))
            it.insertAbsences(
                citizenUser.evakaUserId,
                listOf(
                    AbsenceInsert(
                        childId = testChild_1.id,
                        date = holidayPeriodStart,
                        absenceType = AbsenceType.OTHER_ABSENCE,
                    )
                )
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                monday,
                employeeUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = holidayPeriodStart,
                        Reservation.Times(startTime, endTime),
                    )
                )
            )
        }

        // then
        val reservationData =
            db.read {
                it.getReservationsCitizen(
                    monday,
                    testAdult_1.id,
                    holidayPeriod,
                )
            }
        val allReservations =
            reservationData.flatMap { dailyData ->
                dailyData.children.map { child -> dailyData.date to child.reservations }
            }
        val absenceDates =
            reservationData.flatMap { dailyData ->
                if (dailyData.children.any { child -> child.absence != null })
                    listOf(dailyData.date)
                else emptyList()
            }
        assertEquals(1, allReservations.size)
        allReservations.first().let { (date, reservations) ->
            assertEquals(holidayPeriodStart, date)
            assertEquals(listOf(Reservation.Times(startTime, endTime)), reservations)
        }
        assertEquals(0, absenceDates.size)
    }

    @Test
    fun `reservations can be overridden in closed holiday periods`() {
        val holidayPeriodStart = monday.plusMonths(1)
        val holidayPeriodEnd = holidayPeriodStart.plusWeeks(1).minusDays(1)
        val holidayPeriod = FiniteDateRange(holidayPeriodStart, holidayPeriodEnd)

        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = monday.plusYears(1)
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.createHolidayPeriod(holidayPeriod, monday.minusDays(1))
            it.insertTestReservation(
                // NoTimes reservation
                DevReservation(
                    childId = testChild_1.id,
                    date = holidayPeriodStart,
                    startTime = null,
                    endTime = null,
                    createdBy = citizenUser.evakaUserId
                )
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                monday,
                employeeUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = holidayPeriodStart,
                        Reservation.Times(startTime, endTime),
                    )
                )
            )
        }

        // then
        val allReservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        holidayPeriod,
                    )
                }
                .flatMap { dailyData ->
                    dailyData.children.map { child -> dailyData.date to child.reservations }
                }
        assertEquals(1, allReservations.size)
        allReservations.first().let { (date, reservations) ->
            assertEquals(holidayPeriodStart, date)
            assertEquals(listOf(Reservation.Times(startTime, endTime)), reservations)
        }
    }
}
