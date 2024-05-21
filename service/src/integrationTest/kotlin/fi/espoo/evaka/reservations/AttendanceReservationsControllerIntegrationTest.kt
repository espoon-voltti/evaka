// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.absence.ChildServiceNeedInfo
import fi.espoo.evaka.assistance.AssistanceFactorUpdate
import fi.espoo.evaka.assistance.insertAssistanceFactor
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesType
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesValue
import fi.espoo.evaka.daycare.insertPreschoolTerm
import fi.espoo.evaka.holidayperiod.insertHolidayPeriod
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.ScheduleType
import fi.espoo.evaka.preschoolTerm2020
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.serviceneed.insertServiceNeed
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDailyServiceTimes
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevHoliday
import fi.espoo.evaka.shared.dev.DevMobileDevice
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeInterval
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.snDaycareContractDays10
import fi.espoo.evaka.snDaycareContractDays15
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.snDefaultPreschool
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testChild_5
import fi.espoo.evaka.testChild_6
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class AttendanceReservationsControllerIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var attendanceReservationController: AttendanceReservationController

    private val employeeId = EmployeeId(UUID.randomUUID())
    private val employeeId2 = EmployeeId(UUID.randomUUID())

    private val mon = LocalDate.of(2021, 3, 1)
    private val tue = LocalDate.of(2021, 3, 2)
    private val wed = LocalDate.of(2021, 3, 3)
    private val thu = LocalDate.of(2021, 3, 4)
    private val fri = LocalDate.of(2021, 3, 5)
    private val monFri = FiniteDateRange(mon, fri)

    private val now = HelsinkiDateTime.of(mon, LocalTime.of(10, 0))
    private val clock = MockEvakaClock(now)

    private val testGroup1 = DevDaycareGroup(daycareId = testDaycare.id, name = "Test group 1")
    private val testGroup2 = DevDaycareGroup(daycareId = testDaycare.id, name = "Test group 2")
    private val testGroupInDaycare2 =
        DevDaycareGroup(daycareId = testDaycare2.id, name = "Test group 3")

    private val fullDay = TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59"))

    @BeforeEach
    fun beforeEach() {
        db.transaction {
            it.insertGeneralTestFixtures()
            it.insert(testGroup1)
            it.insert(testGroup2)
            it.insert(testGroupInDaycare2)

            it.insert(DevEmployee(employeeId))
            it.insertDaycareAclRow(testDaycare.id, employeeId, UserRole.STAFF)
            it.insert(DevEmployee(employeeId2))
            it.insertDaycareAclRow(testDaycare.id, employeeId2, UserRole.STAFF)
        }
    }

    @Test
    fun `generates the correct result in all cases`() {
        db.transaction {
            val child1PlacementId =
                it.insert(
                    DevPlacement(
                        childId = testChild_1.id,
                        unitId = testDaycare.id,
                        startDate = mon,
                        endDate = fri
                    )
                )
            it.insertServiceNeed(
                placementId = child1PlacementId,
                startDate = mon,
                endDate = thu,
                optionId = snDaycareContractDays15.id,
                shiftCare = ShiftCareType.NONE,
                partWeek = false,
                confirmedBy = null,
                confirmedAt = null
            )
            it.insertServiceNeed(
                placementId = child1PlacementId,
                startDate = fri,
                endDate = fri,
                optionId = snDaycareFullDay35.id,
                shiftCare = ShiftCareType.NONE,
                partWeek = false,
                confirmedBy = null,
                confirmedAt = null
            )
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = child1PlacementId,
                    daycareGroupId = testGroup1.id,
                    startDate = mon,
                    endDate = thu
                )
            )
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = child1PlacementId,
                    daycareGroupId = testGroup2.id,
                    startDate = fri,
                    endDate = fri
                )
            )
            it.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = mon,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0),
                    createdBy = EvakaUserId(employeeId.raw)
                )
            )
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = HelsinkiDateTime.of(mon, LocalTime.of(8, 15)),
                departed = HelsinkiDateTime.of(mon, LocalTime.of(16, 5))
            )
            it.insert(
                DevAbsence(
                    childId = testChild_1.id,
                    date = tue,
                    absenceType = AbsenceType.OTHER_ABSENCE,
                    modifiedBy = EvakaUserId(employeeId.raw),
                    absenceCategory = AbsenceCategory.BILLABLE
                )
            )
            // Reservation with no times
            it.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = wed,
                    startTime = null,
                    endTime = null,
                    createdBy = EvakaUserId(employeeId.raw)
                )
            )

            // No group placement -> ungrouped
            // Placement doesn't cover the whole period
            it.insert(
                DevPlacement(
                    childId = testChild_4.id,
                    unitId = testDaycare.id,
                    startDate = wed,
                    endDate = thu
                )
            )

            // Placement in other unit, backup in this unit's group 2
            val testChild5PlacementId =
                it.insert(
                    DevPlacement(
                        type = PlacementType.CLUB, // <- reservations not needed
                        childId = testChild_5.id,
                        unitId = testDaycare2.id,
                        startDate = mon,
                        endDate = fri
                    )
                )
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = testChild5PlacementId,
                    daycareGroupId = testGroupInDaycare2.id,
                    startDate = mon,
                    endDate = fri
                )
            )
            it.insert(
                DevBackupCare(
                    childId = testChild_5.id,
                    unitId = testDaycare.id,
                    groupId = testGroup2.id,
                    period = FiniteDateRange(fri, fri)
                )
            )
            it.insertServiceNeed(
                placementId = testChild5PlacementId,
                startDate = mon,
                endDate = fri,
                optionId = snDaycareContractDays15.id,
                shiftCare = ShiftCareType.NONE,
                partWeek = false,
                confirmedBy = null,
                confirmedAt = null
            )
            it.insert(
                DevDailyServiceTimes(
                    childId = testChild_5.id,
                    validityPeriod = monFri.asDateRange(),
                    type = DailyServiceTimesType.REGULAR,
                    regularTimes = TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0))
                )
            )

            // Placed to this unit's group 1...
            val child6PlacementId =
                it.insert(
                    DevPlacement(
                        childId = testChild_6.id,
                        unitId = testDaycare.id,
                        startDate = wed,
                        endDate = fri
                    )
                )
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = child6PlacementId,
                    daycareGroupId = testGroup1.id,
                    startDate = wed,
                    endDate = fri
                )
            )
            // ... and has a backup in another group in this unit
            it.insert(
                DevBackupCare(
                    childId = testChild_6.id,
                    unitId = testDaycare.id,
                    groupId = testGroup2.id,
                    period = FiniteDateRange(thu, thu)
                )
            )
            // ... and has a backup in another unit
            it.insert(
                DevBackupCare(
                    childId = testChild_6.id,
                    unitId = testDaycare2.id,
                    groupId = testGroupInDaycare2.id,
                    period = FiniteDateRange(fri, fri)
                )
            )
            // Reservation is shown in the result because the child is in this unit
            it.insert(
                DevReservation(
                    childId = testChild_6.id,
                    date = thu,
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(15, 0),
                    createdBy = EvakaUserId(employeeId.raw)
                )
            )
            // Reservation is NOT shown in the result because the child is in another unit
            it.insert(
                DevReservation(
                    childId = testChild_6.id,
                    date = fri,
                    startTime = LocalTime.of(7, 0),
                    endTime = LocalTime.of(17, 0),
                    createdBy = EvakaUserId(employeeId.raw)
                )
            )
        }

        val response = getAttendanceReservations()
        assertEquals(testDaycare.name, response.unit)

        assertEquals(
            setOf(testChild_1.id, testChild_4.id, testChild_5.id, testChild_6.id),
            response.children.map { it.id }.toSet()
        )
        response.children
            .first { it.id == testChild_1.id }
            .also { child1 ->
                assertEquals(
                    listOf(
                        ChildServiceNeedInfo(
                            childId = testChild_1.id,
                            hasContractDays = true,
                            daycareHoursPerMonth = null,
                            optionName = snDaycareContractDays15.nameFi,
                            validDuring = FiniteDateRange(mon, thu),
                            shiftCare = ShiftCareType.NONE,
                            partWeek = false
                        ),
                        ChildServiceNeedInfo(
                            childId = testChild_1.id,
                            hasContractDays = false,
                            daycareHoursPerMonth = null,
                            optionName = snDaycareFullDay35.nameFi,
                            validDuring = FiniteDateRange(fri, fri),
                            shiftCare = ShiftCareType.NONE,
                            partWeek = false
                        )
                    ),
                    child1.serviceNeeds
                )
            }
        response.children
            .first { it.id == testChild_4.id }
            .also { child4 -> assertEquals(emptyList(), child4.serviceNeeds) }
        response.children
            .first { it.id == testChild_5.id }
            .also { child5 ->
                assertEquals(
                    listOf(
                        ChildServiceNeedInfo(
                            childId = testChild_5.id,
                            hasContractDays = true,
                            daycareHoursPerMonth = null,
                            optionName = snDaycareContractDays15.nameFi,
                            validDuring = monFri,
                            shiftCare = ShiftCareType.NONE,
                            partWeek = false
                        )
                    ),
                    child5.serviceNeeds
                )
            }
        response.children
            .first { it.id == testChild_6.id }
            .also { child6 -> assertEquals(emptyList(), child6.serviceNeeds) }

        assertEquals(
            listOf(
                UnitAttendanceReservations.ReservationGroup(testGroup1.id, testGroup1.name),
                UnitAttendanceReservations.ReservationGroup(testGroup2.id, testGroup2.name),
            ),
            response.groups
        )

        assertEquals(5, response.days.size)
        assertTrue(
            response.days.all {
                it.dateInfo ==
                    UnitAttendanceReservations.UnitDateInfo(
                        normalOperatingTimes = TimeRange(LocalTime.of(0, 0), LocalTime.of(23, 59)),
                        shiftCareOperatingTimes =
                            TimeRange(LocalTime.of(0, 0), LocalTime.of(23, 59)),
                        shiftCareOpenOnHoliday = false,
                        isHoliday = false,
                        isInHolidayPeriod = false
                    )
            }
        )

        response.days
            .first { it.date == mon }
            .children
            .also { monChildren ->
                assertEquals(
                    listOf(
                        UnitAttendanceReservations.ChildRecordOfDay(
                            childId = testChild_1.id,
                            reservations =
                                listOf(
                                    ReservationResponse.Times(
                                        TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
                                        true
                                    )
                                ),
                            attendances =
                                listOf(
                                    TimeInterval(
                                        start = LocalTime.of(8, 15),
                                        end = LocalTime.of(16, 5)
                                    )
                                ),
                            absenceBillable = null,
                            absenceNonbillable = null,
                            possibleAbsenceCategories = setOf(AbsenceCategory.BILLABLE),
                            dailyServiceTimes = null,
                            groupId = testGroup1.id,
                            backupGroupId = null,
                            inOtherUnit = false,
                            scheduleType = ScheduleType.RESERVATION_REQUIRED
                        )
                    ),
                    monChildren
                )
            }

        response.days
            .first { it.date == tue }
            .children
            .also { tueChildren ->
                assertEquals(
                    listOf(
                        UnitAttendanceReservations.ChildRecordOfDay(
                            childId = testChild_1.id,
                            reservations = emptyList(),
                            attendances = emptyList(),
                            absenceBillable = AbsenceTypeResponse(AbsenceType.OTHER_ABSENCE, true),
                            absenceNonbillable = null,
                            possibleAbsenceCategories = setOf(AbsenceCategory.BILLABLE),
                            dailyServiceTimes = null,
                            groupId = testGroup1.id,
                            backupGroupId = null,
                            inOtherUnit = false,
                            scheduleType = ScheduleType.RESERVATION_REQUIRED
                        )
                    ),
                    tueChildren
                )
            }

        response.days
            .first { it.date == wed }
            .children
            .also { wedChildren ->
                assertEquals(3, wedChildren.size)
                assertEquals(
                    UnitAttendanceReservations.ChildRecordOfDay(
                        childId = testChild_1.id,
                        reservations = listOf(ReservationResponse.NoTimes(true)),
                        attendances = emptyList(),
                        absenceBillable = null,
                        absenceNonbillable = null,
                        possibleAbsenceCategories = setOf(AbsenceCategory.BILLABLE),
                        dailyServiceTimes = null,
                        groupId = testGroup1.id,
                        backupGroupId = null,
                        inOtherUnit = false,
                        scheduleType = ScheduleType.RESERVATION_REQUIRED
                    ),
                    wedChildren.first { it.childId == testChild_1.id }
                )
                assertEquals(
                    UnitAttendanceReservations.ChildRecordOfDay(
                        childId = testChild_4.id,
                        reservations = emptyList(),
                        attendances = emptyList(),
                        absenceBillable = null,
                        absenceNonbillable = null,
                        possibleAbsenceCategories = setOf(AbsenceCategory.BILLABLE),
                        dailyServiceTimes = null,
                        groupId = null,
                        backupGroupId = null,
                        inOtherUnit = false,
                        scheduleType = ScheduleType.RESERVATION_REQUIRED
                    ),
                    wedChildren.first { it.childId == testChild_4.id }
                )
                assertEquals(
                    UnitAttendanceReservations.ChildRecordOfDay(
                        childId = testChild_6.id,
                        reservations = emptyList(),
                        attendances = emptyList(),
                        absenceBillable = null,
                        absenceNonbillable = null,
                        possibleAbsenceCategories = setOf(AbsenceCategory.BILLABLE),
                        dailyServiceTimes = null,
                        groupId = testGroup1.id,
                        backupGroupId = null,
                        inOtherUnit = false,
                        scheduleType = ScheduleType.RESERVATION_REQUIRED
                    ),
                    wedChildren.first { it.childId == testChild_6.id }
                )
            }

        response.days
            .first { it.date == thu }
            .children
            .also { thuChildren ->
                assertEquals(3, thuChildren.size)
                assertEquals(
                    UnitAttendanceReservations.ChildRecordOfDay(
                        childId = testChild_1.id,
                        reservations = emptyList(),
                        attendances = emptyList(),
                        absenceBillable = null,
                        absenceNonbillable = null,
                        possibleAbsenceCategories = setOf(AbsenceCategory.BILLABLE),
                        dailyServiceTimes = null,
                        groupId = testGroup1.id,
                        backupGroupId = null,
                        inOtherUnit = false,
                        scheduleType = ScheduleType.RESERVATION_REQUIRED
                    ),
                    thuChildren.first { it.childId == testChild_1.id }
                )
                assertEquals(
                    UnitAttendanceReservations.ChildRecordOfDay(
                        childId = testChild_4.id,
                        reservations = emptyList(),
                        attendances = emptyList(),
                        absenceBillable = null,
                        absenceNonbillable = null,
                        possibleAbsenceCategories = setOf(AbsenceCategory.BILLABLE),
                        dailyServiceTimes = null,
                        groupId = null,
                        backupGroupId = null,
                        inOtherUnit = false,
                        scheduleType = ScheduleType.RESERVATION_REQUIRED
                    ),
                    thuChildren.first { it.childId == testChild_4.id }
                )
                assertEquals(
                    UnitAttendanceReservations.ChildRecordOfDay(
                        childId = testChild_6.id,
                        reservations =
                            listOf(
                                ReservationResponse.Times(
                                    TimeRange(LocalTime.of(9, 0), LocalTime.of(15, 0)),
                                    true
                                )
                            ),
                        attendances = emptyList(),
                        absenceBillable = null,
                        absenceNonbillable = null,
                        possibleAbsenceCategories = setOf(AbsenceCategory.BILLABLE),
                        dailyServiceTimes = null,
                        groupId = testGroup1.id,
                        backupGroupId = testGroup2.id,
                        inOtherUnit = false,
                        scheduleType = ScheduleType.RESERVATION_REQUIRED
                    ),
                    thuChildren.first { it.childId == testChild_6.id }
                )
            }

        response.days
            .first { it.date == fri }
            .children
            .also { friChildren ->
                assertEquals(3, friChildren.size)
                assertEquals(
                    UnitAttendanceReservations.ChildRecordOfDay(
                        childId = testChild_1.id,
                        reservations = emptyList(),
                        attendances = emptyList(),
                        absenceBillable = null,
                        absenceNonbillable = null,
                        possibleAbsenceCategories = setOf(AbsenceCategory.BILLABLE),
                        dailyServiceTimes = null,
                        groupId = testGroup2.id,
                        backupGroupId = null,
                        inOtherUnit = false,
                        scheduleType = ScheduleType.RESERVATION_REQUIRED
                    ),
                    friChildren.first { it.childId == testChild_1.id }
                )
                assertEquals(
                    UnitAttendanceReservations.ChildRecordOfDay(
                        childId = testChild_5.id,
                        reservations = emptyList(),
                        attendances = emptyList(),
                        absenceBillable = null,
                        absenceNonbillable = null,
                        possibleAbsenceCategories = setOf(AbsenceCategory.NONBILLABLE),
                        dailyServiceTimes =
                            DailyServiceTimesValue.RegularTimes(
                                validityPeriod = monFri.asDateRange(),
                                regularTimes = TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0))
                            ),
                        groupId = null,
                        backupGroupId = testGroup2.id,
                        inOtherUnit = false,
                        scheduleType = ScheduleType.FIXED_SCHEDULE
                    ),
                    friChildren.first { it.childId == testChild_5.id }
                )
                assertEquals(
                    UnitAttendanceReservations.ChildRecordOfDay(
                        childId = testChild_6.id,
                        reservations = emptyList(),
                        attendances = emptyList(),
                        absenceBillable = null,
                        absenceNonbillable = null,
                        possibleAbsenceCategories = setOf(AbsenceCategory.BILLABLE),
                        dailyServiceTimes = null,
                        groupId = testGroup1.id,
                        backupGroupId = null,
                        inOtherUnit = true,
                        scheduleType = ScheduleType.RESERVATION_REQUIRED
                    ),
                    friChildren.first { it.childId == testChild_6.id }
                )
            }
    }

    @Test
    fun `two reservations or attendances in the same day`() {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = mon,
                    endDate = fri
                )
            )
            listOf(
                    DevReservation(
                        childId = testChild_1.id,
                        date = mon,
                        startTime = LocalTime.of(19, 0),
                        endTime = LocalTime.of(23, 59),
                        createdBy = EvakaUserId(employeeId.raw)
                    ),
                    DevReservation(
                        childId = testChild_1.id,
                        date = tue,
                        startTime = LocalTime.of(0, 0),
                        endTime = LocalTime.of(8, 0),
                        createdBy = EvakaUserId(employeeId.raw)
                    ),
                    DevReservation(
                        childId = testChild_1.id,
                        date = tue,
                        startTime = LocalTime.of(17, 30),
                        endTime = LocalTime.of(23, 59),
                        createdBy = EvakaUserId(employeeId.raw)
                    ),
                    DevReservation(
                        childId = testChild_1.id,
                        date = wed,
                        startTime = LocalTime.of(0, 0),
                        endTime = LocalTime.of(9, 30),
                        createdBy = EvakaUserId(employeeId.raw)
                    )
                )
                .forEach { tx.insert(it) }

            listOf(
                    Pair(
                        HelsinkiDateTime.of(mon, LocalTime.of(19, 10)),
                        HelsinkiDateTime.of(mon, LocalTime.of(23, 59))
                    ),
                    Pair(
                        HelsinkiDateTime.of(tue, LocalTime.of(0, 0)),
                        HelsinkiDateTime.of(tue, LocalTime.of(10, 30))
                    ),
                    Pair(HelsinkiDateTime.of(tue, LocalTime.of(17, 0)), null)
                )
                .forEach {
                    tx.insertTestChildAttendance(
                        childId = testChild_1.id,
                        unitId = testDaycare.id,
                        arrived = it.first,
                        departed = it.second
                    )
                }
        }

        val response = getAttendanceReservations()

        assertEquals(
            listOf(
                UnitAttendanceReservations.ChildRecordOfDay(
                    childId = testChild_1.id,
                    reservations =
                        listOf(
                            ReservationResponse.Times(
                                TimeRange(LocalTime.of(19, 0), LocalTime.of(23, 59)),
                                true
                            )
                        ),
                    attendances = listOf(TimeInterval(LocalTime.of(19, 10), LocalTime.of(23, 59))),
                    absenceBillable = null,
                    absenceNonbillable = null,
                    possibleAbsenceCategories = setOf(AbsenceCategory.BILLABLE),
                    dailyServiceTimes = null,
                    groupId = null,
                    backupGroupId = null,
                    inOtherUnit = false,
                    scheduleType = ScheduleType.RESERVATION_REQUIRED
                )
            ),
            response.days.first { it.date == mon }.children
        )

        assertEquals(
            listOf(
                UnitAttendanceReservations.ChildRecordOfDay(
                    childId = testChild_1.id,
                    reservations =
                        listOf(
                            ReservationResponse.Times(
                                TimeRange(LocalTime.of(0, 0), LocalTime.of(8, 0)),
                                true
                            ),
                            ReservationResponse.Times(
                                TimeRange(LocalTime.of(17, 30), LocalTime.of(23, 59)),
                                true
                            ),
                        ),
                    attendances =
                        listOf(
                            TimeInterval(LocalTime.of(0, 0), LocalTime.of(10, 30)),
                            TimeInterval(LocalTime.of(17, 0), null),
                        ),
                    absenceBillable = null,
                    absenceNonbillable = null,
                    possibleAbsenceCategories = setOf(AbsenceCategory.BILLABLE),
                    dailyServiceTimes = null,
                    groupId = null,
                    backupGroupId = null,
                    inOtherUnit = false,
                    scheduleType = ScheduleType.RESERVATION_REQUIRED
                )
            ),
            response.days.first { it.date == tue }.children
        )

        assertEquals(
            listOf(
                UnitAttendanceReservations.ChildRecordOfDay(
                    childId = testChild_1.id,
                    reservations =
                        listOf(
                            ReservationResponse.Times(
                                TimeRange(LocalTime.of(0, 0), LocalTime.of(9, 30)),
                                true
                            )
                        ),
                    attendances = emptyList(),
                    absenceBillable = null,
                    absenceNonbillable = null,
                    possibleAbsenceCategories = setOf(AbsenceCategory.BILLABLE),
                    dailyServiceTimes = null,
                    groupId = null,
                    backupGroupId = null,
                    inOtherUnit = false,
                    scheduleType = ScheduleType.RESERVATION_REQUIRED
                )
            ),
            response.days.first { it.date == wed }.children
        )
    }

    @Test
    fun `operational days for holiday period with upcoming deadline`() {
        db.transaction { tx -> tx.insertHolidayPeriod(monFri, mon) }

        val result = getAttendanceReservations()
        assertEquals(5, result.days.size)
        result.days.forEach { day ->
            assertEquals(
                UnitAttendanceReservations.UnitDateInfo(
                    normalOperatingTimes = TimeRange(LocalTime.of(0, 0), LocalTime.of(23, 59)),
                    shiftCareOperatingTimes = TimeRange(LocalTime.of(0, 0), LocalTime.of(23, 59)),
                    shiftCareOpenOnHoliday = false,
                    isHoliday = false,
                    isInHolidayPeriod = true
                ),
                day.dateInfo
            )
        }
    }

    @Test
    fun `operational days for holiday period with past deadline`() {
        db.transaction { tx -> tx.insertHolidayPeriod(monFri, mon) }

        val result =
            getAttendanceReservations(
                clock = MockEvakaClock(HelsinkiDateTime.of(tue, LocalTime.of(10, 0)))
            )
        assertEquals(5, result.days.size)
        result.days.forEach { day ->
            assertEquals(
                UnitAttendanceReservations.UnitDateInfo(
                    normalOperatingTimes = TimeRange(LocalTime.of(0, 0), LocalTime.of(23, 59)),
                    shiftCareOperatingTimes = TimeRange(LocalTime.of(0, 0), LocalTime.of(23, 59)),
                    shiftCareOpenOnHoliday = false,
                    isHoliday = false,
                    isInHolidayPeriod = true
                ),
                day.dateInfo
            )
        }
    }

    @Test
    fun `operational day for holiday`() {
        db.transaction { tx -> tx.insert(DevHoliday(mon, "holiday")) }

        val result = getAttendanceReservations()
        assertEquals(
            UnitAttendanceReservations.UnitDateInfo(
                normalOperatingTimes = TimeRange(LocalTime.of(0, 0), LocalTime.of(23, 59)),
                shiftCareOperatingTimes = TimeRange(LocalTime.of(0, 0), LocalTime.of(23, 59)),
                shiftCareOpenOnHoliday = false,
                isHoliday = true,
                isInHolidayPeriod = false
            ),
            result.days.first().dateInfo
        )
    }

    @Test
    fun `get confirmed range reservations returns correct data`() {
        val mobileDeviceId =
            db.transaction { tx ->
                tx.insert(
                    DevPlacement(
                        childId = testChild_1.id,
                        unitId = testDaycare.id,
                        startDate = mon,
                        endDate = fri
                    )
                )
                tx.insert(DevMobileDevice(unitId = testDaycare.id))
            }
        val reservations =
            attendanceReservationController.getConfirmedRangeData(
                dbInstance(),
                AuthenticatedUser.MobileDevice(id = mobileDeviceId),
                clock,
                testChild_1.id
            )
        assertEquals(
            listOf(
                ConfirmedRangeDate(
                    date = tue,
                    scheduleType = ScheduleType.RESERVATION_REQUIRED,
                    reservations = emptyList(),
                    absenceType = null,
                    dailyServiceTimes = null
                ),
                ConfirmedRangeDate(
                    date = wed,
                    scheduleType = ScheduleType.RESERVATION_REQUIRED,
                    reservations = emptyList(),
                    absenceType = null,
                    dailyServiceTimes = null
                ),
                ConfirmedRangeDate(
                    date = thu,
                    scheduleType = ScheduleType.RESERVATION_REQUIRED,
                    reservations = emptyList(),
                    absenceType = null,
                    dailyServiceTimes = null
                ),
                ConfirmedRangeDate(
                    date = fri,
                    scheduleType = ScheduleType.RESERVATION_REQUIRED,
                    reservations = emptyList(),
                    absenceType = null,
                    dailyServiceTimes = null
                ),
            ),
            reservations
        )
    }

    @Test
    fun `post child date presence - insert, update and delete reservation, attendance and absence`() {
        val testNow = HelsinkiDateTime.of(wed, LocalTime.of(18, 0))
        val testClock = MockEvakaClock(testNow)

        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = mon,
                    endDate = fri
                )
            )
        }

        attendanceReservationController.postChildDatePresence(
            dbInstance(),
            AuthenticatedUser.Employee(employeeId, setOf(UserRole.STAFF)),
            testClock,
            ChildDatePresence(
                date = testClock.today(),
                childId = testChild_1.id,
                unitId = testDaycare.id,
                reservations =
                    listOf(
                        Reservation.Times(TimeRange(LocalTime.of(9, 0), LocalTime.of(17, 0))),
                        Reservation.Times(TimeRange(LocalTime.of(22, 0), LocalTime.of(23, 59)))
                    ),
                attendances = listOf(TimeInterval(start = LocalTime.of(12, 30), null)),
                absenceBillable = AbsenceType.OTHER_ABSENCE,
                absenceNonbillable = AbsenceType.OTHER_ABSENCE
            )
        )

        assertEquals(
            UnitAttendanceReservations.ChildRecordOfDay(
                childId = testChild_1.id,
                reservations =
                    listOf(
                        ReservationResponse.Times(
                            TimeRange(LocalTime.of(9, 0), LocalTime.of(17, 0)),
                            true
                        ),
                        ReservationResponse.Times(
                            TimeRange(LocalTime.of(22, 0), LocalTime.of(23, 59)),
                            true
                        )
                    ),
                attendances = listOf(TimeInterval(start = LocalTime.of(12, 30), end = null)),
                absenceBillable = AbsenceTypeResponse(AbsenceType.OTHER_ABSENCE, true),
                absenceNonbillable = AbsenceTypeResponse(AbsenceType.OTHER_ABSENCE, true),
                possibleAbsenceCategories =
                    setOf(AbsenceCategory.NONBILLABLE, AbsenceCategory.BILLABLE),
                dailyServiceTimes = null,
                groupId = null,
                backupGroupId = null,
                inOtherUnit = false,
                scheduleType = ScheduleType.RESERVATION_REQUIRED
            ),
            getAttendanceReservations(testClock).days[2].children.first()
        )

        // updating as a different user

        attendanceReservationController.postChildDatePresence(
            dbInstance(),
            AuthenticatedUser.Employee(employeeId2, setOf(UserRole.STAFF)),
            testClock,
            ChildDatePresence(
                date = testClock.today(),
                childId = testChild_1.id,
                unitId = testDaycare.id,
                reservations =
                    listOf(
                        Reservation.Times(TimeRange(LocalTime.of(9, 0), LocalTime.of(17, 0))),
                        Reservation.Times(TimeRange(LocalTime.of(21, 30), LocalTime.of(23, 59)))
                    ),
                attendances =
                    listOf(TimeInterval(start = LocalTime.of(12, 30), LocalTime.of(17, 0))),
                absenceBillable = AbsenceType.FORCE_MAJEURE,
                absenceNonbillable = AbsenceType.OTHER_ABSENCE
            )
        )

        assertEquals(
            UnitAttendanceReservations.ChildRecordOfDay(
                childId = testChild_1.id,
                reservations =
                    listOf(
                        ReservationResponse.Times(
                            TimeRange(LocalTime.of(9, 0), LocalTime.of(17, 0)),
                            true
                        ),
                        ReservationResponse.Times(
                            TimeRange(LocalTime.of(21, 30), LocalTime.of(23, 59)),
                            true
                        )
                    ),
                attendances =
                    listOf(TimeInterval(start = LocalTime.of(12, 30), end = LocalTime.of(17, 0))),
                absenceBillable = AbsenceTypeResponse(AbsenceType.FORCE_MAJEURE, true),
                absenceNonbillable = AbsenceTypeResponse(AbsenceType.OTHER_ABSENCE, true),
                possibleAbsenceCategories =
                    setOf(AbsenceCategory.NONBILLABLE, AbsenceCategory.BILLABLE),
                dailyServiceTimes = null,
                groupId = null,
                backupGroupId = null,
                inOtherUnit = false,
                scheduleType = ScheduleType.RESERVATION_REQUIRED
            ),
            getAttendanceReservations(testClock).days[2].children.first()
        )
        db.read { tx ->
            val reservationCreators =
                @Suppress("DEPRECATION")
                tx.createQuery("SELECT created_by FROM attendance_reservation").toSet<EmployeeId>()
            val absenceCreators =
                @Suppress("DEPRECATION")
                tx.createQuery("SELECT modified_by FROM absence").toSet<EmployeeId>()
            // original should be preserved when unchanged
            assertEquals(setOf(employeeId, employeeId2), reservationCreators)
            assertEquals(setOf(employeeId, employeeId2), absenceCreators)
        }

        // deleting

        attendanceReservationController.postChildDatePresence(
            dbInstance(),
            AuthenticatedUser.Employee(employeeId, setOf(UserRole.STAFF)),
            testClock,
            ChildDatePresence(
                date = testClock.today(),
                childId = testChild_1.id,
                unitId = testDaycare.id,
                reservations = emptyList(),
                attendances = emptyList(),
                absenceBillable = null,
                absenceNonbillable = null
            )
        )

        assertEquals(
            UnitAttendanceReservations.ChildRecordOfDay(
                childId = testChild_1.id,
                reservations = emptyList(),
                attendances = emptyList(),
                absenceBillable = null,
                absenceNonbillable = null,
                possibleAbsenceCategories =
                    setOf(AbsenceCategory.NONBILLABLE, AbsenceCategory.BILLABLE),
                dailyServiceTimes = null,
                groupId = null,
                backupGroupId = null,
                inOtherUnit = false,
                scheduleType = ScheduleType.RESERVATION_REQUIRED
            ),
            getAttendanceReservations(testClock).days[2].children.first()
        )
    }

    @Test
    fun `get non-reservable reservations throws forbidden when child doesn't have placement`() {
        val mobileDeviceId =
            db.transaction { tx -> tx.insert(DevMobileDevice(unitId = testDaycare.id)) }
        assertThrows<Forbidden> {
            attendanceReservationController.getConfirmedRangeData(
                dbInstance(),
                AuthenticatedUser.MobileDevice(id = mobileDeviceId),
                clock,
                testChild_1.id
            )
        }
    }

    @Test
    fun `get non-reservable reservations throws forbidden when child has placement to other unit`() {
        val mobileDeviceId =
            db.transaction { tx ->
                tx.insert(
                    DevPlacement(
                        childId = testChild_1.id,
                        unitId = testDaycare2.id,
                        startDate = mon,
                        endDate = fri
                    )
                )
                tx.insert(DevMobileDevice(unitId = testDaycare.id))
            }
        assertThrows<Forbidden> {
            attendanceReservationController.getConfirmedRangeData(
                dbInstance(),
                AuthenticatedUser.MobileDevice(id = mobileDeviceId),
                clock,
                testChild_1.id
            )
        }
    }

    @Test
    fun `set non-reservable reservations updates correct data`() {
        val mobileDeviceId =
            db.transaction { tx ->
                tx.insert(
                    DevPlacement(
                        childId = testChild_1.id,
                        unitId = testDaycare.id,
                        startDate = mon,
                        endDate = fri
                    )
                )
                tx.insert(DevMobileDevice(unitId = testDaycare.id))
            }
        attendanceReservationController.setConfirmedRangeReservations(
            dbInstance(),
            AuthenticatedUser.MobileDevice(id = mobileDeviceId),
            clock,
            testChild_1.id,
            listOf(
                ConfirmedRangeDateUpdate(
                    clock.today().plusDays(1),
                    reservations = emptyList(),
                    absenceType = null,
                )
            )
        )
    }

    @Test
    fun `daily confirmed reservation stats for unit`() {
        insertConfirmedReservationTestData()
        val mobileDeviceId = insertMobileDevice(testDaycare.id)

        val expectation =
            listOf(
                AttendanceReservationController.DayReservationStatisticsResult(
                    date = LocalDate.of(2021, 2, 24),
                    groupStatistics =
                        listOf(
                            AttendanceReservationController.GroupReservationStatisticResult(
                                calculatedPresent = BigDecimal.ZERO,
                                presentCount = 0,
                                absentCount = 0,
                                groupId = null
                            )
                        )
                ),
                AttendanceReservationController.DayReservationStatisticsResult(
                    date = LocalDate.of(2021, 2, 25),
                    groupStatistics =
                        listOf(
                            AttendanceReservationController.GroupReservationStatisticResult(
                                calculatedPresent = BigDecimal.ZERO,
                                presentCount = 0,
                                absentCount = 0,
                                groupId = null
                            )
                        )
                ),
                AttendanceReservationController.DayReservationStatisticsResult(
                    date = mon,
                    groupStatistics =
                        listOf(
                            AttendanceReservationController.GroupReservationStatisticResult(
                                calculatedPresent = BigDecimal("1.0000"),
                                presentCount = 1,
                                absentCount = 0,
                                groupId = testGroup1.id
                            ),
                            AttendanceReservationController.GroupReservationStatisticResult(
                                calculatedPresent = BigDecimal.ZERO,
                                presentCount = 0,
                                absentCount = 1,
                                groupId = testGroup2.id
                            )
                        )
                ),
                AttendanceReservationController.DayReservationStatisticsResult(
                    date = tue,
                    groupStatistics =
                        listOf(
                            AttendanceReservationController.GroupReservationStatisticResult(
                                calculatedPresent = BigDecimal.ZERO,
                                presentCount = 0,
                                absentCount = 1,
                                groupId = testGroup1.id
                            ),
                            AttendanceReservationController.GroupReservationStatisticResult(
                                calculatedPresent = BigDecimal.ZERO,
                                presentCount = 0,
                                absentCount = 1,
                                groupId = testGroup2.id
                            )
                        )
                ),
                AttendanceReservationController.DayReservationStatisticsResult(
                    date = wed,
                    groupStatistics =
                        listOf(
                            AttendanceReservationController.GroupReservationStatisticResult(
                                calculatedPresent = BigDecimal.ZERO,
                                presentCount = 0,
                                absentCount = 1,
                                groupId = testGroup1.id
                            ),
                            AttendanceReservationController.GroupReservationStatisticResult(
                                calculatedPresent = BigDecimal("0.5000"),
                                presentCount = 1,
                                absentCount = 0,
                                groupId = testGroup2.id
                            )
                        )
                ),
                AttendanceReservationController.DayReservationStatisticsResult(
                    date = thu,
                    groupStatistics =
                        listOf(
                            AttendanceReservationController.GroupReservationStatisticResult(
                                calculatedPresent = BigDecimal("1.5000"),
                                presentCount = 2,
                                absentCount = 0,
                                groupId = testGroup2.id
                            )
                        )
                ),
                AttendanceReservationController.DayReservationStatisticsResult(
                    date = fri,
                    groupStatistics =
                        listOf(
                            AttendanceReservationController.GroupReservationStatisticResult(
                                calculatedPresent = BigDecimal("1.250"),
                                presentCount = 1,
                                absentCount = 0,
                                groupId = testGroup1.id
                            ),
                            AttendanceReservationController.GroupReservationStatisticResult(
                                calculatedPresent = BigDecimal("0.5000"),
                                presentCount = 1,
                                absentCount = 0,
                                groupId = testGroup2.id
                            )
                        )
                ),
            )

        val testClock = MockEvakaClock(HelsinkiDateTime.of(tue.minusWeeks(1), LocalTime.of(10, 0)))

        val result =
            getConfirmedDailyReservationStats(
                testDaycare.id,
                mobileDeviceId = mobileDeviceId,
                clock = testClock
            )

        result.forEachIndexed { index, dayReservationStatisticsResult ->
            val expected = expectation[index]
            assertEquals(expected.date, dayReservationStatisticsResult.date)
            assertThat(dayReservationStatisticsResult.groupStatistics)
                .containsExactlyInAnyOrder(*expected.groupStatistics.toTypedArray())
        }
    }

    @Test
    fun `daily confirmed child reservation for unit`() {
        insertConfirmedReservationTestData()
        val mobileDeviceId = insertMobileDevice(testDaycare.id)

        val mondayResult =
            getConfirmedChildReservationsForDay(
                mon,
                testDaycare.id,
                mobileDeviceId = mobileDeviceId
            )

        val childMap =
            mapOf(
                Pair(
                    testChild_1.id,
                    AttendanceReservationController.ReservationChildInfo(
                        id = testChild_1.id,
                        firstName = testChild_1.firstName,
                        lastName = testChild_1.lastName,
                        preferredName = testChild_1.preferredName,
                        dateOfBirth = testChild_1.dateOfBirth
                    )
                ),
                Pair(
                    testChild_2.id,
                    AttendanceReservationController.ReservationChildInfo(
                        id = testChild_2.id,
                        firstName = testChild_2.firstName,
                        lastName = testChild_2.lastName,
                        preferredName = testChild_2.preferredName,
                        dateOfBirth = testChild_2.dateOfBirth
                    )
                )
            )

        val child1Expectation =
            AttendanceReservationController.ChildReservationInfo(
                childId = testChild_1.id,
                reservations =
                    listOf(
                        ReservationResponse.Times(
                            TimeRange(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                            true
                        ),
                        ReservationResponse.Times(
                            TimeRange(LocalTime.of(13, 0), LocalTime.of(16, 0)),
                            true
                        )
                    ),
                groupId = testGroup1.id,
                absent = false,
                outOnBackupPlacement = false,
                dailyServiceTimes = null,
                scheduleType = ScheduleType.RESERVATION_REQUIRED,
                isInHolidayPeriod = false
            )

        val child2Expectation =
            child1Expectation.copy(
                childId = testChild_2.id,
                groupId = testGroup2.id,
                scheduleType = ScheduleType.TERM_BREAK,
                reservations = listOf()
            )

        val mondayExpectation =
            AttendanceReservationController.DailyChildReservationResult(
                children = childMap,
                childReservations = listOf(child1Expectation, child2Expectation)
            )

        val tuesdayResult =
            getConfirmedChildReservationsForDay(
                tue,
                testDaycare.id,
                mobileDeviceId = mobileDeviceId
            )

        val tuesdayExpectation =
            AttendanceReservationController.DailyChildReservationResult(
                children = childMap,
                childReservations =
                    listOf(
                        child1Expectation.copy(reservations = listOf(), absent = true),
                        child2Expectation
                    )
            )

        val fridayResult =
            getConfirmedChildReservationsForDay(
                fri,
                testDaycare.id,
                mobileDeviceId = mobileDeviceId
            )

        val fridayExpectation =
            AttendanceReservationController.DailyChildReservationResult(
                children = childMap,
                childReservations =
                    listOf(
                        child1Expectation.copy(
                            reservations = listOf(),
                            dailyServiceTimes =
                                DailyServiceTimesValue.RegularTimes(
                                    validityPeriod =
                                        DateRange(
                                            LocalDate.of(2021, 3, 5),
                                            LocalDate.of(2021, 3, 5)
                                        ),
                                    regularTimes =
                                        TimeRange(LocalTime.of(8, 0), LocalTime.of(15, 0))
                                )
                        ),
                        child2Expectation.copy(scheduleType = ScheduleType.FIXED_SCHEDULE)
                    )
            )
        assertEquals(mondayExpectation.children, mondayResult.children)
        assertThat(mondayResult.childReservations)
            .containsExactlyInAnyOrderElementsOf(mondayExpectation.childReservations)
        assertEquals(tuesdayExpectation.children, tuesdayResult.children)
        assertThat(tuesdayResult.childReservations)
            .containsExactlyInAnyOrderElementsOf(tuesdayExpectation.childReservations)
        assertEquals(fridayExpectation.children, fridayResult.children)
        assertThat(fridayResult.childReservations)
            .containsExactlyInAnyOrderElementsOf(fridayExpectation.childReservations)
    }

    @Test
    fun `should not include children on their non-operational days unless service need is intermittent`() {
        val range =
            FiniteDateRange(
                LocalDate.of(2024, 5, 20), // Mon
                LocalDate.of(2024, 5, 26) // Sun
            )
        val daycareId =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea(shortName = "area"))
                val daycareId =
                    tx.insert(
                        DevDaycare(
                            areaId = areaId,
                            // mon-fri
                            operationTimes =
                                listOf(fullDay, fullDay, fullDay, fullDay, fullDay, null, null),
                            // mon-sat
                            shiftCareOperationTimes =
                                listOf(fullDay, fullDay, fullDay, fullDay, fullDay, fullDay, null)
                        )
                    )
                tx.insertDaycareAclRow(daycareId, employeeId, UserRole.STAFF)
                daycareId
            }
        val groupId = db.transaction { it.insert(DevDaycareGroup(daycareId = daycareId)) }
        val normalChild =
            db.transaction { insertChildData(it, range, daycareId, groupId, ShiftCareType.NONE) }
        val shiftCareChild =
            db.transaction { insertChildData(it, range, daycareId, groupId, ShiftCareType.FULL) }
        val intermittentShiftCareChild =
            db.transaction {
                insertChildData(it, range, daycareId, groupId, ShiftCareType.INTERMITTENT)
            }

        val result =
            getAttendanceReservations(
                range = range,
                daycareId = daycareId,
                includeNonOperationalDays = true
            )
        assertEquals(7, result.days.size)
        result.days.take(5).forEach { monToFri ->
            assertEquals(
                setOf(normalChild, shiftCareChild, intermittentShiftCareChild),
                monToFri.children.map { it.childId }.toSet()
            )
        }
        result.days[5].let { saturday ->
            assertEquals(
                setOf(shiftCareChild, intermittentShiftCareChild),
                saturday.children.map { it.childId }.toSet()
            )
        }
        result.days[6].let { sunday ->
            assertEquals(
                setOf(intermittentShiftCareChild),
                sunday.children.map { it.childId }.toSet()
            )
        }
    }

    private fun insertMobileDevice(unitId: DaycareId): MobileDeviceId {
        return db.transaction { tx -> tx.insert(DevMobileDevice(unitId = unitId)) }
    }

    private fun insertConfirmedReservationTestData() {
        db.transaction {
            // clear existing term that has no term breaks
            @Suppress("DEPRECATION")
            it.createUpdate("DELETE FROM preschool_term where extended_term @> :day")
                .bind("day", mon)
                .execute()

            val previousFriday = mon.minusDays(3)
            it.insertPreschoolTerm(
                preschoolTerm2020.finnishPreschool,
                preschoolTerm2020.swedishPreschool,
                preschoolTerm2020.extendedTerm,
                preschoolTerm2020.applicationPeriod,
                DateSet.of(FiniteDateRange(mon, tue))
            )

            it.insert(DevHoliday(previousFriday, "holiday"))

            val child1PlacementId =
                it.insert(
                    DevPlacement(
                        childId = testChild_1.id,
                        unitId = testDaycare.id,
                        startDate = previousFriday,
                        endDate = fri
                    )
                )

            it.insertServiceNeed(
                placementId = child1PlacementId,
                startDate = previousFriday,
                endDate = thu,
                optionId = snDaycareContractDays15.id,
                shiftCare = ShiftCareType.NONE,
                partWeek = false,
                confirmedBy = null,
                confirmedAt = null
            )
            it.insertServiceNeed(
                placementId = child1PlacementId,
                startDate = fri,
                endDate = fri,
                optionId = snDaycareContractDays10.id,
                shiftCare = ShiftCareType.NONE,
                partWeek = false,
                confirmedBy = null,
                confirmedAt = null
            )
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = child1PlacementId,
                    daycareGroupId = testGroup1.id,
                    startDate = previousFriday,
                    endDate = fri
                )
            )

            val child2PlacementId =
                it.insert(
                    DevPlacement(
                        type = PlacementType.PRESCHOOL,
                        childId = testChild_2.id,
                        unitId = testDaycare.id,
                        startDate = previousFriday,
                        endDate = fri
                    )
                )
            it.insertServiceNeed(
                placementId = child2PlacementId,
                startDate = previousFriday,
                endDate = fri,
                optionId = snDefaultPreschool.id,
                shiftCare = ShiftCareType.NONE,
                partWeek = false,
                confirmedBy = null,
                confirmedAt = null
            )
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = child2PlacementId,
                    daycareGroupId = testGroup2.id,
                    startDate = previousFriday,
                    endDate = fri
                )
            )

            it.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = mon,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(12, 0),
                    createdBy = EvakaUserId(employeeId.raw)
                )
            )
            it.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = mon,
                    startTime = LocalTime.of(13, 0),
                    endTime = LocalTime.of(16, 0),
                    createdBy = EvakaUserId(employeeId.raw)
                )
            )
            it.insert(
                DevAbsence(
                    childId = testChild_1.id,
                    date = tue,
                    absenceType = AbsenceType.OTHER_ABSENCE,
                    modifiedBy = EvakaUserId(employeeId.raw),
                    absenceCategory = AbsenceCategory.BILLABLE
                )
            )
            it.insert(
                DevAbsence(
                    childId = testChild_1.id,
                    date = tue,
                    absenceType = AbsenceType.OTHER_ABSENCE,
                    modifiedBy = EvakaUserId(employeeId.raw),
                    absenceCategory = AbsenceCategory.NONBILLABLE
                )
            )
            it.insert(
                DevAbsence(
                    childId = testChild_1.id,
                    date = fri,
                    absenceType = AbsenceType.OTHER_ABSENCE,
                    modifiedBy = EvakaUserId(employeeId.raw),
                    absenceCategory = AbsenceCategory.NONBILLABLE
                )
            )
            it.insert(
                DevAbsence(
                    childId = testChild_2.id,
                    date = tue,
                    absenceType = AbsenceType.OTHER_ABSENCE,
                    modifiedBy = EvakaUserId(employeeId.raw),
                    absenceCategory = AbsenceCategory.BILLABLE
                )
            )
            it.insert(
                DevBackupCare(
                    childId = testChild_1.id,
                    unitId = testDaycare2.id,
                    groupId = null,
                    period = FiniteDateRange(wed, wed)
                )
            )
            it.insert(
                DevBackupCare(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    groupId = testGroup2.id,
                    period = FiniteDateRange(thu, thu)
                )
            )
            it.insert(
                DevDailyServiceTimes(
                    childId = testChild_1.id,
                    validityPeriod = DateRange(fri, fri),
                    regularTimes = TimeRange(LocalTime.of(8, 0), LocalTime.of(15, 0))
                )
            )
            it.insertAssistanceFactor(
                child = testChild_1.id,
                user = AuthenticatedUser.Employee(employeeId, setOf(UserRole.ADMIN)),
                update =
                    AssistanceFactorUpdate(
                        validDuring = FiniteDateRange(start = fri, end = fri),
                        capacityFactor = 2.5
                    ),
                now = HelsinkiDateTime.atStartOfDay(mon)
            )
        }
    }

    private fun getAttendanceReservations(
        clock: EvakaClock = this.clock,
        range: FiniteDateRange = monFri,
        daycareId: DaycareId = testDaycare.id,
        includeNonOperationalDays: Boolean = false
    ): UnitAttendanceReservations =
        attendanceReservationController.getAttendanceReservations(
            dbInstance(),
            AuthenticatedUser.Employee(employeeId, setOf(UserRole.STAFF)),
            clock,
            daycareId,
            from = range.start,
            to = range.end,
            includeNonOperationalDays = includeNonOperationalDays
        )

    private fun getConfirmedDailyReservationStats(
        daycareId: DaycareId,
        mobileDeviceId: MobileDeviceId,
        clock: EvakaClock = this.clock,
    ): List<AttendanceReservationController.DayReservationStatisticsResult> =
        attendanceReservationController.getReservationStatisticsForConfirmedDays(
            dbInstance(),
            AuthenticatedUser.MobileDevice(id = mobileDeviceId),
            clock,
            daycareId,
        )

    private fun getConfirmedChildReservationsForDay(
        date: LocalDate,
        daycareId: DaycareId,
        mobileDeviceId: MobileDeviceId,
        clock: EvakaClock = this.clock
    ): AttendanceReservationController.DailyChildReservationResult =
        attendanceReservationController.getChildReservationsForDay(
            dbInstance(),
            AuthenticatedUser.MobileDevice(id = mobileDeviceId),
            clock,
            daycareId,
            date,
        )

    private fun insertChildData(
        tx: Database.Transaction,
        range: FiniteDateRange,
        daycareId: DaycareId,
        groupId: GroupId,
        shiftCareType: ShiftCareType
    ): ChildId {
        val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
        tx.insert(
                DevPlacement(
                    childId = childId,
                    unitId = daycareId,
                    startDate = range.start,
                    endDate = range.end
                )
            )
            .also { placementId ->
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placementId,
                        daycareGroupId = groupId,
                        startDate = range.start,
                        endDate = range.end
                    )
                )
                tx.insert(
                    DevServiceNeed(
                        placementId = placementId,
                        startDate = range.start,
                        endDate = range.end,
                        optionId = snDaycareFullDay35.id,
                        shiftCare = shiftCareType,
                        confirmedBy = AuthenticatedUser.SystemInternalUser.evakaUserId
                    )
                )
            }
        return childId
    }
}
