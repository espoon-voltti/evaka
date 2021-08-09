package fi.espoo.evaka.pis

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InactiveEmployeesRoleResetIntegrationTest : PureJdbiTest() {
    private val firstOfAugust2021 = HelsinkiDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(3, 15)).toInstant()

    @BeforeEach
    fun beforeEach() {
        db.transaction {
            it.resetDatabase()
        }
    }

    @Test
    fun `global roles are not reset when last_login is now`() {
        val employeeId = db.transaction {
            it.insertTestEmployee(DevEmployee(lastLogin = firstOfAugust2021, roles = setOf(UserRole.ADMIN)))
        }

        db.transaction { it.clearRolesForInactiveEmployees(firstOfAugust2021) }

        val globalRoles = db.read { it.getEmployeeWithRoles(employeeId) }!!.globalRoles
        assertEquals(setOf(UserRole.ADMIN), globalRoles.toSet())
    }

    @Test
    fun `global roles are not reset when last_login is null and now is before first of Sep`() {
        val employeeId = db.transaction {
            it.insertTestEmployee(DevEmployee(lastLogin = null, roles = setOf(UserRole.ADMIN)))
        }

        db.transaction { it.clearRolesForInactiveEmployees(firstOfAugust2021) }

        val globalRoles = db.read { it.getEmployeeWithRoles(employeeId) }!!.globalRoles
        assertEquals(setOf(UserRole.ADMIN), globalRoles.toSet())
    }

    @Test
    fun `global roles are reset when last_login is over 3 months ago`() {
        val employeeId = db.transaction {
            it.insertTestEmployee(
                DevEmployee(
                    lastLogin = firstOfAugust2021.minus(31L + 30 + 31 + 1, ChronoUnit.DAYS),
                    roles = setOf(UserRole.ADMIN)
                )
            )
        }

        db.transaction { it.clearRolesForInactiveEmployees(firstOfAugust2021) }

        val globalRoles = db.read { it.getEmployeeWithRoles(employeeId) }!!.globalRoles
        assertTrue(globalRoles.isEmpty())
    }

    @Test
    fun `global roles are reset when last_login is null and now is after first of Sep`() {
        val employeeId = db.transaction {
            it.insertTestEmployee(DevEmployee(lastLogin = null, roles = setOf(UserRole.ADMIN)))
        }

        db.transaction { it.clearRolesForInactiveEmployees(firstOfAugust2021.plus(31, ChronoUnit.DAYS)) }

        val globalRoles = db.read { it.getEmployeeWithRoles(employeeId) }!!.globalRoles
        assertTrue(globalRoles.isEmpty())
    }

    @Test
    fun `scoped roles are not reset when last_login is now`() {
        val employeeId = db.transaction {
            val employeeId = it.insertTestEmployee(DevEmployee(lastLogin = firstOfAugust2021))
            val areaId = it.insertTestCareArea(DevCareArea())
            val unitId = it.insertTestDaycare(DevDaycare(areaId = areaId))
            it.insertDaycareAclRow(daycareId = unitId, employeeId = employeeId, role = UserRole.STAFF)
            employeeId
        }

        db.transaction { it.clearRolesForInactiveEmployees(firstOfAugust2021) }

        val scopedRoles = db.read { it.getEmployeeWithRoles(employeeId) }!!.daycareRoles.map { it.role }
        assertEquals(setOf(UserRole.STAFF), scopedRoles.toSet())
    }

    @Test
    fun `scoped roles are not reset when last_login is null and now is before first of Sep`() {
        val employeeId = db.transaction {
            val employeeId = it.insertTestEmployee(DevEmployee(lastLogin = null))
            val areaId = it.insertTestCareArea(DevCareArea())
            val unitId = it.insertTestDaycare(DevDaycare(areaId = areaId))
            it.insertDaycareAclRow(daycareId = unitId, employeeId = employeeId, role = UserRole.STAFF)
            employeeId
        }

        db.transaction { it.clearRolesForInactiveEmployees(firstOfAugust2021) }

        val scopedRoles = db.read { it.getEmployeeWithRoles(employeeId) }!!.daycareRoles.map { it.role }
        assertEquals(setOf(UserRole.STAFF), scopedRoles.toSet())
    }

    @Test
    fun `scoped roles are reset when last_login is over 3 months ago`() {
        val employeeId = db.transaction {
            val employeeId = it.insertTestEmployee(
                DevEmployee(lastLogin = firstOfAugust2021.minus(31L + 30 + 31 + 1, ChronoUnit.DAYS))
            )
            val areaId = it.insertTestCareArea(DevCareArea())
            val unitId = it.insertTestDaycare(DevDaycare(areaId = areaId))
            it.insertDaycareAclRow(daycareId = unitId, employeeId = employeeId, role = UserRole.STAFF)
            employeeId
        }

        db.transaction { it.clearRolesForInactiveEmployees(firstOfAugust2021) }

        val scopedRoles = db.read { it.getEmployeeWithRoles(employeeId) }!!.daycareRoles.map { it.role }
        assertTrue(scopedRoles.isEmpty())
    }

    @Test
    fun `scoped roles are reset when last_login is null and now is after first of Sep`() {
        val employeeId = db.transaction {
            val employeeId = it.insertTestEmployee(DevEmployee(lastLogin = null))
            val areaId = it.insertTestCareArea(DevCareArea())
            val unitId = it.insertTestDaycare(DevDaycare(areaId = areaId))
            it.insertDaycareAclRow(daycareId = unitId, employeeId = employeeId, role = UserRole.STAFF)
            employeeId
        }

        db.transaction { it.clearRolesForInactiveEmployees(firstOfAugust2021.plus(31, ChronoUnit.DAYS)) }

        val scopedRoles = db.read { it.getEmployeeWithRoles(employeeId) }!!.daycareRoles.map { it.role }
        assertTrue(scopedRoles.isEmpty())
    }
}
