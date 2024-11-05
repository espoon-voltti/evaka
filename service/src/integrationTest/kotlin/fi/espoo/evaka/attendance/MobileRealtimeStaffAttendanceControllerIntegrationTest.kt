// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.StaffAttendanceRealtimeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.auth.insertDaycareGroupAcl
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupAcl
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevEmployeePin
import fi.espoo.evaka.shared.dev.DevStaffAttendance
import fi.espoo.evaka.shared.dev.DevStaffAttendancePlan
import fi.espoo.evaka.shared.dev.createMobileDeviceToUnit
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired

class MobileRealtimeStaffAttendanceControllerIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired
    private lateinit var mobileRealtimeStaffAttendanceController:
        MobileRealtimeStaffAttendanceController
    private val mobileUser = AuthenticatedUser.MobileDevice(MobileDeviceId(UUID.randomUUID()))
    private val mobileUser2 = AuthenticatedUser.MobileDevice(MobileDeviceId(UUID.randomUUID()))
    private val today = LocalDate.of(2022, 2, 3)
    private val now = HelsinkiDateTime.of(today, LocalTime.of(14, 5, 1))
    private val groupId = GroupId(UUID.randomUUID())
    private val groupId2 = GroupId(UUID.randomUUID())

    @BeforeEach
    fun beforeEach() {
        val groupName = "Group in daycare 1"
        val groupName2 = "Group in daycare 2"

        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDaycare2)
            tx.insert(DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = groupName))
            tx.insert(
                DevDaycareGroup(id = groupId2, daycareId = testDaycare2.id, name = groupName2)
            )

            tx.createMobileDeviceToUnit(mobileUser.id, testDaycare.id)
            tx.createMobileDeviceToUnit(mobileUser2.id, testDaycare2.id)
        }
    }

    @Test
    fun `Employee present in one daycare unit should not be present in another`() {
        db.transaction { tx ->
            val employee = DevEmployee()
            tx.insert(employee)
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareAclRow(testDaycare2.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
            tx.insertDaycareGroupAcl(testDaycare2.id, employee.id, listOf(groupId2), now)

            tx.markStaffArrival(
                employee.id,
                groupId,
                HelsinkiDateTime.of(today, LocalTime.of(8, 0, 0)),
                BigDecimal(7.0),
            )
        }

        val attnInDaycare1 = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        val attnInDaycare2 = fetchRealtimeStaffAttendances(testDaycare2.id, mobileUser2)

        assertNotNull(attnInDaycare1.staff.first().present)
        assertNull(attnInDaycare2.staff.first().present)
    }

    @Test
    fun `Employee with no planned attendances can be marked as arrived without occupancy effect`() {
        val pinCode = "1212"
        val employee = DevEmployee()
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
        }

        val arrivalTime = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        markArrival(
            arrivalTime,
            employee.id,
            pinCode,
            groupId,
            arrivalTime.toLocalTime(),
            null,
            hasStaffOccupancyEffect = false,
        )
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employee.id, it.employeeId)
            assertEquals(groupId, it.present)
            assertEquals(1, it.attendances.size)
            assertEquals(arrivalTime, it.attendances.first().arrived)
            assertEquals(null, it.attendances.first().departed)
            assertEquals(StaffAttendanceType.PRESENT, it.attendances.first().type)
            assertEquals(
                occupancyCoefficientZero,
                it.attendances.first().occupancyCoefficient.stripTrailingZeros(),
            )
        }
    }

    @Test
    fun `Employee with no planned attendances can be marked as arrived with occupancy effect`() {
        val pinCode = "1212"
        val employee = DevEmployee()
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
        }

        val arrivalTime = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        markArrival(
            arrivalTime,
            employee.id,
            pinCode,
            groupId,
            arrivalTime.toLocalTime(),
            null,
            hasStaffOccupancyEffect = true,
        )
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(
                occupancyCoefficientSeven.stripTrailingZeros(),
                it.attendances.first().occupancyCoefficient.stripTrailingZeros(),
            )
        }
    }

    @Test
    fun `Employee with no planned attendances can be marked as departed`() {
        val pinCode = "1212"
        val employee = DevEmployee()
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
        }

        val arrivalTime = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        markArrival(arrivalTime, employee.id, pinCode, groupId, arrivalTime.toLocalTime(), null)
        val departureTime = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        markDeparture(
            departureTime,
            employee.id,
            pinCode,
            groupId,
            departureTime.toLocalTime(),
            null,
        )
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employee.id, it.employeeId)
            assertEquals(null, it.present)
            assertEquals(1, it.attendances.size)
            assertEquals(arrivalTime, it.attendances.first().arrived)
            assertEquals(departureTime, it.attendances.first().departed)
            assertEquals(StaffAttendanceType.PRESENT, it.attendances.last().type)
        }
    }

    @Test
    fun `Employee arriving at planned time does not require a type`() {
        val pinCode = "1212"
        val employee = DevEmployee()
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(16, 0))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee.id,
                    startTime = plannedStart,
                    endTime = plannedEnd,
                )
            )
        }

        markArrival(plannedStart, employee.id, pinCode, groupId, plannedStart.toLocalTime(), null)
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employee.id, it.employeeId)
            assertEquals(groupId, it.present)
            assertEquals(1, it.attendances.size)
            assertEquals(plannedStart, it.attendances.first().arrived)
            assertEquals(null, it.attendances.first().departed)
            assertEquals(StaffAttendanceType.PRESENT, it.attendances.last().type)
        }
    }

    @Test
    fun `Employee departing at planned time does not require a type`() {
        val pinCode = "1212"
        val employee = DevEmployee()
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee.id,
                    startTime = plannedStart,
                    endTime = plannedEnd,
                )
            )
        }

        markArrival(plannedStart, employee.id, pinCode, groupId, plannedStart.toLocalTime(), null)
        markDeparture(plannedEnd, employee.id, pinCode, groupId, plannedEnd.toLocalTime(), null)
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employee.id, it.employeeId)
            assertEquals(null, it.present)
            assertEquals(1, it.attendances.size)
            assertEquals(plannedStart, it.attendances.first().arrived)
            assertEquals(plannedEnd, it.attendances.first().departed)
            assertEquals(StaffAttendanceType.PRESENT, it.attendances.last().type)
        }
    }

    @Test
    fun `Employee arriving within 5 minutes of planned time does not require a type`() {
        val pinCode = "1212"
        val employee = DevEmployee()
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(16, 0))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee.id,
                    startTime = plannedStart,
                    endTime = plannedEnd,
                )
            )
        }

        val arrivalTime = plannedStart.plusMinutes(5)
        markArrival(arrivalTime, employee.id, pinCode, groupId, arrivalTime.toLocalTime(), null)
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employee.id, it.employeeId)
            assertEquals(groupId, it.present)
            assertEquals(1, it.attendances.size)
            assertEquals(arrivalTime, it.attendances.first().arrived)
            assertEquals(null, it.attendances.first().departed)
            assertEquals(StaffAttendanceType.PRESENT, it.attendances.last().type)
        }
    }

    @Test
    fun `Employee departing within 5 minutes of planned time does not require a type`() {
        val pinCode = "1212"
        val employee = DevEmployee()
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee.id,
                    startTime = plannedStart,
                    endTime = plannedEnd,
                )
            )
        }

        val arrivalTime = plannedStart.minusMinutes(5)
        markArrival(arrivalTime, employee.id, pinCode, groupId, arrivalTime.toLocalTime(), null)
        val departureTime = plannedEnd.plusMinutes(5)
        markDeparture(
            departureTime,
            employee.id,
            pinCode,
            groupId,
            departureTime.toLocalTime(),
            null,
        )
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employee.id, it.employeeId)
            assertEquals(null, it.present)
            assertEquals(1, it.attendances.size)
            assertEquals(arrivalTime, it.attendances.first().arrived)
            assertEquals(departureTime, it.attendances.first().departed)
            assertEquals(StaffAttendanceType.PRESENT, it.attendances.last().type)
        }
    }

    @Test
    fun `Employee arriving 20 minutes before planned time requires a type`() {
        val pinCode = "1212"
        val employee = DevEmployee()
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(16, 0))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee.id,
                    startTime = plannedStart,
                    endTime = plannedEnd,
                )
            )
        }

        val arrivalTime = plannedStart.minusMinutes(20)
        assertThrows<BadRequest> {
            markArrival(arrivalTime, employee.id, pinCode, groupId, arrivalTime.toLocalTime(), null)
        }
    }

    @Test
    fun `last_login should update when Employee is marked as arrived`() {
        val pinCode = "1212"
        val initialLastLogin = HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0, 0))
        val employee = DevEmployee(lastLogin = initialLastLogin)
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
        }

        val lastLoginBeforeArrival = db.read { db -> db.getEmployeeLastLogin(employee.id) }
        assertEquals(initialLastLogin, lastLoginBeforeArrival)

        val arrivalTime = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        markArrival(arrivalTime, employee.id, pinCode, groupId, arrivalTime.toLocalTime(), null)

        val lastLogin = db.read { db -> db.getEmployeeLastLogin(employee.id) }
        assertEquals(arrivalTime, lastLogin)
    }

    @ParameterizedTest(
        name = "Employee arriving 20 minutes before planned time can be marked arrived with {0}"
    )
    @EnumSource(names = ["OVERTIME", "JUSTIFIED_CHANGE"])
    fun testStaffArrivalBeforePlanStartWithAllowedType(type: StaffAttendanceType) {
        val pinCode = "1212"
        val employee = DevEmployee()
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(16, 0))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee.id,
                    startTime = plannedStart,
                    endTime = plannedEnd,
                )
            )
        }

        val arrivalTime = plannedStart.minusMinutes(20)
        markArrival(arrivalTime, employee.id, pinCode, groupId, arrivalTime.toLocalTime(), type)
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employee.id, it.employeeId)
            assertEquals(groupId, it.present)
            assertEquals(1, it.attendances.size)
            assertEquals(arrivalTime, it.attendances.first().arrived)
            assertEquals(null, it.attendances.first().departed)
            assertEquals(type, it.attendances.first().type)
        }
    }

    @ParameterizedTest(
        name = "Employee arriving 20 minutes before planned time cannot be marked arrived with {0}"
    )
    @EnumSource(names = ["PRESENT", "OTHER_WORK", "TRAINING"])
    fun testStaffArrivalBeforePlanStartWithUnallowedType(type: StaffAttendanceType) {
        val pinCode = "1212"
        val employee = DevEmployee()
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(16, 0))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee.id,
                    startTime = plannedStart,
                    endTime = plannedEnd,
                )
            )
        }

        val arrivalTime = plannedStart.minusMinutes(20)
        assertThrows<BadRequest> {
            markArrival(arrivalTime, employee.id, pinCode, groupId, arrivalTime.toLocalTime(), type)
        }
    }

    @Test
    fun `Employee arriving 35 minutes after planned time does not require a type`() {
        val pinCode = "1212"
        val employee = DevEmployee()
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(16, 0))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee.id,
                    startTime = plannedStart,
                    endTime = plannedEnd,
                )
            )
        }

        val arrivalTime = plannedStart.plusMinutes(35)
        markArrival(arrivalTime, employee.id, pinCode, groupId, arrivalTime.toLocalTime(), null)
    }

    @Test
    fun `Employee arriving 20 minutes after planned time can be marked arrived with JUSTIFIED_CHANGE`() {
        val pinCode = "1212"
        val employee = DevEmployee()
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(16, 0))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee.id,
                    startTime = plannedStart,
                    endTime = plannedEnd,
                )
            )
        }

        val arrivalTime = plannedStart.plusMinutes(20)
        markArrival(
            arrivalTime,
            employee.id,
            pinCode,
            groupId,
            arrivalTime.toLocalTime(),
            StaffAttendanceType.JUSTIFIED_CHANGE,
        )
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employee.id, it.employeeId)
            assertEquals(groupId, it.present)
            assertEquals(1, it.attendances.size)
            assertEquals(arrivalTime, it.attendances.first().arrived)
            assertEquals(null, it.attendances.first().departed)
            assertEquals(StaffAttendanceType.JUSTIFIED_CHANGE, it.attendances.first().type)
        }
    }

    @ParameterizedTest(
        name = "Employee arriving 20 minutes after planned time can be marked arrived with {0}"
    )
    @EnumSource(names = ["OTHER_WORK", "TRAINING"])
    fun testStaffArrivalAfterPlanStartWithAllowedType(type: StaffAttendanceType) {
        val pinCode = "1212"
        val employee = DevEmployee()
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(16, 0))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee.id,
                    startTime = plannedStart,
                    endTime = plannedEnd,
                )
            )
        }

        val arrivalTime = plannedStart.plusMinutes(20)
        markArrival(arrivalTime, employee.id, pinCode, groupId, arrivalTime.toLocalTime(), type)
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employee.id, it.employeeId)
            assertEquals(groupId, it.present)
            assertEquals(2, it.attendances.size)
            assertEquals(plannedStart, it.attendances.first().arrived)
            assertEquals(arrivalTime, it.attendances.first().departed)
            assertEquals(type, it.attendances.first().type)
            assertEquals(arrivalTime, it.attendances.last().arrived)
            assertEquals(null, it.attendances.last().departed)
            assertEquals(StaffAttendanceType.PRESENT, it.attendances.last().type)
        }
    }

    @ParameterizedTest(
        name = "Employee arriving 20 minutes after planned time cannot be marked arrived with {0}"
    )
    @EnumSource(names = ["PRESENT", "OVERTIME"])
    fun testStaffArrivalAfterPlanStartWithUnallowedType(type: StaffAttendanceType) {
        val pinCode = "1212"
        val employee = DevEmployee()
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(16, 0))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee.id,
                    startTime = plannedStart,
                    endTime = plannedEnd,
                )
            )
        }

        val arrivalTime = plannedStart.plusMinutes(20)
        assertThrows<BadRequest> {
            markArrival(arrivalTime, employee.id, pinCode, groupId, arrivalTime.toLocalTime(), type)
        }
    }

    @Test
    fun `Employee departing without reason before planned time is possible`() {
        val pinCode = "1212"
        val employee = DevEmployee()
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee.id,
                    startTime = plannedStart,
                    endTime = plannedEnd,
                )
            )
        }

        val arrivalTime = plannedStart
        markArrival(arrivalTime, employee.id, pinCode, groupId, arrivalTime.toLocalTime(), null)
        val departureTime = plannedEnd.minusMinutes(6)
        markDeparture(
            departureTime,
            employee.id,
            pinCode,
            groupId,
            departureTime.toLocalTime(),
            null,
        )
    }

    @Test
    fun `Employee departing 20 minutes before planned time can be marked departed with JUSTIFIED_CHANGE`() {
        val pinCode = "1212"
        val employee = DevEmployee()
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee.id,
                    startTime = plannedStart,
                    endTime = plannedEnd,
                )
            )
        }

        val arrivalTime = plannedStart
        markArrival(arrivalTime, employee.id, pinCode, groupId, arrivalTime.toLocalTime(), null)
        val departureTime = plannedEnd.minusMinutes(20)
        markDeparture(
            plannedEnd,
            employee.id,
            pinCode,
            groupId,
            departureTime.toLocalTime(),
            StaffAttendanceType.JUSTIFIED_CHANGE,
        )
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employee.id, it.employeeId)
            assertEquals(null, it.present)
            assertEquals(1, it.attendances.size)
            assertEquals(arrivalTime, it.attendances.first().arrived)
            assertEquals(departureTime, it.attendances.first().departed)
            assertEquals(StaffAttendanceType.JUSTIFIED_CHANGE, it.attendances.first().type)
        }
    }

    @Test
    fun `Employee arriving 90min minutes after planned time does not produce an attendance for the preceding time`() {
        val pinCode = "1212"
        val employee = DevEmployee()
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee.id,
                    startTime = plannedStart,
                    endTime = plannedEnd,
                )
            )
        }

        val arrivalTime = plannedStart.plusMinutes(90)
        markArrival(arrivalTime, employee.id, pinCode, groupId, arrivalTime.toLocalTime(), null)

        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employee.id, it.employeeId)
            assertEquals(groupId, it.present)
            assertEquals(1, it.attendances.size)
            assertEquals(arrivalTime, it.attendances.first().arrived)
            assertEquals(null, it.attendances.first().departed)
            assertEquals(StaffAttendanceType.PRESENT, it.attendances.first().type)
        }
    }

    @ParameterizedTest(
        name = "Employee departing 20 minutes before planned time can be marked departed with {0}"
    )
    @EnumSource(names = ["OTHER_WORK", "TRAINING"])
    fun testStaffDepartureBeforePlanStartWithAllowedType(type: StaffAttendanceType) {
        val pinCode = "1212"
        val employee = DevEmployee()
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee.id,
                    startTime = plannedStart,
                    endTime = plannedEnd,
                )
            )
        }

        val arrivalTime = plannedStart
        markArrival(arrivalTime, employee.id, pinCode, groupId, arrivalTime.toLocalTime(), null)
        val departureTime = plannedEnd.minusMinutes(20)
        markDeparture(
            departureTime,
            employee.id,
            pinCode,
            groupId,
            departureTime.toLocalTime(),
            type,
        )
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employee.id, it.employeeId)
            assertEquals(null, it.present)
            assertEquals(2, it.attendances.size)
            assertEquals(arrivalTime, it.attendances.first().arrived)
            assertEquals(departureTime, it.attendances.first().departed)
            assertEquals(StaffAttendanceType.PRESENT, it.attendances.first().type)
            assertEquals(departureTime, it.attendances.last().arrived)
            assertEquals(null, it.attendances.last().departed)
            assertEquals(type, it.attendances.last().type)
        }
    }

    @ParameterizedTest(
        name =
            "Employee departing 20 minutes before planned time cannot be marked departed with {0}"
    )
    @EnumSource(names = ["PRESENT", "OVERTIME"])
    fun testStaffDepartureBeforePlanStartWithUnallowedType(type: StaffAttendanceType) {
        val pinCode = "1212"
        val employee = DevEmployee()
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee.id,
                    startTime = plannedStart,
                    endTime = plannedEnd,
                )
            )
        }

        val arrivalTime = plannedStart
        markArrival(arrivalTime, employee.id, pinCode, groupId, arrivalTime.toLocalTime(), null)
        val departureTime = plannedEnd.minusMinutes(20)
        assertThrows<BadRequest> {
            markDeparture(
                departureTime,
                employee.id,
                pinCode,
                groupId,
                departureTime.toLocalTime(),
                type,
            )
        }
    }

    @Test
    fun `Employee departing 20 minutes after planned time does not require a type`() {
        val pinCode = "1212"
        val employee = DevEmployee()
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee.id,
                    startTime = plannedStart,
                    endTime = plannedEnd,
                )
            )
        }

        val arrivalTime = plannedStart
        markArrival(arrivalTime, employee.id, pinCode, groupId, arrivalTime.toLocalTime(), null)
        val departureTime = plannedEnd.plusMinutes(20)
        markDeparture(
            departureTime,
            employee.id,
            pinCode,
            groupId,
            departureTime.toLocalTime(),
            null,
        )
    }

    @ParameterizedTest(
        name = "Employee departing 20 minutes after planned time can be marked departed with {0}"
    )
    @EnumSource(names = ["OVERTIME", "JUSTIFIED_CHANGE"])
    fun testStaffDepartureAfterPlanStartWithAllowedType(type: StaffAttendanceType) {
        val pinCode = "1212"
        val employee = DevEmployee()
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee.id,
                    startTime = plannedStart,
                    endTime = plannedEnd,
                )
            )
        }

        val arrivalTime = plannedStart
        markArrival(arrivalTime, employee.id, pinCode, groupId, arrivalTime.toLocalTime(), null)
        val departureTime = plannedEnd.plusMinutes(20)
        markDeparture(
            departureTime,
            employee.id,
            pinCode,
            groupId,
            departureTime.toLocalTime(),
            type,
        )
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employee.id, it.employeeId)
            assertEquals(null, it.present)
            assertEquals(1, it.attendances.size)
            assertEquals(arrivalTime, it.attendances.first().arrived)
            assertEquals(departureTime, it.attendances.first().departed)
            assertEquals(type, it.attendances.first().type)
        }
    }

    @ParameterizedTest(
        name = "Employee departing 20 minutes after planned time cannot be marked departed with {0}"
    )
    @EnumSource(names = ["PRESENT", "OTHER_WORK", "TRAINING"])
    fun testStaffDepartureAfterPlanStartWithUnallowedType(type: StaffAttendanceType) {
        val pinCode = "1212"
        val employee = DevEmployee()
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee.id,
                    startTime = plannedStart,
                    endTime = plannedEnd,
                )
            )
        }

        val arrivalTime = plannedStart
        markArrival(arrivalTime, employee.id, pinCode, groupId, arrivalTime.toLocalTime(), null)
        val departureTime = plannedEnd.plusMinutes(20)
        assertThrows<BadRequest> {
            markDeparture(
                departureTime,
                employee.id,
                pinCode,
                groupId,
                departureTime.toLocalTime(),
                type,
            )
        }
    }

    @Test
    fun `Employee arriving and departing multiple times during same work day`() {
        val pinCode = "1212"
        val employee = DevEmployee()
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(14, 0))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(DevEmployeePin(userId = employee.id, pin = pinCode))
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId), now)
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employee.id,
                    startTime = plannedStart,
                    endTime = plannedEnd,
                )
            )
        }

        val firstArrivalTime = plannedStart.minusMinutes(20)
        markArrival(
            firstArrivalTime,
            employee.id,
            pinCode,
            groupId,
            firstArrivalTime.toLocalTime(),
            StaffAttendanceType.OVERTIME,
        )
        val firstDepartureTime = firstArrivalTime.plusHours(1)
        markDeparture(
            firstDepartureTime,
            employee.id,
            pinCode,
            groupId,
            firstDepartureTime.toLocalTime(),
            StaffAttendanceType.OTHER_WORK,
        )
        val secondArrivalTime = firstDepartureTime.plusHours(1)
        markArrival(
            secondArrivalTime,
            employee.id,
            pinCode,
            groupId,
            secondArrivalTime.toLocalTime(),
            StaffAttendanceType.OTHER_WORK,
        )
        val secondDepartureTime = secondArrivalTime.plusHours(1)
        markDeparture(
            secondDepartureTime,
            employee.id,
            pinCode,
            groupId,
            secondDepartureTime.toLocalTime(),
            StaffAttendanceType.TRAINING,
        )
        val thirdArrivalTime = secondDepartureTime.plusHours(1)
        markArrival(
            thirdArrivalTime,
            employee.id,
            pinCode,
            groupId,
            thirdArrivalTime.toLocalTime(),
            StaffAttendanceType.TRAINING,
        )
        val thirdDepartureTime = thirdArrivalTime.plusMinutes(15)
        markDeparture(
            thirdDepartureTime,
            employee.id,
            pinCode,
            groupId,
            thirdDepartureTime.toLocalTime(),
            null,
        )
        val fourthArrivalTime = thirdDepartureTime.plusMinutes(30)
        markArrival(
            fourthArrivalTime,
            employee.id,
            pinCode,
            groupId,
            fourthArrivalTime.toLocalTime(),
            null,
        )
        val fourthDepartureTime = plannedEnd.minusMinutes(5)
        markDeparture(
            fourthDepartureTime,
            employee.id,
            pinCode,
            groupId,
            fourthDepartureTime.toLocalTime(),
            null,
        )
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        assertEquals(1, attendances.staff.size)
        attendances.staff.first().let {
            assertEquals(employee.id, it.employeeId)
            assertEquals(null, it.present)
            assertEquals(6, it.attendances.size)
            assertEquals(firstArrivalTime, it.attendances[0].arrived)
            assertEquals(firstDepartureTime, it.attendances[0].departed)
            assertEquals(StaffAttendanceType.OVERTIME, it.attendances[0].type)
            assertEquals(firstDepartureTime, it.attendances[1].arrived)
            assertEquals(secondArrivalTime, it.attendances[1].departed)
            assertEquals(StaffAttendanceType.OTHER_WORK, it.attendances[1].type)
            assertEquals(secondArrivalTime, it.attendances[2].arrived)
            assertEquals(secondDepartureTime, it.attendances[2].departed)
            assertEquals(StaffAttendanceType.PRESENT, it.attendances[2].type)
            assertEquals(secondDepartureTime, it.attendances[3].arrived)
            assertEquals(thirdArrivalTime, it.attendances[3].departed)
            assertEquals(StaffAttendanceType.TRAINING, it.attendances[3].type)
            assertEquals(thirdArrivalTime, it.attendances[4].arrived)
            assertEquals(thirdDepartureTime, it.attendances[4].departed)
            assertEquals(StaffAttendanceType.PRESENT, it.attendances[4].type)
            assertEquals(fourthArrivalTime, it.attendances[5].arrived)
            assertEquals(fourthDepartureTime, it.attendances[5].departed)
            assertEquals(StaffAttendanceType.PRESENT, it.attendances[5].type)
        }
    }

    private fun addEmployee(): EmployeeId {
        return db.transaction { tx ->
            val employeeId = tx.insert(DevEmployee())
            tx.insertDaycareAclRow(
                daycareId = testDaycare.id,
                employeeId = employeeId,
                UserRole.STAFF,
            )
            tx.insert(DevDaycareGroupAcl(groupId = groupId, employeeId = employeeId))
            tx.insert(DevEmployeePin(userId = employeeId, pin = "1122"))
            employeeId
        }
    }

    @Test
    fun `set attendances crud works`() {
        val employeeId = addEmployee()

        val insertResponse =
            mobileRealtimeStaffAttendanceController.setAttendances(
                dbInstance(),
                mobileUser,
                MockEvakaClock(now),
                testDaycare.id,
                MobileRealtimeStaffAttendanceController.StaffAttendanceUpdateRequest(
                    employeeId = employeeId,
                    pinCode = "1122",
                    date = now.toLocalDate(),
                    rows =
                        listOf(
                            RealtimeStaffAttendanceController.StaffAttendanceUpsert(
                                id = null,
                                groupId = groupId,
                                arrived = now,
                                departed = null,
                                type = StaffAttendanceType.PRESENT,
                                hasStaffOccupancyEffect = true,
                            )
                        ),
                ),
            )
        assertThat(insertResponse.deleted).isEmpty()
        assertThat(insertResponse.inserted).hasSize(1)
        val attendances =
            mobileRealtimeStaffAttendanceController.getAttendancesByUnit(
                dbInstance(),
                mobileUser,
                MockEvakaClock(now),
                testDaycare.id,
                date = null,
            )
        assertThat(attendances.staff).hasSize(1)
        assertThat(attendances.staff.first().attendances).hasSize(1)
        attendances.staff.first().attendances.first().also {
            assertEquals(
                occupancyCoefficientSeven.stripTrailingZeros(),
                it.occupancyCoefficient.stripTrailingZeros(),
            )
            assertEquals(now, it.arrived)
            assertEquals(null, it.departed)
            assertEquals(StaffAttendanceType.PRESENT, it.type)
            assertEquals(groupId, it.groupId)
        }

        val staffAttendanceId = insertResponse.inserted.first()

        val updateResponse =
            mobileRealtimeStaffAttendanceController.setAttendances(
                dbInstance(),
                mobileUser,
                MockEvakaClock(now),
                testDaycare.id,
                MobileRealtimeStaffAttendanceController.StaffAttendanceUpdateRequest(
                    employeeId = employeeId,
                    pinCode = "1122",
                    date = now.toLocalDate(),
                    rows =
                        listOf(
                            RealtimeStaffAttendanceController.StaffAttendanceUpsert(
                                id = staffAttendanceId,
                                groupId = groupId,
                                arrived = now.minusHours(1),
                                departed = now,
                                type = StaffAttendanceType.JUSTIFIED_CHANGE,
                                hasStaffOccupancyEffect = false,
                            )
                        ),
                ),
            )
        assertThat(updateResponse.deleted).containsExactly(staffAttendanceId)
        assertThat(updateResponse.inserted).hasSize(1)
        val attendances2 =
            mobileRealtimeStaffAttendanceController.getAttendancesByUnit(
                dbInstance(),
                mobileUser,
                MockEvakaClock(now),
                testDaycare.id,
                date = null,
            )
        assertThat(attendances2.staff).hasSize(1)
        assertThat(attendances2.staff.first().attendances).hasSize(1)
        attendances2.staff.first().attendances.first().also {
            assertEquals(occupancyCoefficientZero, it.occupancyCoefficient.stripTrailingZeros())
            assertEquals(now.minusHours(1), it.arrived)
            assertEquals(now, it.departed)
            assertEquals(StaffAttendanceType.JUSTIFIED_CHANGE, it.type)
            assertEquals(groupId, it.groupId)
        }

        val deleteResponse =
            mobileRealtimeStaffAttendanceController.setAttendances(
                dbInstance(),
                mobileUser,
                MockEvakaClock(now),
                testDaycare.id,
                MobileRealtimeStaffAttendanceController.StaffAttendanceUpdateRequest(
                    employeeId = employeeId,
                    pinCode = "1122",
                    date = now.toLocalDate(),
                    rows = emptyList(),
                ),
            )
        assertThat(deleteResponse.deleted).hasSize(1)
        assertThat(deleteResponse.inserted).isEmpty()
    }

    @Test
    fun `set attendances with overlapping times`() {
        val employeeId = addEmployee()
        val staffAttendance1Id =
            db.transaction { tx ->
                tx.insert(
                    DevStaffAttendance(
                        id = StaffAttendanceRealtimeId(UUID.randomUUID()),
                        employeeId = employeeId,
                        groupId = groupId,
                        arrived = HelsinkiDateTime.of(today, LocalTime.of(8, 0)),
                        departed = HelsinkiDateTime.of(today, LocalTime.of(12, 0)),
                        occupancyCoefficient = occupancyCoefficientSeven,
                        type = StaffAttendanceType.PRESENT,
                        departedAutomatically = false,
                    )
                )
            }
        val staffAttendance2Id =
            db.transaction { tx ->
                tx.insert(
                    DevStaffAttendance(
                        id = StaffAttendanceRealtimeId(UUID.randomUUID()),
                        employeeId = employeeId,
                        groupId = groupId,
                        arrived = HelsinkiDateTime.of(today, LocalTime.of(14, 0)),
                        departed = HelsinkiDateTime.of(today, LocalTime.of(18, 0)),
                        occupancyCoefficient = occupancyCoefficientSeven,
                        type = StaffAttendanceType.PRESENT,
                        departedAutomatically = false,
                    )
                )
            }

        val response =
            mobileRealtimeStaffAttendanceController.setAttendances(
                dbInstance(),
                mobileUser,
                MockEvakaClock(now),
                testDaycare.id,
                MobileRealtimeStaffAttendanceController.StaffAttendanceUpdateRequest(
                    employeeId = employeeId,
                    pinCode = "1122",
                    date = now.toLocalDate(),
                    rows =
                        listOf(
                            RealtimeStaffAttendanceController.StaffAttendanceUpsert(
                                id = staffAttendance1Id,
                                groupId = groupId,
                                arrived = HelsinkiDateTime.of(today, LocalTime.of(8, 0)),
                                departed = HelsinkiDateTime.of(today, LocalTime.of(15, 0)),
                                type = StaffAttendanceType.PRESENT,
                                hasStaffOccupancyEffect = false,
                            ),
                            RealtimeStaffAttendanceController.StaffAttendanceUpsert(
                                id = staffAttendance2Id,
                                groupId = groupId,
                                arrived = HelsinkiDateTime.of(today, LocalTime.of(16, 0)),
                                departed = HelsinkiDateTime.of(today, LocalTime.of(18, 0)),
                                type = StaffAttendanceType.PRESENT,
                                hasStaffOccupancyEffect = true,
                            ),
                        ),
                ),
            )
        assertThat(response.deleted)
            .containsExactlyInAnyOrder(staffAttendance1Id, staffAttendance2Id)
        assertThat(response.inserted).hasSize(2)
    }

    @Test
    fun `set attendances without group id works`() {
        val employeeId = addEmployee()
        val body =
            MobileRealtimeStaffAttendanceController.StaffAttendanceUpdateRequest(
                employeeId = employeeId,
                pinCode = "1122",
                date = now.toLocalDate(),
                rows =
                    listOf(
                        RealtimeStaffAttendanceController.StaffAttendanceUpsert(
                            id = null,
                            groupId = null,
                            arrived = now,
                            departed = null,
                            type = StaffAttendanceType.TRAINING,
                            hasStaffOccupancyEffect = false,
                        )
                    ),
            )

        val response =
            mobileRealtimeStaffAttendanceController.setAttendances(
                dbInstance(),
                mobileUser,
                MockEvakaClock(now),
                testDaycare.id,
                body,
            )

        assertThat(response.deleted).isEmpty()
        assertThat(response.inserted).hasSize(1)
    }

    @Test
    fun `set attendances for other unit doesn't remove attendances`() {
        val employeeId =
            db.transaction { tx ->
                val employeeId = tx.insert(DevEmployee())
                tx.insertDaycareAclRow(
                    daycareId = testDaycare.id,
                    employeeId = employeeId,
                    UserRole.STAFF,
                )
                tx.insert(DevDaycareGroupAcl(groupId = groupId, employeeId = employeeId))
                tx.insertDaycareAclRow(
                    daycareId = testDaycare2.id,
                    employeeId = employeeId,
                    UserRole.STAFF,
                )
                tx.insert(DevDaycareGroupAcl(groupId = groupId2, employeeId = employeeId))
                tx.insert(DevEmployeePin(userId = employeeId, pin = "1122"))
                employeeId
            }

        val response1 =
            mobileRealtimeStaffAttendanceController.setAttendances(
                dbInstance(),
                mobileUser,
                MockEvakaClock(now),
                testDaycare.id,
                MobileRealtimeStaffAttendanceController.StaffAttendanceUpdateRequest(
                    employeeId = employeeId,
                    pinCode = "1122",
                    date = now.toLocalDate(),
                    rows =
                        listOf(
                            RealtimeStaffAttendanceController.StaffAttendanceUpsert(
                                id = null,
                                groupId = groupId,
                                arrived = now.minusHours(2),
                                departed = now,
                                type = StaffAttendanceType.PRESENT,
                                hasStaffOccupancyEffect = true,
                            )
                        ),
                ),
            )
        assertThat(response1.deleted).isEmpty()
        assertThat(response1.inserted).hasSize(1)

        val response2 =
            mobileRealtimeStaffAttendanceController.setAttendances(
                dbInstance(),
                mobileUser2,
                MockEvakaClock(now),
                testDaycare2.id,
                MobileRealtimeStaffAttendanceController.StaffAttendanceUpdateRequest(
                    employeeId = employeeId,
                    pinCode = "1122",
                    date = now.toLocalDate(),
                    rows =
                        listOf(
                            RealtimeStaffAttendanceController.StaffAttendanceUpsert(
                                id = null,
                                groupId = groupId2,
                                arrived = now,
                                departed = now.plusHours(2),
                                type = StaffAttendanceType.PRESENT,
                                hasStaffOccupancyEffect = false,
                            )
                        ),
                ),
            )
        assertThat(response2.deleted).isEmpty()
        assertThat(response2.inserted).hasSize(1)
    }

    @Test
    fun `set attendances with another unit's mobile device throws forbidden`() {
        val employeeId = addEmployee()
        val body =
            MobileRealtimeStaffAttendanceController.StaffAttendanceUpdateRequest(
                employeeId = employeeId,
                pinCode = "1122",
                date = now.toLocalDate(),
                rows =
                    listOf(
                        RealtimeStaffAttendanceController.StaffAttendanceUpsert(
                            id = StaffAttendanceRealtimeId(UUID.randomUUID()),
                            groupId = groupId,
                            arrived = now,
                            departed = null,
                            type = StaffAttendanceType.PRESENT,
                            hasStaffOccupancyEffect = true,
                        )
                    ),
            )

        val exception =
            assertThrows<Forbidden> {
                mobileRealtimeStaffAttendanceController.setAttendances(
                    dbInstance(),
                    mobileUser2,
                    MockEvakaClock(now),
                    testDaycare.id,
                    body,
                )
            }
        assertEquals("Permission denied", exception.message)
    }

    @Test
    fun `set attendances with wrong pin code throws forbidden`() {
        val employeeId = addEmployee()
        val body =
            MobileRealtimeStaffAttendanceController.StaffAttendanceUpdateRequest(
                employeeId = employeeId,
                pinCode = "3344",
                date = now.toLocalDate(),
                rows =
                    listOf(
                        RealtimeStaffAttendanceController.StaffAttendanceUpsert(
                            id = StaffAttendanceRealtimeId(UUID.randomUUID()),
                            groupId = groupId,
                            arrived = now,
                            departed = null,
                            type = StaffAttendanceType.PRESENT,
                            hasStaffOccupancyEffect = true,
                        )
                    ),
            )

        (0..5).forEach { attempt ->
            val exception =
                assertThrows<Forbidden> {
                    mobileRealtimeStaffAttendanceController.setAttendances(
                        dbInstance(),
                        mobileUser,
                        MockEvakaClock(now),
                        testDaycare.id,
                        body,
                    )
                }
            assertEquals("Invalid pin code", exception.message)

            // Pin is locked after 5 attempts
            if (attempt < 5) {
                assertEquals("WRONG_PIN", exception.errorCode)
            } else {
                assertEquals("PIN_LOCKED", exception.errorCode)
            }
        }
    }

    @Test
    fun `set attendances with attendance outside given date throws bad request`() {
        val employeeId = addEmployee()
        val body =
            MobileRealtimeStaffAttendanceController.StaffAttendanceUpdateRequest(
                employeeId = employeeId,
                pinCode = "1122",
                date = now.toLocalDate(),
                rows =
                    listOf(
                        RealtimeStaffAttendanceController.StaffAttendanceUpsert(
                            id = StaffAttendanceRealtimeId(UUID.randomUUID()),
                            groupId = groupId,
                            arrived = now.plusDays(1),
                            departed = null,
                            type = StaffAttendanceType.PRESENT,
                            hasStaffOccupancyEffect = true,
                        )
                    ),
            )

        val expection =
            assertThrows<BadRequest> {
                mobileRealtimeStaffAttendanceController.setAttendances(
                    dbInstance(),
                    mobileUser,
                    MockEvakaClock(now),
                    testDaycare.id,
                    body,
                )
            }
        assertEquals("Attendances outside given date", expection.message)
    }

    @Test
    fun `set attendances with unknown attendance id throws bad request`() {
        val employeeId = addEmployee()
        val body =
            MobileRealtimeStaffAttendanceController.StaffAttendanceUpdateRequest(
                employeeId = employeeId,
                pinCode = "1122",
                date = now.toLocalDate(),
                rows =
                    listOf(
                        RealtimeStaffAttendanceController.StaffAttendanceUpsert(
                            id = StaffAttendanceRealtimeId(UUID.randomUUID()),
                            groupId = groupId,
                            arrived = now,
                            departed = null,
                            type = StaffAttendanceType.PRESENT,
                            hasStaffOccupancyEffect = true,
                        )
                    ),
            )

        val exception =
            assertThrows<BadRequest> {
                mobileRealtimeStaffAttendanceController.setAttendances(
                    dbInstance(),
                    mobileUser,
                    MockEvakaClock(now),
                    testDaycare.id,
                    body,
                )
            }
        assertEquals("Unknown id was given", exception.message)
    }

    private fun fetchRealtimeStaffAttendances(
        unitId: DaycareId,
        user: AuthenticatedUser.MobileDevice,
    ): CurrentDayStaffAttendanceResponse {
        return mobileRealtimeStaffAttendanceController.getAttendancesByUnit(
            dbInstance(),
            user,
            MockEvakaClock(now),
            unitId,
            date = null,
        )
    }

    private fun markArrival(
        now: HelsinkiDateTime,
        employeeId: EmployeeId,
        pinCode: String,
        groupId: GroupId,
        time: LocalTime,
        type: StaffAttendanceType?,
        user: AuthenticatedUser.MobileDevice = mobileUser,
        hasStaffOccupancyEffect: Boolean = true,
    ) {
        mobileRealtimeStaffAttendanceController.markArrival(
            dbInstance(),
            user,
            MockEvakaClock(now),
            MobileRealtimeStaffAttendanceController.StaffArrivalRequest(
                employeeId = employeeId,
                pinCode = pinCode,
                groupId = groupId,
                time = time,
                type = type,
                hasStaffOccupancyEffect = hasStaffOccupancyEffect,
            ),
        )
    }

    private fun markDeparture(
        now: HelsinkiDateTime,
        employeeId: EmployeeId,
        pinCode: String,
        groupId: GroupId,
        time: LocalTime,
        type: StaffAttendanceType?,
        user: AuthenticatedUser.MobileDevice = mobileUser,
    ) {
        mobileRealtimeStaffAttendanceController.markDeparture(
            dbInstance(),
            user,
            MockEvakaClock(now),
            MobileRealtimeStaffAttendanceController.StaffDepartureRequest(
                employeeId = employeeId,
                pinCode = pinCode,
                groupId = groupId,
                time = time,
                type = type,
            ),
        )
    }
}

private fun Database.Read.getEmployeeLastLogin(id: EmployeeId) =
    createQuery { sql("SELECT last_login FROM employee WHERE id = ${bind(id)}") }
        .exactlyOneOrNull<HelsinkiDateTime>()
