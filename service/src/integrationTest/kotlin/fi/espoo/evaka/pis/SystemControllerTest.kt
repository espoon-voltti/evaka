// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.pairing.MobileDeviceIdentity
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevMobileDevice
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.MockEvakaClock
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
    private lateinit var areaId: AreaId
    private lateinit var unitId: DaycareId

    private val user = AuthenticatedUser.SystemInternalUser
    private val clock = MockEvakaClock(2024, 10, 15, 10, 0)

    @BeforeEach
    fun beforeEach() {
        areaId = db.transaction { it.insert(DevCareArea()) }
        unitId = db.transaction { it.insert(DevDaycare(areaId = areaId)) }
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

    private fun Database.Transaction.insertTestDevice(longTermToken: UUID? = null): MobileDeviceId {
        val id = MobileDeviceId(UUID.randomUUID())
        insert(DevMobileDevice(id = id, unitId = unitId, longTermToken = longTermToken))
        return id
    }
}
