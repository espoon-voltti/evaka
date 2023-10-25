// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.assistance.AssistanceFactorUpdate
import fi.espoo.evaka.assistance.insertAssistanceFactor
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesType
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesValue
import fi.espoo.evaka.daycare.insertPreschoolTerm
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.ChildServiceNeedInfo
import fi.espoo.evaka.holidayperiod.insertHolidayPeriod
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.ScheduleType
import fi.espoo.evaka.preschoolTerm2020
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.serviceneed.insertServiceNeed
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.dev.DevDailyServiceTimes
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevMobileDevice
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestBackUpCare
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestHoliday
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
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
import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class AttendanceReservationsControllerIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var attendanceReservationController: AttendanceReservationController

    private val employeeId = EmployeeId(UUID.randomUUID())

    private val mon = LocalDate.of(2021, 3, 1)
    private val tue = LocalDate.of(2021, 3, 2)
    private val wed = LocalDate.of(2021, 3, 3)
    private val thu = LocalDate.of(2021, 3, 4)
    private val fri = LocalDate.of(2021, 3, 5)
    private val monFri = FiniteDateRange(mon, fri)

    private val now = HelsinkiDateTime.of(mon, LocalTime.of(10, 0))
    private val clock = MockEvakaClock(now)

    private val unitOperationalDays =
        monFri
            .dates()
            .map {
                UnitAttendanceReservations.OperationalDay(
                    it,
                    time = TimeRange(LocalTime.of(0, 0), LocalTime.of(23, 59)),
                    isHoliday = false,
                    isInHolidayPeriod = false
                )
            }
            .toList()

    private val testGroup1 = DevDaycareGroup(daycareId = testDaycare.id, name = "Test group 1")
    private val testGroup2 = DevDaycareGroup(daycareId = testDaycare.id, name = "Test group 2")

    @BeforeEach
    fun beforeEach() {
        db.transaction {
            it.insertGeneralTestFixtures()
            it.insert(testGroup1)
            it.insert(testGroup2)

            it.insert(DevEmployee(employeeId))
            it.insertDaycareAclRow(testDaycare.id, employeeId, UserRole.STAFF)
        }
    }

    @Test
    fun `generates the correct result in all cases`() {
        db.transaction {
            val child1PlacementId =
                it.insertTestPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = mon,
                    endDate = fri
                )
            it.insertServiceNeed(
                placementId = child1PlacementId,
                startDate = mon,
                endDate = thu,
                optionId = snDaycareContractDays15.id,
                shiftCare = ShiftCareType.NONE,
                confirmedBy = null,
                confirmedAt = null
            )
            it.insertServiceNeed(
                placementId = child1PlacementId,
                startDate = fri,
                endDate = fri,
                optionId = snDaycareFullDay35.id,
                shiftCare = ShiftCareType.NONE,
                confirmedBy = null,
                confirmedAt = null
            )
            it.insertTestDaycareGroupPlacement(
                daycarePlacementId = child1PlacementId,
                groupId = testGroup1.id,
                startDate = mon,
                endDate = thu
            )
            it.insertTestDaycareGroupPlacement(
                daycarePlacementId = child1PlacementId,
                groupId = testGroup2.id,
                startDate = fri,
                endDate = fri
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
            it.insertTestAbsence(
                childId = testChild_1.id,
                date = tue,
                category = AbsenceCategory.BILLABLE,
                absenceType = AbsenceType.OTHER_ABSENCE,
                modifiedBy = EvakaUserId(employeeId.raw)
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
            it.insertTestPlacement(
                childId = testChild_4.id,
                unitId = testDaycare.id,
                startDate = wed,
                endDate = fri
            )

            // Placement in other unit, backup in this unit's group 2
            val testChild5PlacementId =
                it.insertTestPlacement(
                    childId = testChild_5.id,
                    unitId = testDaycare2.id,
                    type = PlacementType.CLUB, // <- reservations not needed
                    startDate = mon,
                    endDate = fri
                )
            it.insertTestBackUpCare(
                childId = testChild_5.id,
                unitId = testDaycare.id,
                groupId = testGroup2.id,
                startDate = fri,
                endDate = fri
            )
            it.insertServiceNeed(
                placementId = testChild5PlacementId,
                startDate = mon,
                endDate = fri,
                optionId = snDaycareContractDays15.id,
                shiftCare = ShiftCareType.NONE,
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
                it.insertTestPlacement(
                    childId = testChild_6.id,
                    unitId = testDaycare.id,
                    startDate = wed,
                    endDate = fri
                )
            it.insertTestDaycareGroupPlacement(
                daycarePlacementId = child6PlacementId,
                groupId = testGroup1.id,
                startDate = wed,
                endDate = fri
            )
            // ... and has a backup in another group in this unit
            it.insertTestBackUpCare(
                childId = testChild_6.id,
                unitId = testDaycare.id,
                groupId = testGroup2.id,
                startDate = thu,
                endDate = thu
            )
            // ... and has a backup in another unit
            it.insertTestBackUpCare(
                childId = testChild_6.id,
                unitId = testDaycare2.id,
                startDate = fri,
                endDate = fri
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

        val attendanceReservations = getAttendanceReservations()
        assertEquals(testDaycare.name, attendanceReservations.unit)
        assertEquals(unitOperationalDays, attendanceReservations.operationalDays)

        val group1SnInfos =
            attendanceReservations.unitServiceNeedInfo.groups.find { it.groupId == testGroup1.id }!!
        assertEquals(
            UnitAttendanceReservations.GroupServiceNeedInfo(
                groupId = testGroup1.id,
                childInfos =
                    listOf(
                        ChildServiceNeedInfo(
                            childId = testChild_1.id,
                            hasContractDays = true,
                            optionName = snDaycareContractDays15.nameFi,
                            validDuring = FiniteDateRange(mon, thu),
                            shiftCare = ShiftCareType.NONE,
                        )
                    )
            ),
            group1SnInfos
        )

        val group2SnInfos =
            attendanceReservations.unitServiceNeedInfo.groups.find { it.groupId == testGroup2.id }!!
        assertEquals(
            listOf(
                ChildServiceNeedInfo(
                    childId = testChild_5.id,
                    hasContractDays = true,
                    optionName = snDaycareContractDays15.nameFi,
                    validDuring = FiniteDateRange(mon, fri),
                    shiftCare = ShiftCareType.NONE,
                ),
                ChildServiceNeedInfo(
                    childId = testChild_1.id,
                    hasContractDays = false,
                    optionName = snDaycareFullDay35.nameFi,
                    validDuring = FiniteDateRange(fri, fri),
                    shiftCare = ShiftCareType.NONE,
                )
            ),
            group2SnInfos.childInfos.sortedBy { it.validDuring.start }
        )

        val group1 = attendanceReservations.groups.find { it.group.id == testGroup1.id }!!
        assertEquals(
            listOf(
                UnitAttendanceReservations.ChildDailyRecords(
                    UnitAttendanceReservations.Child(
                        testChild_1.id,
                        testChild_1.firstName,
                        testChild_1.lastName,
                        testChild_1.preferredName,
                        testChild_1.dateOfBirth
                    ),
                    listOf(
                        mapOf(
                            mon to
                                UnitAttendanceReservations.ChildRecordOfDay(
                                    reservations =
                                        Reservation.Times(
                                            LocalTime.of(8, 0),
                                            LocalTime.of(16, 0),
                                        ),
                                    attendances =
                                        UnitAttendanceReservations.AttendanceTimes(
                                            "08:15",
                                            "16:05"
                                        ),
                                    absence = null,
                                    dailyServiceTimes = null,
                                    inOtherUnit = false,
                                    isInBackupGroup = false,
                                    scheduleType = ScheduleType.RESERVATION_REQUIRED
                                ),
                            tue to
                                UnitAttendanceReservations.ChildRecordOfDay(
                                    reservations = null,
                                    attendances = null,
                                    absence =
                                        UnitAttendanceReservations.Absence(
                                            AbsenceType.OTHER_ABSENCE
                                        ),
                                    dailyServiceTimes = null,
                                    inOtherUnit = false,
                                    isInBackupGroup = false,
                                    scheduleType = ScheduleType.RESERVATION_REQUIRED
                                ),
                            wed to
                                UnitAttendanceReservations.ChildRecordOfDay(
                                    reservations = Reservation.NoTimes,
                                    attendances = null,
                                    absence = null,
                                    dailyServiceTimes = null,
                                    inOtherUnit = false,
                                    isInBackupGroup = false,
                                    scheduleType = ScheduleType.RESERVATION_REQUIRED
                                ),
                            thu to
                                UnitAttendanceReservations.ChildRecordOfDay(
                                    reservations = null,
                                    attendances = null,
                                    absence = null,
                                    dailyServiceTimes = null,
                                    inOtherUnit = false,
                                    isInBackupGroup = false,
                                    scheduleType = ScheduleType.RESERVATION_REQUIRED
                                )
                        )
                    )
                ),
                UnitAttendanceReservations.ChildDailyRecords(
                    UnitAttendanceReservations.Child(
                        testChild_6.id,
                        testChild_6.firstName,
                        testChild_6.lastName,
                        testChild_6.preferredName,
                        testChild_6.dateOfBirth
                    ),
                    listOf(
                        mapOf(
                            wed to
                                UnitAttendanceReservations.ChildRecordOfDay(
                                    reservations = null,
                                    attendances = null,
                                    absence = null,
                                    dailyServiceTimes = null,
                                    inOtherUnit = false,
                                    isInBackupGroup = false,
                                    scheduleType = ScheduleType.RESERVATION_REQUIRED
                                ),
                            // Backup in group 2
                            thu to
                                UnitAttendanceReservations.ChildRecordOfDay(
                                    reservations =
                                        Reservation.Times(
                                            LocalTime.of(9, 0),
                                            LocalTime.of(15, 0),
                                        ),
                                    attendances = null,
                                    absence = null,
                                    dailyServiceTimes = null,
                                    inOtherUnit = false,
                                    isInBackupGroup = true,
                                    scheduleType = ScheduleType.RESERVATION_REQUIRED,
                                ),
                            // Backup in another unit
                            fri to
                                UnitAttendanceReservations.ChildRecordOfDay(
                                    reservations = null,
                                    attendances = null,
                                    absence = null,
                                    dailyServiceTimes = null,
                                    inOtherUnit = true,
                                    isInBackupGroup = false,
                                    scheduleType = ScheduleType.RESERVATION_REQUIRED,
                                )
                        )
                    )
                )
            ),
            group1.children.sortedBy { it.child.lastName }
        )

        val group2 = attendanceReservations.groups.find { it.group.id == testGroup2.id }!!
        assertEquals(
            listOf(
                UnitAttendanceReservations.ChildDailyRecords(
                    UnitAttendanceReservations.Child(
                        testChild_1.id,
                        testChild_1.firstName,
                        testChild_1.lastName,
                        testChild_1.preferredName,
                        testChild_1.dateOfBirth
                    ),
                    listOf(emptyChildRecords(FiniteDateRange(fri, fri)))
                ),
                UnitAttendanceReservations.ChildDailyRecords(
                    UnitAttendanceReservations.Child(
                        testChild_6.id,
                        testChild_6.firstName,
                        testChild_6.lastName,
                        testChild_6.preferredName,
                        testChild_6.dateOfBirth
                    ),
                    listOf(
                        mapOf(
                            // Backup in group 2
                            thu to
                                UnitAttendanceReservations.ChildRecordOfDay(
                                    reservations =
                                        Reservation.Times(
                                            LocalTime.of(9, 0),
                                            LocalTime.of(15, 0),
                                        ),
                                    attendances = null,
                                    absence = null,
                                    dailyServiceTimes = null,
                                    inOtherUnit = false,
                                    isInBackupGroup = false,
                                    scheduleType = ScheduleType.RESERVATION_REQUIRED,
                                )
                        )
                    )
                ),
                UnitAttendanceReservations.ChildDailyRecords(
                    UnitAttendanceReservations.Child(
                        testChild_5.id,
                        testChild_5.firstName,
                        testChild_5.lastName,
                        testChild_5.preferredName,
                        testChild_5.dateOfBirth
                    ),
                    listOf(
                        mapOf(
                            // Normally in another unit, backup in group 2
                            fri to
                                UnitAttendanceReservations.ChildRecordOfDay(
                                    reservations = null,
                                    attendances = null,
                                    absence = null,
                                    dailyServiceTimes =
                                        DailyServiceTimesValue.RegularTimes(
                                            monFri.asDateRange(),
                                            TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0))
                                        ),
                                    inOtherUnit = false,
                                    isInBackupGroup = false,
                                    scheduleType = ScheduleType.FIXED_SCHEDULE,
                                )
                        )
                    )
                )
            ),
            group2.children.sortedBy { it.child.lastName }
        )

        // Ungrouped
        assertEquals(
            listOf(
                UnitAttendanceReservations.ChildDailyRecords(
                    UnitAttendanceReservations.Child(
                        testChild_4.id,
                        testChild_4.firstName,
                        testChild_4.lastName,
                        testChild_4.preferredName,
                        testChild_4.dateOfBirth
                    ),
                    listOf(emptyChildRecords(FiniteDateRange(wed, fri)))
                )
            ),
            attendanceReservations.ungrouped.sortedBy { it.child.lastName }
        )
    }

    @Test
    fun `two reservations or attendances in the same day`() {
        db.transaction { tx ->
            val child1PlacementId =
                tx.insertTestPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = mon,
                    endDate = fri
                )
            tx.insertTestDaycareGroupPlacement(
                daycarePlacementId = child1PlacementId,
                groupId = testGroup1.id,
                startDate = mon,
                endDate = fri
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

        val attendanceReservations = getAttendanceReservations()

        val group1 = attendanceReservations.groups.find { it.group.id == testGroup1.id }!!
        assertEquals(
            listOf(
                UnitAttendanceReservations.ChildDailyRecords(
                    UnitAttendanceReservations.Child(
                        testChild_1.id,
                        testChild_1.firstName,
                        testChild_1.lastName,
                        testChild_1.preferredName,
                        testChild_1.dateOfBirth
                    ),
                    listOf(
                        emptyChildRecords(monFri) +
                            mapOf(
                                mon to
                                    UnitAttendanceReservations.ChildRecordOfDay(
                                        reservations =
                                            Reservation.Times(
                                                LocalTime.of(19, 0),
                                                LocalTime.of(23, 59),
                                            ),
                                        attendances =
                                            UnitAttendanceReservations.AttendanceTimes(
                                                "19:10",
                                                "23:59"
                                            ),
                                        absence = null,
                                        dailyServiceTimes = null,
                                        inOtherUnit = false,
                                        isInBackupGroup = false,
                                        scheduleType = ScheduleType.RESERVATION_REQUIRED,
                                    ),
                                tue to
                                    UnitAttendanceReservations.ChildRecordOfDay(
                                        reservations =
                                            Reservation.Times(
                                                LocalTime.of(0, 0),
                                                LocalTime.of(8, 0),
                                            ),
                                        attendances =
                                            UnitAttendanceReservations.AttendanceTimes(
                                                "00:00",
                                                "10:30"
                                            ),
                                        absence = null,
                                        dailyServiceTimes = null,
                                        inOtherUnit = false,
                                        isInBackupGroup = false,
                                        scheduleType = ScheduleType.RESERVATION_REQUIRED,
                                    ),
                                wed to
                                    UnitAttendanceReservations.ChildRecordOfDay(
                                        reservations =
                                            Reservation.Times(
                                                LocalTime.of(0, 0),
                                                LocalTime.of(9, 30),
                                            ),
                                        attendances = null,
                                        absence = null,
                                        dailyServiceTimes = null,
                                        inOtherUnit = false,
                                        isInBackupGroup = false,
                                        scheduleType = ScheduleType.RESERVATION_REQUIRED,
                                    )
                            ),

                        // Second daily entries go to another map
                        emptyChildRecords(monFri) +
                            mapOf(
                                tue to
                                    UnitAttendanceReservations.ChildRecordOfDay(
                                        reservations =
                                            Reservation.Times(
                                                LocalTime.of(17, 30),
                                                LocalTime.of(23, 59),
                                            ),
                                        attendances =
                                            UnitAttendanceReservations.AttendanceTimes(
                                                "17:00",
                                                null
                                            ),
                                        absence = null,
                                        dailyServiceTimes = null,
                                        inOtherUnit = false,
                                        isInBackupGroup = false,
                                        scheduleType = ScheduleType.RESERVATION_REQUIRED,
                                    )
                            )
                    )
                )
            ),
            group1.children.sortedBy { it.child.lastName }
        )

        assertEquals(emptyList(), attendanceReservations.ungrouped)
    }

    @Test
    fun `operational days for holiday period with upcoming deadline`() {
        db.transaction { tx -> tx.insertHolidayPeriod(monFri, mon) }

        val result = getAttendanceReservations()
        assertEquals(
            monFri
                .dates()
                .map {
                    UnitAttendanceReservations.OperationalDay(
                        it,
                        time = TimeRange(LocalTime.of(0, 0), LocalTime.of(23, 59)),
                        isHoliday = false,
                        isInHolidayPeriod = true
                    )
                }
                .toList(),
            result.operationalDays,
        )
    }

    @Test
    fun `operational days for holiday period with past deadline`() {
        db.transaction { tx -> tx.insertHolidayPeriod(monFri, mon) }

        val result =
            getAttendanceReservations(
                clock = MockEvakaClock(HelsinkiDateTime.of(tue, LocalTime.of(10, 0)))
            )
        assertEquals(
            monFri
                .dates()
                .map {
                    UnitAttendanceReservations.OperationalDay(
                        it,
                        time = TimeRange(LocalTime.of(0, 0), LocalTime.of(23, 59)),
                        isHoliday = false,
                        isInHolidayPeriod = true
                    )
                }
                .toList(),
            result.operationalDays,
        )
    }

    @Test
    fun `get non-reservable reservations returns correct data`() {
        val mobileDeviceId =
            db.transaction { tx ->
                tx.insertTestPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = mon,
                    endDate = fri
                )
                tx.insert(DevMobileDevice(unitId = testDaycare.id))
            }
        val reservations =
            attendanceReservationController.getNonReservableReservations(
                dbInstance(),
                AuthenticatedUser.MobileDevice(id = mobileDeviceId),
                clock,
                testChild_1.id
            )
        assertEquals(
            listOf(
                NonReservableReservation(
                    date = tue,
                    reservations = emptyList(),
                    absenceType = null,
                    dailyServiceTimes = null
                ),
                NonReservableReservation(
                    date = wed,
                    reservations = emptyList(),
                    absenceType = null,
                    dailyServiceTimes = null
                ),
                NonReservableReservation(
                    date = thu,
                    reservations = emptyList(),
                    absenceType = null,
                    dailyServiceTimes = null
                ),
                NonReservableReservation(
                    date = fri,
                    reservations = emptyList(),
                    absenceType = null,
                    dailyServiceTimes = null
                ),
            ),
            reservations
        )
    }

    @Test
    fun `get non-reservable reservations throws forbidden when child doesn't have placement`() {
        val mobileDeviceId =
            db.transaction { tx -> tx.insert(DevMobileDevice(unitId = testDaycare.id)) }
        assertThrows<Forbidden> {
            attendanceReservationController.getNonReservableReservations(
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
                tx.insertTestPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare2.id,
                    startDate = mon,
                    endDate = fri
                )
                tx.insert(DevMobileDevice(unitId = testDaycare.id))
            }
        assertThrows<Forbidden> {
            attendanceReservationController.getNonReservableReservations(
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
                tx.insertTestPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = mon,
                    endDate = fri
                )
                tx.insert(DevMobileDevice(unitId = testDaycare.id))
            }
        attendanceReservationController.setNonReservableReservations(
            dbInstance(),
            AuthenticatedUser.MobileDevice(id = mobileDeviceId),
            clock,
            testChild_1.id,
            listOf(
                NonReservableReservation(
                    clock.today().plusDays(1),
                    reservations = emptyList(),
                    absenceType = null,
                    dailyServiceTimes = null
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
                        Reservation.Times(
                            startTime = LocalTime.of(8, 0),
                            endTime = LocalTime.of(12, 0)
                        ),
                        Reservation.Times(
                            startTime = LocalTime.of(13, 0),
                            endTime = LocalTime.of(16, 0)
                        )
                    ),
                groupId = testGroup1.id,
                absent = false,
                outOnBackupPlacement = false,
                dailyServiceTimes = null,
                onTermBreak = false,
                isInHolidayPeriod = false
            )

        val child2Expectation =
            child1Expectation.copy(
                childId = testChild_2.id,
                groupId = testGroup2.id,
                onTermBreak = true,
                reservations = listOf(Reservation.NoTimes)
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
                        child1Expectation.copy(
                            reservations = listOf(Reservation.NoTimes),
                            absent = true
                        ),
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
                            reservations = listOf(Reservation.NoTimes),
                            dailyServiceTimes =
                                DailyServiceTimesValue.RegularTimes(
                                    validityPeriod =
                                        DateRange(
                                            start = LocalDate.of(2021, 3, 5),
                                            end = LocalDate.of(2021, 3, 5)
                                        ),
                                    regularTimes =
                                        TimeRange(
                                            start = LocalTime.of(8, 0),
                                            end = LocalTime.of(15, 0)
                                        )
                                )
                        ),
                        child2Expectation.copy(onTermBreak = false)
                    )
            )

        assertConfirmedDayResult(mondayExpectation, mondayResult)
        assertConfirmedDayResult(tuesdayExpectation, tuesdayResult)
        assertConfirmedDayResult(fridayExpectation, fridayResult)
    }

    private fun assertConfirmedDayResult(
        expectation: AttendanceReservationController.DailyChildReservationResult,
        result: AttendanceReservationController.DailyChildReservationResult
    ) {
        assertEquals(expectation.children, result.children)
        assertEquals(expectation.childReservations.size, result.childReservations.size)
        result.childReservations.forEach { childReservationInfo ->
            assertContains(expectation.childReservations, childReservationInfo)
        }
    }

    private fun insertMobileDevice(unitId: DaycareId): MobileDeviceId {
        return db.transaction { tx -> tx.insert(DevMobileDevice(unitId = unitId)) }
    }

    private fun insertConfirmedReservationTestData() {
        db.transaction {
            // clear existing term that has no term breaks
            it.createUpdate("DELETE FROM preschool_term where extended_term @> :day")
                .bind("day", mon)
                .execute()

            val previousFriday = mon.minusDays(3)
            it.insertPreschoolTerm(
                preschoolTerm2020.copy(termBreaks = DateSet.of(FiniteDateRange(mon, tue)))
            )

            it.insertTestHoliday(previousFriday)

            val child1PlacementId =
                it.insertTestPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = previousFriday,
                    endDate = fri
                )

            it.insertServiceNeed(
                placementId = child1PlacementId,
                startDate = previousFriday,
                endDate = thu,
                optionId = snDaycareContractDays15.id,
                shiftCare = ShiftCareType.NONE,
                confirmedBy = null,
                confirmedAt = null
            )
            it.insertServiceNeed(
                placementId = child1PlacementId,
                startDate = fri,
                endDate = fri,
                optionId = snDaycareContractDays10.id,
                shiftCare = ShiftCareType.NONE,
                confirmedBy = null,
                confirmedAt = null
            )
            it.insertTestDaycareGroupPlacement(
                daycarePlacementId = child1PlacementId,
                groupId = testGroup1.id,
                startDate = previousFriday,
                endDate = fri
            )

            val child2PlacementId =
                it.insertTestPlacement(
                    childId = testChild_2.id,
                    unitId = testDaycare.id,
                    startDate = previousFriday,
                    endDate = fri,
                    type = PlacementType.PRESCHOOL
                )
            it.insertServiceNeed(
                placementId = child2PlacementId,
                startDate = previousFriday,
                endDate = fri,
                optionId = snDefaultPreschool.id,
                shiftCare = ShiftCareType.NONE,
                confirmedBy = null,
                confirmedAt = null
            )
            it.insertTestDaycareGroupPlacement(
                daycarePlacementId = child2PlacementId,
                groupId = testGroup2.id,
                startDate = previousFriday,
                endDate = fri
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
            it.insertTestAbsence(
                childId = testChild_1.id,
                date = tue,
                category = AbsenceCategory.BILLABLE,
                absenceType = AbsenceType.OTHER_ABSENCE,
                modifiedBy = EvakaUserId(employeeId.raw)
            )
            it.insertTestAbsence(
                childId = testChild_1.id,
                date = tue,
                category = AbsenceCategory.NONBILLABLE,
                absenceType = AbsenceType.OTHER_ABSENCE,
                modifiedBy = EvakaUserId(employeeId.raw)
            )
            it.insertTestAbsence(
                childId = testChild_1.id,
                date = fri,
                category = AbsenceCategory.NONBILLABLE,
                absenceType = AbsenceType.OTHER_ABSENCE,
                modifiedBy = EvakaUserId(employeeId.raw)
            )
            it.insertTestAbsence(
                childId = testChild_2.id,
                date = tue,
                category = AbsenceCategory.BILLABLE,
                absenceType = AbsenceType.OTHER_ABSENCE,
                modifiedBy = EvakaUserId(employeeId.raw)
            )
            it.insertTestBackUpCare(
                childId = testChild_1.id,
                unitId = testDaycare2.id,
                startDate = wed,
                endDate = wed
            )
            it.insertTestBackUpCare(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = thu,
                endDate = thu,
                groupId = testGroup2.id
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

    private fun emptyChildRecords(
        period: FiniteDateRange
    ): Map<LocalDate, UnitAttendanceReservations.ChildRecordOfDay> =
        period
            .dates()
            .map {
                it to
                    UnitAttendanceReservations.ChildRecordOfDay(
                        reservations = null,
                        attendances = null,
                        absence = null,
                        dailyServiceTimes = null,
                        inOtherUnit = false,
                        isInBackupGroup = false,
                        scheduleType = ScheduleType.RESERVATION_REQUIRED,
                    )
            }
            .toMap()

    private fun getAttendanceReservations(
        clock: EvakaClock = this.clock
    ): UnitAttendanceReservations =
        attendanceReservationController.getAttendanceReservations(
            dbInstance(),
            AuthenticatedUser.Employee(employeeId, setOf(UserRole.STAFF)),
            clock,
            testDaycare.id,
            from = mon,
            to = fri,
            includeNonOperationalDays = false
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
}
