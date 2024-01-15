// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.allWeekOpTimes
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesController
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesValue
import fi.espoo.evaka.daycare.insertPreschoolTerm
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.getAbsencesOfChildByRange
import fi.espoo.evaka.espoo.EspooActionRuleMapping
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.ScheduleType
import fi.espoo.evaka.preschoolTerm2021
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertServiceNeedOption
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.dev.insertTestHoliday
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestServiceNeed
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.snDaycareContractDays10
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.snPreschoolDaycareContractDays13
import io.opentracing.noop.NoopTracerFactory
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ReservationControllerCitizenIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var reservationControllerCitizen: ReservationControllerCitizen

    // Monday
    private val mockToday: LocalDate = LocalDate.of(2021, 11, 1)
    private val beforeThreshold = HelsinkiDateTime.of(mockToday, LocalTime.of(12, 0))
    private val afterThreshold = HelsinkiDateTime.of(mockToday.plusDays(7), LocalTime.of(21, 0))

    private val monday = LocalDate.of(2021, 11, 15)
    private val tuesday = monday.plusDays(1)
    private val wednesday = monday.plusDays(2)
    private val thursday = monday.plusDays(3)

    private val sundayLastWeek = monday.minusDays(1)
    private val saturdayLastWeek = monday.minusDays(2)
    private val fridayLastWeek = monday.minusDays(3)

    private val startTime = LocalTime.of(9, 0)
    private val endTime = LocalTime.of(17, 0)

    @BeforeEach
    fun before() {
        db.transaction { tx ->
            tx.insertServiceNeedOptions()
            tx.insertServiceNeedOption(snPreschoolDaycareContractDays13)
        }
    }

    @Test
    fun `get reservations returns correct children every day in range`() {
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS))
        val employee = DevEmployee()

        val adult = DevPerson()
        val child1 = DevPerson(dateOfBirth = LocalDate.of(2015, 1, 1))
        val child2 = DevPerson(dateOfBirth = LocalDate.of(2016, 1, 1))
        val child3 = DevPerson(dateOfBirth = LocalDate.of(2017, 1, 1))

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(employee)

            tx.insert(adult, DevPersonType.ADULT)
            listOf(child1, child2, child3).forEach { child ->
                tx.insert(child, DevPersonType.CHILD)
                tx.insertGuardian(adult.id, child.id)
            }

            tx.insertTestPlacement(
                childId = child1.id,
                unitId = daycare.id,
                type = PlacementType.PRESCHOOL_DAYCARE,
                startDate = monday,
                endDate = tuesday
            )
            tx.insertTestPlacement(
                    childId = child2.id,
                    unitId = daycare.id,
                    type = PlacementType.DAYCARE,
                    startDate = fridayLastWeek,
                    endDate = tuesday
                )
                .also { placementId ->
                    // contract days on monday and tuesday
                    tx.insertTestServiceNeed(
                        confirmedBy = employee.evakaUserId,
                        placementId = placementId,
                        period = FiniteDateRange(monday, tuesday),
                        optionId = snDaycareContractDays10.id,
                        shiftCare = ShiftCareType.NONE
                    )
                }

            // Fixed schedule (PRESCHOOL)
            tx.insertTestPlacement(
                childId = child3.id,
                unitId = daycare.id,
                type = PlacementType.PRESCHOOL,
                startDate = monday,
                endDate = thursday
            )

            // Holiday on wednesday
            tx.insertTestHoliday(wednesday)

            // Term break on thursday
            tx.insertPreschoolTerm(
                preschoolTerm2021.copy(termBreaks = DateSet.of(FiniteDateRange(thursday, thursday)))
            )
        }

        val res =
            getReservations(
                adult.user(CitizenAuthLevel.WEAK),
                FiniteDateRange(sundayLastWeek, thursday)
            )

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
                            id = child1.id,
                            firstName = child1.firstName,
                            lastName = child1.lastName,
                            preferredName = "",
                            duplicateOf = null,
                            imageId = null,
                            upcomingPlacementType = PlacementType.PRESCHOOL_DAYCARE,
                        ),
                        ReservationChild(
                            id = child2.id,
                            firstName = child2.firstName,
                            lastName = child2.lastName,
                            preferredName = "",
                            duplicateOf = null,
                            imageId = null,
                            upcomingPlacementType = PlacementType.DAYCARE,
                        ),
                        ReservationChild(
                            id = child3.id,
                            firstName = child3.firstName,
                            lastName = child3.lastName,
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
                                            child1.id,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    daycare.operationTimes[0]!!
                                                )
                                        ),
                                        dayChild(
                                            child2.id,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    daycare.operationTimes[0]!!
                                                )
                                        ),
                                        dayChild(
                                            child3.id,
                                            scheduleType = ScheduleType.FIXED_SCHEDULE,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    daycare.operationTimes[0]!!
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
                                            child1.id,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    daycare.operationTimes[1]!!
                                                )
                                        ),
                                        dayChild(
                                            child2.id,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    daycare.operationTimes[1]!!
                                                )
                                        ),
                                        dayChild(
                                            child3.id,
                                            scheduleType = ScheduleType.FIXED_SCHEDULE,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    daycare.operationTimes[1]!!
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
                                        child3.id,
                                        scheduleType = ScheduleType.TERM_BREAK,
                                        reservableTimeRange =
                                            ReservableTimeRange.Normal(daycare.operationTimes[0]!!)
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
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS))
        val roundTheClockDaycare =
            DevDaycare(
                areaId = area.id,
                enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
                roundTheClock = true,
                operationTimes = allWeekOpTimes,
            )
        val employee = DevEmployee()

        val adult = DevPerson()
        val child1 = DevPerson(dateOfBirth = LocalDate.of(2015, 1, 1))
        val child2 = DevPerson(dateOfBirth = LocalDate.of(2016, 1, 1))

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(roundTheClockDaycare)
            tx.insert(employee)

            tx.insert(adult, DevPersonType.ADULT)
            listOf(child1, child2).forEach { child ->
                tx.insert(child, DevPersonType.CHILD)
                tx.insertGuardian(adult.id, child.id)
            }

            // Normal shift care
            tx.insertTestPlacement(
                childId = child1.id,
                unitId = roundTheClockDaycare.id,
                type = PlacementType.DAYCARE,
                startDate = sundayLastWeek,
                endDate = tuesday
            )

            // Intermittent shift care
            tx.insertTestPlacement(
                    childId = child2.id,
                    unitId = daycare.id,
                    type = PlacementType.DAYCARE,
                    startDate = sundayLastWeek,
                    endDate = wednesday
                )
                .also { placementId ->
                    tx.insertTestServiceNeed(
                        confirmedBy = employee.evakaUserId,
                        placementId = placementId,
                        period = FiniteDateRange(sundayLastWeek, tuesday),
                        optionId = snDaycareFullDay35.id,
                        shiftCare = ShiftCareType.INTERMITTENT,
                    )
                }

            tx.insertTestHoliday(tuesday)
        }

        val res =
            getReservations(
                adult.user(CitizenAuthLevel.WEAK),
                FiniteDateRange(sundayLastWeek, wednesday)
            )

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
                            id = child1.id,
                            firstName = child1.firstName,
                            lastName = child1.lastName,
                            preferredName = "",
                            duplicateOf = null,
                            imageId = null,
                            upcomingPlacementType = PlacementType.DAYCARE,
                        ),
                        ReservationChild(
                            id = child2.id,
                            firstName = child2.firstName,
                            lastName = child2.lastName,
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
                                            child1.id,
                                            shiftCare = true,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    roundTheClockDaycare.operationTimes[6]!!
                                                )
                                        ),
                                        dayChild(
                                            child2.id,
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
                                            child1.id,
                                            shiftCare = true,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    roundTheClockDaycare.operationTimes[0]!!
                                                )
                                        ),
                                        dayChild(
                                            child2.id,
                                            shiftCare = true,
                                            reservableTimeRange =
                                                ReservableTimeRange.IntermittentShiftCare(
                                                    daycare.operationTimes[0]!!
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
                                            child1.id,
                                            shiftCare = true,
                                            reservableTimeRange =
                                                ReservableTimeRange.Normal(
                                                    daycare.operationTimes[1]!!
                                                )
                                        ),
                                        dayChild(
                                            child2.id,
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
                                        child2.id,
                                        shiftCare = false,
                                        reservableTimeRange =
                                            ReservableTimeRange.Normal(daycare.operationTimes[2]!!)
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
        val dailyServiceTimesController =
            DailyServiceTimesController(
                AccessControl(EspooActionRuleMapping(), NoopTracerFactory.create())
            )

        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS))
        val employee = DevEmployee()

        val adult = DevPerson()
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(employee)
            tx.insertDaycareAclRow(daycare.id, employee.id, UserRole.UNIT_SUPERVISOR)

            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(adult.id, child.id)

            tx.insertTestPlacement(
                childId = child.id,
                unitId = daycare.id,
                type = PlacementType.DAYCARE,
                startDate = monday,
                endDate = tuesday
            )
        }

        dailyServiceTimesController.postDailyServiceTimes(
            dbInstance(),
            employee.user(setOf()),
            MockEvakaClock(HelsinkiDateTime.of(mockToday, LocalTime.of(12, 0))),
            child.id,
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

        val res =
            getReservations(adult.user(CitizenAuthLevel.WEAK), FiniteDateRange(monday, tuesday))

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
                            id = child.id,
                            firstName = child.firstName,
                            lastName = child.lastName,
                            preferredName = "",
                            duplicateOf = null,
                            imageId = null,
                            upcomingPlacementType = PlacementType.DAYCARE,
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
                                        child.id,
                                        absence =
                                            AbsenceInfo(
                                                AbsenceType.OTHER_ABSENCE,
                                                editable = false
                                            ),
                                        reservableTimeRange =
                                            ReservableTimeRange.Normal(daycare.operationTimes[0]!!)
                                    ),
                                )
                        ),
                        ReservationResponseDay(
                            date = tuesday,
                            holiday = false,
                            children =
                                listOf(
                                    dayChild(
                                        child.id,
                                        reservableTimeRange =
                                            ReservableTimeRange.Normal(daycare.operationTimes[1]!!)
                                    ),
                                )
                        ),
                    )
            ),
            res
        )
    }

    @Test
    fun `adding reservations works in a basic case`() {
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS))
        val employee = DevEmployee()

        val adult = DevPerson()
        val child1 = DevPerson()
        val child2 = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(employee)

            tx.insert(adult, DevPersonType.ADULT)
            listOf(child1, child2).forEach { child ->
                tx.insert(child, DevPersonType.CHILD)
                tx.insertGuardian(adult.id, child.id)
            }

            tx.insertTestPlacement(
                childId = child1.id,
                unitId = daycare.id,
                type = PlacementType.PRESCHOOL_DAYCARE,
                startDate = monday,
                endDate = tuesday
            )
            tx.insertTestPlacement(
                childId = child2.id,
                unitId = daycare.id,
                type = PlacementType.DAYCARE,
                startDate = fridayLastWeek,
                endDate = tuesday
            )
        }

        postReservations(
            adult.user(CitizenAuthLevel.WEAK),
            listOf(child1.id, child2.id).flatMap { child ->
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

        val res =
            getReservations(adult.user(CitizenAuthLevel.WEAK), FiniteDateRange(monday, wednesday))

        assertEquals(
            listOf(
                ReservationChild(
                    id = child1.id,
                    firstName = child1.firstName,
                    lastName = child1.lastName,
                    preferredName = "",
                    duplicateOf = null,
                    imageId = null,
                    upcomingPlacementType = PlacementType.PRESCHOOL_DAYCARE,
                ),
                ReservationChild(
                    id = child2.id,
                    firstName = child2.firstName,
                    lastName = child2.lastName,
                    preferredName = "",
                    duplicateOf = null,
                    imageId = null,
                    upcomingPlacementType = PlacementType.DAYCARE,
                ),
            ),
            res.children
        )

        assertEquals(3, res.days.size)

        res.days[0].let { day ->
            assertEquals(monday, day.date)
            assertEquals(
                listOf(
                        dayChild(
                            child1.id,
                            reservations = listOf(ReservationDto.Times(startTime, endTime, false)),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(daycare.operationTimes[0]!!)
                        ),
                        dayChild(
                            child2.id,
                            reservations = listOf(ReservationDto.Times(startTime, endTime, false)),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(daycare.operationTimes[0]!!)
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
                            child1.id,
                            reservations = listOf(ReservationDto.Times(startTime, endTime, false)),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(daycare.operationTimes[1]!!)
                        ),
                        dayChild(
                            child2.id,
                            reservations = listOf(ReservationDto.Times(startTime, endTime, false)),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(daycare.operationTimes[1]!!)
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
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS))
        val employee = DevEmployee()

        val adult = DevPerson()
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(employee)

            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(adult.id, child.id)

            tx.insertTestPlacement(
                childId = child.id,
                unitId = daycare.id,
                type = PlacementType.PRESCHOOL_DAYCARE,
                startDate = monday,
                endDate = tuesday
            )
        }

        postReservations(
            adult.user(CitizenAuthLevel.WEAK),
            listOf(
                DailyReservationRequest.Reservations(
                    child.id,
                    monday,
                    TimeRange(startTime, endTime),
                ),
                DailyReservationRequest.Absent(
                    child.id,
                    tuesday,
                )
            )
        )

        val res =
            getReservations(adult.user(CitizenAuthLevel.WEAK), FiniteDateRange(monday, wednesday))

        assertEquals(3, res.days.size)

        res.days[0].let { day ->
            assertEquals(monday, day.date)
            assertEquals(
                listOf(
                    dayChild(
                        child.id,
                        reservations = listOf(ReservationDto.Times(startTime, endTime, false)),
                        reservableTimeRange =
                            ReservableTimeRange.Normal(daycare.operationTimes[0]!!)
                    )
                ),
                day.children
            )
        }

        res.days[1].let { day ->
            assertEquals(tuesday, day.date)
            assertEquals(
                listOf(
                        dayChild(
                            child.id,
                            absence =
                                AbsenceInfo(type = AbsenceType.OTHER_ABSENCE, editable = true),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(daycare.operationTimes[1]!!)
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
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS))
        val employee = DevEmployee()

        val adult = DevPerson()
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(employee)

            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(adult.id, child.id)

            tx.insertTestPlacement(
                childId = child.id,
                unitId = daycare.id,
                type = PlacementType.PRESCHOOL_DAYCARE,
                startDate = monday,
                endDate = tuesday
            )
        }

        val request =
            listOf(
                DailyReservationRequest.Reservations(
                    child.id,
                    mockToday,
                    TimeRange(startTime, endTime),
                ),
                DailyReservationRequest.Reservations(
                    child.id,
                    mockToday.plusDays(1),
                    TimeRange(startTime, endTime),
                )
            )

        assertThrows<BadRequest> { postReservations(adult.user(CitizenAuthLevel.WEAK), request) }
            .also { exception -> assertEquals("Some days are not reservable", exception.message) }
    }

    @Test
    fun `adding absences works`() {
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS))
        val employee = DevEmployee()

        val adult = DevPerson()
        val child1 = DevPerson()
        val child2 = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(employee)

            tx.insert(adult, DevPersonType.ADULT)
            listOf(child1, child2).forEach { child ->
                tx.insert(child, DevPersonType.CHILD)
                tx.insertGuardian(adult.id, child.id)
            }

            tx.insertTestPlacement(
                childId = child1.id,
                unitId = daycare.id,
                type = PlacementType.PRESCHOOL_DAYCARE,
                startDate = monday,
                endDate = tuesday
            )
            tx.insertTestPlacement(
                    childId = child2.id,
                    unitId = daycare.id,
                    type = PlacementType.DAYCARE,
                    startDate = fridayLastWeek,
                    endDate = tuesday
                )
                .also { placementId ->
                    // contract days on monday and tuesday
                    tx.insertTestServiceNeed(
                        confirmedBy = employee.evakaUserId,
                        placementId = placementId,
                        period = FiniteDateRange(monday, tuesday),
                        optionId = snDaycareContractDays10.id,
                        shiftCare = ShiftCareType.NONE
                    )
                }
        }

        val request =
            AbsenceRequest(
                childIds = setOf(child1.id, child2.id),
                dateRange = FiniteDateRange(fridayLastWeek, wednesday),
                absenceType = AbsenceType.OTHER_ABSENCE
            )
        postAbsences(adult.user(CitizenAuthLevel.WEAK), request)

        val res =
            getReservations(
                adult.user(CitizenAuthLevel.WEAK),
                FiniteDateRange(fridayLastWeek, wednesday)
            )
        assertEquals(6, res.days.size)

        res.days[0].let { day ->
            assertEquals(fridayLastWeek, day.date)
            assertEquals(
                listOf(
                    dayChild(
                        child2.id,
                        // no contract days -> OTHER_ABSENCE was kept
                        absence = AbsenceInfo(type = AbsenceType.OTHER_ABSENCE, editable = true),
                        reservableTimeRange =
                            ReservableTimeRange.Normal(daycare.operationTimes[0]!!)
                    )
                ),
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
                            child1.id,
                            absence =
                                AbsenceInfo(type = AbsenceType.OTHER_ABSENCE, editable = true),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(daycare.operationTimes[1]!!)
                        ),
                        dayChild(
                            child2.id,
                            absence =
                                // contract days -> OTHER_ABSENCE changed to PLANNED_ABSENCE
                                AbsenceInfo(type = AbsenceType.PLANNED_ABSENCE, editable = true),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(daycare.operationTimes[1]!!)
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
                            child1.id,
                            absence =
                                AbsenceInfo(type = AbsenceType.OTHER_ABSENCE, editable = true),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(daycare.operationTimes[1]!!)
                        ),
                        dayChild(
                            child2.id,
                            absence =
                                // contract days -> OTHER_ABSENCE changed to PLANNED_ABSENCE
                                AbsenceInfo(type = AbsenceType.PLANNED_ABSENCE, editable = true),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(daycare.operationTimes[1]!!)
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
        assertAbsenceCounts(child1.id, listOf(monday to 2, tuesday to 2))

        // DAYCARE generates one absence per day
        assertAbsenceCounts(
            child2.id,
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
        val adult = DevPerson()
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(adult.id, child.id)
        }

        assertThrows<BadRequest> {
                postAbsences(
                    adult.user(CitizenAuthLevel.WEAK),
                    AbsenceRequest(
                        childIds = setOf(child.id),
                        dateRange = FiniteDateRange(mockToday.minusDays(1), mockToday.plusDays(1)),
                        absenceType = AbsenceType.OTHER_ABSENCE
                    )
                )
            }
            .also { exception ->
                assertEquals("Cannot mark absences for past days", exception.message)
            }
    }

    @Test
    fun `cannot add prohibited absence types`() {
        val adult = DevPerson()
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(adult.id, child.id)
        }

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
                            adult.user(CitizenAuthLevel.WEAK),
                            AbsenceRequest(
                                childIds = setOf(child.id),
                                dateRange = FiniteDateRange(monday, monday),
                                absenceType = absenceType
                            )
                        )
                    }
                    .also { exception -> assertEquals("Invalid absence type", exception.message) }
            }
    }

    @Test
    fun `citizen cannot override an absence that was added by staff`() {
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS))
        val employee = DevEmployee()

        val adult = DevPerson()
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(employee)

            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(adult.id, child.id)

            tx.insertTestPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    type = PlacementType.DAYCARE,
                    startDate = monday,
                    endDate = tuesday
                )
                .let { placementId ->
                    tx.insertTestServiceNeed(
                        confirmedBy = employee.evakaUserId,
                        placementId = placementId,
                        period = FiniteDateRange(monday, tuesday),
                        optionId = snDaycareContractDays10.id
                    )
                }

            tx.insertTestAbsence(
                childId = child.id,
                date = monday,
                category = AbsenceCategory.BILLABLE,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                modifiedBy = employee.evakaUserId,
            )
        }

        val request =
            AbsenceRequest(
                childIds = setOf(child.id),
                dateRange = FiniteDateRange(monday, tuesday),
                absenceType = AbsenceType.OTHER_ABSENCE
            )
        postAbsences(adult.user(CitizenAuthLevel.WEAK), request)

        val res =
            getReservations(adult.user(CitizenAuthLevel.WEAK), FiniteDateRange(monday, tuesday))
        assertEquals(2, res.days.size)

        res.days[0].let { day ->
            assertEquals(monday, day.date)
            assertEquals(
                listOf(
                        dayChild(
                            child.id,
                            absence =
                                AbsenceInfo(type = AbsenceType.PLANNED_ABSENCE, editable = false),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(daycare.operationTimes[0]!!)
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
                            child.id,
                            absence =
                                // contract days -> OTHER_ABSENCE changed to PLANNED_ABSENCE
                                AbsenceInfo(type = AbsenceType.PLANNED_ABSENCE, editable = true),
                            reservableTimeRange =
                                ReservableTimeRange.Normal(daycare.operationTimes[1]!!)
                        )
                    )
                    .sortedBy { it.childId },
                day.children
            )
        }

        assertAbsenceCounts(child.id, listOf(monday to 1, tuesday to 1))
    }

    @Test
    fun `citizen can override billable planned absence in contract day placement with sick leave before threshold`() {
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS))
        val employee = DevEmployee()

        val adult = DevPerson()
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(employee)

            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(adult.id, child.id)

            tx.insertTestPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    startDate = monday,
                    endDate = tuesday
                )
                .let { placementId ->
                    tx.insertTestServiceNeed(
                        confirmedBy = employee.evakaUserId,
                        placementId = placementId,
                        period = FiniteDateRange(monday, tuesday),
                        optionId = snPreschoolDaycareContractDays13.id
                    )
                }

            tx.insertTestAbsence(
                childId = child.id,
                date = monday,
                category = AbsenceCategory.NONBILLABLE,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                modifiedBy = adult.evakaUserId,
            )
            tx.insertTestAbsence(
                childId = child.id,
                date = monday,
                category = AbsenceCategory.BILLABLE,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                modifiedBy = adult.evakaUserId,
            )
        }

        postAbsences(
            adult.user(CitizenAuthLevel.WEAK),
            AbsenceRequest(
                childIds = setOf(child.id),
                dateRange = FiniteDateRange(monday, tuesday),
                absenceType = AbsenceType.SICKLEAVE
            ),
            mockNow = beforeThreshold
        )

        assertThat(
                db.read { tx -> tx.getAbsencesOfChildByRange(child.id, DateRange(monday, tuesday)) }
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
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS))
        val employee = DevEmployee()

        val adult = DevPerson()
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(employee)

            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(adult.id, child.id)

            tx.insertTestPlacement(
                childId = child.id,
                unitId = daycare.id,
                type = PlacementType.PRESCHOOL_DAYCARE,
                startDate = monday,
                endDate = tuesday
            )

            tx.insertTestAbsence(
                childId = child.id,
                date = monday,
                category = AbsenceCategory.NONBILLABLE,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                modifiedBy = adult.evakaUserId,
            )
            tx.insertTestAbsence(
                childId = child.id,
                date = monday,
                category = AbsenceCategory.BILLABLE,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                modifiedBy = adult.evakaUserId,
            )
        }

        postAbsences(
            adult.user(CitizenAuthLevel.WEAK),
            AbsenceRequest(
                childIds = setOf(child.id),
                dateRange = FiniteDateRange(monday, tuesday),
                absenceType = AbsenceType.SICKLEAVE
            ),
            mockNow = afterThreshold
        )

        assertThat(
                db.read { tx -> tx.getAbsencesOfChildByRange(child.id, DateRange(monday, tuesday)) }
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
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS))
        val employee = DevEmployee()

        val adult = DevPerson()
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(employee)

            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(adult.id, child.id)

            tx.insertTestPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    startDate = monday,
                    endDate = tuesday
                )
                .let { placementId ->
                    tx.insertTestServiceNeed(
                        confirmedBy = employee.evakaUserId,
                        placementId = placementId,
                        period = FiniteDateRange(monday, tuesday),
                        optionId = snPreschoolDaycareContractDays13.id,
                    )
                }

            tx.insertTestAbsence(
                childId = child.id,
                date = monday,
                category = AbsenceCategory.NONBILLABLE,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                modifiedBy = adult.evakaUserId
            )
            tx.insertTestAbsence(
                childId = child.id,
                date = monday,
                category = AbsenceCategory.BILLABLE,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                modifiedBy = adult.evakaUserId
            )
        }

        postAbsences(
            adult.user(CitizenAuthLevel.WEAK),
            AbsenceRequest(
                childIds = setOf(child.id),
                dateRange = FiniteDateRange(monday, tuesday),
                absenceType = AbsenceType.SICKLEAVE
            ),
            mockNow = afterThreshold
        )

        assertThat(
                db.read { tx -> tx.getAbsencesOfChildByRange(child.id, DateRange(monday, tuesday)) }
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
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS))

        val adult = DevPerson()
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)

            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(adult.id, child.id)

            tx.insertTestChildAttendance(
                unitId = daycare.id,
                childId = child.id,
                arrived = HelsinkiDateTime.of(mockToday, LocalTime.of(9, 0, 0)),
                departed = HelsinkiDateTime.of(mockToday, LocalTime.of(11, 0, 0)),
            )
        }
        assertThrows<BadRequest> {
                postAbsences(
                    adult.user(CitizenAuthLevel.WEAK),
                    AbsenceRequest(
                        childIds = setOf(child.id, child.id),
                        dateRange = FiniteDateRange(mockToday, mockToday),
                        absenceType = AbsenceType.SICKLEAVE
                    )
                )
            }
            .also { exception ->
                assertEquals("ATTENDANCE_ALREADY_EXISTS", exception.errorCode)
                assertEquals("Attendance already exists for given dates", exception.message)
            }
    }

    @Test
    fun `adding a reservation before threshold removes absences on the same day`() {
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS))
        val employee = DevEmployee()

        val child = DevPerson()
        val adult = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(employee)

            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(adult.id, child.id)

            tx.insertTestPlacement(
                childId = child.id,
                unitId = daycare.id,
                type = PlacementType.DAYCARE,
                startDate = monday,
                endDate = monday
            )

            tx.insertTestAbsence(
                childId = child.id,
                date = monday,
                category = AbsenceCategory.BILLABLE,
                absenceType = AbsenceType.OTHER_ABSENCE,
                modifiedBy = adult.evakaUserId,
            )
        }

        postReservations(
            adult.user(CitizenAuthLevel.WEAK),
            listOf(
                DailyReservationRequest.Reservations(
                    child.id,
                    monday,
                    TimeRange(startTime, endTime),
                ),
            ),
            mockNow = beforeThreshold
        )

        assertAbsenceCounts(child.id, listOf())
    }

    @Test
    fun `adding an absence before threshold removes reservations on the same day`() {
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS))
        val employee = DevEmployee()

        val child = DevPerson()
        val adult = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(employee)

            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(adult.id, child.id)

            tx.insertTestPlacement(
                childId = child.id,
                unitId = daycare.id,
                type = PlacementType.DAYCARE,
                startDate = monday,
                endDate = monday
            )

            tx.insert(
                DevReservation(
                    childId = child.id,
                    date = monday,
                    startTime = startTime,
                    endTime = endTime,
                    createdBy = adult.evakaUserId,
                )
            )
        }

        postAbsences(
            adult.user(CitizenAuthLevel.WEAK),
            AbsenceRequest(
                childIds = setOf(child.id),
                dateRange = FiniteDateRange(monday, monday),
                absenceType = AbsenceType.OTHER_ABSENCE
            ),
            mockNow = beforeThreshold
        )

        assertAbsenceCounts(child.id, listOf(monday to 1))
        assertReservationCounts(child.id, listOf())
    }

    @Test
    fun `adding an absence after threshold keeps reservations on the same day`() {
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS))
        val employee = DevEmployee()

        val child = DevPerson()
        val adult = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(employee)

            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(adult.id, child.id)

            tx.insertTestPlacement(
                childId = child.id,
                unitId = daycare.id,
                type = PlacementType.DAYCARE,
                startDate = monday,
                endDate = monday
            )

            tx.insert(
                DevReservation(
                    childId = child.id,
                    date = monday,
                    startTime = startTime,
                    endTime = endTime,
                    createdBy = adult.evakaUserId,
                )
            )
        }

        postAbsences(
            adult.user(CitizenAuthLevel.WEAK),
            AbsenceRequest(
                childIds = setOf(child.id),
                dateRange = FiniteDateRange(monday, monday),
                absenceType = AbsenceType.OTHER_ABSENCE
            ),
            mockNow = afterThreshold
        )

        assertAbsenceCounts(child.id, listOf(monday to 1))
        assertReservationCounts(child.id, listOf(monday to 1))
    }

    private fun dayChild(
        childId: ChildId,
        reservableTimeRange: ReservableTimeRange,
        scheduleType: ScheduleType = ScheduleType.RESERVATION_REQUIRED,
        shiftCare: Boolean = false,
        absence: AbsenceInfo? = null,
        reservations: List<ReservationDto> = emptyList(),
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

    private fun postReservations(
        user: AuthenticatedUser.Citizen,
        request: List<DailyReservationRequest>,
        mockNow: HelsinkiDateTime = beforeThreshold,
    ) {
        reservationControllerCitizen.postReservations(
            dbInstance(),
            user,
            MockEvakaClock(mockNow),
            request
        )
    }

    private fun postAbsences(
        user: AuthenticatedUser.Citizen,
        request: AbsenceRequest,
        mockNow: HelsinkiDateTime = beforeThreshold
    ) {
        reservationControllerCitizen.postAbsences(
            dbInstance(),
            user,
            MockEvakaClock(mockNow),
            request,
        )
    }

    private fun getReservations(
        user: AuthenticatedUser.Citizen,
        range: FiniteDateRange,
        mockNow: HelsinkiDateTime = beforeThreshold
    ): ReservationsResponse {
        return reservationControllerCitizen.getReservations(
            dbInstance(),
            user,
            MockEvakaClock(mockNow),
            range.start,
            range.end,
        )
    }

    private fun assertAbsenceCounts(childId: ChildId, counts: List<Pair<LocalDate, Int>>) {
        data class QueryResult(val date: LocalDate, val count: Int)

        val expected = counts.map { QueryResult(it.first, it.second) }
        val actual =
            db.read {
                it.createQuery<Any> {
                        sql(
                            """
                            SELECT date, COUNT(category) as count
                            FROM absence WHERE
                            child_id = ${bind(childId)}
                            GROUP BY date
                            ORDER BY date
                            """
                        )
                    }
                    .toList<QueryResult>()
            }

        assertEquals(expected, actual)
    }

    private fun assertReservationCounts(childId: ChildId, counts: List<Pair<LocalDate, Int>>) {
        data class QueryResult(val date: LocalDate, val count: Int)

        val expected = counts.map { QueryResult(it.first, it.second) }
        val actual =
            db.read {
                it.createQuery<Any> {
                        sql(
                            """
                            SELECT date, COUNT(*) as count
                            FROM attendance_reservation
                            WHERE child_id = ${bind(childId)}
                            GROUP BY date
                            ORDER BY date
                            """
                        )
                    }
                    .toList<QueryResult>()
            }
        assertEquals(expected, actual)
    }
}
