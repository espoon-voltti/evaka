// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.linkity

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.attendance.*
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.StaffAttendanceRealtimeId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.*
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.PilotFeature
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class LinkityServiceTest : FullApplicationTest(resetDbBeforeEach = true) {
    val employeeNumber = "SARASTIA_1"
    lateinit var employee: DevEmployee
    lateinit var area: DevCareArea
    lateinit var daycare: DevDaycare

    @BeforeEach
    fun beforeEach() {
        employee = DevEmployee(employeeNumber = employeeNumber)
        area = DevCareArea()
        daycare =
            DevDaycare(
                areaId = area.id,
                enabledPilotFeatures = setOf(PilotFeature.STAFF_ATTENDANCE_INTEGRATION),
            )
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(employee)
            tx.insertDaycareAclRow(daycare.id, employee.id, UserRole.STAFF)
        }
    }

    @Test
    fun `new plans from Linkity replace old plans`() {
        val today = LocalDate.now()
        db.transaction { tx ->
            tx.insertStaffAttendancePlans(
                sequenceOf(-1, 0, 1, 2)
                    .map { days ->
                        StaffAttendancePlan(
                            employee.id,
                            StaffAttendanceType.PRESENT,
                            HelsinkiDateTime.of(today.plusDays(days.toLong()).atTime(7, 0)),
                            HelsinkiDateTime.of(today.plusDays(days.toLong()).atTime(15, 0)),
                            "Tänään$days",
                        )
                    }
                    .toList()
            )
        }
        val period = FiniteDateRange(today, today.plusDays(1))
        val shifts =
            listOf(
                Shift(
                    employeeNumber,
                    "WORK_SHIFT_1",
                    HelsinkiDateTime.of(today.atTime(8, 0)),
                    HelsinkiDateTime.of(today.atTime(16, 0)),
                    ShiftType.PRESENT,
                    "Uusi",
                )
            )
        val client = MockLinkityClient(shifts)
        updateStaffAttendancePlansFromLinkity(period, db, client)

        val plans = db.transaction { tx -> tx.findStaffAttendancePlansBy() }
        // There will be a new plan for today, and the old plan for tomorrow has been removed
        // Other days will have the same plans as before
        assertTrue { plans.any { it.description == "Tänään-1" && it.startTime.hour == 7 } }
        assertTrue { plans.any { it.description == "Uusi" && it.startTime.hour == 8 } }
        assertTrue { plans.none { it.description == "Tänään1" } }
        assertTrue { plans.any { it.description == "Tänään2" && it.startTime.hour == 7 } }
        assertEquals(3, plans.size)
        assertTrue { plans.all { it.employeeId == employee.id } }
    }

    @Test
    fun `new plans with unknown sarastia id are ignored`() {
        val today = LocalDate.now()
        val period = FiniteDateRange(today, today.plusDays(1))
        val shifts =
            listOf(
                Shift(
                    "UNKNOWN",
                    "WORK_SHIFT_1",
                    HelsinkiDateTime.of(today.atTime(8, 0)),
                    HelsinkiDateTime.of(today.atTime(16, 0)),
                    ShiftType.PRESENT,
                    "Uusi",
                )
            )
        val client = MockLinkityClient(shifts)
        updateStaffAttendancePlansFromLinkity(period, db, client)

        val plans = db.transaction { tx -> tx.findStaffAttendancePlansBy() }
        // There will be no plans
        assertEquals(0, plans.size)
    }

    @Test
    fun `new plans with invalid times are ignored`() {
        val today = LocalDate.now()
        val period = FiniteDateRange(today, today.plusDays(1))
        val shifts =
            listOf(
                Shift(
                    employeeNumber,
                    "WORK_SHIFT_1",
                    HelsinkiDateTime.of(today.atTime(16, 0)),
                    HelsinkiDateTime.of(today.atTime(8, 0)),
                    ShiftType.PRESENT,
                    "Uusi",
                )
            )
        val client = MockLinkityClient(shifts)
        updateStaffAttendancePlansFromLinkity(period, db, client)

        val plans = db.transaction { tx -> tx.findStaffAttendancePlansBy() }
        // There will be no plans
        assertEquals(0, plans.size)
    }

    @Test
    fun `new plans with overlapping times are ignored`() {
        val today = LocalDate.now()
        val period = FiniteDateRange(today, today.plusDays(1))
        val shifts =
            listOf(
                Shift(
                    employeeNumber,
                    "WORK_SHIFT_1",
                    HelsinkiDateTime.of(today.atTime(8, 0)),
                    HelsinkiDateTime.of(today.atTime(16, 0)),
                    ShiftType.PRESENT,
                    "Uusi",
                ),
                Shift(
                    employeeNumber,
                    "WORK_SHIFT_2",
                    HelsinkiDateTime.of(today.atTime(10, 0)),
                    HelsinkiDateTime.of(today.atTime(18, 0)),
                    ShiftType.PRESENT,
                    "Päällekkäin",
                ),
            )
        val client = MockLinkityClient(shifts)
        updateStaffAttendancePlansFromLinkity(period, db, client)

        val plans = db.transaction { tx -> tx.findStaffAttendancePlansBy() }
        // Only the first shift will be added
        assertEquals(1, plans.size)
        assertTrue { plans.any { it.description == "Uusi" && it.startTime.hour == 8 } }
    }

    @Test
    fun `new plans for employees in disabled daycares are ignored`() {
        val today = LocalDate.now()
        val employeeNumber2 = "SARASTIA_2"
        val employee2 = DevEmployee(employeeNumber = employeeNumber2)
        val daycare2 = DevDaycare(areaId = area.id, enabledPilotFeatures = setOf())
        db.transaction { tx ->
            tx.insert(daycare2)
            tx.insert(employee2)
            tx.insertDaycareAclRow(daycare2.id, employee2.id, UserRole.STAFF)
        }
        val period = FiniteDateRange(today, today.plusDays(1))
        val shifts =
            listOf(
                Shift(
                    employeeNumber2,
                    "WORK_SHIFT_1",
                    HelsinkiDateTime.of(today.atTime(8, 0)),
                    HelsinkiDateTime.of(today.atTime(16, 0)),
                    ShiftType.PRESENT,
                    "Uusi",
                )
            )
        val client = MockLinkityClient(shifts)
        updateStaffAttendancePlansFromLinkity(period, db, client)

        val plans = db.transaction { tx -> tx.findStaffAttendancePlansBy() }
        // There will be no plans
        assertEquals(0, plans.size)
    }

    private fun upsertStaffAttendance(
        tx: Database.Transaction,
        employee: DevEmployee,
        groupId: GroupId,
        arrived: HelsinkiDateTime,
        departed: HelsinkiDateTime?,
        type: StaffAttendanceType,
    ): StaffAttendanceRealtimeId? {
        val change =
            tx.upsertStaffAttendance(
                null,
                employee.id,
                groupId,
                arrived,
                departed,
                "1".toBigDecimal(),
                type,
                false,
                arrived,
                employee.evakaUserId,
            )
        return change?.new?.id
    }

    @Test
    fun `relevant attendances are sent to Linkity`() {
        val now = HelsinkiDateTime.now()
        lateinit var attendanceIds: List<StaffAttendanceRealtimeId?>
        db.transaction { tx ->
            val group = DevDaycareGroup(daycareId = daycare.id)
            val employee2 = DevEmployee(employeeNumber = "SARASTIA_2")
            val employee3 = DevEmployee(employeeNumber = null)
            val daycare2 = DevDaycare(areaId = area.id, enabledPilotFeatures = setOf())
            val group2 = DevDaycareGroup(daycareId = daycare2.id)
            tx.insert(daycare2)
            tx.insert(group)
            tx.insert(group2)
            tx.insert(employee2)
            tx.insert(employee3)
            tx.insertDaycareAclRow(daycare2.id, employee2.id, UserRole.STAFF)
            tx.insertDaycareAclRow(daycare.id, employee3.id, UserRole.STAFF)
            tx.insertStaffAttendancePlans(
                listOf(
                    StaffAttendancePlan(
                        employee.id,
                        StaffAttendanceType.PRESENT,
                        now.minusDays(1).minusHours(3),
                        now.minusDays(1),
                        null,
                    ),
                    StaffAttendancePlan(
                        employee.id,
                        StaffAttendanceType.OTHER_WORK,
                        now.minusDays(1).plusHours(3),
                        now.minusDays(1).plusHours(5),
                        null,
                    ),
                    StaffAttendancePlan(
                        employee.id,
                        StaffAttendanceType.PRESENT,
                        now.minusHours(1),
                        now.plusHours(5),
                        null,
                    ),
                )
            )
            attendanceIds =
                listOf(
                    // 1st
                    upsertStaffAttendance(
                        tx,
                        employee,
                        group.id,
                        now.minusDays(2),
                        now.minusDays(1).minusHours(3).plusMinutes(6),
                        StaffAttendanceType.PRESENT,
                    ),
                    // 2nd
                    upsertStaffAttendance(
                        tx,
                        employee,
                        group.id,
                        now.minusDays(1).minusHours(3).plusMinutes(6),
                        now.minusDays(1).minusMinutes(4),
                        StaffAttendanceType.PRESENT,
                    ),
                    // 3rd
                    upsertStaffAttendance(
                        tx,
                        employee,
                        group.id,
                        now.minusDays(1).minusMinutes(4),
                        now.minusDays(1).plusHours(3).plusMinutes(4),
                        StaffAttendanceType.PRESENT,
                    ),
                    // 4th
                    upsertStaffAttendance(
                        tx,
                        employee,
                        group.id,
                        now.minusDays(1).plusHours(3).plusMinutes(5),
                        now.minusDays(1).plusHours(5).plusMinutes(4),
                        StaffAttendanceType.OTHER_WORK,
                    ),
                    // 5th
                    upsertStaffAttendance(
                        tx,
                        employee,
                        group.id,
                        now.minusHours(2).minusMinutes(3),
                        now.minusHours(1).minusMinutes(2),
                        StaffAttendanceType.TRAINING,
                    ),
                    // 6th
                    upsertStaffAttendance(
                        tx,
                        employee,
                        group.id,
                        now.minusHours(1).minusMinutes(2),
                        null,
                        StaffAttendanceType.PRESENT,
                    ),
                    // 7th
                    upsertStaffAttendance(
                        tx,
                        employee3,
                        group.id,
                        now.minusHours(4),
                        now.minusHours(3),
                        StaffAttendanceType.PRESENT,
                    ),
                    // 8th
                    upsertStaffAttendance(
                        tx,
                        employee2,
                        group2.id,
                        now.minusHours(5),
                        now.minusHours(4),
                        StaffAttendanceType.PRESENT,
                    ),
                )
        }
        val period = FiniteDateRange(now.minusDays(1).toLocalDate(), now.toLocalDate())

        val client = MockLinkityClient()
        sendStaffAttendancesToLinkity(period, db, client)

        val expected =
            StampingBatch(
                now.minusDays(1).atStartOfDay(),
                now.plusDays(1).atStartOfDay(),
                listOf(
                        // 1st attendance is not included because it started before the period
                        // 2nd attendance start is not rounded to any plan because diff > 5 min, but
                        // end is rounded
                        Stamping(
                            attendanceIds[1]!!.toString(),
                            employeeNumber,
                            now.minusDays(1).minusHours(3).plusMinutes(6),
                            now.minusDays(1),
                            StampingType.PRESENT,
                        ),
                        // 3rd attendance start and end are rounded to times from different plans
                        Stamping(
                            attendanceIds[2]!!.toString(),
                            employeeNumber,
                            now.minusDays(1),
                            now.minusDays(1).plusHours(3),
                            StampingType.PRESENT,
                        ),
                        // 4th attendance is rounded to the matching plan
                        Stamping(
                            attendanceIds[3]!!.toString(),
                            employeeNumber,
                            now.minusDays(1).plusHours(3),
                            now.minusDays(1).plusHours(5),
                            StampingType.OTHER_WORK,
                        ),
                        // 5th attendance start is not rounded because no matching plan, but end is
                        // rounded
                        Stamping(
                            attendanceIds[4]!!.toString(),
                            employeeNumber,
                            now.minusHours(2).minusMinutes(3),
                            now.minusHours(1),
                            StampingType.TRAINING,
                        ),
                        // 6th attendance is not included because it has not ended yet
                        // 7th attendance is not included because the employee has no employee
                        // number
                        // 8th attendance is not included because the employee is not in an enabled
                        // daycare
                    )
                    .sortedBy { it.startTime },
            )

        // The stampings are sorted by start time for easier comparison
        assertEquals(
            expected,
            client.getPreviouslyPostedStampings()?.let { batch ->
                batch.copy(stampings = batch.stampings.sortedBy { it.startTime })
            },
        )
    }

    @Test
    fun `date ranges for plan queries`() {
        val startDate = LocalDate.now()
        val endDate = startDate.plusWeeks(6).minusDays(1)
        val chunkSizeDays = 7L
        val ranges =
            generateDateRangesForStaffAttendancePlanRequests(startDate, endDate, chunkSizeDays)
                .toList()
        val expected =
            listOf(
                FiniteDateRange(startDate, startDate.plusDays(6)),
                FiniteDateRange(startDate.plusDays(7), startDate.plusDays(13)),
                FiniteDateRange(startDate.plusDays(14), startDate.plusDays(20)),
                FiniteDateRange(startDate.plusDays(21), startDate.plusDays(27)),
                FiniteDateRange(startDate.plusDays(28), startDate.plusDays(34)),
                FiniteDateRange(startDate.plusDays(35), startDate.plusDays(41)),
            )
        assertEquals(expected, ranges)
    }
}
