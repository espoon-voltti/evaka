// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis

import evaka.core.FullApplicationTest
import evaka.core.identity.ExternalId
import evaka.core.pairing.MobileDeviceIdentity
import evaka.core.shared.EmployeeId
import evaka.core.shared.MobileDeviceId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevEmployeePin
import evaka.core.shared.dev.DevMobileDevice
import evaka.core.shared.dev.DevPersonalMobileDevice
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.MockEvakaClock
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class SystemControllerTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var systemController: SystemController

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val user = AuthenticatedUser.SystemInternalUser
    private val clock = MockEvakaClock(2024, 10, 15, 10, 0)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
        }
    }

    @Test
    fun `employee login works without employee number`() {
        val externalId = ExternalId.of("evaka", "123456")
        val employeeId = EmployeeId(UUID.randomUUID())
        val input =
            SystemController.EmployeeLoginRequest(
                externalId = externalId,
                firstName = "Teppo",
                lastName = "Testaaja",
                employeeNumber = null,
                email = null,
            )

        val result = systemController.employeeLogin(dbInstance(), user, clock, input)

        val expected =
            EmployeeUser(
                id = employeeId,
                firstName = "Teppo",
                lastName = "Testaaja",
                globalRoles = setOf(),
                allScopedRoles = setOf(),
                active = true,
            )
        assertEquals(expected, result.copy(id = employeeId))
        assertNull(db.read { tx -> tx.getEmployeeNumber(result.id) })
    }

    @Test
    fun `employee number is set for new employee`() {
        val externalId = ExternalId.of("evaka", "123456")
        val employeeId = EmployeeId(UUID.randomUUID())
        val input =
            SystemController.EmployeeLoginRequest(
                externalId = externalId,
                employeeNumber = "666666",
                firstName = "Teppo",
                lastName = "Testaaja",
                email = null,
            )

        val result = systemController.employeeLogin(dbInstance(), user, clock, input)

        val expected =
            EmployeeUser(
                id = employeeId,
                firstName = "Teppo",
                lastName = "Testaaja",
                globalRoles = setOf(),
                allScopedRoles = setOf(),
                active = true,
            )
        assertEquals(expected, result.copy(id = employeeId))
        assertEquals("666666", db.read { tx -> tx.getEmployeeNumber(result.id) })
    }

    @Test
    fun `employee number is set for existing employee`() {
        val externalId = ExternalId.of("evaka", "123456")
        val employeeId = EmployeeId(UUID.randomUUID())
        val input =
            SystemController.EmployeeLoginRequest(
                externalId = externalId,
                employeeNumber = "666666",
                firstName = "Teppo",
                lastName = "Testaaja",
                email = null,
            )
        db.transaction { tx ->
            tx.insert(
                DevEmployee(
                    id = employeeId,
                    externalId = externalId,
                    roles = setOf(UserRole.FINANCE_ADMIN),
                    preferredFirstName = "Kutsumanimi",
                )
            )
        }

        val result = systemController.employeeLogin(dbInstance(), user, clock, input)

        val expected =
            EmployeeUser(
                id = employeeId,
                firstName = "Kutsumanimi",
                lastName = "Testaaja",
                globalRoles = setOf(UserRole.FINANCE_ADMIN),
                allScopedRoles = setOf(),
                active = true,
            )
        assertEquals(expected, result)
        assertEquals("666666", db.read { tx -> tx.getEmployeeNumber(result.id) })
    }

    @Test
    fun `external id is set when employee number already exists`() {
        val externalId = ExternalId.of("evaka", "123456")
        val employeeId = EmployeeId(UUID.randomUUID())
        val input =
            SystemController.EmployeeLoginRequest(
                externalId = externalId,
                employeeNumber = "666666",
                firstName = "Teppo",
                lastName = "Testaaja",
                email = null,
            )
        db.transaction { tx ->
            tx.insert(
                DevEmployee(
                    id = employeeId,
                    externalId = null,
                    employeeNumber = "666666",
                    roles = setOf(UserRole.FINANCE_ADMIN),
                    preferredFirstName = "Kutsumanimi",
                )
            )
        }

        val result = systemController.employeeLogin(dbInstance(), user, clock, input)

        val expected =
            EmployeeUser(
                id = employeeId,
                firstName = "Kutsumanimi",
                lastName = "Testaaja",
                globalRoles = setOf(UserRole.FINANCE_ADMIN),
                allScopedRoles = setOf(),
                active = true,
            )
        assertEquals(expected, result)
        assertEquals("666666", db.read { tx -> tx.getEmployeeNumber(result.id) })
    }

    @Test
    fun `external id is updated when employee number already exists`() {
        val employeeId = EmployeeId(UUID.randomUUID())
        val employeeNumber = "666666"

        val externalId1 = ExternalId.of("evaka", "1")
        val input1 =
            SystemController.EmployeeLoginRequest(
                externalId = externalId1,
                employeeNumber = employeeNumber,
                firstName = "Teppo",
                lastName = "Testaaja",
                email = null,
            )
        val result1 = systemController.employeeLogin(dbInstance(), user, clock, input1)
        val expected1 =
            EmployeeUser(
                id = employeeId,
                firstName = "Teppo",
                lastName = "Testaaja",
                globalRoles = setOf(),
                allScopedRoles = setOf(),
                active = true,
            )
        assertEquals(expected1, result1.copy(id = employeeId))
        assertEquals(employeeNumber, db.read { tx -> tx.getEmployeeNumber(result1.id) })
        assertNotNull(db.read { tx -> tx.getEmployeeByExternalId(externalId1) })

        val externalId2 = ExternalId.of("evaka", "2")
        val input2 =
            SystemController.EmployeeLoginRequest(
                externalId = externalId2,
                employeeNumber = employeeNumber,
                firstName = "Teppo",
                lastName = "Testaaja",
                email = null,
            )
        val result2 = systemController.employeeLogin(dbInstance(), user, clock, input2)
        val expected =
            EmployeeUser(
                id = employeeId,
                firstName = "Teppo",
                lastName = "Testaaja",
                globalRoles = setOf(),
                allScopedRoles = setOf(),
                active = true,
            )
        assertEquals(expected, result2.copy(id = employeeId))
        assertEquals(employeeNumber, db.read { tx -> tx.getEmployeeNumber(result2.id) })
        assertNotNull(db.read { tx -> tx.getEmployeeByExternalId(externalId2) })

        assertNull(db.read { tx -> tx.getEmployeeByExternalId(externalId1) })
    }

    @Test
    fun `employee login fails if employee number is updated after login without employee number and duplicate exists`() {
        val employeeId = EmployeeId(UUID.randomUUID())
        val employeeNumber = "666666"

        val externalId1 = ExternalId.of("evaka", "1")
        val input1 =
            SystemController.EmployeeLoginRequest(
                externalId = externalId1,
                employeeNumber = employeeNumber,
                firstName = "Teppo",
                lastName = "Testaaja",
                email = null,
            )
        val result1 = systemController.employeeLogin(dbInstance(), user, clock, input1)
        val expected1 =
            EmployeeUser(
                id = employeeId,
                firstName = "Teppo",
                lastName = "Testaaja",
                globalRoles = setOf(),
                allScopedRoles = setOf(),
                active = true,
            )
        assertEquals(expected1, result1.copy(id = employeeId))
        assertEquals(employeeNumber, db.read { tx -> tx.getEmployeeNumber(result1.id) })
        assertNotNull(db.read { tx -> tx.getEmployeeByExternalId(externalId1) })

        val externalId2 = ExternalId.of("evaka", "2")
        val input2 =
            SystemController.EmployeeLoginRequest(
                externalId = externalId2,
                employeeNumber = null,
                firstName = "Teppo",
                lastName = "Testaaja",
                email = null,
            )
        val result2 = systemController.employeeLogin(dbInstance(), user, clock, input2)
        val expected =
            EmployeeUser(
                id = employeeId,
                firstName = "Teppo",
                lastName = "Testaaja",
                globalRoles = setOf(),
                allScopedRoles = setOf(),
                active = true,
            )
        assertEquals(expected, result2.copy(id = employeeId))
        assertEquals(null, db.read { tx -> tx.getEmployeeNumber(result2.id) })
        assertNotNull(db.read { tx -> tx.getEmployeeByExternalId(externalId2) })

        val input3 =
            SystemController.EmployeeLoginRequest(
                externalId = externalId2,
                employeeNumber = employeeNumber,
                firstName = "Teppo",
                lastName = "Testaaja",
                email = null,
            )
        assertThrows<Exception> {
            systemController.employeeLogin(dbInstance(), user, clock, input3)
        }
    }

    @Test
    fun `mobile identity endpoint can find a device by its token`() {
        val token = UUID.randomUUID()
        val deviceId = db.transaction { it.insertTestDevice(longTermToken = token) }

        val result = systemController.mobileIdentity(dbInstance(), user, token)
        assertEquals(MobileDeviceIdentity(id = deviceId, longTermToken = token), result)
    }

    @Test
    fun `pin login succeeds for an employee of the device's unit`() {
        val employee = DevEmployee()
        val device = DevMobileDevice(unitId = daycare.id)
        db.transaction { tx ->
            tx.insert(employee, unitRoles = mapOf(daycare.id to UserRole.STAFF))
            tx.insert(DevEmployeePin(userId = employee.id, pin = "2580"))
            tx.insert(device)
        }

        assertEquals(
            SystemController.PinLoginStatus.SUCCESS,
            pinLogin(device.id, employee.id, "2580").status,
        )
    }

    @Test
    fun `pin login is rejected for an employee of another unit even with the correct pin`() {
        val otherDaycare = DevDaycare(areaId = area.id, name = "Other daycare")
        val employee = DevEmployee()
        val device = DevMobileDevice(unitId = daycare.id)
        db.transaction { tx ->
            tx.insert(otherDaycare)
            tx.insert(employee, unitRoles = mapOf(otherDaycare.id to UserRole.STAFF))
            tx.insert(DevEmployeePin(userId = employee.id, pin = "2580"))
            tx.insert(device)
        }

        assertEquals(
            SystemController.PinLoginStatus.WRONG_PIN,
            pinLogin(device.id, employee.id, "2580").status,
        )
    }

    @Test
    fun `failed pin logins from another unit's device do not lock the pin`() {
        val otherDaycare = DevDaycare(areaId = area.id, name = "Other daycare")
        val employee = DevEmployee()
        val ownDevice = DevMobileDevice(unitId = daycare.id)
        val otherDevice = DevMobileDevice(unitId = otherDaycare.id)
        db.transaction { tx ->
            tx.insert(otherDaycare)
            tx.insert(employee, unitRoles = mapOf(daycare.id to UserRole.STAFF))
            tx.insert(DevEmployeePin(userId = employee.id, pin = "2580"))
            tx.insert(ownDevice)
            tx.insert(otherDevice)
        }

        repeat(10) { pinLogin(otherDevice.id, employee.id, "9999") }

        assertEquals(
            SystemController.PinLoginStatus.SUCCESS,
            pinLogin(ownDevice.id, employee.id, "2580").status,
        )
    }

    @Test
    fun `pin login on a personal device is only allowed for the device owner`() {
        val owner = DevEmployee()
        val colleague = DevEmployee()
        val device = DevPersonalMobileDevice(employeeId = owner.id)
        db.transaction { tx ->
            tx.insert(owner, unitRoles = mapOf(daycare.id to UserRole.UNIT_SUPERVISOR))
            tx.insert(colleague, unitRoles = mapOf(daycare.id to UserRole.STAFF))
            tx.insert(DevEmployeePin(userId = owner.id, pin = "2580"))
            tx.insert(DevEmployeePin(userId = colleague.id, pin = "1470"))
            tx.insert(device)
        }

        assertEquals(
            SystemController.PinLoginStatus.WRONG_PIN,
            pinLogin(device.id, colleague.id, "1470").status,
        )
        assertEquals(
            SystemController.PinLoginStatus.SUCCESS,
            pinLogin(device.id, owner.id, "2580").status,
        )
    }

    private fun pinLogin(deviceId: MobileDeviceId, employeeId: EmployeeId, pin: String) =
        systemController.pinLogin(
            dbInstance(),
            user,
            clock,
            SystemController.PinLoginRequest(
                pin = pin,
                employeeId = employeeId,
                deviceId = deviceId,
            ),
        )

    private fun Database.Transaction.insertTestDevice(longTermToken: UUID? = null): MobileDeviceId {
        val id = MobileDeviceId(UUID.randomUUID())
        insert(DevMobileDevice(id = id, unitId = daycare.id, longTermToken = longTermToken))
        return id
    }
}
