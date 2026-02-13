// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.children.Group
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesType
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevAssistanceFactor
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChildAttendance
import fi.espoo.evaka.shared.dev.DevDailyServiceTimes
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.DevStaffAttendance
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.domain.toFiniteDateRange
import fi.espoo.evaka.snDaycareContractDays10
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class AttendanceReservationReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired
    private lateinit var attendanceReservationReportController:
        AttendanceReservationReportController

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val daycare2 = DevDaycare(areaId = area.id, name = "Test Daycare 2")
    private val adminEmployee = DevEmployee()
    private val staffEmployee = DevEmployee()
    private val admin = AuthenticatedUser.Employee(adminEmployee.id, setOf(UserRole.ADMIN))
    private val staff = AuthenticatedUser.Employee(staffEmployee.id, setOf(UserRole.STAFF))
    private val daycareGroup = DevDaycareGroup(daycareId = daycare.id)

    // 2 children over 3 on 2020-05-28, 6 children under 3
    private val child1 = DevPerson(dateOfBirth = LocalDate.of(2017, 6, 1))
    private val child2 = DevPerson(dateOfBirth = LocalDate.of(2016, 3, 1))
    private val child3 = DevPerson(dateOfBirth = LocalDate.of(2018, 9, 1))
    private val child4 = DevPerson(dateOfBirth = LocalDate.of(2019, 3, 2))
    private val child5 = DevPerson(dateOfBirth = LocalDate.of(2018, 11, 13))
    private val child6 = DevPerson(dateOfBirth = LocalDate.of(2018, 11, 13))
    private val child7 = DevPerson(dateOfBirth = LocalDate.of(2018, 7, 28))
    private val child8 = DevPerson(dateOfBirth = LocalDate.of(2013, 3, 10))

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(daycare2)
            tx.insert(adminEmployee)
            tx.insert(staffEmployee)
            listOf(child1, child2, child3, child4, child5, child6, child7, child8).forEach {
                tx.insert(it, DevPersonType.CHILD)
            }
            tx.insertServiceNeedOptions()
            tx.insert(daycareGroup)
        }
    }

    @Test
    fun `returns only unit's operation days`() {
        val startDate = LocalDate.of(2022, 8, 8) // Mon
        val endDate = LocalDate.of(2022, 8, 14) // Sun

        val result = getReport(startDate, endDate)
        val expected = createEmptyReport(startDate, endDate.minusDays(2)) // Mon-Fri
        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `end date is inclusive`() {
        val date = LocalDate.of(2022, 8, 10)

        val result = getReport(date, date)
        val expected = createEmptyReport(date, date)
        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `child without placement is not included`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            tx.insert(
                DevReservation(
                    childId = child1.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(15, 48),
                    createdBy = admin.evakaUserId,
                )
            )
        }

        val result = getReport(date, date)
        val expected = createEmptyReport(date, date)
        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `child with placement to a different unit is not included`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare2.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child1.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(15, 48),
                    createdBy = admin.evakaUserId,
                )
            )
        }

        val result = getReport(date, date)
        val expected = createEmptyReport(date, date)
        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `service need factor is picked from placement's default option`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child1.id,
                    date = date,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(8, 15),
                    createdBy = admin.evakaUserId,
                )
            )
        }

        val result = getReport(date, date)
        val expected =
            createEmptyReport(date, date).also {
                addExpectedRow(
                    it,
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 0)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 0.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 15)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 0.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                )
            }
        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `service need factor is picked from placement's service need`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = child1.id,
                        unitId = daycare.id,
                        startDate = date,
                        endDate = date,
                    )
                )
            val period = FiniteDateRange(date, date)
            tx.insert(
                DevServiceNeed(
                    placementId = placementId,
                    startDate = period.start,
                    endDate = period.end,
                    optionId = snDaycareContractDays10.id,
                    confirmedBy = admin.evakaUserId,
                    confirmedAt = HelsinkiDateTime.now(),
                )
            )
            tx.insert(
                DevReservation(
                    childId = child1.id,
                    date = date,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(8, 15),
                    createdBy = admin.evakaUserId,
                )
            )
        }

        val result = getReport(date, date)
        val expected =
            createEmptyReport(date, date).also {
                addExpectedRow(
                    it,
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 0)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 0.88,
                        staffCount = 0.1,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 15)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 0.88,
                        staffCount = 0.1,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                )
            }
        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `age is calculated from attendance reservation date`() {
        val startDate = LocalDate.of(2020, 5, 29) // Fri
        val endDate = LocalDate.of(2020, 6, 1) // Mon & 3rd birthday
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child1.id,
                    date = startDate,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(8, 15),
                    createdBy = admin.evakaUserId,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child1.id,
                    date = endDate,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(8, 15),
                    createdBy = admin.evakaUserId,
                )
            )
        }

        val result = getReport(startDate, endDate)
        val expected =
            createEmptyReport(startDate, endDate).also {
                addExpectedRow(
                    it,
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 29), LocalTime.of(8, 0)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 0.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 29), LocalTime.of(8, 15)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 0.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 6, 1), LocalTime.of(8, 0)),
                        childCountUnder3 = 0,
                        childCountOver3 = 1,
                        childCount = 1,
                        capacityFactor = 1.0,
                        staffCount = 0.1,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 6, 1), LocalTime.of(8, 15)),
                        childCountUnder3 = 0,
                        childCountOver3 = 1,
                        childCount = 1,
                        capacityFactor = 1.0,
                        staffCount = 0.1,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                )
            }
        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `assistance factor is supported`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevAssistanceFactor(
                    childId = child1.id,
                    modifiedBy = adminEmployee.evakaUser,
                    validDuring = date.toFiniteDateRange(),
                    capacityFactor = 5.0,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child1.id,
                    date = date,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(8, 15),
                    createdBy = admin.evakaUserId,
                )
            )
        }

        val result = getReport(date, date)
        val expected =
            createEmptyReport(date, date).also {
                addExpectedRow(
                    it,
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 0)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 8.75,
                        staffCount = 1.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 15)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 8.75,
                        staffCount = 1.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                )
            }

        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `backup care is supported`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            val groupId =
                tx.insert(
                    DevDaycareGroup(daycareId = daycare2.id, startDate = date, endDate = date)
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId =
                        tx.insert(
                            DevPlacement(
                                childId = child1.id,
                                unitId = daycare2.id,
                                startDate = date,
                                endDate = date,
                            )
                        ),
                    daycareGroupId = groupId,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevBackupCare(
                    childId = child1.id,
                    unitId = daycare.id,
                    groupId = null,
                    period = FiniteDateRange(date, date),
                )
            )
            tx.insert(
                DevReservation(
                    childId = child1.id,
                    date = date,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(8, 15),
                    createdBy = admin.evakaUserId,
                )
            )
        }

        val result = getReport(date, date)
        val expected =
            createEmptyReport(date, date).also {
                addExpectedRow(
                    it,
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 0)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 0.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 15)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 0.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                )
            }

        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `end time is supported in first 30 minutes`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child1.id,
                    date = date,
                    startTime = LocalTime.of(8, 14),
                    endTime = LocalTime.of(15, 22),
                    createdBy = admin.evakaUserId,
                )
            )
        }

        val result = getReport(date, date)
        val changedRows =
            generateSequence(LocalTime.of(8, 0)) { it.plusMinutes(15) }
                .takeWhile { it <= LocalTime.of(15, 30) }
                .map {
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), it),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 0.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    )
                }
                .toList()
        val expected =
            createEmptyReport(date, date).also { addExpectedRow(it, *changedRows.toTypedArray()) }
        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `end time is supported in last 30 minutes`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child1.id,
                    date = date,
                    startTime = LocalTime.of(8, 14),
                    endTime = LocalTime.of(15, 48),
                    createdBy = admin.evakaUserId,
                )
            )
        }

        val result = getReport(date, date)
        val changedRows =
            generateSequence(LocalTime.of(8, 0)) { it.plusMinutes(15) }
                .takeWhile { it <= LocalTime.of(16, 0) }
                .map {
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), it),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 0.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    )
                }
                .toList()
        val expected =
            createEmptyReport(date, date).also { addExpectedRow(it, *changedRows.toTypedArray()) }
        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `reservation with no times is considered as unknown presence`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child1.id,
                    date = date,
                    startTime = null,
                    endTime = null,
                    createdBy = admin.evakaUserId,
                )
            )
        }

        val result = getReport(date, date)
        val expected =
            createEmptyReport(date, date).mapValues { (_, value) ->
                value.copy(unknownChildCount = 1, unknownChildCapacityFactor = 1.75)
            }
        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `no reservation nor service times is considered as unknown presence`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
        }

        val result = getReport(date, date)
        val expected =
            createEmptyReport(date, date).mapValues { (_, value) ->
                value.copy(unknownChildCount = 1, unknownChildCapacityFactor = 1.75)
            }
        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `unknown presence outside of service times`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevDailyServiceTimes(
                    childId = child1.id,
                    validityPeriod = DateRange(date.plusMonths(1), date.plusMonths(2)),
                )
            )
        }

        val result = getReport(date, date)
        val expected =
            createEmptyReport(date, date).mapValues { (_, value) ->
                value.copy(unknownChildCount = 1, unknownChildCapacityFactor = 1.75)
            }
        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `Service times type VARIABLE_TIME produces unknown presence`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevDailyServiceTimes(
                    childId = child1.id,
                    validityPeriod = DateRange(date, date),
                    type = DailyServiceTimesType.VARIABLE_TIME,
                    regularTimes = null,
                )
            )
        }

        val result = getReport(date, date)
        val expected =
            createEmptyReport(date, date).mapValues { (_, value) ->
                value.copy(unknownChildCount = 1, unknownChildCapacityFactor = 1.75)
            }
        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `times are inclusive`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child1.id,
                    date = date,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(8, 15),
                    createdBy = admin.evakaUserId,
                )
            )
        }

        val result = getReport(date, date)
        val expected =
            createEmptyReport(date, date).also {
                addExpectedRow(
                    it,
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 0)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 0.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 15)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 0.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                )
            }
        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `multiple children is supported`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            listOf(child1, child2, child3, child4, child5, child6, child7, child8).forEach {
                testChild ->
                tx.insert(
                    DevPlacement(
                        childId = testChild.id,
                        unitId = daycare.id,
                        startDate = date,
                        endDate = date,
                    )
                )
                tx.insert(
                    DevReservation(
                        childId = testChild.id,
                        date = date,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(8, 15),
                        createdBy = admin.evakaUserId,
                    )
                )
            }
        }

        val result = getReport(date, date)
        val expected =
            createEmptyReport(date, date).also {
                addExpectedRow(
                    it,
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 0)),
                        childCountUnder3 = 6,
                        childCountOver3 = 2,
                        childCount = 8,
                        capacityFactor = 12.5,
                        staffCount = 1.8,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 15)),
                        childCountUnder3 = 6,
                        childCountOver3 = 2,
                        childCount = 8,
                        capacityFactor = 12.5,
                        staffCount = 1.8,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                )
            }

        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `group ids filter works`() {
        val date = LocalDate.of(2020, 5, 28)
        val group1 =
            db.transaction { tx ->
                tx.insert(DevDaycareGroup(daycareId = daycare.id, name = "Testiläiset 1")).let {
                    Group(it, "Testiläiset 1")
                }
            }
        val group2 =
            db.transaction { tx ->
                tx.insert(DevDaycareGroup(daycareId = daycare.id, name = "Testiläiset 2")).let {
                    Group(it, "Testiläiset 2")
                }
            }
        db.transaction { tx ->
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId =
                        tx.insert(
                            DevPlacement(
                                childId = child1.id,
                                unitId = daycare.id,
                                startDate = date,
                                endDate = date,
                            )
                        ),
                    daycareGroupId = group1.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child1.id,
                    date = date,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(8, 15),
                    createdBy = admin.evakaUserId,
                )
            )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId =
                        tx.insert(
                            DevPlacement(
                                childId = child2.id,
                                unitId = daycare.id,
                                startDate = date,
                                endDate = date,
                            )
                        ),
                    daycareGroupId = group1.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child2.id,
                    date = date,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(8, 15),
                    createdBy = admin.evakaUserId,
                )
            )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId =
                        tx.insert(
                            DevPlacement(
                                childId = child3.id,
                                unitId = daycare.id,
                                startDate = date,
                                endDate = date,
                            )
                        ),
                    daycareGroupId = group2.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child3.id,
                    date = date,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(8, 15),
                    createdBy = admin.evakaUserId,
                )
            )
            tx.insert(
                DevPlacement(
                    childId = child4.id,
                    unitId = daycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child4.id,
                    date = date,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(8, 15),
                    createdBy = admin.evakaUserId,
                )
            )
        }

        val result = getReport(date, date, listOf(group1.id, group2.id))
        val expected =
            createEmptyReport(date, date, listOf(group1, group2)).also {
                addExpectedRow(
                    it,
                    AttendanceReservationReportRow(
                        groupId = group1.id,
                        groupName = group1.name,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 0)),
                        childCountUnder3 = 1,
                        childCountOver3 = 1,
                        childCount = 2,
                        capacityFactor = 2.75,
                        staffCount = 0.4,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                    AttendanceReservationReportRow(
                        groupId = group1.id,
                        groupName = group1.name,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 15)),
                        childCountUnder3 = 1,
                        childCountOver3 = 1,
                        childCount = 2,
                        capacityFactor = 2.75,
                        staffCount = 0.4,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                    AttendanceReservationReportRow(
                        groupId = group2.id,
                        groupName = group2.name,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 0)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 0.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                    AttendanceReservationReportRow(
                        groupId = group2.id,
                        groupName = group2.name,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 15)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 0.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                )
            }

        assertThat(result).containsExactlyInAnyOrderElementsOf(expected.values)
    }

    @Test
    fun `group placement without group ids filter works`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = child1.id,
                        unitId = daycare.id,
                        startDate = date,
                        endDate = date,
                    )
                )
            val groupId =
                tx.insert(DevDaycareGroup(daycareId = daycare.id, startDate = date, endDate = date))
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = groupId,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child1.id,
                    date = date,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(8, 15),
                    createdBy = admin.evakaUserId,
                )
            )
        }

        val result = getReport(date, date, null)
        val expected =
            createEmptyReport(date, date).also {
                addExpectedRow(
                    it,
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 0)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 0.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 15)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 0.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                )
            }
        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `empty group ids works`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child1.id,
                    date = date,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(8, 15),
                    createdBy = admin.evakaUserId,
                )
            )
        }

        val result = getReport(date, date, listOf())
        val expected =
            createEmptyReport(date, date).also {
                addExpectedRow(
                    it,
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 0)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 0.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 15)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 0.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                )
            }
        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `absences work correctly`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child2.id,
                    unitId = daycare.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child2.id,
                    date = date,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(8, 15),
                    createdBy = admin.evakaUserId,
                )
            )
            // Full-day absence
            tx.insert(
                DevAbsence(
                    childId = child2.id,
                    date = date,
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )

            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevReservation(
                    childId = child1.id,
                    date = date,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(8, 15),
                    createdBy = admin.evakaUserId,
                )
            )
            // PRESCHOOL_DAYCARE child with only one absence -> should be included
            tx.insert(
                DevAbsence(
                    childId = child1.id,
                    date = date,
                    absenceType = AbsenceType.OTHER_ABSENCE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
        }

        val result = getReport(date, date, null)
        val expected =
            createEmptyReport(date, date).also {
                addExpectedRow(
                    it,
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 0)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 0.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 15)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 0.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                )
            }
        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `Daily service times are used if attendance reservation is missing`() {
        // Three consecutive days
        val startDate = LocalDate.of(2020, 5, 28)
        val endDate = startDate.plusDays(2)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            // Reservation for 8:00-8:15 on the first day only
            tx.insert(
                DevReservation(
                    childId = child1.id,
                    date = startDate,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(8, 15),
                    createdBy = admin.evakaUserId,
                )
            )

            // Daily service times 8-16 on all three days, so these should show on the second day
            tx.insert(
                DevDailyServiceTimes(
                    childId = child1.id,
                    validityPeriod = DateRange(startDate, endDate),
                )
            )

            // Absence on the third day
            tx.insert(
                DevAbsence(
                    childId = child1.id,
                    date = startDate.plusDays(2),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
        }

        val result = getReport(startDate, endDate, null)
        val expected =
            createEmptyReport(startDate, endDate).also {
                addExpectedRow(
                    it,
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(startDate, LocalTime.of(8, 0)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 0.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(startDate, LocalTime.of(8, 15)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 0.3,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                    *createRowsForTimespan(
                            AttendanceReservationReportRow(
                                groupId = null,
                                groupName = null,
                                HelsinkiDateTime.of(startDate, LocalTime.of(8, 30)),
                                childCountUnder3 = 1,
                                childCountOver3 = 0,
                                childCount = 1,
                                capacityFactor = 1.75,
                                staffCount = 0.3,
                                unknownChildCount = 0,
                                unknownChildCapacityFactor = 0.0,
                            ),
                            HelsinkiDateTime.of(startDate.plusDays(1), LocalTime.of(8, 0)),
                            HelsinkiDateTime.of(startDate.plusDays(1), LocalTime.of(16, 0)),
                        )
                        .toTypedArray(),
                )
            }
        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `Child and staff attendances are used with realization report`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = child1.id,
                        unitId = daycare.id,
                        startDate = date,
                        endDate = date,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = daycareGroup.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevChildAttendance(
                    childId = child1.id,
                    unitId = daycare.id,
                    date = date,
                    arrived = LocalTime.of(8, 0),
                    departed = LocalTime.of(8, 15),
                )
            )
            tx.insert(
                DevStaffAttendance(
                    employeeId = staff.id,
                    groupId = daycareGroup.id,
                    arrived = HelsinkiDateTime.of(date, LocalTime.of(8, 0)),
                    departed = HelsinkiDateTime.of(date, LocalTime.of(8, 30)),
                    modifiedAt = HelsinkiDateTime.of(date, LocalTime.of(8, 0)),
                    modifiedBy = admin.evakaUserId,
                )
            )
        }
        val result = getReport(date, date, reservationType = ReservationType.REALIZATION)
        val expected =
            createEmptyReport(date, date).also {
                addExpectedRow(
                    it,
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 0)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 1.0,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 15)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 1.0,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                )
            }
        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `Only include responsible staff attendances with realization report`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = child1.id,
                        unitId = daycare.id,
                        startDate = date,
                        endDate = date,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = daycareGroup.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevChildAttendance(
                    childId = child1.id,
                    unitId = daycare.id,
                    date = date,
                    arrived = LocalTime.of(8, 0),
                    departed = LocalTime.of(8, 15),
                )
            )
            tx.insert(
                DevStaffAttendance(
                    employeeId = staff.id,
                    groupId = daycareGroup.id,
                    arrived = HelsinkiDateTime.of(date, LocalTime.of(8, 0)),
                    departed = HelsinkiDateTime.of(date, LocalTime.of(8, 30)),
                    modifiedAt = HelsinkiDateTime.of(date, LocalTime.of(8, 0)),
                    modifiedBy = admin.evakaUserId,
                )
            )

            val nonResponsibleStaff = DevEmployee()
            tx.insert(nonResponsibleStaff)
            tx.insert(
                DevStaffAttendance(
                    employeeId = nonResponsibleStaff.id,
                    groupId = daycareGroup.id,
                    arrived = HelsinkiDateTime.of(date, LocalTime.of(8, 0)),
                    departed = HelsinkiDateTime.of(date, LocalTime.of(8, 30)),
                    modifiedAt = HelsinkiDateTime.of(date, LocalTime.of(8, 0)),
                    modifiedBy = admin.evakaUserId,
                    occupancyCoefficient = BigDecimal.ZERO,
                )
            )
        }
        val result = getReport(date, date, reservationType = ReservationType.REALIZATION)
        val expected =
            createEmptyReport(date, date).also {
                addExpectedRow(
                    it,
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 0)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 1.0,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 15)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 1.0,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                )
            }
        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `Only include departed children with realization report`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = child1.id,
                        unitId = daycare.id,
                        startDate = date,
                        endDate = date,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = daycareGroup.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevChildAttendance(
                    childId = child1.id,
                    unitId = daycare.id,
                    date = date,
                    arrived = LocalTime.of(8, 0),
                    departed = LocalTime.of(8, 15),
                )
            )

            val placementId2 =
                tx.insert(
                    DevPlacement(
                        childId = child2.id,
                        unitId = daycare.id,
                        startDate = date,
                        endDate = date,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId2,
                    daycareGroupId = daycareGroup.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insert(
                DevChildAttendance(
                    childId = child2.id,
                    unitId = daycare.id,
                    date = date,
                    arrived = LocalTime.of(8, 0),
                    departed = null,
                )
            )
            tx.insert(
                DevStaffAttendance(
                    employeeId = staff.id,
                    groupId = daycareGroup.id,
                    arrived = HelsinkiDateTime.of(date, LocalTime.of(8, 0)),
                    departed = HelsinkiDateTime.of(date, LocalTime.of(8, 30)),
                    modifiedAt = HelsinkiDateTime.of(date, LocalTime.of(8, 0)),
                    modifiedBy = admin.evakaUserId,
                )
            )

            val nonResponsibleStaff = DevEmployee()
            tx.insert(nonResponsibleStaff)
            tx.insert(
                DevStaffAttendance(
                    employeeId = nonResponsibleStaff.id,
                    groupId = daycareGroup.id,
                    arrived = HelsinkiDateTime.of(date, LocalTime.of(8, 0)),
                    departed = HelsinkiDateTime.of(date, LocalTime.of(8, 30)),
                    modifiedAt = HelsinkiDateTime.of(date, LocalTime.of(8, 0)),
                    modifiedBy = admin.evakaUserId,
                    occupancyCoefficient = BigDecimal.ZERO,
                )
            )
        }
        val result = getReport(date, date, reservationType = ReservationType.REALIZATION)
        val expected =
            createEmptyReport(date, date).also {
                addExpectedRow(
                    it,
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 0)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 1.0,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                    AttendanceReservationReportRow(
                        groupId = null,
                        groupName = null,
                        HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 15)),
                        childCountUnder3 = 1,
                        childCountOver3 = 0,
                        childCount = 1,
                        capacityFactor = 1.75,
                        staffCount = 1.0,
                        unknownChildCount = 0,
                        unknownChildCapacityFactor = 0.0,
                    ),
                )
            }
        assertThat(result).containsExactlyElementsOf(expected.values)
    }

    private fun getReport(
        startDate: LocalDate,
        endDate: LocalDate,
        groupIds: List<GroupId>? = null,
        reservationType: ReservationType = ReservationType.RESERVATION,
    ): List<AttendanceReservationReportRow> {
        return attendanceReservationReportController.getAttendanceReservationReportByUnit(
            dbInstance(),
            RealEvakaClock(),
            admin,
            daycare.id,
            startDate,
            endDate,
            groupIds,
            reservationType,
        )
    }
}

