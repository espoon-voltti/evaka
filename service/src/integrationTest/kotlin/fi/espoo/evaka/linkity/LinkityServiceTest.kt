// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.linkity

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.attendance.*
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.*
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

internal class LinkityServiceTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Test
    fun `new plans from Linkity replace old plans`() {
        val today = LocalDate.now()
        val employeeNumber = "SARASTIA_1"
        val employee = DevEmployee(employeeNumber = employeeNumber)
        db.transaction { tx ->
            tx.insert(employee)
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
        val employeeNumber = "SARASTIA_2"
        val employee = DevEmployee(employeeNumber = employeeNumber)
        db.transaction { tx -> tx.insert(employee) }
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
        val employeeNumber = "SARASTIA_3"
        val employee = DevEmployee(employeeNumber = employeeNumber)
        db.transaction { tx -> tx.insert(employee) }
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
        val employeeNumber = "SARASTIA_4"
        val employee = DevEmployee(employeeNumber = employeeNumber)
        db.transaction { tx -> tx.insert(employee) }
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

    private fun upsertStaffAttendance(
        tx: Database.Transaction,
        employee: DevEmployee,
        groupId: GroupId,
        arrived: HelsinkiDateTime,
        departed: HelsinkiDateTime?,
        type: StaffAttendanceType,
    ) {
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
    }

    @Test
    fun `relevant attendances are sent to Linkity`() {
        val now = HelsinkiDateTime.now()
        val employeeNumber = "SARASTIA_1"
        db.transaction { tx ->
            val area = DevCareArea()
            val unit = DevDaycare(areaId = area.id)
            val group = DevDaycareGroup(daycareId = unit.id)
            val employee = DevEmployee(employeeNumber = employeeNumber)
            val employee2 = DevEmployee(employeeNumber = null)
            tx.insert(area)
            tx.insert(unit)
            tx.insert(group)
            tx.insert(employee)
            tx.insert(employee2)
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
            // 1st
            upsertStaffAttendance(
                tx,
                employee,
                group.id,
                now.minusDays(2),
                now.minusDays(1).minusHours(3).plusMinutes(6),
                StaffAttendanceType.PRESENT,
            )
            // 2nd
            upsertStaffAttendance(
                tx,
                employee,
                group.id,
                now.minusDays(1).minusHours(3).plusMinutes(6),
                now.minusDays(1).minusMinutes(4),
                StaffAttendanceType.PRESENT,
            )
            // 3rd
            upsertStaffAttendance(
                tx,
                employee,
                group.id,
                now.minusDays(1).minusMinutes(4),
                now.minusDays(1).plusHours(3).plusMinutes(4),
                StaffAttendanceType.PRESENT,
            )
            // 4th
            upsertStaffAttendance(
                tx,
                employee,
                group.id,
                now.minusDays(1).plusHours(3).plusMinutes(5),
                now.minusDays(1).plusHours(5).plusMinutes(4),
                StaffAttendanceType.OTHER_WORK,
            )
            // 5th
            upsertStaffAttendance(
                tx,
                employee,
                group.id,
                now.minusHours(2).minusMinutes(3),
                now.minusHours(1).minusMinutes(2),
                StaffAttendanceType.TRAINING,
            )
            // 6th
            upsertStaffAttendance(
                tx,
                employee,
                group.id,
                now.minusHours(1).minusMinutes(2),
                null,
                StaffAttendanceType.PRESENT,
            )
            // 7th
            upsertStaffAttendance(
                tx,
                employee2,
                group.id,
                now.minusHours(4),
                now.minusHours(3),
                StaffAttendanceType.PRESENT,
            )
        }
        val period = FiniteDateRange(now.minusDays(1).toLocalDate(), now.toLocalDate())

        val client = MockLinkityClient()
        sendWorkLogsToLinkity(period, db, client)

        val expected =
            setOf(
                // 1st attendance is not included because it started before the period
                // 2nd attendance start is not rounded to any plan because diff > 5 min, but end is
                // rounded
                WorkLog(
                    employeeNumber,
                    now.minusDays(1).minusHours(3).plusMinutes(6),
                    now.minusDays(1),
                    WorkLogType.PRESENT,
                ),
                // 3rd attendance start and end are rounded to times from different plans
                WorkLog(
                    employeeNumber,
                    now.minusDays(1),
                    now.minusDays(1).plusHours(3),
                    WorkLogType.PRESENT,
                ),
                // 4th attendance is rounded to the matching plan
                WorkLog(
                    employeeNumber,
                    now.minusDays(1).plusHours(3),
                    now.minusDays(1).plusHours(5),
                    WorkLogType.OTHER_WORK,
                ),
                // 5th attendance start is not rounded because no matching plan, but end is rounded
                WorkLog(
                    employeeNumber,
                    now.minusHours(2).minusMinutes(3),
                    now.minusHours(1),
                    WorkLogType.TRAINING,
                ),
                // 6th attendance is not included because it has not ended yet
                // 7th attendance is not included because the employee has no employee number
            )

        assertEquals(expected, client.getPreviouslyPostedWorkLogs().toSet())
    }
}
