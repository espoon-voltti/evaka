// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.AttendanceReservationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevAssistanceFactor
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareAssistance
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevHolidayPeriod
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevPreschoolAssistance
import fi.espoo.evaka.shared.dev.DevPreschoolTerm
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.snDefaultDaycare
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class HolidayPeriodAttendanceReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var holidayPeriodAttendanceReport: HolidayPeriodAttendanceReport

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val adminLoginUser = AuthenticatedUser.Employee(admin.id, admin.roles)

    private val unitSupervisorA = DevEmployee(roles = setOf())

    private final val mockClock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 9, 9), LocalTime.of(12, 15)))
    private final val mockToday = mockClock.today()

    private val holidayPeriod = // mon - sun: 2024-12-02 - 2024-12-08
        DevHolidayPeriod(
            period = FiniteDateRange(mockToday.plusWeeks(12), mockToday.plusWeeks(13).minusDays(1)),
            reservationDeadline = mockToday.plusWeeks(1),
            reservationsOpenOn = mockToday,
        )

    @Test
    fun `Unit supervisor can see own unit's report results`() {
        val testUnitData = initTestUnitData(holidayPeriod.period.start)
        initTestPlacementData(holidayPeriod.period.start, testUnitData[0])
        val results =
            holidayPeriodAttendanceReport.getHolidayPeriodAttendanceReport(
                dbInstance(),
                mockClock,
                unitSupervisorA.user,
                unitId = testUnitData[0].id,
                groupIds = emptySet(),
                periodId = holidayPeriod.id,
            )

        assertThat(results.isNotEmpty())
    }

    @Test
    fun `Report for unit without RESERVATIONS feature not available`() {
        val daycareId = DaycareId(UUID.randomUUID())
        val monday = holidayPeriod.period.start
        db.transaction { tx ->
            tx.insertServiceNeedOptions()
            tx.insert(holidayPeriod)
            tx.insert(admin)
            tx.insert(unitSupervisorA)

            val areaAId = tx.insert(DevCareArea(name = "Area A", shortName = "Area A"))
            tx.insert(
                DevDaycare(
                    id = daycareId,
                    name = "Daycare without RESERVATIONS",
                    areaId = areaAId,
                    openingDate = monday.minusDays(7),
                    type = setOf(CareType.CENTRE),
                    operationTimes =
                        List(5) { TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0)) } +
                            List(2) { null },
                    enabledPilotFeatures = emptySet(),
                )
            )
            tx.insertDaycareAclRow(daycareId, unitSupervisorA.id, UserRole.UNIT_SUPERVISOR)
        }

        assertThrows<Forbidden> {
            holidayPeriodAttendanceReport.getHolidayPeriodAttendanceReport(
                dbInstance(),
                mockClock,
                unitSupervisorA.user,
                groupIds = emptySet(),
                unitId = daycareId,
                periodId = holidayPeriod.id,
            )
        }
    }

    @Test
    fun `Admin can see report results`() {
        val testUnitData = initTestUnitData(holidayPeriod.period.start)
        initTestPlacementData(holidayPeriod.period.start, testUnitData[0])
        val results =
            holidayPeriodAttendanceReport.getHolidayPeriodAttendanceReport(
                dbInstance(),
                mockClock,
                adminLoginUser,
                unitId = testUnitData[0].id,
                groupIds = emptySet(),
                periodId = holidayPeriod.id,
            )
        assertThat(results.isNotEmpty())
    }

    @Test
    fun `Unit supervisor cannot see another unit's report results`() {
        val testUnitData = initTestUnitData(holidayPeriod.period.start)
        initTestPlacementData(holidayPeriod.period.start, testUnitData[0])
        assertThrows<Forbidden> {
            holidayPeriodAttendanceReport.getHolidayPeriodAttendanceReport(
                dbInstance(),
                mockClock,
                unitSupervisorA.user,
                unitId = testUnitData[1].id,
                groupIds = emptySet(),
                periodId = holidayPeriod.id,
            )
        }
    }

    @Test
    fun `Report returns all period days as per unit operation days`() {
        val testUnitData = initTestUnitData(holidayPeriod.period.start)
        initTestPlacementData(holidayPeriod.period.start, testUnitData[0])

        val reportResultsA =
            holidayPeriodAttendanceReport.getHolidayPeriodAttendanceReport(
                dbInstance(),
                mockClock,
                adminLoginUser,
                unitId = testUnitData[0].id,
                groupIds = emptySet(),
                periodId = holidayPeriod.id,
            )
        val periodDays = holidayPeriod.period.dates().toList()
        val periodDaysInA = periodDays.filter { it.dayOfWeek < DayOfWeek.FRIDAY }
        val resultDaysA = reportResultsA.map { it.date }

        assertThat(resultDaysA).containsAll(periodDaysInA)

        val reportResultsB =
            holidayPeriodAttendanceReport.getHolidayPeriodAttendanceReport(
                dbInstance(),
                mockClock,
                adminLoginUser,
                unitId = testUnitData[1].id,
                groupIds = emptySet(),
                periodId = holidayPeriod.id,
            )

        val resultDaysB = reportResultsB.map { it.date }

        assertThat(resultDaysB).containsAll(periodDays)
    }

    @Test
    fun `Report returns correct attendance data for day unit`() {
        val testUnitData = initTestUnitData(holidayPeriod.period.start)
        val testData = initTestPlacementData(holidayPeriod.period.start, testUnitData[0])

        val reportResultsByDate =
            holidayPeriodAttendanceReport
                .getHolidayPeriodAttendanceReport(
                    dbInstance(),
                    mockClock,
                    adminLoginUser,
                    unitId = testUnitData[0].id,
                    groupIds = emptySet(),
                    periodId = holidayPeriod.id,
                )
                .associateBy { it.date }

        val expectedMonday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 2,
                requiredStaff = 0,
                presentChildren = emptyList(),
                assistanceChildren = emptyList(),
                noResponseChildren =
                    listOf(testData[1].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                date = holidayPeriod.period.start,
                presentOccupancyCoefficient = 0.0,
            )

        val expectedTuesday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 0,
                requiredStaff = 2,
                presentChildren =
                    testData.slice(0..2).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren =
                    testData.slice(1..2).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                noResponseChildren = emptyList(),
                date = holidayPeriod.period.start.plusDays(1),
                presentOccupancyCoefficient = 8.25,
            )

        val expectedWednesday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 2,
                requiredStaff = 1,
                presentChildren =
                    listOf(testData[2].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                assistanceChildren =
                    listOf(testData[2].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                noResponseChildren = emptyList(),
                date = holidayPeriod.period.start.plusDays(2),
                presentOccupancyCoefficient = 5.5,
            )

        val expectedThursday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 0,
                requiredStaff = 1,
                presentChildren =
                    testData.slice(0..2).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
                date = holidayPeriod.period.start.plusDays(3),
                presentOccupancyCoefficient = 3.75,
            )

        listOf(expectedMonday, expectedTuesday, expectedWednesday, expectedThursday)
            .forEachIndexed { index, expected ->
                assertReportDay(
                    expected,
                    reportResultsByDate[holidayPeriod.period.start.plusDays(index.toLong())]
                        ?: fail("${index + 1}. date not found"),
                )
            }
    }

    @Test
    fun `Report returns correct attendance data for shift care unit`() {
        val monday = holidayPeriod.period.start
        val testUnitData = initTestUnitData(monday)
        val testData = initTestPlacementData(monday, testUnitData[1])
        addTestServiceNeed(testData[0].second[0], ShiftCareType.FULL)

        val reportResultsByDate =
            holidayPeriodAttendanceReport
                .getHolidayPeriodAttendanceReport(
                    dbInstance(),
                    mockClock,
                    adminLoginUser,
                    unitId = testUnitData[1].id,
                    groupIds = emptySet(),
                    periodId = holidayPeriod.id,
                )
                .associateBy { it.date }

        val expectedMonday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 2,
                requiredStaff = 0,
                presentChildren = emptyList(),
                assistanceChildren = emptyList(),
                noResponseChildren =
                    listOf(testData[1].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                date = monday,
                presentOccupancyCoefficient = 0.0,
            )

        val expectedTuesday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 0,
                requiredStaff = 2,
                presentChildren =
                    testData.slice(0..2).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren =
                    testData.slice(1..2).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                noResponseChildren = emptyList(),
                date = monday.plusDays(1),
                presentOccupancyCoefficient = 8.25,
            )

        val expectedWednesday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 2,
                requiredStaff = 1,
                presentChildren =
                    listOf(testData[2].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                assistanceChildren =
                    listOf(testData[2].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                noResponseChildren = emptyList(),
                date = monday.plusDays(2),
                presentOccupancyCoefficient = 5.5,
            )

        val expectedThursday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 0,
                requiredStaff = 1,
                presentChildren =
                    testData.slice(0..2).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
                date = monday.plusDays(3),
                presentOccupancyCoefficient = 3.75,
            )
        val expectedFriday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 0,
                requiredStaff = 1,
                presentChildren =
                    listOf(testData[0].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
                date = monday.plusDays(4),
                presentOccupancyCoefficient = 1.75,
            )

        val expectedSaturday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 0,
                requiredStaff = 1,
                presentChildren =
                    listOf(testData[0].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
                date = monday.plusDays(5),
                presentOccupancyCoefficient = 1.75,
            )

        val expectedSunday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 0,
                requiredStaff = 0,
                presentChildren = emptyList(),
                assistanceChildren = emptyList(),
                noResponseChildren =
                    listOf(testData[0].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                date = monday.plusDays(6),
                presentOccupancyCoefficient = 0.0,
            )
        listOf(
                expectedMonday,
                expectedTuesday,
                expectedWednesday,
                expectedThursday,
                expectedFriday,
                expectedSaturday,
                expectedSunday,
            )
            .forEachIndexed { index, expected ->
                assertReportDay(
                    expected,
                    reportResultsByDate[monday.plusDays(index.toLong())]
                        ?: fail("${index + 1}. date not found"),
                )
            }
    }

    @Test
    fun `Report returns correct attendance data for unit with backup care`() {
        val monday = holidayPeriod.period.start
        val testUnitData = initTestUnitData(monday)
        val testData = initTestPlacementData(monday, testUnitData[0])
        // incoming
        addTestBackupCare(
            testData[3].first.id,
            testUnitData[0].id,
            FiniteDateRange(monday, monday.plusDays(2)),
            testUnitData[0].groups.first,
            DevPlacement(
                unitId = testUnitData[2].id,
                childId = testData[3].first.id,
                startDate = monday,
                endDate = monday.plusDays(6),
            ),
        )
        // outgoing
        addTestBackupCare(
            testData[0].first.id,
            testUnitData[1].id,
            FiniteDateRange(monday.plusDays(2), monday.plusDays(2)),
            testUnitData[1].groups.first,
            null,
        )

        val reportResultsByDate =
            holidayPeriodAttendanceReport
                .getHolidayPeriodAttendanceReport(
                    dbInstance(),
                    mockClock,
                    adminLoginUser,
                    unitId = testUnitData[0].id,
                    groupIds = emptySet(),
                    periodId = holidayPeriod.id,
                )
                .associateBy { it.date }

        val expectedMonday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 3,
                requiredStaff = 0,
                presentChildren = emptyList(),
                assistanceChildren = emptyList(),
                noResponseChildren =
                    listOf(testData[1].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                date = monday,
                presentOccupancyCoefficient = 0.0,
            )

        val expectedTuesday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 0,
                requiredStaff = 2,
                presentChildren =
                    testData.slice(0..3).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren =
                    testData.slice(1..2).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                noResponseChildren = emptyList(),
                date = monday.plusDays(1),
                presentOccupancyCoefficient = 9.25,
            )

        val expectedWednesday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 2,
                requiredStaff = 1,
                presentChildren =
                    listOf(testData[2].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                assistanceChildren =
                    listOf(testData[2].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                noResponseChildren = emptyList(),
                date = monday.plusDays(2),
                presentOccupancyCoefficient = 5.5,
            )

        val expectedThursday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 0,
                requiredStaff = 1,
                presentChildren =
                    testData.slice(0..2).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
                date = monday.plusDays(3),
                presentOccupancyCoefficient = 3.75,
            )

        listOf(expectedMonday, expectedTuesday, expectedWednesday, expectedThursday)
            .forEachIndexed { index, expected ->
                assertReportDay(
                    expected,
                    reportResultsByDate[monday.plusDays(index.toLong())]
                        ?: fail("${index + 1}. date not found"),
                )
            }
    }

    @Test
    fun `Report counts preschoolers as absent for entire period in day unit`() {
        val monday = holidayPeriod.period.start // 2024-12-02
        val testUnitData = initTestUnitData(monday)
        val testData = initTestPlacementData(monday, testUnitData[0])
        // add preschooler
        val placementDuration = FiniteDateRange(monday.minusMonths(1), monday.plusMonths(5))
        val testChildEmil =
            DevPerson(
                ChildId(UUID.randomUUID()),
                dateOfBirth = monday.minusYears(6),
                firstName = "Emil",
                lastName = "Eskarilainen",
            )
        val testDataEmil =
            Pair(
                testChildEmil,
                listOf(
                    DevPlacement(
                        type = PlacementType.PRESCHOOL,
                        childId = testChildEmil.id,
                        unitId = testUnitData[0].id,
                        startDate = placementDuration.start,
                        endDate = monday.plusDays(1),
                    ),
                    DevPlacement(
                        type = PlacementType.PRESCHOOL,
                        childId = testChildEmil.id,
                        unitId = testUnitData[0].id,
                        startDate = monday.plusDays(2),
                        endDate = placementDuration.end,
                    ),
                ),
            )

        db.transaction { tx ->
            tx.insert(testChildEmil, DevPersonType.CHILD)
            testDataEmil.second.forEach { tx.insert(it) }

            val term =
                DevPreschoolTerm(
                    finnishPreschool = placementDuration,
                    swedishPreschool = placementDuration,
                    extendedTerm = placementDuration,
                    applicationPeriod = placementDuration,
                    termBreaks = DateSet.ofDates(monday, monday.plusDays(1), monday.plusDays(2)),
                )
            tx.insert(term)
        }

        val reportResultsByDate =
            holidayPeriodAttendanceReport
                .getHolidayPeriodAttendanceReport(
                    dbInstance(),
                    mockClock,
                    adminLoginUser,
                    unitId = testUnitData[0].id,
                    groupIds = emptySet(),
                    periodId = holidayPeriod.id,
                )
                .associateBy { it.date }

        val expectedMonday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 3,
                requiredStaff = 0,
                presentChildren = emptyList(),
                assistanceChildren = emptyList(),
                noResponseChildren =
                    listOf(testData[1].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                date = monday,
                presentOccupancyCoefficient = 0.0,
            )

        val expectedTuesday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 1,
                requiredStaff = 2,
                presentChildren =
                    testData.slice(0..2).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren =
                    testData.slice(1..2).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                noResponseChildren = emptyList(),
                date = monday.plusDays(1),
                presentOccupancyCoefficient = 8.25,
            )

        val expectedWednesday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 3,
                requiredStaff = 1,
                presentChildren =
                    listOf(testData[2].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                assistanceChildren =
                    listOf(testData[2].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                noResponseChildren = listOf(),
                date = monday.plusDays(2),
                presentOccupancyCoefficient = 5.5,
            )

        val expectedThursday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 1,
                requiredStaff = 1,
                presentChildren =
                    (testData.slice(0..2)).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
                date = monday.plusDays(3),
                presentOccupancyCoefficient = 3.75,
            )

        listOf(expectedMonday, expectedTuesday, expectedWednesday, expectedThursday)
            .forEachIndexed { index, expected ->
                assertReportDay(
                    expected,
                    reportResultsByDate[monday.plusDays(index.toLong())]
                        ?: fail("${index + 1}. date not found"),
                )
            }
    }

    @Test
    fun `Report counts preschoolers as absent for entire period in shift care unit`() {
        val monday = holidayPeriod.period.start // 2024-12-02
        val testUnitData = initTestUnitData(monday)
        val testData = initTestPlacementData(monday, testUnitData[1])
        addTestServiceNeed(testData[0].second[0], ShiftCareType.FULL)

        // add preschooler
        val placementDuration = FiniteDateRange(monday.minusMonths(1), monday.plusMonths(5))
        val testChildEmil =
            DevPerson(
                ChildId(UUID.randomUUID()),
                dateOfBirth = monday.minusYears(6),
                firstName = "Emil",
                lastName = "Eskarilainen",
            )
        val testDataEmil =
            Pair(
                testChildEmil,
                listOf(
                    DevPlacement(
                        type = PlacementType.PRESCHOOL,
                        childId = testChildEmil.id,
                        unitId = testUnitData[1].id,
                        startDate = placementDuration.start,
                        endDate = monday.plusDays(1),
                    ),
                    DevPlacement(
                        type = PlacementType.PRESCHOOL,
                        childId = testChildEmil.id,
                        unitId = testUnitData[1].id,
                        startDate = monday.plusDays(2),
                        endDate = placementDuration.end,
                    ),
                ),
            )

        db.transaction { tx ->
            tx.insert(testChildEmil, DevPersonType.CHILD)
            testDataEmil.second.forEach { tx.insert(it) }
            val term =
                DevPreschoolTerm(
                    finnishPreschool = placementDuration,
                    swedishPreschool = placementDuration,
                    extendedTerm = placementDuration,
                    applicationPeriod = placementDuration,
                    termBreaks = DateSet.ofDates(monday, monday.plusDays(1), monday.plusDays(2)),
                )
            tx.insert(term)
        }

        val reportResultsByDate =
            holidayPeriodAttendanceReport
                .getHolidayPeriodAttendanceReport(
                    dbInstance(),
                    mockClock,
                    adminLoginUser,
                    unitId = testUnitData[1].id,
                    groupIds = emptySet(),
                    periodId = holidayPeriod.id,
                )
                .associateBy { it.date }

        val expectedMonday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 3,
                requiredStaff = 0,
                presentChildren = emptyList(),
                assistanceChildren = emptyList(),
                noResponseChildren =
                    listOf(testData[1].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                date = monday,
                presentOccupancyCoefficient = 0.0,
            )

        val expectedTuesday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 1,
                requiredStaff = 2,
                presentChildren =
                    testData.slice(0..2).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren =
                    testData.slice(1..2).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                noResponseChildren = emptyList(),
                date = monday.plusDays(1),
                presentOccupancyCoefficient = 8.25,
            )

        val expectedWednesday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 3,
                requiredStaff = 1,
                presentChildren =
                    listOf(testData[2].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                assistanceChildren =
                    listOf(testData[2].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                noResponseChildren = listOf(),
                date = monday.plusDays(2),
                presentOccupancyCoefficient = 5.5,
            )

        val expectedThursday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 1,
                requiredStaff = 1,
                presentChildren =
                    (testData.slice(0..2)).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
                date = monday.plusDays(3),
                presentOccupancyCoefficient = 3.75,
            )

        val expectedFriday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 1,
                requiredStaff = 1,
                presentChildren =
                    listOf(testData[0].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
                date = monday.plusDays(4),
                presentOccupancyCoefficient = 1.75,
            )

        val expectedSaturday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 1,
                requiredStaff = 1,
                presentChildren =
                    listOf(testData[0].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
                date = monday.plusDays(5),
                presentOccupancyCoefficient = 1.75,
            )

        val expectedSunday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 1,
                requiredStaff = 0,
                presentChildren = emptyList(),
                assistanceChildren = emptyList(),
                noResponseChildren =
                    listOf(testData[0].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                date = monday.plusDays(6),
                presentOccupancyCoefficient = 0.0,
            )
        listOf(
                expectedMonday,
                expectedTuesday,
                expectedWednesday,
                expectedThursday,
                expectedFriday,
                expectedSaturday,
                expectedSunday,
            )
            .forEachIndexed { index, expected ->
                assertReportDay(
                    expected,
                    reportResultsByDate[monday.plusDays(index.toLong())]
                        ?: fail("${index + 1}. date not found"),
                )
            }
    }

    @Test
    fun `Report counts group results correctly`() {
        val monday = holidayPeriod.period.start // 2024-12-02
        val testUnitData = initTestUnitData(monday)
        val testData = initTestPlacementData(monday, testUnitData[0])

        val placementG =
            DevPlacement(
                childId = testData[4].first.id,
                unitId = testUnitData[0].id,
                startDate = monday.minusMonths(1),
                endDate = monday.plusMonths(1),
                type = PlacementType.DAYCARE,
            )

        db.transaction { tx -> tx.insert(placementG) }

        val expectedMonday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 3,
                requiredStaff = 0,
                presentChildren = emptyList(),
                assistanceChildren = emptyList(),
                noResponseChildren =
                    listOf(testData[1].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                date = monday,
                presentOccupancyCoefficient = 0.0,
            )

        val expectedTuesday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 0,
                requiredStaff = 2,
                presentChildren =
                    (testData.slice(listOf(0, 1, 2, 4))).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren =
                    testData.slice(1..2).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                noResponseChildren = emptyList(),
                date = monday.plusDays(1),
                presentOccupancyCoefficient = 9.25,
            )

        val expectedWednesday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 2,
                requiredStaff = 1,
                presentChildren =
                    listOf(testData[2].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                assistanceChildren =
                    listOf(testData[2].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                noResponseChildren =
                    listOf(testData[4].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                date = monday.plusDays(2),
                presentOccupancyCoefficient = 5.5,
            )

        val expectedThursday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 0,
                requiredStaff = 1,
                presentChildren =
                    (testData.slice(0..2)).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren = emptyList(),
                noResponseChildren =
                    listOf(testData[4].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                date = monday.plusDays(3),
                presentOccupancyCoefficient = 3.75,
            )

        // full unit test
        val expectedFullUnitResponse =
            listOf(expectedMonday, expectedTuesday, expectedWednesday, expectedThursday)

        val fullUnitReportResultsByDate =
            holidayPeriodAttendanceReport
                .getHolidayPeriodAttendanceReport(
                    dbInstance(),
                    mockClock,
                    adminLoginUser,
                    unitId = testUnitData[0].id,
                    groupIds = emptySet(),
                    periodId = holidayPeriod.id,
                )
                .associateBy { it.date }

        expectedFullUnitResponse.forEachIndexed { index, expected ->
            assertReportDay(
                expected,
                fullUnitReportResultsByDate[monday.plusDays(index.toLong())]
                    ?: fail("${index + 1}. date not found"),
            )
        }

        // individual group test

        val expectedGroupAResponse =
            listOf(
                expectedMonday.copy(absentCount = 2),
                expectedTuesday.copy(
                    presentChildren =
                        testData.slice(0..2).map {
                            ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                        },
                    presentOccupancyCoefficient = 8.25,
                ),
                expectedWednesday.copy(noResponseChildren = emptyList()),
                expectedThursday.copy(
                    presentChildren =
                        listOf(testData[0]).map {
                            ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                        },
                    presentOccupancyCoefficient = 1.75,
                    noResponseChildren = emptyList(),
                ),
            )

        val groupAReportResultsByDate =
            holidayPeriodAttendanceReport
                .getHolidayPeriodAttendanceReport(
                    dbInstance(),
                    mockClock,
                    adminLoginUser,
                    unitId = testUnitData[0].id,
                    groupIds = setOf(testUnitData[0].groups.first),
                    periodId = holidayPeriod.id,
                )
                .associateBy { it.date }

        expectedGroupAResponse.forEachIndexed { index, expected ->
            assertReportDay(
                expected,
                groupAReportResultsByDate[monday.plusDays(index.toLong())]
                    ?: fail("${index + 1}. date not found"),
            )
        }

        // combination group test
        val expectedCombinationResponse =
            listOf(
                expectedMonday.copy(absentCount = 2),
                expectedTuesday.copy(
                    presentChildren =
                        testData.slice(0..2).map {
                            ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                        },
                    presentOccupancyCoefficient = 8.25,
                ),
                expectedWednesday.copy(noResponseChildren = emptyList()),
                expectedThursday.copy(noResponseChildren = emptyList()),
            )

        val combinationReportResultsByDate =
            holidayPeriodAttendanceReport
                .getHolidayPeriodAttendanceReport(
                    dbInstance(),
                    mockClock,
                    adminLoginUser,
                    unitId = testUnitData[0].id,
                    groupIds = setOf(testUnitData[0].groups.first, testUnitData[0].groups.second),
                    periodId = holidayPeriod.id,
                )
                .associateBy { it.date }

        expectedCombinationResponse.forEachIndexed { index, expected ->
            assertReportDay(
                expected,
                combinationReportResultsByDate[monday.plusDays(index.toLong())]
                    ?: fail("${index + 1}. date not found"),
            )
        }
    }

    @Test
    fun `Report counts backup care group results correctly`() {
        val monday = holidayPeriod.period.start // 2024-12-02
        val testUnitData = initTestUnitData(monday)
        val testData = initTestPlacementData(monday, testUnitData[0])

        // incoming bc
        addTestBackupCare(
            testData[3].first.id,
            testUnitData[0].id,
            FiniteDateRange(monday, monday.plusDays(2)),
            testUnitData[0].groups.second,
            DevPlacement(
                unitId = testUnitData[2].id,
                childId = testData[3].first.id,
                startDate = monday,
                endDate = monday.plusDays(6),
            ),
        )
        // incoming intergroup bc
        addTestBackupCare(
            testData[0].first.id,
            testUnitData[0].id,
            FiniteDateRange(monday.plusDays(2), monday.plusDays(2)),
            testUnitData[0].groups.second,
            null,
        )

        val reportResultsByDate =
            holidayPeriodAttendanceReport
                .getHolidayPeriodAttendanceReport(
                    dbInstance(),
                    mockClock,
                    adminLoginUser,
                    unitId = testUnitData[0].id,
                    groupIds = setOf(testUnitData[0].groups.second),
                    periodId = holidayPeriod.id,
                )
                .associateBy { it.date }

        val expectedMonday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 1,
                requiredStaff = 0,
                presentChildren = emptyList(),
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
                date = monday,
                presentOccupancyCoefficient = 0.0,
            )

        val expectedTuesday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 0,
                requiredStaff = 1,
                presentChildren =
                    listOf(testData[3]).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
                date = monday.plusDays(1),
                presentOccupancyCoefficient = 1.0,
            )

        val expectedWednesday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 2,
                requiredStaff = 0,
                presentChildren = emptyList(),
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
                date = monday.plusDays(2),
                presentOccupancyCoefficient = 0.0,
            )

        val expectedThursday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 0,
                requiredStaff = 1,
                presentChildren =
                    testData.slice(1..2).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
                date = monday.plusDays(3),
                presentOccupancyCoefficient = 2.0,
            )

        listOf(expectedMonday, expectedTuesday, expectedWednesday, expectedThursday)
            .forEachIndexed { index, expected ->
                assertReportDay(
                    expected,
                    reportResultsByDate[monday.plusDays(index.toLong())]
                        ?: fail("${index + 1}. date not found"),
                )
            }
    }

    private data class TestDaycare(val id: DaycareId, val groups: Pair<GroupId, GroupId>)

    private fun initTestUnitData(monday: LocalDate): List<TestDaycare> {
        return db.transaction { tx ->
            val areaAId = tx.insert(DevCareArea(name = "Area A", shortName = "Area A"))
            val areaBId = tx.insert(DevCareArea(name = "Area B", shortName = "Area B"))

            val daycareAId =
                tx.insert(
                    DevDaycare(
                        name = "Daycare A",
                        areaId = areaAId,
                        openingDate = monday.minusDays(7),
                        type = setOf(CareType.CENTRE, CareType.PRESCHOOL),
                        operationTimes =
                            List(5) { TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0)) } +
                                List(2) { null },
                        enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
                    )
                )

            val groupAAId =
                tx.insert(
                    DevDaycareGroup(
                        name = "Group AA",
                        startDate = monday.minusDays(7),
                        daycareId = daycareAId,
                    )
                )

            val groupABId =
                tx.insert(
                    DevDaycareGroup(
                        name = "Group AB",
                        startDate = monday.minusDays(7),
                        daycareId = daycareAId,
                    )
                )

            val daycareBId =
                tx.insert(
                    DevDaycare(
                        name = "Daycare B",
                        areaId = areaBId,
                        openingDate = monday.minusDays(7),
                        type = setOf(CareType.CENTRE),
                        operationTimes =
                            List(5) { TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0)) } +
                                List(2) { null },
                        shiftCareOperationTimes =
                            List(7) { TimeRange(LocalTime.of(0, 0), LocalTime.of(23, 59)) },
                        shiftCareOpenOnHolidays = true,
                        enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
                    )
                )

            val groupBAId =
                tx.insert(
                    DevDaycareGroup(
                        name = "Group BA",
                        startDate = monday.minusDays(7),
                        daycareId = daycareBId,
                    )
                )

            val groupBBId =
                tx.insert(
                    DevDaycareGroup(
                        name = "Group BB",
                        startDate = monday.minusDays(7),
                        daycareId = daycareBId,
                    )
                )

            val daycareCId =
                tx.insert(
                    DevDaycare(
                        name = "Daycare C",
                        areaId = areaAId,
                        openingDate = monday.minusMonths(1),
                        type = setOf(CareType.CENTRE),
                        operationTimes =
                            List(5) { TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0)) } +
                                List(2) { null },
                        enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
                    )
                )

            val groupCAId =
                tx.insert(
                    DevDaycareGroup(
                        name = "Group CA",
                        startDate = monday.minusMonths(1),
                        daycareId = daycareCId,
                    )
                )

            val groupCBId =
                tx.insert(
                    DevDaycareGroup(
                        name = "Group CB",
                        startDate = monday.minusMonths(1),
                        daycareId = daycareCId,
                    )
                )

            listOf(
                TestDaycare(daycareAId, Pair(groupAAId, groupABId)),
                TestDaycare(daycareBId, Pair(groupBAId, groupBBId)),
                TestDaycare(daycareCId, Pair(groupCAId, groupCBId)),
            )
        }
    }

    private fun initTestPlacementData(
        monday: LocalDate,
        daycare: TestDaycare,
    ): List<Pair<DevPerson, List<DevPlacement>>> {
        val tuesday = monday.plusDays(1)
        val wednesday = monday.plusDays(2)
        val thursday = monday.plusDays(3)
        val friday = monday.plusDays(4)
        val saturday = monday.plusDays(5)

        return db.transaction { tx ->
            tx.insertServiceNeedOptions()
            tx.insert(admin)

            tx.insert(unitSupervisorA)

            tx.insertDaycareAclRow(daycare.id, unitSupervisorA.id, UserRole.UNIT_SUPERVISOR)

            val testChildAapo =
                DevPerson(
                    dateOfBirth = monday.minusYears(2),
                    firstName = "Aapo",
                    lastName = "Aarnio",
                )
            tx.insert(testChildAapo, DevPersonType.CHILD)

            val testChildBertil =
                DevPerson(
                    dateOfBirth = monday.minusYears(4),
                    firstName = "Bertil",
                    lastName = "Becker",
                )
            tx.insert(testChildBertil, DevPersonType.CHILD)

            val testChildCecil =
                DevPerson(
                    dateOfBirth = monday.minusYears(4),
                    firstName = "Cecil",
                    lastName = "Cilliacus",
                )
            tx.insert(testChildCecil, DevPersonType.CHILD)

            val testChildVille =
                DevPerson(
                    dateOfBirth = monday.minusYears(4),
                    firstName = "Ville",
                    lastName = "Varahoidettava",
                )

            tx.insert(testChildVille, DevPersonType.CHILD)

            val testChildGunnar =
                DevPerson(
                    dateOfBirth = monday.minusYears(4),
                    firstName = "Gunnar",
                    lastName = "Groupless",
                )

            tx.insert(testChildGunnar, DevPersonType.CHILD)

            val defaultPlacementDuration =
                FiniteDateRange(monday.minusMonths(1), monday.plusMonths(5))

            // 4 children
            // A: < 3, no assistance                -> 1.75 coefficient
            // B, V: > 3, assistance without factor -> 1.00 coefficient
            // C: > 3, assistance factor of 5.50    -> 5.50 coefficient

            val placementA =
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChildAapo.id,
                    unitId = daycare.id,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )

            tx.insert(placementA)

            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementA.id,
                    daycareGroupId = daycare.groups.first,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            )

            val placementB =
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChildBertil.id,
                    unitId = daycare.id,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            tx.insert(placementB)

            tx.insert(
                DevServiceNeed(
                    placementId = placementB.id,
                    startDate = defaultPlacementDuration.start,
                    endDate = wednesday,
                    confirmedBy = admin.evakaUserId,
                    optionId = snDefaultDaycare.id,
                )
            )
            tx.insert(
                DevServiceNeed(
                    placementId = placementB.id,
                    startDate = thursday,
                    endDate = defaultPlacementDuration.end,
                    confirmedBy = admin.evakaUserId,
                    optionId = snDaycareFullDay35.id,
                )
            )

            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementB.id,
                    daycareGroupId = daycare.groups.first,
                    startDate = defaultPlacementDuration.start,
                    endDate = wednesday,
                )
            )

            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementB.id,
                    daycareGroupId = daycare.groups.second,
                    startDate = thursday,
                    endDate = defaultPlacementDuration.end,
                )
            )

            tx.insert(
                DevPreschoolAssistance(
                    childId = testChildBertil.id,
                    validDuring = FiniteDateRange(monday.minusMonths(2), tuesday),
                )
            )

            val placementC1 =
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChildCecil.id,
                    unitId = daycare.id,
                    startDate = monday.minusMonths(1),
                    endDate = wednesday,
                )
            tx.insert(placementC1)

            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementC1.id,
                    daycareGroupId = daycare.groups.first,
                    startDate = placementC1.startDate,
                    endDate = placementC1.endDate,
                )
            )

            val placementC2 =
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChildCecil.id,
                    unitId = daycare.id,
                    startDate = thursday,
                    endDate = thursday.plusMonths(1),
                )

            tx.insert(placementC2)

            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementC2.id,
                    daycareGroupId = daycare.groups.second,
                    startDate = placementC2.startDate,
                    endDate = placementC2.endDate,
                )
            )

            tx.insert(
                DevAssistanceFactor(
                    childId = testChildCecil.id,
                    validDuring = FiniteDateRange(monday, wednesday),
                    capacityFactor = 5.50,
                )
            )

            tx.insert(
                DevDaycareAssistance(
                    childId = testChildCecil.id,
                    validDuring = FiniteDateRange(monday.minusMonths(3), wednesday),
                )
            )

            // Monday - everyone is absent, but Bertil has a missing absence category -> no response

            listOf(testChildAapo, testChildBertil, testChildCecil, testChildVille, testChildGunnar)
                .forEach { createOtherAbsence(monday, it.id, AbsenceCategory.BILLABLE, tx) }

            // Tuesday - everyone is present

            listOf(testChildAapo, testChildBertil, testChildCecil, testChildVille, testChildGunnar)
                .forEach { createNullReservation(tuesday, it.id, tx) }

            // Wednesday - Cecil present, 2 (+ bc Ville) absent

            listOf(testChildAapo, testChildBertil, testChildVille).forEach {
                createOtherAbsence(wednesday, it.id, AbsenceCategory.BILLABLE, tx)
            }
            createOtherAbsence(wednesday, testChildBertil.id, AbsenceCategory.NONBILLABLE, tx)

            createNullReservation(wednesday, testChildCecil.id, tx)

            // Thursday - 3 present, Cecil no longer assisted (Ville no longer incoming bc)

            listOf(testChildAapo, testChildBertil, testChildCecil).forEach {
                createNullReservation(thursday, it.id, tx)
            }
            listOf(testChildVille).forEach {
                createOtherAbsence(thursday, it.id, AbsenceCategory.BILLABLE, tx)
            }

            // Friday -  holiday, only Aapo present
            createNullReservation(friday, testChildAapo.id, tx)

            // Saturday - 1 present
            createNullReservation(saturday, testChildAapo.id, tx)

            // Sunday - no markings

            tx.insert(holidayPeriod)

            listOf(
                Pair(testChildAapo, listOf(placementA)),
                Pair(testChildBertil, listOf(placementB)),
                Pair(testChildCecil, listOf(placementC1, placementC2)),
                Pair(testChildVille, emptyList()),
                Pair(testChildGunnar, emptyList()),
            )
        }
    }

    private fun addTestServiceNeed(
        placement: DevPlacement,
        shiftCareType: ShiftCareType,
        optionId: ServiceNeedOptionId = snDaycareFullDay35.id,
    ) {
        return db.transaction { tx ->
            tx.insert(
                DevServiceNeed(
                    placementId = placement.id,
                    startDate = placement.startDate,
                    endDate = placement.endDate,
                    shiftCare = shiftCareType,
                    optionId = optionId,
                    confirmedBy = adminLoginUser.evakaUserId,
                )
            )
        }
    }

    private fun addTestBackupCare(
        childId: PersonId,
        targetDaycareId: DaycareId,
        duration: FiniteDateRange,
        targetGroupId: GroupId?,
        placementEquivalent: DevPlacement?,
    ) {
        return db.transaction { tx ->
            if (placementEquivalent != null) {
                tx.insert(placementEquivalent)
            }
            tx.insert(
                DevBackupCare(
                    period = duration,
                    childId = childId,
                    unitId = targetDaycareId,
                    groupId = targetGroupId,
                )
            )
        }
    }

    private fun createNullReservation(
        date: LocalDate,
        childId: PersonId,
        tx: Database.Transaction,
    ) =
        tx.insert(
            DevReservation(
                id = AttendanceReservationId(UUID.randomUUID()),
                childId,
                date,
                null,
                null,
                HelsinkiDateTime.atStartOfDay(date),
                EvakaUserId(admin.id.raw),
            )
        )

    private fun createOtherAbsence(
        date: LocalDate,
        childId: PersonId,
        category: AbsenceCategory,
        tx: Database.Transaction,
    ) =
        tx.insert(
            DevAbsence(
                id = AbsenceId(UUID.randomUUID()),
                childId,
                date,
                AbsenceType.OTHER_ABSENCE,
                HelsinkiDateTime.atStartOfDay(date),
                EvakaUserId(admin.id.raw),
                category,
            )
        )

    private fun assertReportDay(
        expected: HolidayPeriodAttendanceReportRow,
        actual: HolidayPeriodAttendanceReportRow,
    ) {
        assertEquals(expected.date, actual.date)
        assertEquals(expected.absentCount, actual.absentCount, "${actual.date}: absentCount")
        assertThat(actual.presentChildren)
            .describedAs("${actual.date}: presentChildren")
            .containsExactlyInAnyOrderElementsOf(expected.presentChildren)

        assertThat(actual.assistanceChildren)
            .describedAs("${actual.date}: assistanceChildren")
            .containsExactlyInAnyOrderElementsOf(expected.assistanceChildren)

        assertEquals(
            expected.presentOccupancyCoefficient,
            actual.presentOccupancyCoefficient,
            "${actual.date}: coefficientSum",
        )
        assertEquals(
            expected.requiredStaff,
            actual.requiredStaff,
            "${actual.date}: staffRequirement",
        )
        assertThat(actual.noResponseChildren)
            .describedAs("${actual.date}: noResponseChildren")
            .containsExactlyInAnyOrderElementsOf(expected.noResponseChildren)
    }
}
