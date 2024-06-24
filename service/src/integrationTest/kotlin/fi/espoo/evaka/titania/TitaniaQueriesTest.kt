// SPDX-FileCopyrightText: 2021-2022 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.titania

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.attendance.StaffAttendanceType
import fi.espoo.evaka.attendance.upsertStaffAttendance
import fi.espoo.evaka.pis.createEmployee
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TitaniaQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    @Test
    fun `getEmployeeIdsByNumbers works`() {
        db.transaction { tx ->
            val employee1 = tx.createEmployee(testEmployee.copy(employeeNumber = "1111"))
            val employee2 = tx.createEmployee(testEmployee.copy(employeeNumber = "2222"))
            val employee3 = tx.createEmployee(testEmployee.copy(employeeNumber = "3333"))

            assertThat(tx.getEmployeeIdsByNumbers(listOf("1111", "3333")))
                .containsExactlyInAnyOrderEntriesOf(
                    mapOf("1111" to employee1.id, "3333" to employee3.id)
                )
            assertThat(tx.getEmployeeIdsByNumbers(listOf("2222", "5555")))
                .containsExactlyEntriesOf(mapOf("2222" to employee2.id))
        }
    }

    @Test
    fun `getEmployeeIdsByNumbers works with empty input`() {
        val employeeNumbers = emptyList<String>()
        val employeeIds = db.transaction { tx -> tx.getEmployeeIdsByNumbers(employeeNumbers) }
        assertThat(employeeIds).isEmpty()
    }

    @Test
    fun `staff attendance plan insert and delete should work`() {
        db.transaction { tx ->
            val employee1 = tx.createEmployee(testEmployee)
            val employee2 = tx.createEmployee(testEmployee)
            val employee3 = tx.createEmployee(testEmployee)

            val inserted =
                tx.insertStaffAttendancePlans(
                    listOf(
                        StaffAttendancePlan(
                            employeeId = employee1.id,
                            type = StaffAttendanceType.PRESENT,
                            startTime =
                                HelsinkiDateTime.of(LocalDate.of(2022, 5, 31), LocalTime.of(7, 32)),
                            endTime =
                                HelsinkiDateTime.of(
                                    LocalDate.of(2022, 5, 31),
                                    LocalTime.of(14, 54)
                                ),
                            description = null
                        ),
                        StaffAttendancePlan(
                            employeeId = employee2.id,
                            type = StaffAttendanceType.PRESENT,
                            startTime =
                                HelsinkiDateTime.of(LocalDate.of(2022, 5, 31), LocalTime.of(7, 32)),
                            endTime =
                                HelsinkiDateTime.of(
                                    LocalDate.of(2022, 5, 31),
                                    LocalTime.of(14, 54)
                                ),
                            description = null
                        ),
                        StaffAttendancePlan(
                            employeeId = employee2.id,
                            type = StaffAttendanceType.PRESENT,
                            startTime =
                                HelsinkiDateTime.of(LocalDate.of(2022, 6, 1), LocalTime.of(7, 32)),
                            endTime =
                                HelsinkiDateTime.of(LocalDate.of(2022, 6, 1), LocalTime.of(14, 54)),
                            description = null
                        ),
                        StaffAttendancePlan(
                            employeeId = employee3.id,
                            type = StaffAttendanceType.PRESENT,
                            startTime =
                                HelsinkiDateTime.of(LocalDate.of(2022, 5, 31), LocalTime.of(7, 32)),
                            endTime =
                                HelsinkiDateTime.of(
                                    LocalDate.of(2022, 5, 31),
                                    LocalTime.of(14, 54)
                                ),
                            description = null
                        )
                    )
                )
            assertThat(inserted).containsExactlyInAnyOrder(1, 1, 1, 1)

            val deleted =
                tx.deleteStaffAttendancePlansBy(
                    employeeIds = listOf(employee2.id, employee3.id),
                    period = LocalDate.of(2022, 5, 31).let { FiniteDateRange(it, it) }
                )
            assertThat(deleted)
                .extracting<EmployeeId> { it.employeeId }
                .containsExactlyInAnyOrder(employee2.id, employee3.id)

            val existing =
                tx.findStaffAttendancePlansBy(
                    period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
                )
            assertThat(existing)
                .extracting<EmployeeId> { it.employeeId }
                .containsExactlyInAnyOrder(employee1.id, employee2.id)
        }
    }

    @Test
    fun `insertStaffAttendancePlans should work with empty`() {
        val plans = emptyList<StaffAttendancePlan>()
        val inserted = db.transaction { tx -> tx.insertStaffAttendancePlans(plans) }
        assertThat(inserted).isEmpty()
    }

    @Test
    fun `findStaffAttendancePlansBy should return all with empty criteria`() {
        val inserted =
            db.transaction { tx ->
                val employee = tx.createEmployee(testEmployee)
                tx.insertStaffAttendancePlans(
                    listOf(testStaffAttendancePlan.copy(employeeId = employee.id))
                )
                employee
            }

        val selected = db.read { tx -> tx.findStaffAttendancePlansBy() }

        assertThat(selected).extracting<EmployeeId> { it.employeeId }.containsExactly(inserted.id)
    }

    @Test
    fun `findStaffAttendancePlansBy should return empty with empty employee ids`() {
        db.transaction { tx ->
            val employee = tx.createEmployee(testEmployee)
            tx.insertStaffAttendancePlans(
                listOf(testStaffAttendancePlan.copy(employeeId = employee.id))
            )
            employee
        }

        val selected = db.read { tx -> tx.findStaffAttendancePlansBy(employeeIds = emptyList()) }

        assertThat(selected).extracting<EmployeeId> { it.employeeId }.isEmpty()
    }

    @Test
    fun `deleteStaffAttendancePlansBy should return all with empty criteria`() {
        val inserted =
            db.transaction { tx ->
                val employee = tx.createEmployee(testEmployee)
                tx.insertStaffAttendancePlans(
                    listOf(testStaffAttendancePlan.copy(employeeId = employee.id))
                )
                employee
            }

        val deleted = db.transaction { tx -> tx.deleteStaffAttendancePlansBy() }

        assertThat(deleted).extracting<EmployeeId> { it.employeeId }.containsExactly(inserted.id)
    }

    @Test
    fun `deleteStaffAttendancePlansBy should return empty with empty employee ids`() {
        db.transaction { tx ->
            val employee = tx.createEmployee(testEmployee)
            tx.insertStaffAttendancePlans(
                listOf(testStaffAttendancePlan.copy(employeeId = employee.id))
            )
            employee
        }

        val deleted =
            db.transaction { tx -> tx.deleteStaffAttendancePlansBy(employeeIds = emptyList()) }

        assertThat(deleted).isEmpty()
    }

    @Test
    fun `findStaffAttendancesBy() should filter with period`() {
        db.transaction { tx ->
            tx.insertRealtimeStaffAttendanceTestData()
            val attendances =
                tx.findStaffAttendancesBy(
                    null,
                    FiniteDateRange(LocalDate.of(2023, 11, 1), LocalDate.of(2023, 11, 7))
                )
            assertThat(attendances)
                .extracting<Int> { it.arrived.dayOfMonth }
                .containsExactlyInAnyOrder(1, 6)
        }
    }

    @Test
    fun `findStaffAttendancesBy() should filter with employee`() {
        db.transaction { tx ->
            val employeeIds = tx.insertRealtimeStaffAttendanceTestData()
            val attendances = tx.findStaffAttendancesBy(listOf(employeeIds[0]), null)
            assertThat(attendances)
                .extracting<Int> { it.arrived.dayOfMonth }
                .containsExactlyInAnyOrder(1, 10)
        }
    }

    @Test
    fun `findStaffAttendancesBy() should filter with period and employee`() {
        db.transaction { tx ->
            val employeeIds = tx.insertRealtimeStaffAttendanceTestData()
            val attendances =
                tx.findStaffAttendancesBy(
                    listOf(employeeIds[0]),
                    FiniteDateRange(LocalDate.of(2023, 11, 1), LocalDate.of(2023, 11, 7))
                )
            assertThat(attendances).extracting<Int> { it.arrived.dayOfMonth }.containsExactly(1)
        }
    }

    private val testStaffAttendancePlan =
        StaffAttendancePlan(
            employeeId = EmployeeId(UUID.randomUUID()),
            type = StaffAttendanceType.PRESENT,
            startTime = HelsinkiDateTime.of(LocalDate.of(2022, 6, 7), LocalTime.of(8, 12)),
            endTime = HelsinkiDateTime.of(LocalDate.of(2022, 6, 7), LocalTime.of(15, 41)),
            description = null
        )
}

fun Database.Transaction.insertRealtimeStaffAttendanceTestData(): List<EmployeeId> {
    val areaId = insert(DevCareArea())
    val dayCareId = insert(DevDaycare(areaId = areaId))
    val groupId = insert(DevDaycareGroup(daycareId = dayCareId))
    val employee1 = createEmployee(testEmployee)
    val employee2 = createEmployee(testEmployee)
    listOf(Pair(1, employee1.id), Pair(6, employee2.id), Pair(10, employee1.id)).forEach {
        addAttendance(
            it.second,
            groupId,
            LocalDate.of(2023, 11, it.first),
            LocalTime.of(8, 0),
            LocalTime.of(16, 0)
        )
    }
    return listOf(employee1.id, employee2.id)
}

fun Database.Transaction.addAttendance(
    employeeId: EmployeeId,
    groupId: GroupId,
    date: LocalDate,
    arrival: LocalTime,
    departure: LocalTime
) {
    upsertStaffAttendance(
        null,
        employeeId,
        groupId,
        HelsinkiDateTime.of(date, arrival),
        HelsinkiDateTime.of(date, departure),
        BigDecimal(1.0),
        StaffAttendanceType.PRESENT
    )
}
