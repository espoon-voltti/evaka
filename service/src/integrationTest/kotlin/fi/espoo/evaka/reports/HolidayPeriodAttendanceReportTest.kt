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
import fi.espoo.evaka.shared.AssistanceFactorId
import fi.espoo.evaka.shared.AttendanceReservationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.HolidayPeriodId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevAssistanceFactor
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevHoliday
import fi.espoo.evaka.shared.dev.DevHolidayPeriod
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
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

    private val unitSupervisorA = DevEmployee(id = EmployeeId(UUID.randomUUID()), roles = setOf())

    private final val mockClock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 9, 9), LocalTime.of(12, 15)))
    private final val mockToday = mockClock.today()

    private val holidayPeriod =
        DevHolidayPeriod(
            id = HolidayPeriodId(UUID.randomUUID()),
            period = FiniteDateRange(mockToday.plusWeeks(15), mockToday.plusWeeks(16).minusDays(1)),
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
                testUnitData[0],
                holidayPeriod.id,
            )

        assertThat(results.isNotEmpty())
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
                testUnitData[0],
                holidayPeriod.id,
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
                testUnitData[1],
                holidayPeriod.id,
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
                testUnitData[0],
                holidayPeriod.id,
            )
        val periodDays = holidayPeriod.period.dates().toList()
        val periodDaysInA = periodDays.filter { it.dayOfWeek < DayOfWeek.SATURDAY }
        val resultDaysA = reportResultsA.map { it.date }

        assertThat(resultDaysA).containsAll(periodDaysInA)

        val reportResultsB =
            holidayPeriodAttendanceReport.getHolidayPeriodAttendanceReport(
                dbInstance(),
                mockClock,
                adminLoginUser,
                testUnitData[1],
                holidayPeriod.id,
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
                    testUnitData[0],
                    holidayPeriod.id,
                )
                .associateBy { it.date }

        val expectedMonday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 3,
                requiredStaff = 0,
                presentChildren = emptyList(),
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
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
                    listOf(testData[2].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                noResponseChildren = emptyList(),
                date = holidayPeriod.period.start.plusDays(1),
                presentOccupancyCoefficient = 8.25,
            )

        val expectedWednesday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 0,
                requiredStaff = 1,
                presentChildren =
                    listOf(testData[0].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                assistanceChildren = emptyList(),
                noResponseChildren =
                    testData.slice(1..2).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                date = holidayPeriod.period.start.plusDays(2),
                presentOccupancyCoefficient = 1.75,
            )

        val expectedThursday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 1,
                requiredStaff = 1,
                presentChildren =
                    testData.slice(0..1).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
                date = holidayPeriod.period.start.plusDays(3),
                presentOccupancyCoefficient = 2.75,
            )
        val expectedFriday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 2,
                requiredStaff = 1,
                presentChildren =
                    listOf(testData[2].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
                date = holidayPeriod.period.start.plusDays(4),
                presentOccupancyCoefficient = 5.5,
            )

        listOf(expectedMonday, expectedTuesday, expectedWednesday, expectedThursday, expectedFriday)
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
        val testData = initTestPlacementData(monday, testUnitData[1], listOf(monday.plusDays(2)))
        addTestServiceNeed(testData[0].second[0], ShiftCareType.FULL)

        val reportResultsByDate =
            holidayPeriodAttendanceReport
                .getHolidayPeriodAttendanceReport(
                    dbInstance(),
                    mockClock,
                    adminLoginUser,
                    testUnitData[1],
                    holidayPeriod.id,
                )
                .associateBy { it.date }

        val expectedMonday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 3,
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
                requiredStaff = 2,
                presentChildren =
                    testData.slice(0..2).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren =
                    listOf(testData[2].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                noResponseChildren = emptyList(),
                date = monday.plusDays(1),
                presentOccupancyCoefficient = 8.25,
            )

        val expectedWednesday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 0,
                requiredStaff = 1,
                presentChildren =
                    listOf(testData[0].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
                date = monday.plusDays(2),
                presentOccupancyCoefficient = 1.75,
            )

        val expectedThursday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 1,
                requiredStaff = 1,
                presentChildren =
                    testData.slice(0..1).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
                date = monday.plusDays(3),
                presentOccupancyCoefficient = 2.75,
            )
        val expectedFriday =
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
                date = monday.plusDays(4),
                presentOccupancyCoefficient = 5.5,
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
            testUnitData[0],
            FiniteDateRange(monday, monday.plusDays(2)),
            DevPlacement(
                unitId = testUnitData[2],
                childId = testData[3].first.id,
                startDate = monday,
                endDate = monday.plusDays(6),
            ),
        )
        // outgoing
        addTestBackupCare(
            testData[0].first.id,
            testUnitData[1],
            FiniteDateRange(monday.plusDays(2), monday.plusDays(2)),
            null,
        )

        val reportResultsByDate =
            holidayPeriodAttendanceReport
                .getHolidayPeriodAttendanceReport(
                    dbInstance(),
                    mockClock,
                    adminLoginUser,
                    testUnitData[0],
                    holidayPeriod.id,
                )
                .associateBy { it.date }

        val expectedMonday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 4,
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
                requiredStaff = 2,
                presentChildren =
                    testData.map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren =
                    listOf(testData[2].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                noResponseChildren = emptyList(),
                date = monday.plusDays(1),
                presentOccupancyCoefficient = 9.25,
            )

        val expectedWednesday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 0,
                requiredStaff = 0,
                presentChildren = emptyList(),
                assistanceChildren = emptyList(),
                noResponseChildren =
                    testData.slice(1..3).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                date = monday.plusDays(2),
                presentOccupancyCoefficient = 0.0,
            )

        val expectedThursday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 1,
                requiredStaff = 1,
                presentChildren =
                    testData.slice(0..1).map {
                        ChildWithName(it.first.id, it.first.firstName, it.first.lastName)
                    },
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
                date = monday.plusDays(3),
                presentOccupancyCoefficient = 2.75,
            )
        val expectedFriday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 2,
                requiredStaff = 1,
                presentChildren =
                    listOf(testData[2].first).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
                date = monday.plusDays(4),
                presentOccupancyCoefficient = 5.5,
            )

        listOf(expectedMonday, expectedTuesday, expectedWednesday, expectedThursday, expectedFriday)
            .forEachIndexed { index, expected ->
                assertReportDay(
                    expected,
                    reportResultsByDate[monday.plusDays(index.toLong())]
                        ?: fail("${index + 1}. date not found"),
                )
            }
    }

    private fun initTestUnitData(monday: LocalDate): List<DaycareId> {
        return db.transaction { tx ->
            val areaAId = tx.insert(DevCareArea(name = "Area A", shortName = "Area A"))
            val areaBId = tx.insert(DevCareArea(name = "Area B", shortName = "Area B"))

            val daycareAId =
                tx.insert(
                    DevDaycare(
                        name = "Daycare A",
                        areaId = areaAId,
                        openingDate = monday.minusDays(7),
                        type = setOf(CareType.CENTRE),
                        operationTimes =
                            List(5) { TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0)) } +
                                List(2) { null },
                        enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
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
            val daycareCId =
                tx.insert(
                    DevDaycare(
                        name = "Daycare C",
                        areaId = areaAId,
                        openingDate = monday.minusDays(7),
                        type = setOf(CareType.CENTRE),
                        operationTimes =
                            List(5) { TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0)) } +
                                List(2) { null },
                        enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
                    )
                )

            listOf(daycareAId, daycareBId, daycareCId)
        }
    }

    private fun initTestPlacementData(
        monday: LocalDate,
        daycareId: DaycareId,
        holidays: List<LocalDate> = emptyList(),
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

            tx.insertDaycareAclRow(daycareId, unitSupervisorA.id, UserRole.UNIT_SUPERVISOR)

            holidays.forEachIndexed { index, date ->
                tx.insert(DevHoliday(date, "Test holiday ${index + 1}"))
            }

            val testChildAapo =
                DevPerson(
                    id = ChildId(UUID.randomUUID()),
                    dateOfBirth = monday.minusYears(2),
                    firstName = "Aapo",
                    lastName = "Aarnio",
                )
            tx.insert(testChildAapo, DevPersonType.CHILD)

            val testChildBertil =
                DevPerson(
                    ChildId(UUID.randomUUID()),
                    dateOfBirth = monday.minusYears(4),
                    firstName = "Bertil",
                    lastName = "Becker",
                )
            tx.insert(testChildBertil, DevPersonType.CHILD)

            val testChildCecil =
                DevPerson(
                    ChildId(UUID.randomUUID()),
                    dateOfBirth = monday.minusYears(4),
                    firstName = "Cecil",
                    lastName = "Cilliacus",
                )
            tx.insert(testChildCecil, DevPersonType.CHILD)

            val testChildVille =
                DevPerson(
                    ChildId(UUID.randomUUID()),
                    dateOfBirth = monday.minusYears(4),
                    firstName = "Ville",
                    lastName = "Varahoidettava",
                )

            tx.insert(testChildVille, DevPersonType.CHILD)

            val defaultPlacementDuration =
                FiniteDateRange(monday.minusMonths(1), monday.plusMonths(5))

            // 3 children
            // A: < 3, no assistance             -> 1.75 coefficient
            // B: > 3, no assistance             -> 1.00 coefficient
            // C: > 3, assistance factor of 5.50 -> 5.50 coefficient

            val placementA =
                DevPlacement(
                    id = PlacementId(UUID.randomUUID()),
                    type = PlacementType.DAYCARE,
                    childId = testChildAapo.id,
                    unitId = daycareId,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )

            tx.insert(placementA)

            val placementB =
                DevPlacement(
                    id = PlacementId(UUID.randomUUID()),
                    type = PlacementType.DAYCARE,
                    childId = testChildBertil.id,
                    unitId = daycareId,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            tx.insert(placementB)

            val placementC1 =
                DevPlacement(
                    id = PlacementId(UUID.randomUUID()),
                    type = PlacementType.DAYCARE,
                    childId = testChildCecil.id,
                    unitId = daycareId,
                    startDate = monday.minusMonths(1),
                    endDate = wednesday,
                )
            tx.insert(placementC1)

            val placementC2 =
                DevPlacement(
                    id = PlacementId(UUID.randomUUID()),
                    type = PlacementType.DAYCARE,
                    childId = testChildCecil.id,
                    unitId = daycareId,
                    startDate = thursday,
                    endDate = thursday.plusMonths(1),
                )

            tx.insert(placementC2)

            tx.insert(
                DevAssistanceFactor(
                    id = AssistanceFactorId(UUID.randomUUID()),
                    testChildCecil.id,
                    holidayPeriod.period,
                    capacityFactor = 5.50,
                )
            )

            // Monday - everyone is absent

            listOf(testChildAapo, testChildBertil, testChildCecil, testChildVille).forEach {
                createOtherAbsence(monday, it.id, tx)
            }

            // Tuesday - everyone is present

            listOf(testChildAapo, testChildBertil, testChildCecil, testChildVille).forEach {
                createNullReservation(tuesday, it.id, tx)
            }

            // Wednesday - only Aapo has answered

            createNullReservation(wednesday, testChildAapo.id, tx)

            // Thursday - 2 present, 1 absent

            listOf(testChildAapo, testChildBertil).forEach {
                createNullReservation(thursday, it.id, tx)
            }
            createOtherAbsence(thursday, testChildCecil.id, tx)

            // Friday - 1 present, 2 (+ bc Ville) absent
            listOf(testChildAapo, testChildBertil, testChildVille).forEach {
                createOtherAbsence(friday, it.id, tx)
            }
            createNullReservation(friday, testChildCecil.id, tx)

            // Saturday - 1 present

            createNullReservation(saturday, testChildAapo.id, tx)

            tx.insert(holidayPeriod)

            listOf(
                Pair(testChildAapo, listOf(placementA)),
                Pair(testChildBertil, listOf(placementB)),
                Pair(testChildCecil, listOf(placementC1, placementC2)),
                Pair(testChildVille, emptyList()),
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
        placementEquivalent: DevPlacement?,
    ) {
        return db.transaction { tx ->
            if (placementEquivalent != null) {
                tx.insert(placementEquivalent)
            }
            tx.insert(DevBackupCare(period = duration, childId = childId, unitId = targetDaycareId))
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

    private fun createOtherAbsence(date: LocalDate, childId: PersonId, tx: Database.Transaction) =
        tx.insert(
            DevAbsence(
                id = AbsenceId(UUID.randomUUID()),
                childId,
                date,
                AbsenceType.OTHER_ABSENCE,
                HelsinkiDateTime.atStartOfDay(date),
                EvakaUserId(admin.id.raw),
                AbsenceCategory.BILLABLE,
            )
        )

    private fun assertReportDay(
        expected: HolidayPeriodAttendanceReportRow,
        actual: HolidayPeriodAttendanceReportRow,
    ) {
        assertEquals(expected.date, actual.date)
        assertEquals(expected.absentCount, actual.absentCount, "${actual.date}: absentCount")
        assertThat(actual.presentChildren)
            .containsExactlyInAnyOrderElementsOf(expected.presentChildren)
            .withFailMessage("${actual.date}: presentChildren")
        assertThat(actual.assistanceChildren)
            .containsExactlyInAnyOrderElementsOf(expected.assistanceChildren)
            .withFailMessage("${actual.date}: assistanceChildren")
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
            .withFailMessage("${actual.date}: noResponseChildren")
            .containsExactlyInAnyOrderElementsOf(expected.noResponseChildren)
    }
}
