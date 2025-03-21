// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.messaging.upsertEmployeeMessageAccount
import fi.espoo.evaka.pis.DaycareGroupRole
import fi.espoo.evaka.pis.DaycareRole
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.NewEmployee
import fi.espoo.evaka.pis.NewSsnEmployee
import fi.espoo.evaka.pis.ScheduledDaycareRole
import fi.espoo.evaka.pis.controllers.EmployeeController
import fi.espoo.evaka.pis.createEmployee
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
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class EmployeeControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired lateinit var employeeController: EmployeeController

    private val clock = MockEvakaClock(2025, 1, 1, 12, 0)

    @Test
    fun `no employees return empty list`() {
        val employees = getEmployees()
        assertEquals(listOf(), employees)
    }

    @Test
    fun `admin gets all employees`() {
        db.transaction {
            it.createEmployee(requestFromEmployee(employee1))
            it.createEmployee(requestFromEmployee(employee2))
        }
        val employees = getEmployees()
        assertEquals(2, employees.size)
        assertEquals(employee1.email, employees[0].email)
        assertEquals(employee2.email, employees[1].email)
    }

    @Test
    fun `admin can get employee`() {
        val employee = db.transaction { it.createEmployee(requestFromEmployee(employee1)) }
        assertEquals(employee, getEmployee(employee.id))
    }

    @Test
    fun `admin can not delete employee without SSN (AD user)`() {
        val employee = db.transaction { it.createEmployee(requestFromEmployee(employee1)) }

        assertThrows<BadRequest> { deleteEmployee(employee.id) }
    }

    @Test
    fun `admin can delete employee with SSN who has not yet logged in`() {
        val employee =
            createSsnEmployee(
                NewSsnEmployee(
                    ssn = Sensitive("010107A9917"),
                    firstName = "First",
                    lastName = "Last",
                    email = "test@example.com",
                )
            )

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
                unitRoles = mapOf(daycare1.id to UserRole.STAFF, daycare2.id to UserRole.STAFF),
            )
        }

        val endDate = clock.today().plusMonths(9)
        upsertEmployeeDaycareRoles(
            employee.id,
            listOf(daycare2.id, daycare3.id),
            UserRole.SPECIAL_EDUCATION_TEACHER,
            startDate = clock.today(),
            endDate,
        )

        assertEquals(
            setOf(
                DaycareRole(daycare1.id, daycare1.name, UserRole.STAFF, null),
                DaycareRole(
                    daycare2.id,
                    daycare2.name,
                    UserRole.SPECIAL_EDUCATION_TEACHER,
                    endDate,
                ),
                DaycareRole(daycare3.id, daycare3.name, UserRole.SPECIAL_EDUCATION_TEACHER, endDate),
            ),
            getEmployeeDetails(employee.id).daycareRoles.toSet(),
        )
        assertEquals(0, getEmployeeDetails(employee.id).scheduledDaycareRoles.size)
        db.read { assertTrue(it.hasActiveMessagingAccount(employee.id)) }
    }

    @Test
    fun `admin can schedule employee daycare roles`() {
        val careArea = DevCareArea()
        val daycare1 = DevDaycare(areaId = careArea.id)
        val daycare2 = DevDaycare(areaId = careArea.id)
        val employee = DevEmployee()
        db.transaction { tx ->
            tx.insert(careArea)
            tx.insert(daycare1)
            tx.insert(daycare2)
            tx.insert(employee)
        }

        val startDate = clock.today().plusMonths(1)
        val endDate = clock.today().plusMonths(9)
        upsertEmployeeDaycareRoles(
            employee.id,
            listOf(daycare1.id, daycare2.id),
            UserRole.SPECIAL_EDUCATION_TEACHER,
            startDate,
            endDate,
        )

        assertEquals(0, getEmployeeDetails(employee.id).daycareRoles.size)
        assertEquals(
            listOf(
                ScheduledDaycareRole(
                    daycare1.id,
                    daycare1.name,
                    UserRole.SPECIAL_EDUCATION_TEACHER,
                    startDate,
                    endDate,
                ),
                ScheduledDaycareRole(
                    daycare2.id,
                    daycare2.name,
                    UserRole.SPECIAL_EDUCATION_TEACHER,
                    startDate,
                    endDate,
                ),
            ),
            getEmployeeDetails(employee.id).scheduledDaycareRoles,
        )
        db.read { assertFalse(it.hasActiveMessagingAccount(employee.id)) }
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
                        daycare2.id to listOf(daycare2Group.id),
                    ),
            )
            tx.upsertEmployeeMessageAccount(employee.id)
        }

        deleteEmployeeDaycareRoles(id = employee.id, daycareId = daycare1.id)

        val updated = getEmployeeDetails(employee.id)
        assertEquals(
            listOf(DaycareRole(daycare2.id, daycare2.name, UserRole.STAFF, null)),
            updated.daycareRoles,
        )
        assertEquals(
            listOf(
                DaycareGroupRole(daycare2.id, daycare2.name, daycare2Group.id, daycare2Group.name)
            ),
            updated.daycareGroupRoles,
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
                        daycare2.id to listOf(daycare2Group.id),
                    ),
            )
            tx.upsertEmployeeMessageAccount(employee.id)
        }

        deleteEmployeeDaycareRoles(id = employee.id, daycareId = null)

        val updated = getEmployeeDetails(employee.id)
        assertEquals(emptyList(), updated.daycareRoles)
        assertEquals(emptyList(), updated.daycareGroupRoles)
        db.read { assertFalse(it.hasActiveMessagingAccount(employee.id)) }
    }

    @Test
    fun `a new employee can be added using an ssn`() {
        val request =
            NewSsnEmployee(
                ssn = Sensitive("010107A9917"),
                firstName = "First",
                lastName = "Last",
                email = "test@example.com",
            )
        val response = createSsnEmployee(request)
        val employee = getEmployee(response.id)
        assertEquals(request.firstName, employee.firstName)
        assertEquals(request.lastName, employee.lastName)
        assertEquals(request.email, employee.email)
        assertTrue(employee.active)
    }

    val adminUser = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))

    fun getEmployees() = employeeController.getEmployees(dbInstance(), adminUser, clock)

    fun getEmployee(id: EmployeeId) =
        employeeController.getEmployee(dbInstance(), adminUser, clock, id)

    fun upsertEmployeeDaycareRoles(
        id: EmployeeId,
        daycareIds: List<DaycareId>,
        role: UserRole,
        startDate: LocalDate,
        endDate: LocalDate?,
    ) =
        employeeController.upsertEmployeeDaycareRoles(
            dbInstance(),
            adminUser,
            clock,
            id,
            EmployeeController.UpsertEmployeeDaycareRolesRequest(
                daycareIds,
                role,
                startDate,
                endDate,
            ),
        )

    fun updateEmployeeGlobalRoles(id: EmployeeId, roles: List<UserRole>) =
        employeeController.updateEmployeeGlobalRoles(dbInstance(), adminUser, clock, id, roles)

    fun deleteEmployee(id: EmployeeId) =
        employeeController.deleteEmployee(dbInstance(), adminUser, clock, id)

    fun deleteEmployeeDaycareRoles(id: EmployeeId, daycareId: DaycareId?) =
        employeeController.deleteEmployeeDaycareRoles(dbInstance(), adminUser, clock, id, daycareId)

    fun getEmployeeDetails(id: EmployeeId) =
        employeeController.getEmployeeDetails(dbInstance(), adminUser, clock, id)

    fun createSsnEmployee(employee: NewSsnEmployee) =
        employeeController.createSsnEmployee(dbInstance(), adminUser, clock, employee)

    fun requestFromEmployee(employee: Employee) =
        NewEmployee(
            email = employee.email,
            firstName = employee.firstName,
            lastName = employee.lastName,
            externalId = employee.externalId,
            employeeNumber = null,
            temporaryInUnitId = null,
            active = true,
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
            active = true,
            hasSsn = false,
            lastLogin = null,
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
            active = true,
            hasSsn = false,
            lastLogin = null,
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
