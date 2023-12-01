// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesController
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesValue
import fi.espoo.evaka.daycare.insertPreschoolTerm
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.getAbsencesOfChildByRange
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.ScheduleType
import fi.espoo.evaka.preschoolTerm2021
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertServiceNeedOption
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.dev.insertTestHoliday
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestServiceNeed
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.snDaycareContractDays10
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.snPreschoolDaycareContractDays13
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testRoundTheClockDaycare
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ReservationControllerCitizenIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var reservationControllerCitizen: ReservationControllerCitizen
    @Autowired private lateinit var dailyServiceTimesController: DailyServiceTimesController

    // Monday
    private val mockToday: LocalDate = LocalDate.of(2021, 11, 1)

    private val monday = LocalDate.of(2021, 11, 15)
    private val tuesday = monday.plusDays(1)
    private val wednesday = monday.plusDays(2)
    private val thursday = monday.plusDays(3)

    private val sundayLastWeek = monday.minusDays(1)
    private val saturdayLastWeek = monday.minusDays(2)
    private val fridayLastWeek = monday.minusDays(3)

    private val startTime = LocalTime.of(9, 0)
    private val endTime = LocalTime.of(17, 0)

    private val testStaffId = EmployeeId(UUID.randomUUID())

    private lateinit var testChild1PlacementId: PlacementId

    @BeforeEach
    fun before() {
        db.transaction { tx ->
            tx.insertServiceNeedOptions()
            tx.insertServiceNeedOption(snPreschoolDaycareContractDays13)

            tx.insert(testArea)
            tx.insert(testDaycare)

            tx.insert(testDecisionMaker_1)
            tx.insert(DevEmployee(testStaffId))

            tx.insert(testAdult_1, DevPersonType.ADULT)

            listOf(testChild_1, testChild_2).forEach { child ->
                tx.insert(child, DevPersonType.CHILD)
            }

            testChild1PlacementId =
                tx.insertTestPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    startDate = monday,
                    endDate = tuesday
                )
            tx.insertTestPlacement(
                    childId = testChild_2.id,
                    unitId = testDaycare.id,
                    type = PlacementType.DAYCARE,
                    startDate = fridayLastWeek,
                    endDate = tuesday
                )
                .also { placementId ->
                    // contract days on monday and tuesday
                    tx.insertTestServiceNeed(
                        confirmedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                        placementId = placementId,
                        period = FiniteDateRange(monday, tuesday),
                        optionId = snDaycareContractDays10.id,
                        shiftCare = ShiftCareType.NONE
                    )
                }
            tx.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            tx.insertGuardian(guardianId = testAdult_1.id, childId = testChild_2.id)
        }
    }

    @Test
    fun `get reservations returns correct children every day in range`() {
        db.transaction { tx ->
            tx.insertTestHoliday(wednesday)

            tx.insert(testChild_3, DevPersonType.CHILD)

            // Fixed schedule (PRESCHOOL)
            tx.insertTestPlacement(
                childId = testChild_3.id,
                unitId = testDaycare.id,
                type = PlacementType.PRESCHOOL,
                startDate = monday,
                endDate = thursday
            )

            // Term break on thursday
            tx.insertPreschoolTerm(
                preschoolTerm2021.copy(termBreaks = DateSet.of(FiniteDateRange(thursday, thursday)))
            )

            tx.insertGuardian(guardianId = testAdult_1.id, childId = testChild_3.id)
        }

        val res = getReservations(FiniteDateRange(sundayLastWeek, thursday))

        assertEquals(
            ReservationsResponse(
                reservableRange =
                    FiniteDateRange(
                        LocalDate.of(2021, 11, 8), // Next week's monday
                        LocalDate.of(2022, 8, 31),
                    ),
                children =
                    // Sorted by date of birth, oldest child first
                    listOf(
                        ReservationChild(
                            id = testChild_2.id,
                            firstName = testChild_2.firstName,
                            lastName = testChild_2.lastName,
                            preferredName = "",
                            duplicateOf = null,
                            imageId = null,
                            upcomingPlacementType = PlacementType.DAYCARE,
                        ),
                        ReservationChild(
                            id = testChild_1.id,
                            firstName = testChild_1.firstName,
                            lastName = testChild_1.lastName,
                            preferredName = "",
                            duplicateOf = null,
                            imageId = null,
                            upcomingPlacementType = PlacementType.PRESCHOOL_DAYCARE,
                        ),
                        ReservationChild(
                            id = testChild_3.id,
                            firstName = testChild_3.firstName,
                            lastName = testChild_3.lastName,
                            preferredName = "",
                            duplicateOf = null,
                            imageId = null,
                            upcomingPlacementType = PlacementType.PRESCHOOL,
                        ),
                    ),
                days =
                    listOf(
                        // sunday without children is included because weekends are always visible
                        ReservationResponseDay(
                            date = sundayLastWeek,
                            holiday = false,
                            children = emptyList()
                        ),
                        ReservationResponseDay(
                            date = monday,
                            holiday = false,
                            children =
                                listOf(
                                        dayChild(
                                            testChild_1.id,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    testDaycare.operationTimes[0]!!
                                                )
                                        ),
                                        dayChild(
                                            testChild_2.id,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    testDaycare.operationTimes[0]!!
                                                )
                                        ),
                                        dayChild(
                                            testChild_3.id,
                                            scheduleType = ScheduleType.FIXED_SCHEDULE,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    testDaycare.operationTimes[0]!!
                                                )
                                        )
                                    )
                                    .sortedBy { it.childId }
                        ),
                        ReservationResponseDay(
                            date = tuesday,
                            holiday = false,
                            children =
                                listOf(
                                        dayChild(
                                            testChild_1.id,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    testDaycare.operationTimes[1]!!
                                                )
                                        ),
                                        dayChild(
                                            testChild_2.id,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    testDaycare.operationTimes[1]!!
                                                )
                                        ),
                                        dayChild(
                                            testChild_3.id,
                                            scheduleType = ScheduleType.FIXED_SCHEDULE,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    testDaycare.operationTimes[1]!!
                                                )
                                        )
                                    )
                                    .sortedBy { it.childId }
                        ),
                        ReservationResponseDay(
                            date = wednesday,
                            holiday = true,
                            children = emptyList() // Holiday, no children eligible for daycare
                        ),
                        ReservationResponseDay(
                            date = thursday,
                            holiday = false,
                            children =
                                listOf(
                                    dayChild(
                                        testChild_3.id,
                                        scheduleType = ScheduleType.TERM_BREAK,
                                        reservableTimeRange =
                                            ReservableTimeRange.Normal(
                                                testDaycare.operationTimes[0]!!
                                            )
                                    )
                                )
                        )
                    )
            ),
            res
        )
    }

    @Test
    fun `get reservations returns correct children every day in range with children in shift care`() {
        val roundTheClockDaycare =
            testRoundTheClockDaycare.copy(
                id = DaycareId(UUID.randomUUID()),
                enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
            )

        db.transaction { tx ->
            tx.insertTestHoliday(tuesday)

            listOf(testChild_3, testChild_4).forEach { child ->
                tx.insert(child, DevPersonType.CHILD)
            }

            // Normal shift care
            tx.insert(roundTheClockDaycare)
            tx.insertTestPlacement(
                childId = testChild_3.id,
                unitId = roundTheClockDaycare.id,
                type = PlacementType.DAYCARE,
                startDate = sundayLastWeek,
                endDate = tuesday
            )
            tx.insertGuardian(guardianId = testAdult_1.id, childId = testChild_3.id)

            // Intermittent shift care
            tx.insertTestPlacement(
                    childId = testChild_4.id,
                    unitId = testDaycare.id,
                    type = PlacementType.DAYCARE,
                    startDate = sundayLastWeek,
                    endDate = wednesday
                )
                .also { placementId ->
                    tx.insertTestServiceNeed(
                        confirmedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                        placementId = placementId,
                        period = FiniteDateRange(sundayLastWeek, tuesday),
                        optionId = snDaycareFullDay35.id,
                        shiftCare = ShiftCareType.INTERMITTENT,
                    )
                }
            tx.insertGuardian(guardianId = testAdult_1.id, childId = testChild_4.id)
        }

        val res = getReservations(FiniteDateRange(sundayLastWeek, wednesday))

        assertEquals(
            ReservationsResponse(
                reservableRange =
                    FiniteDateRange(
                        LocalDate.of(2021, 11, 8), // Next week's monday
                        LocalDate.of(2022, 8, 31),
                    ),
                children =
                    // Sorted by date of birth, oldest child first
                    listOf(
                        ReservationChild(
                            id = testChild_2.id,
                            firstName = testChild_2.firstName,
                            lastName = testChild_2.lastName,
                            preferredName = "",
                            duplicateOf = null,
                            imageId = null,
                            upcomingPlacementType = PlacementType.DAYCARE,
                        ),
                        ReservationChild(
                            id = testChild_1.id,
                            firstName = testChild_1.firstName,
                            lastName = testChild_1.lastName,
                            preferredName = "",
                            duplicateOf = null,
                            imageId = null,
                            upcomingPlacementType = PlacementType.PRESCHOOL_DAYCARE,
                        ),
                        ReservationChild(
                            id = testChild_3.id,
                            firstName = testChild_3.firstName,
                            lastName = testChild_3.lastName,
                            preferredName = "",
                            duplicateOf = null,
                            imageId = null,
                            upcomingPlacementType = PlacementType.DAYCARE,
                        ),
                        ReservationChild(
                            id = testChild_4.id,
                            firstName = testChild_4.firstName,
                            lastName = testChild_4.lastName,
                            preferredName = "",
                            duplicateOf = null,
                            imageId = null,
                            upcomingPlacementType = PlacementType.DAYCARE,
                        ),
                    ),
                days =
                    listOf(
                        ReservationResponseDay(
                            date = sundayLastWeek,
                            holiday = false,
                            children =
                                listOf(
                                        // Sunday, only shift care is eligible
                                        dayChild(
                                            testChild_3.id,
                                            shiftCare = true,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    roundTheClockDaycare.operationTimes[6]!!
                                                )
                                        ),
                                        dayChild(
                                            testChild_4.id,
                                            shiftCare = true,
                                            reservableTimeRange =
                                                ReservableTimeRange.IntermittentShiftCare(
                                                    // Placement unit is not open on Sundays
                                                    null
                                                )
                                        ),
                                    )
                                    .sortedBy { it.childId }
                        ),
                        ReservationResponseDay(
                            date = monday,
                            holiday = false,
                            children =
                                listOf(
                                        dayChild(
                                            testChild_1.id,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    testDaycare.operationTimes[0]!!
                                                )
                                        ),
                                        dayChild(
                                            testChild_2.id,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    testDaycare.operationTimes[0]!!
                                                )
                                        ),
                                        dayChild(
                                            testChild_3.id,
                                            shiftCare = true,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    roundTheClockDaycare.operationTimes[0]!!
                                                )
                                        ),
                                        dayChild(
                                            testChild_4.id,
                                            shiftCare = true,
                                            reservableTimeRange =
                                                ReservableTimeRange.IntermittentShiftCare(
                                                    testDaycare.operationTimes[0]!!
                                                )
                                        ),
                                    )
                                    .sortedBy { it.childId }
                        ),
                        ReservationResponseDay(
                            date = tuesday,
                            holiday = true,
                            children =
                                listOf(
                                        // Holiday, only shift care is eligible
                                        dayChild(
                                            testChild_3.id,
                                            shiftCare = true,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    testDaycare.operationTimes[1]!!
                                                )
                                        ),
                                        dayChild(
                                            testChild_4.id,
                                            shiftCare = true,
                                            reservableTimeRange =
                                                ReservableTimeRange.IntermittentShiftCare(
                                                    // Placement unit is not open on holidays
                                                    null
                                                )
                                        ),
                                    )
                                    .sortedBy { it.childId }
                        ),
                        ReservationResponseDay(
                            date = wednesday,
                            holiday = false,
                            children =
                                listOf(
                                    // Intermittent shift care ended on Tuesday
                                    dayChild(
                                        testChild_4.id,
                                        shiftCare = false,
                                        reservableTimeRange =
                                            ReservableTimeRange.Normal(
                                                testDaycare.operationTimes[2]!!
                                            )
                                    ),
                                )
                        )
                    )
            ),
            res
        )
    }

    @Test
    fun `irregular daily service time absences are non-editable`() {
        db.transaction { tx ->
            tx.insertDaycareAclRow(testDaycare.id, testDecisionMaker_1.id, UserRole.UNIT_SUPERVISOR)
        }

        dailyServiceTimesController.postDailyServiceTimes(
            dbInstance(),
            AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf()),
            MockEvakaClock(HelsinkiDateTime.of(mockToday, LocalTime.of(12, 0))),
            testChild_1.id,
            DailyServiceTimesValue.IrregularTimes(
                validityPeriod = DateRange(mockToday.plusDays(1), null),
                monday = null,
                tuesday = TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
                wednesday = null,
                thursday = null,
                friday = null,
                saturday = null,
                sunday = null,
            )
        )

        val res = getReservations(FiniteDateRange(monday, tuesday))

        assertEquals(
            ReservationsResponse(
                reservableRange =
                    FiniteDateRange(
                        LocalDate.of(2021, 11, 8), // Next week's monday
                        LocalDate.of(2022, 8, 31),
                    ),
                children =
                    // Sorted by date of birth, oldest child first
                    listOf(
                        ReservationChild(
                            id = testChild_2.id,
                            firstName = testChild_2.firstName,
                            lastName = testChild_2.lastName,
                            preferredName = "",
                            duplicateOf = null,
                            imageId = null,
                            upcomingPlacementType = PlacementType.DAYCARE,
                        ),
                        ReservationChild(
                            id = testChild_1.id,
                            firstName = testChild_1.firstName,
                            lastName = testChild_1.lastName,
                            preferredName = "",
                            duplicateOf = null,
                            imageId = null,
                            upcomingPlacementType = PlacementType.PRESCHOOL_DAYCARE,
                        ),
                    ),
                days =
                    listOf(
                        ReservationResponseDay(
                            date = monday,
                            holiday = false,
                            children =
                                listOf(
                                        dayChild(
                                            testChild_1.id,
                                            absence =
                                                AbsenceInfo(
                                                    AbsenceType.OTHER_ABSENCE,
                                                    editable = false
                                                ),
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    testDaycare.operationTimes[0]!!
                                                )
                                        ),
                                        dayChild(
                                            testChild_2.id,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    testDaycare.operationTimes[0]!!
                                                )
                                        ),
                                    )
                                    .sortedBy { it.childId }
                        ),
                        ReservationResponseDay(
                            date = tuesday,
                            holiday = false,
                            children =
                                listOf(
                                        dayChild(
                                            testChild_1.id,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    testDaycare.operationTimes[1]!!
                                                )
                                        ),
                                        dayChild(
                                            testChild_2.id,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    testDaycare.operationTimes[1]!!
                                                )
                                        ),
                                    )
                                    .sortedBy { it.childId }
                        ),
                    )
            ),
            res
        )
    }

    @Test
    fun `adding reservations works in a basic case`() {
        postReservations(
            listOf(testChild_1.id, testChild_2.id).flatMap { child ->
                listOf(
                    DailyReservationRequest.Reservations(
                        child,
                        monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        child,
                        tuesday,
                        TimeRange(startTime, endTime),
                    )
                )
            }
        )

        val res = getReservations(FiniteDateRange(monday, wednesday))

        assertEquals(
            listOf(
                ReservationChild(
                    id = testChild_2.id,
                    firstName = testChild_2.firstName,
                    lastName = testChild_2.lastName,
                    preferredName = "",
                    duplicateOf = null,
                    imageId = null,
                    upcomingPlacementType = PlacementType.DAYCARE,
                ),
                ReservationChild(
                    id = testChild_1.id,
                    firstName = testChild_1.firstName,
                    lastName = testChild_1.lastName,
                    preferredName = "",
                    duplicateOf = null,
                    imageId = null,
                    upcomingPlacementType = PlacementType.PRESCHOOL_DAYCARE,
                )
            ),
            res.children
        )

        assertEquals(3, res.days.size)

        res.days[0].let { day ->
            assertEquals(monday, day.date)
            assertEquals(
                listOf(
                        dayChild(
                            testChild_1.id,
                            reservations = listOf(Reservation.Times(startTime, endTime)),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(testDaycare.operationTimes[0]!!)
                        ),
                        dayChild(
                            testChild_2.id,
                            reservations = listOf(Reservation.Times(startTime, endTime)),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(testDaycare.operationTimes[0]!!)
                        )
                    )
                    .sortedBy { it.childId },
                day.children
            )
        }

        res.days[1].let { day ->
            assertEquals(tuesday, day.date)
            assertEquals(
                listOf(
                        dayChild(
                            testChild_1.id,
                            reservations = listOf(Reservation.Times(startTime, endTime)),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(testDaycare.operationTimes[1]!!)
                        ),
                        dayChild(
                            testChild_2.id,
                            reservations = listOf(Reservation.Times(startTime, endTime)),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(testDaycare.operationTimes[1]!!)
                        )
                    )
                    .sortedBy { it.childId },
                day.children
            )
        }

        res.days[2].let { day ->
            assertEquals(wednesday, day.date)
            assertEquals(emptyList(), day.children)
        }
    }

    @Test
    fun `adding a reservation and an absence works`() {
        postReservations(
            listOf(
                DailyReservationRequest.Reservations(
                    testChild_1.id,
                    monday,
                    TimeRange(startTime, endTime),
                ),
                DailyReservationRequest.Absent(
                    testChild_1.id,
                    tuesday,
                )
            )
        )

        val res = getReservations(FiniteDateRange(monday, wednesday))

        assertEquals(3, res.days.size)

        res.days[0].let { day ->
            assertEquals(monday, day.date)
            assertEquals(
                listOf(
                        dayChild(
                            testChild_1.id,
                            reservations = listOf(Reservation.Times(startTime, endTime)),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(testDaycare.operationTimes[0]!!)
                        ),
                        dayChild(
                            testChild_2.id,
                            reservableTimeRange =
                                ReservableTimeRange.Normal(testDaycare.operationTimes[0]!!)
                        )
                    )
                    .sortedBy { it.childId },
                day.children
            )
        }

        res.days[1].let { day ->
            assertEquals(tuesday, day.date)
            assertEquals(
                listOf(
                        dayChild(
                            testChild_1.id,
                            absence =
                                AbsenceInfo(type = AbsenceType.OTHER_ABSENCE, editable = true),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(testDaycare.operationTimes[1]!!)
                        ),
                        dayChild(
                            testChild_2.id,
                            reservableTimeRange =
                                ReservableTimeRange.Normal(testDaycare.operationTimes[1]!!)
                        )
                    )
                    .sortedBy { it.childId },
                day.children
            )
        }

        res.days[2].let { day ->
            assertEquals(wednesday, day.date)
            assertEquals(emptyList(), day.children)
        }
    }

    @Test
    fun `adding reservations fails if past citizen reservation threshold setting`() {
        val request =
            listOf(testChild_1.id, testChild_2.id).flatMap { child ->
                listOf(
                    DailyReservationRequest.Reservations(
                        child,
                        mockToday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        child,
                        mockToday.plusDays(1),
                        TimeRange(startTime, endTime),
                    )
                )
            }

        assertThrows<BadRequest> { postReservations(request) }
    }

    @Test
    fun `adding absences works`() {
        val request =
            AbsenceRequest(
                childIds = setOf(testChild_1.id, testChild_2.id),
                dateRange = FiniteDateRange(fridayLastWeek, wednesday),
                absenceType = AbsenceType.OTHER_ABSENCE
            )
        postAbsences(request)

        val res = getReservations(FiniteDateRange(fridayLastWeek, wednesday))
        assertEquals(6, res.days.size)

        res.days[0].let { day ->
            assertEquals(fridayLastWeek, day.date)
            assertEquals(
                listOf(
                        dayChild(
                            testChild_2.id,
                            // no contract days -> OTHER_ABSENCE was kept
                            absence =
                                AbsenceInfo(type = AbsenceType.OTHER_ABSENCE, editable = true),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(testDaycare.operationTimes[0]!!)
                        )
                    )
                    .sortedBy { it.childId },
                day.children
            )
        }

        res.days[1].let { day ->
            assertEquals(saturdayLastWeek, day.date)
            assertEquals(listOf(), day.children)
        }

        res.days[2].let { day ->
            assertEquals(sundayLastWeek, day.date)
            assertEquals(listOf(), day.children)
        }

        res.days[3].let { day ->
            assertEquals(monday, day.date)
            assertEquals(
                listOf(
                        dayChild(
                            testChild_1.id,
                            absence =
                                AbsenceInfo(type = AbsenceType.OTHER_ABSENCE, editable = true),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(testDaycare.operationTimes[1]!!)
                        ),
                        dayChild(
                            testChild_2.id,
                            absence =
                                // contract days -> OTHER_ABSENCE changed to PLANNED_ABSENCE
                                AbsenceInfo(type = AbsenceType.PLANNED_ABSENCE, editable = true),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(testDaycare.operationTimes[1]!!)
                        )
                    )
                    .sortedBy { it.childId },
                day.children
            )
        }

        res.days[4].let { day ->
            assertEquals(tuesday, day.date)
            assertEquals(
                listOf(
                        dayChild(
                            testChild_1.id,
                            absence =
                                AbsenceInfo(type = AbsenceType.OTHER_ABSENCE, editable = true),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(testDaycare.operationTimes[1]!!)
                        ),
                        dayChild(
                            testChild_2.id,
                            absence =
                                // contract days -> OTHER_ABSENCE changed to PLANNED_ABSENCE
                                AbsenceInfo(type = AbsenceType.PLANNED_ABSENCE, editable = true),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(testDaycare.operationTimes[1]!!)
                        )
                    )
                    .sortedBy { it.childId },
                day.children
            )
        }

        res.days[5].let { day ->
            assertEquals(wednesday, day.date)
            assertEquals(emptyList(), day.children)
        }

        // PRESCHOOL_DAYCARE generates two absences per day (nonbillable and billable)
        assertAbsenceCounts(testChild_1.id, listOf(monday to 2, tuesday to 2))

        // DAYCARE generates one absence per day
        assertAbsenceCounts(
            testChild_2.id,
            listOf(
                fridayLastWeek to 1,
                // TODO: Absences are also generated for the weekend even though the unit is closed.
                // This works but is not ideal.
                saturdayLastWeek to 1,
                sundayLastWeek to 1,
                monday to 1,
                tuesday to 1
            )
        )
    }

    @Test
    fun `cannot add absences to the past`() {
        assertThrows<BadRequest> {
            postAbsences(
                AbsenceRequest(
                    childIds = setOf(testChild_1.id, testChild_2.id),
                    dateRange = FiniteDateRange(mockToday.minusDays(1), mockToday.plusDays(1)),
                    absenceType = AbsenceType.OTHER_ABSENCE
                )
            )
        }
    }

    @Test
    fun `cannot add prohibited absence types`() {
        listOf(
                AbsenceType.FREE_ABSENCE,
                AbsenceType.UNAUTHORIZED_ABSENCE,
                AbsenceType.FORCE_MAJEURE,
                AbsenceType.PARENTLEAVE,
                AbsenceType.UNKNOWN_ABSENCE
            )
            .forEach { absenceType ->
                assertThrows<BadRequest> {
                    postAbsences(
                        AbsenceRequest(
                            childIds = setOf(testChild_1.id, testChild_2.id),
                            dateRange = FiniteDateRange(monday, monday),
                            absenceType = absenceType
                        )
                    )
                }
            }
    }

    @Test
    fun `citizen cannot override an absence that was added by staff`() {
        db.transaction { tx ->
            tx.insertTestAbsence(
                childId = testChild_2.id,
                date = monday,
                category = AbsenceCategory.BILLABLE,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                modifiedBy = EvakaUserId(testStaffId.raw)
            )
        }

        val request =
            AbsenceRequest(
                childIds = setOf(testChild_2.id),
                dateRange = FiniteDateRange(monday, tuesday),
                absenceType = AbsenceType.OTHER_ABSENCE
            )
        postAbsences(request)

        val res = getReservations(FiniteDateRange(monday, tuesday))
        assertEquals(2, res.days.size)

        res.days[0].let { day ->
            assertEquals(monday, day.date)
            assertEquals(
                listOf(
                        dayChild(
                            testChild_1.id,
                            reservableTimeRange =
                                ReservableTimeRange.Normal(testDaycare.operationTimes[0]!!)
                        ),
                        dayChild(
                            testChild_2.id,
                            absence =
                                AbsenceInfo(type = AbsenceType.PLANNED_ABSENCE, editable = false),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(testDaycare.operationTimes[0]!!)
                        ),
                    )
                    .sortedBy { it.childId },
                day.children
            )
        }

        res.days[1].let { day ->
            assertEquals(tuesday, day.date)
            assertEquals(
                listOf(
                        dayChild(
                            testChild_1.id,
                            reservableTimeRange =
                                ReservableTimeRange.Normal(testDaycare.operationTimes[1]!!)
                        ),
                        dayChild(
                            testChild_2.id,
                            absence =
                                // contract days -> OTHER_ABSENCE changed to PLANNED_ABSENCE
                                AbsenceInfo(type = AbsenceType.PLANNED_ABSENCE, editable = true),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(testDaycare.operationTimes[1]!!)
                        )
                    )
                    .sortedBy { it.childId },
                day.children
            )
        }

        assertAbsenceCounts(testChild_2.id, listOf(monday to 1, tuesday to 1))
    }

    @Test
    fun `citizen can override billable planned absence in contract day placement with sick leave before threshold`() {
        db.transaction { tx ->
            tx.insertTestServiceNeed(
                confirmedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                placementId = testChild1PlacementId,
                period = FiniteDateRange(monday, tuesday),
                optionId = snPreschoolDaycareContractDays13.id,
                shiftCare = ShiftCareType.NONE
            )
            tx.insertTestAbsence(
                childId = testChild_1.id,
                date = monday,
                category = AbsenceCategory.NONBILLABLE,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                modifiedBy = EvakaUserId(testAdult_1.id.raw)
            )
            tx.insertTestAbsence(
                childId = testChild_1.id,
                date = monday,
                category = AbsenceCategory.BILLABLE,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                modifiedBy = EvakaUserId(testAdult_1.id.raw)
            )
        }

        postAbsences(
            AbsenceRequest(
                childIds = setOf(testChild_1.id),
                dateRange = FiniteDateRange(monday, tuesday),
                absenceType = AbsenceType.SICKLEAVE
            ),
            clock =
                MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2021, 11, 8), LocalTime.of(15, 0)))
        )

        assertThat(
                db.read { tx ->
                    tx.getAbsencesOfChildByRange(testChild_1.id, DateRange(monday, tuesday))
                }
            )
            .extracting({ it.date }, { it.absenceType }, { it.category })
            .containsExactlyInAnyOrder(
                Tuple(monday, AbsenceType.SICKLEAVE, AbsenceCategory.NONBILLABLE),
                Tuple(monday, AbsenceType.SICKLEAVE, AbsenceCategory.BILLABLE),
                Tuple(tuesday, AbsenceType.SICKLEAVE, AbsenceCategory.NONBILLABLE),
                Tuple(tuesday, AbsenceType.SICKLEAVE, AbsenceCategory.BILLABLE),
            )
    }

    @Test
    fun `citizen can override billable planned absence in non contract day placement with sick leave after threshold`() {
        db.transaction { tx ->
            tx.insertTestAbsence(
                childId = testChild_1.id,
                date = monday,
                category = AbsenceCategory.NONBILLABLE,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                modifiedBy = EvakaUserId(testAdult_1.id.raw)
            )
            tx.insertTestAbsence(
                childId = testChild_1.id,
                date = monday,
                category = AbsenceCategory.BILLABLE,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                modifiedBy = EvakaUserId(testAdult_1.id.raw)
            )
        }

        postAbsences(
            AbsenceRequest(
                childIds = setOf(testChild_1.id),
                dateRange = FiniteDateRange(monday, tuesday),
                absenceType = AbsenceType.SICKLEAVE
            ),
            clock =
                MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2021, 11, 8), LocalTime.of(21, 0)))
        )

        assertThat(
                db.read { tx ->
                    tx.getAbsencesOfChildByRange(testChild_1.id, DateRange(monday, tuesday))
                }
            )
            .extracting({ it.date }, { it.absenceType }, { it.category })
            .containsExactlyInAnyOrder(
                Tuple(monday, AbsenceType.SICKLEAVE, AbsenceCategory.NONBILLABLE),
                Tuple(monday, AbsenceType.SICKLEAVE, AbsenceCategory.BILLABLE),
                Tuple(tuesday, AbsenceType.SICKLEAVE, AbsenceCategory.NONBILLABLE),
                Tuple(tuesday, AbsenceType.SICKLEAVE, AbsenceCategory.BILLABLE),
            )
    }

    @Test
    fun `citizen cannot override billable planned absence in contract day placement with sick leave after threshold`() {
        db.transaction { tx ->
            tx.insertTestServiceNeed(
                confirmedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                placementId = testChild1PlacementId,
                period = FiniteDateRange(monday, tuesday),
                optionId = snPreschoolDaycareContractDays13.id,
                shiftCare = ShiftCareType.NONE
            )
            tx.insertTestAbsence(
                childId = testChild_1.id,
                date = monday,
                category = AbsenceCategory.NONBILLABLE,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                modifiedBy = EvakaUserId(testAdult_1.id.raw)
            )
            tx.insertTestAbsence(
                childId = testChild_1.id,
                date = monday,
                category = AbsenceCategory.BILLABLE,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                modifiedBy = EvakaUserId(testAdult_1.id.raw)
            )
        }

        postAbsences(
            AbsenceRequest(
                childIds = setOf(testChild_1.id),
                dateRange = FiniteDateRange(monday, tuesday),
                absenceType = AbsenceType.SICKLEAVE
            ),
            clock =
                MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2021, 11, 8), LocalTime.of(21, 0)))
        )

        assertThat(
                db.read { tx ->
                    tx.getAbsencesOfChildByRange(testChild_1.id, DateRange(monday, tuesday))
                }
            )
            .extracting({ it.date }, { it.absenceType }, { it.category })
            .containsExactlyInAnyOrder(
                Tuple(monday, AbsenceType.SICKLEAVE, AbsenceCategory.NONBILLABLE),
                Tuple(monday, AbsenceType.PLANNED_ABSENCE, AbsenceCategory.BILLABLE),
                Tuple(tuesday, AbsenceType.SICKLEAVE, AbsenceCategory.NONBILLABLE),
                Tuple(tuesday, AbsenceType.SICKLEAVE, AbsenceCategory.BILLABLE),
            )
    }

    @Test
    fun `cannot add absences to day which already contains attendance`() {
        db.transaction { tx ->
            tx.insertTestChildAttendance(
                unitId = testDaycare.id,
                childId = testChild_1.id,
                arrived = HelsinkiDateTime.of(mockToday, LocalTime.of(9, 0, 0)),
                departed = HelsinkiDateTime.of(mockToday, LocalTime.of(11, 0, 0)),
            )
        }
        assertThrows<BadRequest> {
            postAbsences(
                AbsenceRequest(
                    childIds = setOf(testChild_1.id, testChild_2.id),
                    dateRange = FiniteDateRange(mockToday, mockToday),
                    absenceType = AbsenceType.SICKLEAVE
                )
            )
        }
    }

    private fun dayChild(
        childId: ChildId,
        reservableTimeRange: ReservableTimeRange,
        scheduleType: ScheduleType = ScheduleType.RESERVATION_REQUIRED,
        shiftCare: Boolean = false,
        absence: AbsenceInfo? = null,
        reservations: List<Reservation> = emptyList(),
        attendances: List<OpenTimeRange> = emptyList(),
    ) =
        ReservationResponseDayChild(
            childId,
            scheduleType,
            shiftCare,
            absence,
            reservations,
            attendances,
            reservableTimeRange
        )

    private fun postReservations(request: List<DailyReservationRequest>) {
        reservationControllerCitizen.postReservations(
            dbInstance(),
            AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG),
            MockEvakaClock(HelsinkiDateTime.of(mockToday, LocalTime.of(0, 0))),
            request
        )
    }

    private fun postAbsences(
        request: AbsenceRequest,
        clock: EvakaClock = MockEvakaClock(HelsinkiDateTime.of(mockToday, LocalTime.of(12, 0)))
    ) {
        reservationControllerCitizen.postAbsences(
            dbInstance(),
            AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG),
            clock,
            request,
        )
    }

    private fun getReservations(range: FiniteDateRange): ReservationsResponse {
        return reservationControllerCitizen.getReservations(
            dbInstance(),
            AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG),
            MockEvakaClock(HelsinkiDateTime.of(mockToday, LocalTime.of(12, 0))),
            range.start,
            range.end,
        )
    }

    private fun assertAbsenceCounts(childId: ChildId, counts: List<Pair<LocalDate, Int>>) {
        data class QueryResult(val date: LocalDate, val count: Int)

        val expected = counts.map { QueryResult(it.first, it.second) }
        val actual =
            db.read {
                it.createQuery(
                        """
                SELECT date, COUNT(category) as count
                FROM absence WHERE
                child_id = :childId
                GROUP BY date
                ORDER BY date
            """
                    )
                    .bind("childId", childId)
                    .toList<QueryResult>()
            }

        assertEquals(expected, actual)
    }
}