data class RowKey(val group: Group?, val dateTime: HelsinkiDateTime)

private fun createEmptyReport(
    start: LocalDate,
    end: LocalDate,
    groups: List<Group> = emptyList(),
    operationDays: Set<DayOfWeek> = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY),
): MutableMap<RowKey, AttendanceReservationReportRow> {
    val startDateTime = HelsinkiDateTime.of(start, LocalTime.MIN)
    val endDateTime = HelsinkiDateTime.of(end, LocalTime.MAX)
    val times = mutableMapOf<RowKey, AttendanceReservationReportRow>()
    var time = startDateTime
    while (time < endDateTime) {
        if (operationDays.contains(time.dayOfWeek)) {
            if (groups.isEmpty()) {
                times[RowKey(null, time)] =
                    AttendanceReservationReportRow(null, null, time, 0, 0, 0, 0.0, 0.0, 0, 0.0)
            } else {
                groups.forEach { group ->
                    times[RowKey(group, time)] =
                        AttendanceReservationReportRow(
                            group.id,
                            group.name,
                            time,
                            0,
                            0,
                            0,
                            0.0,
                            0.0,
                            0,
                            0.0,
                        )
                }
            }
        }
        time = time.plusMinutes(15)
    }
    return times
}

private fun addExpectedRow(
    map: MutableMap<RowKey, AttendanceReservationReportRow>,
    vararg rows: AttendanceReservationReportRow,
) {
    rows.forEach { row ->
        map[RowKey(row.groupId?.let { Group(it, row.groupName!!) }, row.dateTime)] = row
    }
}

private fun createRowsForTimespan(
    row: AttendanceReservationReportRow,
    startTime: HelsinkiDateTime,
    endTime: HelsinkiDateTime,
): List<AttendanceReservationReportRow> {
    return generateSequence(startTime) { it.plusMinutes(15) }
        .takeWhile { it <= endTime }
        .map { row.copy(dateTime = it) }
        .toList()
}
