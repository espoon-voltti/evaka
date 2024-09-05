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
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevAssistanceFactor
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevHolidayPeriod
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import java.math.BigDecimal
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

    final val mockNow =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 9, 9), LocalTime.of(12, 15)))
    final val mockToday = mockNow.today()

    private val holidayPeriod =
        DevHolidayPeriod(
            id = HolidayPeriodId(UUID.randomUUID()),
            period = FiniteDateRange(mockToday.plusWeeks(15), mockToday.plusWeeks(16).minusDays(1)),
            reservationDeadline = mockToday.plusWeeks(1),
            reservationsOpenOn = mockToday,
        )

    @Test
    fun `Unit supervisor can see own unit's report results`() {

        val testData = initTestData(holidayPeriod.period.start)
        val results =
            holidayPeriodAttendanceReport.getHolidayPeriodAttendanceReport(
                dbInstance(),
                mockNow,
                unitSupervisorA.user,
                testData.daycareAId,
                holidayPeriod.id,
            )

        assertThat(results.isNotEmpty())
    }

    @Test
    fun `Admin can see report results`() {
        val testData = initTestData(holidayPeriod.period.start)
        val results =
            holidayPeriodAttendanceReport.getHolidayPeriodAttendanceReport(
                dbInstance(),
                mockNow,
                adminLoginUser,
                testData.daycareAId,
                holidayPeriod.id,
            )
        assertThat(results.isNotEmpty())
    }

    @Test
    fun `Unit supervisor cannot see another unit's report results`() {
        val testData = initTestData(holidayPeriod.period.start)
        assertThrows<Forbidden> {
            holidayPeriodAttendanceReport.getHolidayPeriodAttendanceReport(
                dbInstance(),
                mockNow,
                unitSupervisorA.user,
                testData.daycareBId,
                holidayPeriod.id,
            )
        }
    }

    @Test
    fun `Report returns all period days as per unit operation days`() {
        val testData = initTestData(holidayPeriod.period.start)

        val reportResultsA =
            holidayPeriodAttendanceReport.getHolidayPeriodAttendanceReport(
                dbInstance(),
                mockNow,
                adminLoginUser,
                testData.daycareAId,
                holidayPeriod.id,
            )
        val periodDays = holidayPeriod.period.dates().toList()
        val periodDaysInA = periodDays.filter { it.dayOfWeek < DayOfWeek.SATURDAY }
        val resultDaysA = reportResultsA.map { it.date }

        assertThat(resultDaysA).containsAll(periodDaysInA)

        val reportResultsB =
            holidayPeriodAttendanceReport.getHolidayPeriodAttendanceReport(
                dbInstance(),
                mockNow,
                adminLoginUser,
                testData.daycareBId,
                holidayPeriod.id,
            )
        val resultDaysB = reportResultsB.map { it.date }

        assertThat(resultDaysB).containsAll(periodDays)
    }

    @Test
    fun `Report returns correct attendance data`() {
        val testData = initTestData(holidayPeriod.period.start)

        val reportResultsByDate =
            holidayPeriodAttendanceReport
                .getHolidayPeriodAttendanceReport(
                    dbInstance(),
                    mockNow,
                    adminLoginUser,
                    testData.daycareAId,
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
                presentOccupancyCoefficient = BigDecimal.ZERO,
            )

        val expectedTuesday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 0,
                requiredStaff = 2,
                presentChildren =
                    listOf(testData.childA, testData.childB, testData.childC).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                assistanceChildren =
                    listOf(testData.childC).map { ChildWithName(it.id, it.firstName, it.lastName) },
                noResponseChildren = emptyList(),
                date = holidayPeriod.period.start.plusDays(1),
                presentOccupancyCoefficient = BigDecimal("8.250"),
            )

        val expectedWednesday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 0,
                requiredStaff = 1,
                presentChildren =
                    listOf(testData.childA).map { ChildWithName(it.id, it.firstName, it.lastName) },
                assistanceChildren = emptyList(),
                noResponseChildren =
                    listOf(testData.childB, testData.childC).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                date = holidayPeriod.period.start.plusDays(2),
                presentOccupancyCoefficient = BigDecimal("1.750"),
            )

        val expectedThursday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 1,
                requiredStaff = 1,
                presentChildren =
                    listOf(testData.childA, testData.childB).map {
                        ChildWithName(it.id, it.firstName, it.lastName)
                    },
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
                date = holidayPeriod.period.start.plusDays(3),
                presentOccupancyCoefficient = BigDecimal("2.750"),
            )
        val expectedFriday =
            HolidayPeriodAttendanceReportRow(
                absentCount = 2,
                requiredStaff = 1,
                presentChildren =
                    listOf(testData.childC).map { ChildWithName(it.id, it.firstName, it.lastName) },
                assistanceChildren = emptyList(),
                noResponseChildren = emptyList(),
                date = holidayPeriod.period.start.plusDays(4),
                presentOccupancyCoefficient = BigDecimal("5.500"),
            )

        assertReportDay(
            reportResultsByDate[holidayPeriod.period.start] ?: fail("No date found"),
            expectedMonday,
        )
        assertReportDay(
            reportResultsByDate[holidayPeriod.period.start.plusDays(1)] ?: fail("No date found"),
            expectedTuesday,
        )
        assertReportDay(
            reportResultsByDate[holidayPeriod.period.start.plusDays(2)] ?: fail("No date found"),
            expectedWednesday,
        )
        assertReportDay(
            reportResultsByDate[holidayPeriod.period.start.plusDays(3)] ?: fail("No date found"),
            expectedThursday,
        )
        assertReportDay(
            reportResultsByDate[holidayPeriod.period.start.plusDays(4)] ?: fail("No date found"),
            expectedFriday,
        )
    }

    private fun initTestData(monday: LocalDate): HolidayPeriodPresenceReportTestData {
        val tuesday = monday.plusDays(1)
        val wednesday = monday.plusDays(2)
        val thursday = monday.plusDays(3)
        val friday = monday.plusDays(4)
        val saturday = monday.plusDays(5)

        return db.transaction { tx ->
            tx.insertServiceNeedOptions()
            tx.insert(admin)

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
                            List(7) { TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0)) },
                        shiftCareOperationTimes =
                            List(7) { TimeRange(LocalTime.of(0, 0), LocalTime.of(23, 59)) },
                    )
                )

            tx.insert(unitSupervisorA)

            tx.insertDaycareAclRow(daycareAId, unitSupervisorA.id, UserRole.UNIT_SUPERVISOR)

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

            val defaultPlacementDuration =
                FiniteDateRange(monday.minusMonths(1), monday.plusMonths(5))

            // 3 children
            // A: < 3, no assistance             -> 1.75 coefficient
            // B: > 3, no assistance             -> 1.00 coefficient
            // C: > 3, assistance factor of 5.50 -> 5.50 coefficient
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChildAapo.id,
                    unitId = daycareAId,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            )

            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChildBertil.id,
                    unitId = daycareAId,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            )

            val placementC1 =
                DevPlacement(
                    id = PlacementId(UUID.randomUUID()),
                    type = PlacementType.DAYCARE,
                    childId = testChildCecil.id,
                    unitId = daycareAId,
                    startDate = monday.minusMonths(1),
                    endDate = wednesday,
                )
            tx.insert(placementC1)

            val placementC2 =
                DevPlacement(
                    id = PlacementId(UUID.randomUUID()),
                    type = PlacementType.DAYCARE,
                    childId = testChildCecil.id,
                    unitId = daycareAId,
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

            listOf(testChildAapo, testChildBertil, testChildCecil).forEach {
                createOtherAbsence(monday, it.id, tx)
            }

            // Tuesday - everyone is present

            listOf(testChildAapo, testChildBertil, testChildCecil).forEach {
                createNullReservation(tuesday, it.id, tx)
            }

            // Wednesday - only Aapo has answered

            createNullReservation(wednesday, testChildAapo.id, tx)

            // Thursday - 2 present, 1 absent

            listOf(testChildAapo, testChildBertil).forEach {
                createNullReservation(thursday, it.id, tx)
            }
            createOtherAbsence(thursday, testChildCecil.id, tx)

            // Friday - 1 present, 2 absent
            listOf(testChildAapo, testChildBertil).forEach { createOtherAbsence(friday, it.id, tx) }
            createNullReservation(friday, testChildCecil.id, tx)

            // reservation on the weekend shouldn't show up

            createNullReservation(saturday, testChildAapo.id, tx)

            tx.insert(holidayPeriod)

            HolidayPeriodPresenceReportTestData(
                testChildAapo,
                testChildBertil,
                testChildCecil,
                daycareAId,
                daycareBId,
            )
        }
    }

    data class HolidayPeriodPresenceReportTestData(
        val childA: DevPerson,
        val childB: DevPerson,
        val childC: DevPerson,
        val daycareAId: DaycareId,
        val daycareBId: DaycareId,
    )

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
                AbsenceCategory.NONBILLABLE,
            )
        )

    private fun assertReportDay(
        expected: HolidayPeriodAttendanceReportRow,
        actual: HolidayPeriodAttendanceReportRow,
    ) {
        assertEquals(expected.date, actual.date)
        assertEquals(expected.absentCount, actual.absentCount)
        assertEquals(expected.requiredStaff, actual.requiredStaff)
        assertEquals(expected.presentOccupancyCoefficient, actual.presentOccupancyCoefficient)
        assertThat(actual.presentChildren)
            .containsExactlyInAnyOrderElementsOf(expected.presentChildren)
        assertThat(actual.noResponseChildren)
            .containsExactlyInAnyOrderElementsOf(expected.noResponseChildren)
    }
}
