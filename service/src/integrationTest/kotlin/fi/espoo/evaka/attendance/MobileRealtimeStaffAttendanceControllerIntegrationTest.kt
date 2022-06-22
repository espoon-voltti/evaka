// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import com.github.kittinunf.fuel.jackson.objectBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FixtureBuilder
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.createMobileDeviceToUnit
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.withMockedTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MobileRealtimeStaffAttendanceControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
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
            tx.insertGeneralTestFixtures()
            tx.insertTestDaycareGroup(DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = groupName))
            tx.insertTestDaycareGroup(DevDaycareGroup(id = groupId2, daycareId = testDaycare2.id, name = groupName2))

            tx.createMobileDeviceToUnit(mobileUser.id, testDaycare.id)
            tx.createMobileDeviceToUnit(mobileUser2.id, testDaycare2.id)
        }
    }

    @Test
    fun `Employee present in one daycare unit should not be present in another`() {
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withGroupAccess(testDaycare2.id, groupId2)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withScopedRole(UserRole.STAFF, testDaycare2.id)
                .saveAnd {
                    tx.markStaffArrival(
                        employeeId,
                        groupId,
                        HelsinkiDateTime.of(today, LocalTime.of(8, 0, 0)),
                        BigDecimal(7.0)
                    )
                }
        }

        val attnInDaycare1 = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        val attnInDaycare2 = fetchRealtimeStaffAttendances(testDaycare2.id, mobileUser2)

        assertNotNull(attnInDaycare1.staff.first().present)
        assertNull(attnInDaycare2.staff.first().present)
    }

    @Test
    fun `Employee with no planned attendances can be marked as arrived`() {
        val pinCode = "1212"
        lateinit var employeeId: EmployeeId
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withPinCode(pinCode)
                .saveAnd { employeeId = this.employeeId }
        }

        val arrivalTime = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        markArrival(employeeId, pinCode, groupId, arrivalTime.toLocalTime(), null)
            .let { assertEquals(200, it) }
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employeeId, it.employeeId)
            assertEquals(groupId, it.present)
            assertEquals(1, it.attendances.size)
            assertEquals(arrivalTime, it.attendances.first().arrived)
            assertEquals(null, it.attendances.first().departed)
            assertEquals(StaffAttendanceType.PRESENT, it.attendances.last().type)
        }
    }

    @Test
    fun `Employee with no planned attendances can be marked as departed`() {
        val pinCode = "1212"
        lateinit var employeeId: EmployeeId
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withPinCode(pinCode)
                .saveAnd { employeeId = this.employeeId }
        }

        val arrivalTime = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        markArrival(employeeId, pinCode, groupId, arrivalTime.toLocalTime(), null)
            .let { assertEquals(200, it) }
        val departureTime = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        markDeparture(employeeId, pinCode, groupId, departureTime.toLocalTime(), null)
            .let { assertEquals(200, it) }
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employeeId, it.employeeId)
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
        lateinit var employeeId: EmployeeId
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(16, 0))
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withPinCode(pinCode)
                .saveAnd {
                    employeeId = this.employeeId
                    addAttendancePlan().withTime(plannedStart, plannedEnd).save()
                }
        }

        markArrival(employeeId, pinCode, groupId, plannedStart.toLocalTime(), null)
            .let { assertEquals(200, it) }
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employeeId, it.employeeId)
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
        lateinit var employeeId: EmployeeId
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withPinCode(pinCode)
                .saveAnd {
                    employeeId = this.employeeId
                    addAttendancePlan().withTime(plannedStart, plannedEnd).save()
                }
        }

        markArrival(employeeId, pinCode, groupId, plannedStart.toLocalTime(), null)
            .let { assertEquals(200, it) }
        markDeparture(employeeId, pinCode, groupId, plannedEnd.toLocalTime(), null)
            .let { assertEquals(200, it) }
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employeeId, it.employeeId)
            assertEquals(null, it.present)
            assertEquals(1, it.attendances.size)
            assertEquals(plannedStart, it.attendances.first().arrived)
            assertEquals(plannedEnd, it.attendances.first().departed)
            assertEquals(StaffAttendanceType.PRESENT, it.attendances.last().type)
        }
    }

    @Test
    fun `Employee arriving within 15 minutes of planned time does not require a type`() {
        val pinCode = "1212"
        lateinit var employeeId: EmployeeId
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(16, 0))
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withPinCode(pinCode)
                .saveAnd {
                    employeeId = this.employeeId
                    addAttendancePlan().withTime(plannedStart, plannedEnd).save()
                }
        }

        val arrivalTime = plannedStart.plusMinutes(15)
        markArrival(employeeId, pinCode, groupId, arrivalTime.toLocalTime(), null)
            .let { assertEquals(200, it) }
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employeeId, it.employeeId)
            assertEquals(groupId, it.present)
            assertEquals(1, it.attendances.size)
            assertEquals(arrivalTime, it.attendances.first().arrived)
            assertEquals(null, it.attendances.first().departed)
            assertEquals(StaffAttendanceType.PRESENT, it.attendances.last().type)
        }
    }

    @Test
    fun `Employee departing within 15 minutes of planned time does not require a type`() {
        val pinCode = "1212"
        lateinit var employeeId: EmployeeId
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withPinCode(pinCode)
                .saveAnd {
                    employeeId = this.employeeId
                    addAttendancePlan().withTime(plannedStart, plannedEnd).save()
                }
        }

        val arrivalTime = plannedStart.minusMinutes(15)
        markArrival(employeeId, pinCode, groupId, arrivalTime.toLocalTime(), null)
            .let { assertEquals(200, it) }
        val departureTime = plannedEnd.plusMinutes(15)
        markDeparture(employeeId, pinCode, groupId, departureTime.toLocalTime(), null)
            .let { assertEquals(200, it) }
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employeeId, it.employeeId)
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
        lateinit var employeeId: EmployeeId
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(16, 0))
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withPinCode(pinCode)
                .saveAnd {
                    employeeId = this.employeeId
                    addAttendancePlan().withTime(plannedStart, plannedEnd).save()
                }
        }

        val arrivalTime = plannedStart.minusMinutes(20)
        markArrival(employeeId, pinCode, groupId, arrivalTime.toLocalTime(), null)
            .let { assertEquals(400, it) }
    }

    @ParameterizedTest(name = "Employee arriving 20 minutes before planned time can be marked arrived with {0}")
    @EnumSource(names = ["OVERTIME", "JUSTIFIED_CHANGE"])
    @Suppress("DEPRECATION")
    fun testStaffArrivalBeforePlanStartWithAllowedType(type: StaffAttendanceType) {
        val pinCode = "1212"
        lateinit var employeeId: EmployeeId
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(16, 0))
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withPinCode(pinCode)
                .saveAnd {
                    employeeId = this.employeeId
                    addAttendancePlan().withTime(plannedStart, plannedEnd).save()
                }
        }

        val arrivalTime = plannedStart.minusMinutes(20)
        markArrival(employeeId, pinCode, groupId, arrivalTime.toLocalTime(), type)
            .let { assertEquals(200, it) }
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employeeId, it.employeeId)
            assertEquals(groupId, it.present)
            assertEquals(1, it.attendances.size)
            assertEquals(arrivalTime, it.attendances.first().arrived)
            assertEquals(null, it.attendances.first().departed)
            assertEquals(type, it.attendances.first().type)
        }
    }

    @ParameterizedTest(name = "Employee arriving 20 minutes before planned time cannot be marked arrived with {0}")
    @EnumSource(names = ["PRESENT", "OTHER_WORK", "TRAINING"])
    @Suppress("DEPRECATION")
    fun testStaffArrivalBeforePlanStartWithUnallowedType(type: StaffAttendanceType) {
        val pinCode = "1212"
        lateinit var employeeId: EmployeeId
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(16, 0))
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withPinCode(pinCode)
                .saveAnd {
                    employeeId = this.employeeId
                    addAttendancePlan().withTime(plannedStart, plannedEnd).save()
                }
        }

        val arrivalTime = plannedStart.minusMinutes(20)
        markArrival(employeeId, pinCode, groupId, arrivalTime.toLocalTime(), type)
            .let { assertEquals(400, it) }
    }

    @Test
    fun `Employee arriving 20 minutes after planned time requires a type`() {
        val pinCode = "1212"
        lateinit var employeeId: EmployeeId
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(16, 0))
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withPinCode(pinCode)
                .saveAnd {
                    employeeId = this.employeeId
                    addAttendancePlan().withTime(plannedStart, plannedEnd).save()
                }
        }

        val arrivalTime = plannedStart.plusMinutes(20)
        markArrival(employeeId, pinCode, groupId, arrivalTime.toLocalTime(), null)
            .let { assertEquals(400, it) }
    }

    @Test
    fun `Employee arriving 20 minutes after planned time can be marked arrived with JUSTIFIED_CHANGE`() {
        val pinCode = "1212"
        lateinit var employeeId: EmployeeId
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(16, 0))
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withPinCode(pinCode)
                .saveAnd {
                    employeeId = this.employeeId
                    addAttendancePlan().withTime(plannedStart, plannedEnd).save()
                }
        }

        val arrivalTime = plannedStart.plusMinutes(20)
        markArrival(employeeId, pinCode, groupId, arrivalTime.toLocalTime(), StaffAttendanceType.JUSTIFIED_CHANGE)
            .let { assertEquals(200, it) }
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employeeId, it.employeeId)
            assertEquals(groupId, it.present)
            assertEquals(1, it.attendances.size)
            assertEquals(arrivalTime, it.attendances.first().arrived)
            assertEquals(null, it.attendances.first().departed)
            assertEquals(StaffAttendanceType.JUSTIFIED_CHANGE, it.attendances.first().type)
        }
    }

    @ParameterizedTest(name = "Employee arriving 20 minutes after planned time can be marked arrived with {0}")
    @EnumSource(names = ["OTHER_WORK", "TRAINING"])
    @Suppress("DEPRECATION")
    fun testStaffArrivalAfterPlanStartWithAllowedType(type: StaffAttendanceType) {
        val pinCode = "1212"
        lateinit var employeeId: EmployeeId
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(16, 0))
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withPinCode(pinCode)
                .saveAnd {
                    employeeId = this.employeeId
                    addAttendancePlan().withTime(plannedStart, plannedEnd).save()
                }
        }

        val arrivalTime = plannedStart.plusMinutes(20)
        markArrival(employeeId, pinCode, groupId, arrivalTime.toLocalTime(), type)
            .let { assertEquals(200, it) }
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employeeId, it.employeeId)
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

    @ParameterizedTest(name = "Employee arriving 20 minutes after planned time cannot be marked arrived with {0}")
    @EnumSource(names = ["PRESENT", "OVERTIME"])
    @Suppress("DEPRECATION")
    fun testStaffArrivalAfterPlanStartWithUnallowedType(type: StaffAttendanceType) {
        val pinCode = "1212"
        lateinit var employeeId: EmployeeId
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(16, 0))
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withPinCode(pinCode)
                .saveAnd {
                    employeeId = this.employeeId
                    addAttendancePlan().withTime(plannedStart, plannedEnd).save()
                }
        }

        val arrivalTime = plannedStart.plusMinutes(20)
        markArrival(employeeId, pinCode, groupId, arrivalTime.toLocalTime(), type)
            .let { assertEquals(400, it) }
    }

    @Test
    fun `Employee departing 20 minutes before planned time requires a type`() {
        val pinCode = "1212"
        lateinit var employeeId: EmployeeId
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withPinCode(pinCode)
                .saveAnd {
                    employeeId = this.employeeId
                    addAttendancePlan().withTime(plannedStart, plannedEnd).save()
                }
        }

        val arrivalTime = plannedStart
        markArrival(employeeId, pinCode, groupId, arrivalTime.toLocalTime(), null)
            .let { assertEquals(200, it) }
        val departureTime = plannedEnd.minusMinutes(20)
        markDeparture(employeeId, pinCode, groupId, departureTime.toLocalTime(), null)
            .let { assertEquals(400, it) }
    }

    @Test
    fun `Employee departing 20 minutes before planned time can be marked departed with JUSTIFIED_CHANGE`() {
        val pinCode = "1212"
        lateinit var employeeId: EmployeeId
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withPinCode(pinCode)
                .saveAnd {
                    employeeId = this.employeeId
                    addAttendancePlan().withTime(plannedStart, plannedEnd).save()
                }
        }

        val arrivalTime = plannedStart
        markArrival(employeeId, pinCode, groupId, arrivalTime.toLocalTime(), null)
            .let { assertEquals(200, it) }
        val departureTime = plannedEnd.minusMinutes(20)
        markDeparture(employeeId, pinCode, groupId, departureTime.toLocalTime(), StaffAttendanceType.JUSTIFIED_CHANGE)
            .let { assertEquals(200, it) }
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employeeId, it.employeeId)
            assertEquals(null, it.present)
            assertEquals(1, it.attendances.size)
            assertEquals(arrivalTime, it.attendances.first().arrived)
            assertEquals(departureTime, it.attendances.first().departed)
            assertEquals(StaffAttendanceType.JUSTIFIED_CHANGE, it.attendances.first().type)
        }
    }

    @ParameterizedTest(name = "Employee departing 20 minutes before planned time can be marked departed with {0}")
    @EnumSource(names = ["OTHER_WORK", "TRAINING"])
    @Suppress("DEPRECATION")
    fun testStaffDepartureBeforePlanStartWithAllowedType(type: StaffAttendanceType) {
        val pinCode = "1212"
        lateinit var employeeId: EmployeeId
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withPinCode(pinCode)
                .saveAnd {
                    employeeId = this.employeeId
                    addAttendancePlan().withTime(plannedStart, plannedEnd).save()
                }
        }

        val arrivalTime = plannedStart
        markArrival(employeeId, pinCode, groupId, arrivalTime.toLocalTime(), null)
            .let { assertEquals(200, it) }
        val departureTime = plannedEnd.minusMinutes(20)
        markDeparture(employeeId, pinCode, groupId, departureTime.toLocalTime(), type)
            .let { assertEquals(200, it) }
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employeeId, it.employeeId)
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

    @ParameterizedTest(name = "Employee departing 20 minutes before planned time cannot be marked departed with {0}")
    @EnumSource(names = ["PRESENT", "OVERTIME"])
    @Suppress("DEPRECATION")
    fun testStaffDepartureBeforePlanStartWithUnallowedType(type: StaffAttendanceType) {
        val pinCode = "1212"
        lateinit var employeeId: EmployeeId
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withPinCode(pinCode)
                .saveAnd {
                    employeeId = this.employeeId
                    addAttendancePlan().withTime(plannedStart, plannedEnd).save()
                }
        }

        val arrivalTime = plannedStart
        markArrival(employeeId, pinCode, groupId, arrivalTime.toLocalTime(), null)
            .let { assertEquals(200, it) }
        val departureTime = plannedEnd.minusMinutes(20)
        markDeparture(employeeId, pinCode, groupId, departureTime.toLocalTime(), type)
            .let { assertEquals(400, it) }
    }

    @Test
    fun `Employee departing 20 minutes after planned time requires a type`() {
        val pinCode = "1212"
        lateinit var employeeId: EmployeeId
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withPinCode(pinCode)
                .saveAnd {
                    employeeId = this.employeeId
                    addAttendancePlan().withTime(plannedStart, plannedEnd).save()
                }
        }

        val arrivalTime = plannedStart
        markArrival(employeeId, pinCode, groupId, arrivalTime.toLocalTime(), null)
            .let { assertEquals(200, it) }
        val departureTime = plannedEnd.plusMinutes(20)
        markDeparture(employeeId, pinCode, groupId, departureTime.toLocalTime(), null)
            .let { assertEquals(400, it) }
    }

    @ParameterizedTest(name = "Employee departing 20 minutes after planned time can be marked departed with {0}")
    @EnumSource(names = ["OVERTIME", "JUSTIFIED_CHANGE"])
    @Suppress("DEPRECATION")
    fun testStaffDepartureAfterPlanStartWithAllowedType(type: StaffAttendanceType) {
        val pinCode = "1212"
        lateinit var employeeId: EmployeeId
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withPinCode(pinCode)
                .saveAnd {
                    employeeId = this.employeeId
                    addAttendancePlan().withTime(plannedStart, plannedEnd).save()
                }
        }

        val arrivalTime = plannedStart
        markArrival(employeeId, pinCode, groupId, arrivalTime.toLocalTime(), null)
            .let { assertEquals(200, it) }
        val departureTime = plannedEnd.plusMinutes(20)
        markDeparture(employeeId, pinCode, groupId, departureTime.toLocalTime(), type)
            .let { assertEquals(200, it) }
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employeeId, it.employeeId)
            assertEquals(null, it.present)
            assertEquals(1, it.attendances.size)
            assertEquals(arrivalTime, it.attendances.first().arrived)
            assertEquals(departureTime, it.attendances.first().departed)
            assertEquals(type, it.attendances.first().type)
        }
    }

    @ParameterizedTest(name = "Employee departing 20 minutes after planned time cannot be marked departed with {0}")
    @EnumSource(names = ["PRESENT", "OTHER_WORK", "TRAINING"])
    @Suppress("DEPRECATION")
    fun testStaffDepartureAfterPlanStartWithUnallowedType(type: StaffAttendanceType) {
        val pinCode = "1212"
        lateinit var employeeId: EmployeeId
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withPinCode(pinCode)
                .saveAnd {
                    employeeId = this.employeeId
                    addAttendancePlan().withTime(plannedStart, plannedEnd).save()
                }
        }

        val arrivalTime = plannedStart
        markArrival(employeeId, pinCode, groupId, arrivalTime.toLocalTime(), null)
            .let { assertEquals(200, it) }
        val departureTime = plannedEnd.plusMinutes(20)
        markDeparture(employeeId, pinCode, groupId, departureTime.toLocalTime(), type)
            .let { assertEquals(400, it) }
    }

    @Test
    fun `Employee arriving and departing multiple times during same work day`() {
        val pinCode = "1212"
        lateinit var employeeId: EmployeeId
        val plannedStart = HelsinkiDateTime.of(today, LocalTime.of(8, 0))
        val plannedEnd = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
        db.transaction { tx ->
            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withPinCode(pinCode)
                .saveAnd {
                    employeeId = this.employeeId
                    addAttendancePlan().withTime(plannedStart, plannedEnd).save()
                }
        }

        val firstArrivalTime = plannedStart.minusMinutes(20)
        markArrival(employeeId, pinCode, groupId, firstArrivalTime.toLocalTime(), StaffAttendanceType.OVERTIME)
            .let { assertEquals(200, it) }
        val firstDepartureTime = firstArrivalTime.plusHours(1)
        markDeparture(employeeId, pinCode, groupId, firstDepartureTime.toLocalTime(), StaffAttendanceType.OTHER_WORK)
            .let { assertEquals(200, it) }
        val secondArrivalTime = firstDepartureTime.plusHours(1)
        markArrival(employeeId, pinCode, groupId, secondArrivalTime.toLocalTime(), StaffAttendanceType.OTHER_WORK)
            .let { assertEquals(200, it) }
        val secondDepartureTime = secondArrivalTime.plusHours(1)
        markDeparture(employeeId, pinCode, groupId, secondDepartureTime.toLocalTime(), StaffAttendanceType.TRAINING)
            .let { assertEquals(200, it) }
        val thirdArrivalTime = secondDepartureTime.plusHours(1)
        markArrival(employeeId, pinCode, groupId, thirdArrivalTime.toLocalTime(), StaffAttendanceType.TRAINING)
            .let { assertEquals(200, it) }
        val thirdDepartureTime = plannedEnd.minusMinutes(15)
        markDeparture(employeeId, pinCode, groupId, thirdDepartureTime.toLocalTime(), null)
            .let { assertEquals(200, it) }
        val attendances = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        attendances.staff.first().let {
            assertEquals(employeeId, it.employeeId)
            assertEquals(null, it.present)
            assertEquals(5, it.attendances.size)
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
        }
    }

    private fun fetchRealtimeStaffAttendances(unitId: DaycareId, user: AuthenticatedUser.MobileDevice): CurrentDayStaffAttendanceResponse {
        val (_, res, result) = http.get(
            "/mobile/realtime-staff-attendances",
            listOf(Pair("unitId", unitId))
        )
            .asUser(user)
            .withMockedTime(now)
            .responseObject<CurrentDayStaffAttendanceResponse>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun markArrival(
        employeeId: EmployeeId,
        pinCode: String,
        groupId: GroupId,
        time: LocalTime,
        type: StaffAttendanceType?,
        user: AuthenticatedUser.MobileDevice = mobileUser
    ): Int {
        val body = MobileRealtimeStaffAttendanceController.StaffArrivalRequest(
            employeeId = employeeId,
            pinCode = pinCode,
            groupId = groupId,
            time = time,
            type = type
        )
        val (_, res, _) = http.post("/mobile/realtime-staff-attendances/arrival")
            .asUser(user)
            .withMockedTime(now)
            .objectBody(body, mapper = jsonMapper)
            .response()
        return res.statusCode
    }

    private fun markDeparture(
        employeeId: EmployeeId,
        pinCode: String,
        groupId: GroupId,
        time: LocalTime,
        type: StaffAttendanceType?,
        user: AuthenticatedUser.MobileDevice = mobileUser
    ): Int {
        val body = MobileRealtimeStaffAttendanceController.StaffDepartureRequest(
            employeeId = employeeId,
            pinCode = pinCode,
            groupId = groupId,
            time = time,
            type = type
        )
        val (_, res, _) = http.post("/mobile/realtime-staff-attendances/departure")
            .asUser(user)
            .withMockedTime(now)
            .objectBody(body, mapper = jsonMapper)
            .response()
        return res.statusCode
    }
}
