// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.linkity

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.attendance.StaffAttendancePlan
import fi.espoo.evaka.attendance.StaffAttendanceType
import fi.espoo.evaka.attendance.findStaffAttendancePlansBy
import fi.espoo.evaka.attendance.insertStaffAttendancePlans
import fi.espoo.evaka.pis.createEmployee
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.titania.testEmployee
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class LinkityServiceTest : FullApplicationTest(resetDbBeforeEach = true) {
    private lateinit var client: MockLinkityClient

    @BeforeEach
    fun beforeEach() {
        client = MockLinkityClient()
    }

    @Test
    fun `new plans from Linkity replace old plans`() {
        val today = LocalDate.now()
        val employeeNumber = "SARASTIA_1"
        lateinit var employeeId: EmployeeId
        db.transaction { tx ->
            val employee = tx.createEmployee(testEmployee.copy(employeeNumber = employeeNumber))
            employeeId = employee.id
            tx.insertStaffAttendancePlans(
                sequenceOf(-1, 0, 1, 2)
                    .map { days ->
                        StaffAttendancePlan(
                            employeeId,
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
        client.setupMockShifts(shifts)

        val plans =
            db.transaction { tx ->
                updateStaffAttendancePlansFromLinkity(period, tx, client)
                tx.findStaffAttendancePlansBy()
            }
        // There will be a new plan for today, and the old plan for tomorrow has been removed
        // Other days will have the same plans as before
        assertTrue { plans.any { it.description == "Tänään-1" && it.startTime.hour == 7 } }
        assertTrue { plans.any { it.description == "Uusi" && it.startTime.hour == 8 } }
        assertTrue { plans.none { it.description == "Tänään1" } }
        assertTrue { plans.any { it.description == "Tänään2" && it.startTime.hour == 7 } }
        assertEquals(3, plans.size)
        assertTrue { plans.all { it.employeeId == employeeId } }
    }

    @Test
    fun `new plans with unknown sarastia id are ignored`() {
        val today = LocalDate.now()
        val employeeNumber = "SARASTIA_2"
        db.transaction { tx ->
            tx.createEmployee(testEmployee.copy(employeeNumber = employeeNumber))
        }
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
        client.setupMockShifts(shifts)

        val plans =
            db.transaction { tx ->
                updateStaffAttendancePlansFromLinkity(period, tx, client)
                tx.findStaffAttendancePlansBy()
            }
        // There will be no plans
        assertEquals(0, plans.size)
    }

    @Test
    fun `new plans with invalid times are ignored`() {
        val today = LocalDate.now()
        val employeeNumber = "SARASTIA_3"
        db.transaction { tx ->
            tx.createEmployee(testEmployee.copy(employeeNumber = employeeNumber))
        }
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
        client.setupMockShifts(shifts)

        val plans =
            db.transaction { tx ->
                updateStaffAttendancePlansFromLinkity(period, tx, client)
                tx.findStaffAttendancePlansBy()
            }
        // There will be no plans
        assertEquals(0, plans.size)
    }

    @Test
    fun `new plans with overlapping times are ignored`() {
        val today = LocalDate.now()
        val employeeNumber = "SARASTIA_4"
        db.transaction { tx ->
            tx.createEmployee(testEmployee.copy(employeeNumber = employeeNumber))
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
        client.setupMockShifts(shifts)

        val plans =
            db.transaction { tx ->
                updateStaffAttendancePlansFromLinkity(period, tx, client)
                tx.findStaffAttendancePlansBy()
            }
        // Only the first shift will be added
        assertEquals(1, plans.size)
        assertTrue { plans.any { it.description == "Uusi" && it.startTime.hour == 8 } }
    }
}
