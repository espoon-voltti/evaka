// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.messaging.upsertEmployeeMessageAccount
import fi.espoo.evaka.pis.DaycareGroupRole
import fi.espoo.evaka.pis.DaycareRole
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.NewEmployee
import fi.espoo.evaka.pis.controllers.EmployeeController
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.RealEvakaClock
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class EmployeeControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired lateinit var employeeController: EmployeeController

    private val clock = RealEvakaClock()

    @Test
    fun `no employees return empty list`() {
        val employees = getEmployees()
        assertEquals(listOf(), employees)
    }

    @Test
    fun `admin can add employee`() {
        val emp = createEmployee(requestFromEmployee(employee1))
        assertEquals(employee1.firstName, emp.firstName)
        assertEquals(employee1.lastName, emp.lastName)
        assertEquals(employee1.email, emp.email)
        assertEquals(employee1.externalId, emp.externalId)
    }

    @Test
    fun `admin gets all employees`() {
        createEmployee(requestFromEmployee(employee1))
        createEmployee(requestFromEmployee(employee2))
        val employees = getEmployees()
        assertEquals(2, employees.size)
        assertEquals(employee1.email, employees[0].email)
        assertEquals(employee2.email, employees[1].email)
    }

    @Test
    fun `admin can first create, then get employee`() {
        assertEquals(0, getEmployees().size)
        val responseCreate = createEmployee(requestFromEmployee(employee1))
        assertEquals(1, getEmployees().size)
        assertEquals(responseCreate.id, getEmployee(responseCreate.id).id)
    }

    @Test
    fun `admin can delete employee`() {
        val employee = createEmployee(requestFromEmployee(employee1))

        deleteEmployee(employee.id)

        assertEquals(0, getEmployees().size)
    }

    @Test
    fun `admin can update employee global roles`() {
        val employee = DevEmployee(roles = setOf(UserRole.SERVICE_WORKER))
        db.transaction { tx -> tx.insert(employee) }

        updateEmployeeGlobalRoles(employee.id, listOf(UserRole.FINANCE_ADMIN, UserRole.DIRECTOR))

        val updated = getEmployeeDetails(employee.id)
        assertEquals(setOf(UserRole.FINANCE_ADMIN, UserRole.DIRECTOR), updated.globalRoles.toSet())
    }

    @Test
    fun `admin can upsert employee daycare roles`() {
        val careArea = DevCareArea()
        val daycare1 = DevDaycare(areaId = careArea.id)
        val daycare2 = DevDaycare(areaId = careArea.id)
        val daycare3 = DevDaycare(areaId = careArea.id)
        val employee = DevEmployee()
        db.transaction { tx ->
            tx.insert(careArea)
            tx.insert(daycare1)
            tx.insert(daycare2)
            tx.insert(daycare3)
            tx.insert(
                employee,
                unitRoles = mapOf(daycare1.id to UserRole.STAFF, daycare2.id to UserRole.STAFF)
            )
        }

        upsertEmployeeDaycareRoles(
            employee.id,
            listOf(daycare2.id, daycare3.id),
            UserRole.SPECIAL_EDUCATION_TEACHER
        )

        assertEquals(
            setOf(
                DaycareRole(daycare1.id, daycare1.name, UserRole.STAFF),
                DaycareRole(daycare2.id, daycare2.name, UserRole.SPECIAL_EDUCATION_TEACHER),
                DaycareRole(daycare3.id, daycare3.name, UserRole.SPECIAL_EDUCATION_TEACHER),
            ),
            getEmployeeDetails(employee.id).daycareRoles.toSet()
        )
        db.read { assertTrue(it.hasActiveMessagingAccount(employee.id)) }
    }

    @Test
    fun `admin can delete employee daycare role`() {
        val careArea = DevCareArea()
        val daycare1 = DevDaycare(areaId = careArea.id)
        val daycare2 = DevDaycare(areaId = careArea.id)
        val daycare1Group = DevDaycareGroup(daycareId = daycare1.id)
        val daycare2Group = DevDaycareGroup(daycareId = daycare2.id)
        val employee = DevEmployee()
        db.transaction { tx ->
            tx.insert(careArea)
            tx.insert(daycare1)
            tx.insert(daycare2)
            tx.insert(daycare1Group)
            tx.insert(daycare2Group)
            tx.insert(
                employee,
                unitRoles = mapOf(daycare1.id to UserRole.STAFF, daycare2.id to UserRole.STAFF),
                groupAcl =
                    mapOf(
                        daycare1.id to listOf(daycare1Group.id),
                        daycare2.id to listOf(daycare2Group.id)
                    )
            )
            tx.upsertEmployeeMessageAccount(employee.id)
        }

        deleteEmployeeDaycareRoles(id = employee.id, daycareId = daycare1.id)

        val updated = getEmployeeDetails(employee.id)
        assertEquals(
            listOf(DaycareRole(daycare2.id, daycare2.name, UserRole.STAFF)),
            updated.daycareRoles
        )
        assertEquals(
            listOf(
                DaycareGroupRole(daycare2.id, daycare2.name, daycare2Group.id, daycare2Group.name)
            ),
            updated.daycareGroupRoles
        )
        db.read { assertTrue(it.hasActiveMessagingAccount(employee.id)) }
    }

    @Test
    fun `admin can delete employee all daycare roles`() {
        val careArea = DevCareArea()
        val daycare1 = DevDaycare(areaId = careArea.id)
        val daycare2 = DevDaycare(areaId = careArea.id)
        val daycare1Group = DevDaycareGroup(daycareId = daycare1.id)
        val daycare2Group = DevDaycareGroup(daycareId = daycare2.id)
        val employee = DevEmployee()
        db.transaction { tx ->
            tx.insert(careArea)
            tx.insert(daycare1)
            tx.insert(daycare2)
            tx.insert(daycare1Group)
            tx.insert(daycare2Group)
            tx.insert(
                employee,
                unitRoles = mapOf(daycare1.id to UserRole.STAFF, daycare2.id to UserRole.STAFF),
                groupAcl =
                    mapOf(
                        daycare1.id to listOf(daycare1Group.id),
                        daycare2.id to listOf(daycare2Group.id)
                    )
            )
            tx.upsertEmployeeMessageAccount(employee.id)
        }

        deleteEmployeeDaycareRoles(id = employee.id, daycareId = null)

        val updated = getEmployeeDetails(employee.id)
        assertEquals(emptyList(), updated.daycareRoles)
        assertEquals(emptyList(), updated.daycareGroupRoles)
        db.read { assertFalse(it.hasActiveMessagingAccount(employee.id)) }
    }

    val adminUser = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))

    fun getEmployees() = employeeController.getEmployees(dbInstance(), adminUser, clock)

    fun getEmployee(id: EmployeeId) =
        employeeController.getEmployee(dbInstance(), adminUser, clock, id)

    fun createEmployee(employee: NewEmployee) =
        employeeController.createEmployee(dbInstance(), adminUser, clock, employee)

    fun upsertEmployeeDaycareRoles(id: EmployeeId, daycareIds: List<DaycareId>, role: UserRole) =
        employeeController.upsertEmployeeDaycareRoles(
            dbInstance(),
            adminUser,
            clock,
            id,
            EmployeeController.UpsertEmployeeDaycareRolesRequest(daycareIds, role)
        )

    fun updateEmployeeGlobalRoles(id: EmployeeId, roles: List<UserRole>) =
        employeeController.updateEmployeeGlobalRoles(dbInstance(), adminUser, clock, id, roles)

    fun deleteEmployee(id: EmployeeId) =
        employeeController.deleteEmployee(dbInstance(), adminUser, clock, id)

    fun deleteEmployeeDaycareRoles(id: EmployeeId, daycareId: DaycareId?) =
        employeeController.deleteEmployeeDaycareRoles(dbInstance(), adminUser, clock, id, daycareId)

    fun getEmployeeDetails(id: EmployeeId) =
        employeeController.getEmployeeDetails(dbInstance(), adminUser, clock, id)

    fun requestFromEmployee(employee: Employee) =
        NewEmployee(
            email = employee.email,
            firstName = employee.firstName,
            lastName = employee.lastName,
            externalId = employee.externalId,
            employeeNumber = null,
            temporaryInUnitId = null,
            active = true
        )

    val employee1 =
        Employee(
            email = "employee1@espoo.fi",
            preferredFirstName = null,
            firstName = "etunimi1",
            lastName = "sukunimi1",
            externalId =
                ExternalId.of(namespace = "espoo-ad", value = UUID.randomUUID().toString()),
            created = HelsinkiDateTime.now(),
            updated = HelsinkiDateTime.now(),
            id = EmployeeId(UUID.randomUUID()),
            temporaryInUnitId = null,
            active = true
        )

    val employee2 =
        Employee(
            email = "employee2@espoo.fi",
            preferredFirstName = null,
            firstName = "etunimi2",
            lastName = "sukunimi2",
            externalId =
                ExternalId.of(namespace = "espoo-ad", value = UUID.randomUUID().toString()),
            created = HelsinkiDateTime.now(),
            updated = HelsinkiDateTime.now(),
            id = EmployeeId(UUID.randomUUID()),
            temporaryInUnitId = null,
            active = true
        )

    private fun Database.Read.hasActiveMessagingAccount(employeeId: EmployeeId) =
        createQuery {
                sql(
                    """
        SELECT EXISTS (SELECT 1 FROM message_account WHERE employee_id = ${bind(employeeId)} AND active)
    """
                )
            }
            .exactlyOne<Boolean>()
}
