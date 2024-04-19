// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.absence

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.allWeekOpTimes
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesValue
import fi.espoo.evaka.dailyservicetimes.createChildDailyServiceTimes
import fi.espoo.evaka.daycare.insertPreschoolTerm
import fi.espoo.evaka.holidayperiod.insertHolidayPeriod
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.ScheduleType
import fi.espoo.evaka.reservations.Reservation
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.serviceneed.insertServiceNeed
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevDailyServiceTimes
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevHoliday
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertServiceNeedOption
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestServiceNeed
import fi.espoo.evaka.shared.dev.updateDaycareOperationTimes
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.domain.isWeekend
import fi.espoo.evaka.snDaycareContractDays15
import fi.espoo.evaka.snDaycareHours120
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDaycareGroup
import fi.espoo.evaka.user.EvakaUserType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AbsenceServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val employeeId = EmployeeId(UUID.randomUUID())
    private val placementStart: LocalDate = LocalDate.of(2019, 8, 1)
    private val placementEnd: LocalDate = LocalDate.of(2019, 12, 31)
    private val now = HelsinkiDateTime.now()

    private fun child(person: DevPerson) =
        GroupMonthCalendarChild(
            id = person.id,
            firstName = person.firstName,
            lastName = person.lastName,
            dateOfBirth = person.dateOfBirth,
            actualServiceNeeds = listOf(),
            reservationTotalHours = 0,
            attendanceTotalHours = 0,
            usedService = null
        )

    private val emptyDayChild =
        GroupMonthCalendarDayChild(
            childId = ChildId(UUID.randomUUID()),
            absenceCategories = setOf(AbsenceCategory.BILLABLE),
            backupCare = false,
            scheduleType = ScheduleType.RESERVATION_REQUIRED,
            shiftCare = ShiftCareType.NONE,
            missingHolidayReservation = false,
            absences = listOf(),
            reservations = listOf(),
            dailyServiceTimes = null
        )

    @BeforeEach
    fun prepare() {
        db.transaction {
            it.insertGeneralTestFixtures()
            it.insert(DevEmployee(id = employeeId))
            it.insert(
                DevDaycareGroup(
                    daycareId = testDaycare.id,
                    id = testDaycareGroup.id,
                    name = testDaycareGroup.name,
                    startDate = placementStart
                )
            )
        }
    }

    @Test
    fun `calendar without placements`() {
        val today = now.toLocalDate()
        val result =
            db.read {
                getGroupMonthCalendar(
                    it,
                    today,
                    testDaycareGroup.id,
                    today.year,
                    today.monthValue,
                )
            }

        assertEquals(
            GroupMonthCalendar(
                groupId = testDaycareGroup.id,
                groupName = testDaycareGroup.name,
                daycareName = testDaycare.name,
                daycareOperationTimes = testDaycare.operationTimes,
                children = listOf(),
                days =
                    FiniteDateRange.ofMonth(today.year, today.month)
                        .dates()
                        .map {
                            GroupMonthCalendarDay(
                                date = it,
                                isOperationDay = !it.isWeekend(),
                                isInHolidayPeriod = false,
                                children = emptyList()
                            )
                        }
                        .toList()
            ),
            result
        )
    }

    @Test
    fun `calendar has correct actual service needs`() {
        insertGroupPlacement(
            testChild_1.id,
            serviceNeedOptionId = snDefaultDaycare.id,
            placementPeriod =
                FiniteDateRange(placementStart, placementStart.plusWeeks(2).minusDays(1))
        )
        insertGroupPlacement(
            testChild_1.id,
            serviceNeedOptionId = snDaycareContractDays15.id,
            placementPeriod = FiniteDateRange(placementStart.plusWeeks(2), placementEnd)
        )
        val result =
            db.read {
                getGroupMonthCalendar(
                    it,
                    placementStart,
                    testDaycareGroup.id,
                    placementStart.year,
                    placementStart.monthValue
                )
            }

        assertEquals(
            listOf(
                child(testChild_1)
                    .copy(
                        actualServiceNeeds =
                            listOf(
                                ChildServiceNeedInfo(
                                    childId = testChild_1.id,
                                    validDuring =
                                        FiniteDateRange(
                                            placementStart,
                                            placementStart.plusWeeks(2).minusDays(1)
                                        ),
                                    shiftCare = ShiftCareType.NONE,
                                    optionName = snDefaultDaycare.nameFi,
                                    hasContractDays = false,
                                    daycareHoursPerMonth = null,
                                ),
                                ChildServiceNeedInfo(
                                    childId = testChild_1.id,
                                    validDuring =
                                        FiniteDateRange(placementStart.plusWeeks(2), placementEnd),
                                    shiftCare = ShiftCareType.NONE,
                                    optionName = snDaycareContractDays15.nameFi,
                                    hasContractDays = true,
                                    daycareHoursPerMonth = null,
                                )
                            )
                    )
            ),
            result.children
        )
    }

    @Test
    fun `calendar has correct reservation and attendance hours`() {
        insertGroupPlacement(testChild_1.id)
        val firstOfMonth = placementStart
        val lastOfMonth = placementStart.plusMonths(1).minusDays(1)

        db.transaction { tx ->
            // 5 weekdays * 5 hours = 25 hours
            tx.insert(
                DevDailyServiceTimes(
                    childId = testChild_1.id,
                    regularTimes = TimeRange(LocalTime.of(10, 0), LocalTime.of(15, 0)),
                    validityPeriod =
                        DateRange(firstOfMonth, firstOfMonth.plusWeeks(1).minusDays(1)),
                )
            )

            // 5 hours from daily service times replaced by a 9-hour actual reservation => +4 hours
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = firstOfMonth,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(17, 0),
                    createdBy = EvakaUserId(employeeId.raw),
                )
            )
            // 7 hours
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = lastOfMonth,
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(16, 0),
                    createdBy = EvakaUserId(employeeId.raw),
                )
            )
            // total reservation: 25 + 4 + 7 = 36 hours

            // 4 hours
            tx.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = HelsinkiDateTime.of(firstOfMonth, LocalTime.of(8, 0)),
                departed = HelsinkiDateTime.of(firstOfMonth, LocalTime.of(12, 0)),
            )
            // 3 hours
            tx.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = HelsinkiDateTime.of(lastOfMonth, LocalTime.of(9, 15)),
                departed = HelsinkiDateTime.of(lastOfMonth, LocalTime.of(12, 15)),
            )
            // total attendance: 4 + 3 = 7 hours
        }

        val result =
            db.read {
                getGroupMonthCalendar(
                    it,
                    firstOfMonth,
                    testDaycareGroup.id,
                    firstOfMonth.year,
                    firstOfMonth.monthValue
                )
            }

        assertEquals(
            listOf(child(testChild_1).copy(reservationTotalHours = 36, attendanceTotalHours = 7)),
            result.children
        )
    }

    @Test
    fun `calendar has correct absence categories`() {
        db.transaction { tx ->
            tx.insertPreschoolTerm(
                finnishPreschool = FiniteDateRange(placementStart, placementEnd),
                swedishPreschool = FiniteDateRange(placementStart, placementEnd),
                extendedTerm = FiniteDateRange(placementStart, placementEnd),
                applicationPeriod = FiniteDateRange(placementStart, placementEnd),
                termBreaks = DateSet.empty()
            )
        }
        insertGroupPlacement(testChild_1.id, PlacementType.PRESCHOOL)
        insertGroupPlacement(testChild_2.id, PlacementType.PRESCHOOL_DAYCARE)
        insertGroupPlacement(testChild_3.id, PlacementType.DAYCARE)

        val placementDate = placementStart
        val result =
            db.read {
                getGroupMonthCalendar(
                    it,
                    placementDate,
                    testDaycareGroup.id,
                    placementDate.year,
                    placementDate.monthValue
                )
            }

        assertEquals(
            GroupMonthCalendar(
                groupId = testDaycareGroup.id,
                groupName = testDaycareGroup.name,
                daycareName = testDaycare.name,
                daycareOperationTimes = testDaycare.operationTimes,
                children =
                    listOf(
                        // Sorted by lastName firstName
                        child(testChild_2),
                        child(testChild_1),
                        child(testChild_3),
                    ),
                days =
                    FiniteDateRange.ofMonth(placementDate.year, placementDate.month)
                        .dates()
                        .map { date ->
                            GroupMonthCalendarDay(
                                date = date,
                                isOperationDay = !date.isWeekend(),
                                isInHolidayPeriod = false,
                                children =
                                    listOf(
                                            emptyDayChild.copy(
                                                childId = testChild_1.id,
                                                scheduleType = ScheduleType.FIXED_SCHEDULE,
                                                absenceCategories =
                                                    setOf(AbsenceCategory.NONBILLABLE),
                                            ),
                                            emptyDayChild.copy(
                                                childId = testChild_2.id,
                                                absenceCategories =
                                                    setOf(
                                                        AbsenceCategory.BILLABLE,
                                                        AbsenceCategory.NONBILLABLE
                                                    ),
                                            ),
                                            emptyDayChild.copy(
                                                childId = testChild_3.id,
                                                absenceCategories = setOf(AbsenceCategory.BILLABLE),
                                            )
                                        )
                                        .sortedBy { it.childId }
                            )
                        }
                        .toList()
            ),
            result
        )
    }

    @Test
    fun `calendar has correct schedule types`() {
        db.transaction { tx ->
            tx.insertPreschoolTerm(
                finnishPreschool = FiniteDateRange(placementStart, placementEnd),
                swedishPreschool = FiniteDateRange(placementStart, placementEnd),
                extendedTerm = FiniteDateRange(placementStart, placementEnd),
                applicationPeriod = FiniteDateRange(placementStart, placementEnd),

                // First day is in a term break
                termBreaks = DateSet.of(FiniteDateRange(placementStart, placementStart))
            )
        }
        insertGroupPlacement(testChild_1.id, PlacementType.PRESCHOOL)

        val result =
            db.read {
                getGroupMonthCalendar(
                    it,
                    placementStart,
                    testDaycareGroup.id,
                    placementStart.year,
                    placementStart.monthValue
                )
            }

        assertEquals(
            GroupMonthCalendar(
                groupId = testDaycareGroup.id,
                groupName = testDaycareGroup.name,
                daycareName = testDaycare.name,
                daycareOperationTimes = testDaycare.operationTimes,
                children = listOf(child(testChild_1)),
                days =
                    FiniteDateRange.ofMonth(placementStart.year, placementStart.month)
                        .dates()
                        .map { date ->
                            GroupMonthCalendarDay(
                                date = date,
                                isOperationDay = !date.isWeekend(),
                                isInHolidayPeriod = false,
                                children =
                                    listOf(
                                        emptyDayChild.copy(
                                            childId = testChild_1.id,
                                            scheduleType =
                                                if (date == placementStart) ScheduleType.TERM_BREAK
                                                else ScheduleType.FIXED_SCHEDULE,
                                            absenceCategories = setOf(AbsenceCategory.NONBILLABLE),
                                        ),
                                    )
                            )
                        }
                        .toList()
            ),
            result
        )
    }

    @Test
    fun `calendar has correct backup care days`() {
        insertGroupPlacement(testChild_1.id)
        val backupCarePeriod =
            FiniteDateRange(placementStart, placementStart.plusWeeks(2).minusDays(1))
        val normalPeriod =
            FiniteDateRange(placementStart.plusWeeks(2), placementStart.plusMonths(1).minusDays(1))

        db.transaction { tx ->
            tx.insert(
                DevBackupCare(
                    childId = testChild_1.id,
                    period = backupCarePeriod,
                    unitId = testDaycare2.id,
                )
            )
        }

        val result =
            db.read {
                getGroupMonthCalendar(
                    it,
                    placementStart,
                    testDaycareGroup.id,
                    placementStart.year,
                    placementStart.monthValue
                )
            }

        assertEquals(
            (backupCarePeriod.dates().map { date ->
                    GroupMonthCalendarDay(
                        date = date,
                        isOperationDay = !date.isWeekend(),
                        isInHolidayPeriod = false,
                        children =
                            listOf(
                                emptyDayChild.copy(
                                    childId = testChild_1.id,
                                    backupCare = true,
                                )
                            )
                    )
                } +
                    normalPeriod.dates().map { date ->
                        GroupMonthCalendarDay(
                            date = date,
                            isOperationDay = !date.isWeekend(),
                            isInHolidayPeriod = false,
                            children =
                                listOf(
                                    emptyDayChild.copy(
                                        childId = testChild_1.id,
                                        backupCare = false,
                                    )
                                )
                        )
                    })
                .toList(),
            result.days,
        )
    }

    @Test
    fun `calendar has correct missing holiday reservation days, reservations and absences`() {
        insertGroupPlacement(testChild_1.id)
        val holidayPeriod =
            FiniteDateRange(placementStart, placementStart.plusWeeks(2).minusDays(1))

        db.transaction { tx ->
            tx.insertHolidayPeriod(
                period = holidayPeriod,
                reservationDeadline = placementStart // doesn't matter for group month calendar
            )
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = holidayPeriod.end.minusDays(2),
                    startTime = null,
                    endTime = null,
                    created = HelsinkiDateTime.of(placementStart, LocalTime.of(8, 0)),
                    createdBy = EvakaUserId(employeeId.raw),
                )
            )
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = holidayPeriod.end.minusDays(1),
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0),
                    created = HelsinkiDateTime.of(placementStart, LocalTime.of(9, 0)),
                    createdBy = EvakaUserId(employeeId.raw),
                )
            )
            tx.insert(
                DevAbsence(
                    childId = testChild_1.id,
                    date = holidayPeriod.end,
                    absenceCategory = AbsenceCategory.BILLABLE,
                    absenceType = AbsenceType.OTHER_ABSENCE,
                    modifiedBy = EvakaUserId(employeeId.raw),
                    modifiedAt = HelsinkiDateTime.of(placementStart, LocalTime.of(10, 0)),
                )
            )
        }

        val missingReservationsPeriod =
            FiniteDateRange(holidayPeriod.start, holidayPeriod.end.minusDays(3))
        val normalPeriod =
            FiniteDateRange(
                holidayPeriod.end.plusDays(1),
                placementStart.plusMonths(1).minusDays(1)
            )

        val result =
            db.read {
                getGroupMonthCalendar(
                    it,
                    placementStart,
                    testDaycareGroup.id,
                    placementStart.year,
                    placementStart.monthValue
                )
            }

        assertEquals(
            // missing reservations
            (missingReservationsPeriod.dates().map { date ->
                    GroupMonthCalendarDay(
                        date = date,
                        isOperationDay = !date.isWeekend(),
                        isInHolidayPeriod = true,
                        children =
                            listOf(
                                emptyDayChild.copy(
                                    childId = testChild_1.id,
                                    missingHolidayReservation = true
                                )
                            )
                    )
                } +
                    // reservations and absences
                    sequenceOf(
                        GroupMonthCalendarDay(
                            date = holidayPeriod.end.minusDays(2),
                            isOperationDay = !holidayPeriod.end.minusDays(2).isWeekend(),
                            isInHolidayPeriod = true,
                            children =
                                listOf(
                                    emptyDayChild.copy(
                                        childId = testChild_1.id,
                                        missingHolidayReservation = false,
                                        reservations =
                                            listOf(
                                                ChildReservation(
                                                    reservation = Reservation.NoTimes,
                                                    created =
                                                        HelsinkiDateTime.of(
                                                            placementStart,
                                                            LocalTime.of(8, 0)
                                                        ),
                                                    createdByEvakaUserType = EvakaUserType.EMPLOYEE,
                                                )
                                            ),
                                    )
                                )
                        ),
                        GroupMonthCalendarDay(
                            date = holidayPeriod.end.minusDays(1),
                            isOperationDay = !holidayPeriod.end.minusDays(1).isWeekend(),
                            isInHolidayPeriod = true,
                            children =
                                listOf(
                                    emptyDayChild.copy(
                                        childId = testChild_1.id,
                                        missingHolidayReservation = false,
                                        reservations =
                                            listOf(
                                                ChildReservation(
                                                    reservation =
                                                        Reservation.Times(
                                                            TimeRange(
                                                                LocalTime.of(8, 0),
                                                                LocalTime.of(16, 0)
                                                            )
                                                        ),
                                                    created =
                                                        HelsinkiDateTime.of(
                                                            placementStart,
                                                            LocalTime.of(9, 0)
                                                        ),
                                                    createdByEvakaUserType = EvakaUserType.EMPLOYEE,
                                                )
                                            ),
                                    )
                                )
                        ),
                        GroupMonthCalendarDay(
                            date = holidayPeriod.end,
                            isOperationDay = !holidayPeriod.end.isWeekend(),
                            isInHolidayPeriod = true,
                            children =
                                listOf(
                                    emptyDayChild.copy(
                                        childId = testChild_1.id,
                                        missingHolidayReservation = false,
                                        absences =
                                            listOf(
                                                AbsenceWithModifierInfo(
                                                    category = AbsenceCategory.BILLABLE,
                                                    absenceType = AbsenceType.OTHER_ABSENCE,
                                                    modifiedByStaff = true,
                                                    modifiedAt =
                                                        HelsinkiDateTime.of(
                                                            placementStart,
                                                            LocalTime.of(10, 0)
                                                        )
                                                )
                                            ),
                                    )
                                )
                        ),
                    ) +
                    // empty days
                    normalPeriod.dates().map { date ->
                        GroupMonthCalendarDay(
                            date = date,
                            isOperationDay = !date.isWeekend(),
                            isInHolidayPeriod = false,
                            children =
                                listOf(
                                    emptyDayChild.copy(
                                        childId = testChild_1.id,
                                        missingHolidayReservation = false,
                                    )
                                )
                        )
                    })
                .toList(),
            result.days,
        )
    }

    @Test
    fun `calendar does not show missing holiday reservations for placement in a unit with reservations not in use`() {
        db.transaction { tx ->
            tx.createUpdate {
                    sql(
                        "UPDATE daycare SET enabled_pilot_features='{}' WHERE id = ${bind(testDaycare.id)}"
                    )
                }
                .execute()
        }
        insertGroupPlacement(testChild_1.id, placementType = PlacementType.DAYCARE)
        val isInHolidayPeriod =
            FiniteDateRange(placementStart, placementStart.plusMonths(1).minusDays(1))

        db.transaction { tx ->
            tx.insertHolidayPeriod(
                period = isInHolidayPeriod,
                reservationDeadline = placementStart // doesn't matter for group month calendar
            )
        }

        val result =
            db.read {
                getGroupMonthCalendar(
                    it,
                    placementStart,
                    testDaycareGroup.id,
                    placementStart.year,
                    placementStart.monthValue
                )
            }

        assertEquals(
            FiniteDateRange(placementStart, placementStart.plusMonths(1).minusDays(1))
                .dates()
                .map { date ->
                    GroupMonthCalendarDay(
                        date = date,
                        isOperationDay = !date.isWeekend(),
                        isInHolidayPeriod = true,
                        children =
                            listOf(
                                emptyDayChild.copy(
                                    childId = testChild_1.id,
                                    missingHolidayReservation = false,
                                    absenceCategories = setOf(AbsenceCategory.BILLABLE),
                                    scheduleType = ScheduleType.RESERVATION_REQUIRED
                                )
                            )
                    )
                }
                .toList(),
            result.days
        )
    }

    @Test
    fun `calendar does not show missing holiday reservations for placement type that does not need reservations`() {
        insertGroupPlacement(testChild_1.id, placementType = PlacementType.CLUB)
        val isInHolidayPeriod =
            FiniteDateRange(placementStart, placementStart.plusMonths(1).minusDays(1))

        db.transaction { tx ->
            tx.insertHolidayPeriod(
                period = isInHolidayPeriod,
                reservationDeadline = placementStart // doesn't matter for group month calendar
            )
        }

        val result =
            db.read {
                getGroupMonthCalendar(
                    it,
                    placementStart,
                    testDaycareGroup.id,
                    placementStart.year,
                    placementStart.monthValue
                )
            }

        assertEquals(
            FiniteDateRange(placementStart, placementStart.plusMonths(1).minusDays(1))
                .dates()
                .map { date ->
                    GroupMonthCalendarDay(
                        date = date,
                        isOperationDay = !date.isWeekend(),
                        isInHolidayPeriod = true,
                        children =
                            listOf(
                                emptyDayChild.copy(
                                    childId = testChild_1.id,
                                    missingHolidayReservation = false,
                                    absenceCategories = setOf(AbsenceCategory.NONBILLABLE),
                                    scheduleType = ScheduleType.TERM_BREAK
                                )
                            )
                    )
                }
                .toList(),
            result.days
        )
    }

    @Test
    fun `calendar has correct daily service times`() {
        insertGroupPlacement(testChild_1.id)
        val dailyServiceTimesPeriod1 =
            FiniteDateRange(placementStart, placementStart.plusWeeks(2).minusDays(1))
        val dailyServiceTimesPeriod2 =
            FiniteDateRange(placementStart.plusWeeks(2), placementStart.plusMonths(1).minusDays(1))

        db.transaction { tx ->
            tx.insert(
                DevDailyServiceTimes(
                    childId = testChild_1.id,
                    validityPeriod = dailyServiceTimesPeriod1.asDateRange(),
                    regularTimes = TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0))
                )
            )
            tx.insert(
                DevDailyServiceTimes(
                    childId = testChild_1.id,
                    validityPeriod = dailyServiceTimesPeriod2.asDateRange(),
                    regularTimes = TimeRange(LocalTime.of(9, 0), LocalTime.of(15, 0))
                )
            )
        }

        val result =
            db.read {
                getGroupMonthCalendar(
                    it,
                    placementStart,
                    testDaycareGroup.id,
                    placementStart.year,
                    placementStart.monthValue
                )
            }

        assertEquals(
            (dailyServiceTimesPeriod1.dates().map { date ->
                    GroupMonthCalendarDay(
                        date = date,
                        isOperationDay = !date.isWeekend(),
                        isInHolidayPeriod = false,
                        children =
                            listOf(
                                emptyDayChild.copy(
                                    childId = testChild_1.id,
                                    dailyServiceTimes =
                                        TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0))
                                )
                            )
                    )
                } +
                    dailyServiceTimesPeriod2.dates().map { date ->
                        GroupMonthCalendarDay(
                            date = date,
                            isOperationDay = !date.isWeekend(),
                            isInHolidayPeriod = false,
                            children =
                                listOf(
                                    emptyDayChild.copy(
                                        childId = testChild_1.id,
                                        dailyServiceTimes =
                                            TimeRange(LocalTime.of(9, 0), LocalTime.of(15, 0))
                                    )
                                )
                        )
                    })
                .toList(),
            result.days,
        )
    }

    @Test
    fun `upsert absences`() {
        insertGroupPlacement(testChild_1.id, PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val initialAbsence =
            AbsenceUpsert(
                testChild_1.id,
                absenceDate,
                AbsenceCategory.NONBILLABLE,
                AbsenceType.SICKLEAVE,
            )
        val initialAbsenceList = listOf(initialAbsence)

        val result =
            db.transaction { tx ->
                tx.upsertAbsences(now, EvakaUserId(employeeId.raw), initialAbsenceList)
                getGroupMonthCalendar(
                    tx,
                    absenceDate,
                    testDaycareGroup.id,
                    absenceDate.year,
                    absenceDate.monthValue
                )
            }
        val child =
            result.days
                .find { it.date == absenceDate }
                ?.children
                ?.find { it.childId == testChild_1.id } ?: error("Day or child not found")

        assertEquals(
            listOf(
                AbsenceWithModifierInfo(
                    category = initialAbsence.category,
                    absenceType = initialAbsence.absenceType,
                    modifiedByStaff = true,
                    modifiedAt = now,
                )
            ),
            child.absences,
        )
    }

    @Test
    fun `cannot create multiple absences for same placement type and same date`() {
        insertGroupPlacement(testChild_1.id, PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val initialAbsence =
            AbsenceUpsert(
                testChild_1.id,
                absenceDate,
                AbsenceCategory.BILLABLE,
                AbsenceType.SICKLEAVE,
            )
        val initialAbsence2 =
            AbsenceUpsert(
                testChild_1.id,
                absenceDate,
                AbsenceCategory.BILLABLE,
                AbsenceType.SICKLEAVE,
            )
        val initialAbsenceList = listOf(initialAbsence, initialAbsence2)

        val result =
            db.transaction { tx ->
                tx.upsertAbsences(now, EvakaUserId(employeeId.raw), initialAbsenceList)
                getGroupMonthCalendar(
                    tx,
                    absenceDate,
                    testDaycareGroup.id,
                    absenceDate.year,
                    absenceDate.monthValue
                )
            }

        val child =
            result.days
                .find { it.date == absenceDate }
                ?.children
                ?.find { it.childId == testChild_1.id } ?: error("Day or child not found")

        assertEquals(1, child.absences.size)
    }

    @Test
    fun `modify absence`() {
        insertGroupPlacement(testChild_1.id, PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val initialAbsence =
            AbsenceUpsert(
                testChild_1.id,
                absenceDate,
                AbsenceCategory.BILLABLE,
                AbsenceType.SICKLEAVE
            )
        val initialAbsenceList = listOf(initialAbsence)

        var result =
            db.transaction { tx ->
                tx.upsertAbsences(now, EvakaUserId(employeeId.raw), initialAbsenceList)
                getGroupMonthCalendar(
                    tx,
                    absenceDate,
                    testDaycareGroup.id,
                    absenceDate.year,
                    absenceDate.monthValue
                )
            }

        var child =
            result.days
                .find { it.date == absenceDate }
                ?.children
                ?.find { it.childId == testChild_1.id } ?: error("Day or child not found")
        var absence = child.absences.single()

        val newAbsenceType = AbsenceType.UNKNOWN_ABSENCE
        val updatedAbsence =
            AbsenceUpsert(
                childId = testChild_1.id,
                date = absenceDate,
                category = absence.category,
                absenceType = newAbsenceType
            )

        result =
            db.transaction { tx ->
                tx.upsertAbsences(now, EvakaUserId(employeeId.raw), listOf(updatedAbsence))
                getGroupMonthCalendar(
                    tx,
                    absenceDate,
                    testDaycareGroup.id,
                    absenceDate.year,
                    absenceDate.monthValue
                )
            }

        child =
            result.days
                .find { it.date == absenceDate }
                ?.children
                ?.find { it.childId == testChild_1.id } ?: error("Day or child not found")
        absence = child.absences.single()

        assertEquals(newAbsenceType, absence.absenceType)
    }

    @Test
    fun `get absence by childId`() {
        insertGroupPlacement(testChild_1.id, PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val initialAbsence =
            AbsenceUpsert(
                testChild_1.id,
                absenceDate,
                AbsenceCategory.BILLABLE,
                AbsenceType.SICKLEAVE
            )
        val initialAbsence2 =
            AbsenceUpsert(
                testChild_1.id,
                absenceDate,
                AbsenceCategory.NONBILLABLE,
                AbsenceType.SICKLEAVE
            )
        val initialAbsenceList = listOf(initialAbsence, initialAbsence2)

        val absences =
            db.transaction { tx ->
                tx.upsertAbsences(now, EvakaUserId(employeeId.raw), initialAbsenceList)
                getAbsencesOfChildByMonth(
                    tx,
                    testChild_1.id,
                    absenceDate.year,
                    absenceDate.monthValue
                )
            }
        assertEquals(initialAbsenceList.size, absences.size)
    }

    @Test
    fun `get absence by childId should not find anything with a wrong childId`() {
        val childId2 = ChildId(UUID.randomUUID())
        insertGroupPlacement(testChild_1.id, PlacementType.PRESCHOOL_DAYCARE)
        db.transaction {
            it.insert(
                DevPerson(id = childId2, dateOfBirth = LocalDate.of(2013, 1, 1)),
                DevPersonType.CHILD
            )
        }

        val absenceDate = placementEnd
        val initialAbsence =
            AbsenceUpsert(
                testChild_1.id,
                absenceDate,
                AbsenceCategory.BILLABLE,
                AbsenceType.SICKLEAVE,
            )
        val initialAbsence2 =
            AbsenceUpsert(
                testChild_1.id,
                absenceDate,
                AbsenceCategory.NONBILLABLE,
                AbsenceType.SICKLEAVE,
            )
        val initialAbsenceList = listOf(initialAbsence, initialAbsence2)

        val absences =
            db.transaction { tx ->
                tx.upsertAbsences(now, EvakaUserId(employeeId.raw), initialAbsenceList)
                getAbsencesOfChildByMonth(tx, childId2, absenceDate.year, absenceDate.monthValue)
            }
        assertEquals(0, absences.size)
    }

    @Test
    fun `get absence by childId find the absences within the date range`() {
        insertGroupPlacement(testChild_1.id, PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val initialAbsence =
            AbsenceUpsert(
                testChild_1.id,
                LocalDate.of(2019, 9, 1),
                AbsenceCategory.BILLABLE,
                AbsenceType.SICKLEAVE,
            )
        val initialAbsence2 =
            AbsenceUpsert(
                testChild_1.id,
                absenceDate,
                AbsenceCategory.NONBILLABLE,
                AbsenceType.SICKLEAVE,
            )
        val initialAbsenceList = listOf(initialAbsence, initialAbsence2)

        val absences =
            db.transaction { tx ->
                tx.upsertAbsences(now, EvakaUserId(employeeId.raw), initialAbsenceList)
                getAbsencesOfChildByMonth(tx, testChild_1.id, 2019, 9)
            }
        assertEquals(1, absences.size)
    }

    @Test
    fun `group operational days do not include holidays`() {
        val firstOfJanuary2020 = LocalDate.of(2020, 1, 1)
        val epiphany2020 = LocalDate.of(2020, 1, 6)
        db.transaction { it.insert(DevHoliday(epiphany2020)) }
        val result =
            db.read {
                getGroupMonthCalendar(
                    it,
                    firstOfJanuary2020,
                    testDaycareGroup.id,
                    firstOfJanuary2020.year,
                    firstOfJanuary2020.monthValue
                )
            }

        val operationDays = result.days.filter { it.isOperationDay }.map { it.date }
        assertFalse(operationDays.contains(epiphany2020))
    }

    @Test
    fun `group operational days include holidays if the unit is operational on every weekday`() {
        val firstOfJanuary2020 = LocalDate.of(2020, 1, 1)
        val epiphany2020 = LocalDate.of(2020, 1, 6)

        db.transaction {
            it.execute("INSERT INTO holiday (date) VALUES (?)", epiphany2020)
            it.updateDaycareOperationTimes(testDaycare.id, allWeekOpTimes)
        }
        val result =
            db.read {
                getGroupMonthCalendar(
                    it,
                    firstOfJanuary2020,
                    testDaycareGroup.id,
                    firstOfJanuary2020.year,
                    firstOfJanuary2020.monthValue
                )
            }

        val operationDays = result.days.map { it.date }
        assertTrue(operationDays.contains(epiphany2020))
    }

    @Test
    fun `reservation sums - basic case`() {
        insertGroupPlacement(testChild_1.id)
        val reservations =
            generateSequence(placementStart) { it.plusDays(1) }
                .map {
                    HelsinkiDateTime.of(it, LocalTime.of(8, 0)) to
                        HelsinkiDateTime.of(it, LocalTime.of(16, 0))
                }
                .take(5)
                .toList()
        insertReservations(testChild_1.id, reservations)

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }
        assertEquals(listOf(40), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - placement changes mid reservation`() {
        insertGroupPlacement(
            testChild_1.id,
            placementPeriod = FiniteDateRange(placementStart, placementStart.plusDays(1))
        )
        insertGroupPlacement(
            testChild_1.id,
            placementPeriod = FiniteDateRange(placementStart.plusDays(2), placementEnd)
        )
        val reservations =
            generateSequence(placementStart) { it.plusDays(1) }
                .map {
                    HelsinkiDateTime.of(it, LocalTime.of(21, 0)) to
                        HelsinkiDateTime.of(it.plusDays(1), LocalTime.of(9, 0))
                }
                .take(5)
                .toList()
        insertReservations(testChild_1.id, reservations)

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }
        assertEquals(listOf(60), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - reservations continuing over the end of month are cut at midnight`() {
        insertGroupPlacement(testChild_1.id)
        val lastDayOfMonth = LocalDate.of(2019, 8, 31)
        insertReservations(
            testChild_1.id,
            listOf(
                HelsinkiDateTime.of(lastDayOfMonth, LocalTime.of(12, 0)) to
                    HelsinkiDateTime.of(lastDayOfMonth.plusDays(1), LocalTime.of(12, 0))
            )
        )

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }
        assertEquals(listOf(12), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - reservations continuing from last month are included from midnight`() {
        insertGroupPlacement(testChild_1.id)
        val firstDayOfMonth = LocalDate.of(2019, 8, 1)
        insertReservations(
            testChild_1.id,
            listOf(
                HelsinkiDateTime.of(firstDayOfMonth.minusDays(1), LocalTime.of(12, 0)) to
                    HelsinkiDateTime.of(firstDayOfMonth, LocalTime.of(12, 0))
            )
        )

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }
        assertEquals(listOf(12), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - reservations longer than placements are cut at placement start`() {
        val placementDate = LocalDate.of(2019, 8, 15)
        insertGroupPlacement(
            testChild_1.id,
            placementPeriod = FiniteDateRange(placementDate, placementDate)
        )
        insertReservations(
            testChild_1.id,
            listOf(
                HelsinkiDateTime.of(placementDate.minusDays(1), LocalTime.of(12, 0)) to
                    HelsinkiDateTime.of(placementDate, LocalTime.of(12, 0))
            )
        )

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }
        assertEquals(listOf(12), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - reservations longer than placements are cut at placement end`() {
        val placementDate = LocalDate.of(2019, 8, 15)
        insertGroupPlacement(
            testChild_1.id,
            placementPeriod = FiniteDateRange(placementDate, placementDate)
        )
        insertReservations(
            testChild_1.id,
            listOf(
                HelsinkiDateTime.of(placementDate, LocalTime.of(12, 0)) to
                    HelsinkiDateTime.of(placementDate.plusDays(1), LocalTime.of(12, 0))
            )
        )

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }
        assertEquals(listOf(12), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - reservations during back up placements are included in placement unit sum`() {
        insertGroupPlacement(testChild_1.id)
        val reservations =
            generateSequence(placementStart) { it.plusDays(1) }
                .map {
                    HelsinkiDateTime.of(it, LocalTime.of(8, 0)) to
                        HelsinkiDateTime.of(it, LocalTime.of(16, 0))
                }
                .take(5)
                .toList()
        insertReservations(testChild_1.id, reservations)
        val (backupUnit, backupGroup) = createNewUnitAndGroup()
        val backupPeriod = FiniteDateRange(placementStart.plusDays(1), placementStart.plusDays(2))
        insertBackupPlacement(testChild_1.id, backupUnit, backupGroup, backupPeriod)

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }
        assertEquals(listOf(40), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - only reservations during back up placements are included in backup unit sum`() {
        insertGroupPlacement(testChild_1.id)
        val reservations =
            generateSequence(placementStart) { it.plusDays(1) }
                .map {
                    HelsinkiDateTime.of(it, LocalTime.of(8, 0)) to
                        HelsinkiDateTime.of(it, LocalTime.of(16, 0))
                }
                .take(5)
                .toList()
        insertReservations(testChild_1.id, reservations)
        val (backupUnit, backupGroup) = createNewUnitAndGroup()
        val backupPeriod = FiniteDateRange(placementStart.plusDays(1), placementStart.plusDays(2))
        insertBackupPlacement(testChild_1.id, backupUnit, backupGroup, backupPeriod)

        val result =
            db.read { getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), backupGroup, 2019, 8) }
        assertEquals(listOf(16), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - reservation without times are not included`() {
        insertGroupPlacement(testChild_1.id)
        db.transaction { tx ->
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = placementStart,
                    startTime = null,
                    endTime = null,
                    createdBy = EvakaUserId(employeeId.raw)
                )
            )
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = placementStart.plusDays(1),
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0),
                    createdBy = EvakaUserId(employeeId.raw)
                )
            )
        }

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }

        // 1 operational day * 0 h + 1 operational day * 8 h
        assertEquals(listOf(8), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - daily service times are used to generate missing reservations and reservations with no times`() {
        insertGroupPlacement(testChild_1.id)
        val dailyServiceTimes =
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = DateRange(placementStart, null),
                regularTimes = TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0))
            )
        insertDailyServiceTimes(testChild_1.id, dailyServiceTimes)

        db.transaction { tx ->
            // Daily service times are used because this reservation has no times
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = placementStart.plusDays(1),
                    startTime = null,
                    endTime = null,
                    createdBy = EvakaUserId(employeeId.raw)
                )
            )
        }

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }
        // 22 operational days * 8h
        assertEquals(listOf(176), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - irregular daily service times are applied according to operational days`() {
        insertGroupPlacement(
            testChild_1.id,
            placementPeriod = FiniteDateRange(LocalDate.of(2019, 8, 5), LocalDate.of(2019, 8, 11))
        )
        val dailyServiceTimes =
            DailyServiceTimesValue.IrregularTimes(
                monday = TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
                tuesday = TimeRange(LocalTime.of(8, 0), LocalTime.of(14, 0)),
                wednesday = null,
                thursday = null,
                friday = null,
                saturday = TimeRange(LocalTime.of(8, 0), LocalTime.of(20, 0)),
                sunday = null,
                validityPeriod = DateRange(placementStart, null)
            )
        insertDailyServiceTimes(testChild_1.id, dailyServiceTimes)

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }
        // 8-16 + 8-14 (saturday is not included because the unit is not operational on saturdays)
        assertEquals(listOf(14), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - daily service times are used only when there is no reservation`() {
        insertGroupPlacement(testChild_1.id)
        val reservations =
            generateSequence(placementStart) { it.plusDays(1) }
                .takeWhile { it < LocalDate.of(2019, 8, 30) } // last operational day
                .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
                .map {
                    HelsinkiDateTime.of(it, LocalTime.of(8, 0)) to
                        HelsinkiDateTime.of(it, LocalTime.of(16, 0))
                }
                .toList()
        insertReservations(testChild_1.id, reservations)
        val dailyServiceTimes =
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = DateRange(placementStart, null),
                regularTimes = TimeRange(LocalTime.of(8, 0), LocalTime.of(20, 0))
            )
        insertDailyServiceTimes(testChild_1.id, dailyServiceTimes)

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }
        // 21 operational days * 8h + 12h
        assertEquals(listOf(180), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - daily service times are cut when they partially overlap with a reservation`() {
        val placementPeriod = FiniteDateRange(LocalDate.of(2019, 8, 5), LocalDate.of(2019, 8, 6))
        insertGroupPlacement(testChild_1.id, placementPeriod = placementPeriod)
        val reservations =
            listOf(
                HelsinkiDateTime.of(placementPeriod.start, LocalTime.of(21, 0)) to
                    HelsinkiDateTime.of(placementPeriod.end, LocalTime.of(9, 0))
            )
        insertReservations(testChild_1.id, reservations)
        val dailyServiceTimes =
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = DateRange(placementStart, null),
                regularTimes = TimeRange(LocalTime.of(7, 0), LocalTime.of(15, 0))
            )
        insertDailyServiceTimes(testChild_1.id, dailyServiceTimes)

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }
        // 21-9 + 9-15 (overlapping 2 hours are left out)
        assertEquals(listOf(18), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - absences leave out reserved times according to their start time`() {
        insertGroupPlacement(testChild_1.id)
        val reservations =
            generateSequence(placementStart) { it.plusDays(1) }
                .map {
                    HelsinkiDateTime.of(it, LocalTime.of(20, 0)) to
                        HelsinkiDateTime.of(it.plusDays(1), LocalTime.of(8, 0))
                }
                .take(5)
                .toList()
        insertReservations(testChild_1.id, reservations)
        db.transaction {
            // the start and end of absence date overlaps with two reservations
            val absence =
                AbsenceUpsert(
                    testChild_1.id,
                    placementStart.plusDays(1),
                    AbsenceCategory.BILLABLE,
                    AbsenceType.OTHER_ABSENCE,
                )
            it.upsertAbsences(now, EvakaUserId(employeeId.raw), listOf(absence))
        }

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }
        // the start and end of absence date overlaps with two reservations but only one reservation
        // is left out
        assertEquals(listOf(4 * 12), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - daily service time is within another reservation`() {
        insertGroupPlacement(testChild_1.id)
        val reservations =
            listOf(
                HelsinkiDateTime.of(placementStart, LocalTime.of(20, 0)) to
                    HelsinkiDateTime.of(placementStart.plusDays(1), LocalTime.of(17, 0))
            )
        insertReservations(testChild_1.id, reservations)
        val dailyServiceTimes =
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = DateRange(placementStart, null),
                regularTimes = TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0))
            )
        insertDailyServiceTimes(testChild_1.id, dailyServiceTimes)

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }
        assertEquals(listOf(21 + 20 * 8), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - multiple daily service times are accounted for`() {
        insertGroupPlacement(testChild_1.id)
        val ninthOfAugust = LocalDate.of(2019, 8, 9)
        insertReservations(
            testChild_1.id,
            listOf(
                HelsinkiDateTime.of(ninthOfAugust, LocalTime.of(10, 0)) to
                    HelsinkiDateTime.of(ninthOfAugust, LocalTime.of(12, 0))
            )
        )

        insertDailyServiceTimes(
            testChild_1.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = DateRange(placementStart, LocalDate.of(2019, 8, 10)),
                regularTimes = TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0))
            )
        )
        insertDailyServiceTimes(
            testChild_1.id,
            DailyServiceTimesValue.RegularTimes(
                validityPeriod = DateRange(LocalDate.of(2019, 8, 13), LocalDate.of(2019, 8, 20)),
                regularTimes = TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0))
            )
        )
        insertDailyServiceTimes(
            testChild_1.id,
            DailyServiceTimesValue.IrregularTimes(
                monday = TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
                tuesday = TimeRange(LocalTime.of(8, 0), LocalTime.of(14, 0)),
                wednesday = TimeRange(LocalTime.of(9, 0), LocalTime.of(16, 0)),
                thursday = TimeRange(LocalTime.of(10, 0), LocalTime.of(17, 0)),
                friday = TimeRange(LocalTime.of(10, 0), LocalTime.of(12, 0)),
                saturday = TimeRange(LocalTime.of(8, 0), LocalTime.of(20, 0)),
                sunday = null,
                validityPeriod = DateRange(LocalDate.of(2019, 8, 21), LocalDate.of(2019, 8, 29))
            )
        )

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }
        assertEquals(
            listOf(
                2 + // reservation
                    6 * 8 + // first regular times (8 h), excluding the reservation
                    6 * 10 + // second regular times (10 h)
                    7 +
                    7 +
                    2 +
                    8 +
                    6 +
                    7 +
                    7 // irregular times
            ),
            result.children.map { it.reservationTotalHours }
        )
    }

    @Test
    fun `attendance sums - basic case`() {
        insertGroupPlacement(testChild_1.id)
        val attendances =
            generateSequence(placementStart) { it.plusDays(1) }
                .map {
                    HelsinkiDateTime.of(it, LocalTime.of(8, 0)) to
                        HelsinkiDateTime.of(it, LocalTime.of(16, 0))
                }
                .take(5)
                .toList()
        insertAttendances(testChild_1.id, testDaycare.id, attendances)

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }
        assertEquals(listOf(40), result.children.map { it.attendanceTotalHours })
    }

    @Test
    fun `attendance sums - placement changes mid attendance`() {
        insertGroupPlacement(
            testChild_1.id,
            placementPeriod = FiniteDateRange(placementStart, placementStart.plusDays(1))
        )
        insertGroupPlacement(
            testChild_1.id,
            placementPeriod = FiniteDateRange(placementStart.plusDays(2), placementEnd)
        )
        val attendances =
            generateSequence(placementStart) { it.plusDays(1) }
                .map {
                    HelsinkiDateTime.of(it, LocalTime.of(21, 0)) to
                        HelsinkiDateTime.of(it.plusDays(1), LocalTime.of(9, 0))
                }
                .take(5)
                .toList()
        insertAttendances(testChild_1.id, testDaycare.id, attendances)

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }
        assertEquals(listOf(60), result.children.map { it.attendanceTotalHours })
    }

    @Test
    fun `attendance sums - attendances continuing over the end of month are cut at midnight`() {
        insertGroupPlacement(testChild_1.id)
        val lastDayOfMonth = LocalDate.of(2019, 8, 31)
        insertAttendances(
            testChild_1.id,
            testDaycare.id,
            listOf(
                HelsinkiDateTime.of(lastDayOfMonth, LocalTime.of(12, 0)) to
                    HelsinkiDateTime.of(lastDayOfMonth.plusDays(1), LocalTime.of(12, 0))
            )
        )

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }
        assertEquals(listOf(12), result.children.map { it.attendanceTotalHours })
    }

    @Test
    fun `attendance sums - attendances continuing from last month are included from midnight`() {
        insertGroupPlacement(testChild_1.id)
        val firstDayOfMonth = LocalDate.of(2019, 8, 1)
        insertAttendances(
            testChild_1.id,
            testDaycare.id,
            listOf(
                HelsinkiDateTime.of(firstDayOfMonth.minusDays(1), LocalTime.of(12, 0)) to
                    HelsinkiDateTime.of(firstDayOfMonth, LocalTime.of(12, 0))
            )
        )

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }
        assertEquals(listOf(12), result.children.map { it.attendanceTotalHours })
    }

    @Test
    fun `attendance sums - attendances longer than placements are cut at placement start and end`() {
        val placementDate = LocalDate.of(2019, 8, 15)
        insertGroupPlacement(
            testChild_1.id,
            placementPeriod = FiniteDateRange(placementDate, placementDate)
        )
        insertAttendances(
            testChild_1.id,
            testDaycare.id,
            listOf(
                HelsinkiDateTime.of(placementDate.minusDays(5), LocalTime.of(12, 0)) to
                    HelsinkiDateTime.of(placementDate.plusDays(5), LocalTime.of(12, 0))
            )
        )

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }
        assertEquals(listOf(24), result.children.map { it.attendanceTotalHours })
    }

    @Test
    fun `attendance sums - attendances during back up placements are included in placement unit sum`() {
        insertGroupPlacement(testChild_1.id)
        val attendances =
            generateSequence(placementStart) { it.plusDays(1) }
                .map {
                    HelsinkiDateTime.of(it, LocalTime.of(8, 0)) to
                        HelsinkiDateTime.of(it, LocalTime.of(16, 0))
                }
                .take(5)
                .toList()
        val (backupUnit, backupGroup) = createNewUnitAndGroup()
        val backupPeriod = FiniteDateRange(placementStart.plusDays(1), placementStart.plusDays(2))
        insertBackupPlacement(testChild_1.id, backupUnit, backupGroup, backupPeriod)
        insertAttendances(testChild_1.id, backupUnit, attendances)

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }
        assertEquals(listOf(40), result.children.map { it.attendanceTotalHours })
    }

    @Test
    fun `attendance sums - only attendances during back up placements are included in backup unit sum`() {
        insertGroupPlacement(testChild_1.id)
        val attendances =
            generateSequence(placementStart) { it.plusDays(1) }
                .map {
                    HelsinkiDateTime.of(it, LocalTime.of(8, 0)) to
                        HelsinkiDateTime.of(it, LocalTime.of(16, 0))
                }
                .take(5)
                .toList()
        val (backupUnit, backupGroup) = createNewUnitAndGroup()
        val backupPeriod = FiniteDateRange(placementStart.plusDays(1), placementStart.plusDays(2))
        insertBackupPlacement(testChild_1.id, backupUnit, backupGroup, backupPeriod)
        insertAttendances(testChild_1.id, backupUnit, attendances)

        val result =
            db.read { getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), backupGroup, 2019, 8) }
        assertEquals(listOf(16), result.children.map { it.attendanceTotalHours })
    }

    @Test
    fun `attendance sums - attendances without a departure time are not included`() {
        insertGroupPlacement(testChild_1.id)
        val attendances = listOf(HelsinkiDateTime.of(placementStart, LocalTime.of(9, 0)) to null)
        insertAttendances(testChild_1.id, testDaycare.id, attendances)

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 1), testDaycareGroup.id, 2019, 8)
            }
        assertEquals(listOf(0), result.children.map { it.attendanceTotalHours })
    }

    @Test
    fun `used service totals`() {
        db.transaction { tx -> tx.insertServiceNeedOption(snDaycareHours120) }
        insertGroupPlacement(testChild_1.id, serviceNeedOptionId = snDaycareHours120.id)
        insertReservations(
            testChild_1.id,
            generateSequence(placementStart) { it.plusDays(1) }
                .map {
                    HelsinkiDateTime.of(it, LocalTime.of(8, 0)) to
                        HelsinkiDateTime.of(it, LocalTime.of(16, 0))
                }
                .take(31)
                .toList()
        )
        insertAttendances(
            testChild_1.id,
            testDaycare.id,
            generateSequence(placementStart) { it.plusDays(1) }
                .map {
                    HelsinkiDateTime.of(it, LocalTime.of(8, 0)) to
                        HelsinkiDateTime.of(it, LocalTime.of(17, 0))
                }
                .take(31)
                .toList()
        )

        val result =
            db.read {
                getGroupMonthCalendar(it, LocalDate.of(2019, 8, 31), testDaycareGroup.id, 2019, 8)
            }
        assertEquals(1, result.children.size)
        assertEquals(
            // 22 operation days
            UsedServiceTotals(
                serviceNeedHours = 120,
                reservedHours = 22 * 8,
                usedServiceHours = 22 * 9,
            ),
            result.children.first().usedService
        )
    }

    @Test
    fun `backup care children have correct service needs`() {
        db.transaction { tx ->
            val placementId =
                tx.insertTestPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                    type = PlacementType.DAYCARE
                )
            tx.insert(
                DevBackupCare(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    groupId = testDaycareGroup.id,
                    period = FiniteDateRange(placementStart, placementEnd)
                )
            )

            tx.insertServiceNeed(
                placementId = placementId,
                startDate = placementStart,
                endDate = placementEnd,
                optionId = snDaycareContractDays15.id,
                shiftCare = ShiftCareType.NONE,
                confirmedBy = null,
                confirmedAt = null
            )
        }

        val placementDate = placementStart
        val result =
            db.read {
                getGroupMonthCalendar(
                    it,
                    placementDate,
                    testDaycareGroup.id,
                    placementDate.year,
                    placementDate.monthValue
                )
            }

        assertEquals(testDaycareGroup.id, result.groupId)
        assertEquals(testDaycare.name, result.daycareName)
        assertEquals(testDaycareGroup.name, result.groupName)
        assertEquals(1, result.children.size)
        assertEquals(
            listOf(
                ChildServiceNeedInfo(
                    childId = testChild_1.id,
                    hasContractDays = true,
                    daycareHoursPerMonth = null,
                    optionName = snDaycareContractDays15.nameFi,
                    validDuring = FiniteDateRange(placementStart, placementEnd),
                    shiftCare = ShiftCareType.NONE,
                )
            ),
            result.children.find { it.id == testChild_1.id }?.actualServiceNeeds!!
        )
    }

    private fun insertGroupPlacement(
        childId: ChildId,
        placementType: PlacementType = PlacementType.DAYCARE,
        placementPeriod: FiniteDateRange = FiniteDateRange(placementStart, placementEnd),
        serviceNeedOptionId: ServiceNeedOptionId? = null
    ) {
        db.transaction { tx ->
            tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = testDaycare.id,
                        type = placementType,
                        startDate = placementPeriod.start,
                        endDate = placementPeriod.end
                    )
                )
                .let { placementId ->
                    if (serviceNeedOptionId != null) {
                        tx.insertTestServiceNeed(
                            optionId = serviceNeedOptionId,
                            period = placementPeriod,
                            placementId = placementId,
                            confirmedBy = EvakaUserId(employeeId.raw),
                        )
                    }
                    tx.insertTestDaycareGroupPlacement(
                        daycarePlacementId = placementId,
                        groupId = testDaycareGroup.id,
                        startDate = placementPeriod.start,
                        endDate = placementPeriod.end
                    )
                }
        }
    }

    private fun createNewUnitAndGroup(): Pair<DaycareId, GroupId> {
        return db.transaction {
            val unitId = it.insert(DevDaycare(areaId = testArea.id))
            unitId to it.insert(DevDaycareGroup(daycareId = unitId))
        }
    }

    private fun insertBackupPlacement(
        childId: ChildId,
        unitId: DaycareId,
        groupId: GroupId,
        placementPeriod: FiniteDateRange = FiniteDateRange(placementStart, placementEnd)
    ) {
        db.transaction {
            it.insert(
                DevBackupCare(
                    childId = childId,
                    unitId = unitId,
                    groupId = groupId,
                    period = placementPeriod
                )
            )
        }
    }

    private fun insertReservations(
        childId: ChildId,
        reservations: List<Pair<HelsinkiDateTime, HelsinkiDateTime>>
    ) {
        db.transaction { tx ->
            reservations
                .flatMap { (start, end) ->
                    if (start.toLocalDate().plusDays(1) == end.toLocalDate()) {
                        listOf(
                            start to HelsinkiDateTime.of(start.toLocalDate(), LocalTime.of(23, 59)),
                            HelsinkiDateTime.of(end.toLocalDate(), LocalTime.of(0, 0)) to end
                        )
                    } else {
                        listOf(start to end)
                    }
                }
                .forEach { (start, end) ->
                    tx.insert(
                        DevReservation(
                            childId = childId,
                            date = start.toLocalDate(),
                            startTime = start.toLocalTime(),
                            endTime = end.toLocalTime(),
                            createdBy = EvakaUserId(employeeId.raw)
                        )
                    )
                }
        }
    }

    private fun insertAttendances(
        childId: ChildId,
        unitId: DaycareId,
        attendances: List<Pair<HelsinkiDateTime, HelsinkiDateTime?>>
    ) {
        db.transaction { tx ->
            attendances.forEach {
                tx.insertTestChildAttendance(
                    childId = childId,
                    unitId = unitId,
                    arrived = it.first,
                    departed = it.second
                )
            }
        }
    }

    private fun insertDailyServiceTimes(
        childId: ChildId,
        dailyServiceTimes: DailyServiceTimesValue
    ) {
        db.transaction { tx -> tx.createChildDailyServiceTimes(childId, dailyServiceTimes) }
    }
}
