// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.attendance.getOccupancyCoefficientsByUnit
import fi.espoo.evaka.pairing.listPersonalDevices
import fi.espoo.evaka.pis.TemporaryEmployee
import fi.espoo.evaka.pis.controllers.PinCode
import fi.espoo.evaka.pis.deactivateInactiveEmployees
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.DaycareAclRow
import fi.espoo.evaka.shared.auth.DaycareAclRowEmployee
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPersonalMobileDevice
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDaycareGroup
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class UnitAclControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var unitAclController: UnitAclController
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    private val employee =
        DaycareAclRowEmployee(
            id = EmployeeId(UUID.randomUUID()),
            firstName = "First",
            lastName = "Last",
            email = "test@example.com",
            temporary = false,
            hasStaffOccupancyEffect = false,
            active = true,
        )
    private lateinit var admin: AuthenticatedUser.Employee

    private fun getRoleBodyString(body: UnitAclController.FullAclInfo) =
        jsonMapper.writeValueAsString(body)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            admin =
                AuthenticatedUser.Employee(
                    tx.insert(DevEmployee(roles = setOf(UserRole.ADMIN))),
                    roles = setOf(UserRole.ADMIN),
                )
            tx.insert(testArea)
            tx.insert(
                DevDaycare(areaId = testArea.id, id = testDaycare.id, name = testDaycare.name)
            )
            tx.insert(
                DevDaycare(areaId = testArea.id, id = testDaycare2.id, name = testDaycare2.name)
            )
            tx.insert(testDaycareGroup)

            employee.also {
                tx.insert(
                    DevEmployee(
                        id = it.id,
                        firstName = it.firstName,
                        lastName = it.lastName,
                        email = it.email,
                    )
                )
            }
        }
    }

    @Test
    fun `add and delete daycare acl for different roles`() {
        assertTrue(getAclRows().isEmpty())

        insertEmployee(
            UnitAclController.AclUpdate(groupIds = null, hasStaffOccupancyEffect = null),
            UserRole.UNIT_SUPERVISOR,
            testDaycare.id,
            employee.id,
        )
        assertEquals(
            listOf(
                DaycareAclRow(
                    employee = employee,
                    role = UserRole.UNIT_SUPERVISOR,
                    groupIds = emptyList(),
                )
            ),
            getAclRows(),
        )

        deleteSupervisor(testDaycare.id)
        assertTrue(getAclRows().isEmpty())

        insertEmployee(
            UnitAclController.AclUpdate(groupIds = null, hasStaffOccupancyEffect = null),
            UserRole.STAFF,
            testDaycare.id,
            employee.id,
        )

        assertEquals(
            listOf(
                DaycareAclRow(employee = employee, role = UserRole.STAFF, groupIds = emptyList())
            ),
            getAclRows(),
        )

        deleteStaff()
        assertTrue(getAclRows().isEmpty())
    }

    @Test
    fun `add and delete daycare acl with group`() {
        assertTrue(getAclRows().isEmpty())

        val aclUpdate =
            UnitAclController.AclUpdate(
                groupIds = listOf(testDaycareGroup.id),
                hasStaffOccupancyEffect = null,
            )

        insertEmployee(aclUpdate, UserRole.UNIT_SUPERVISOR, testDaycare.id, employee.id)

        assertEquals(
            listOf(
                DaycareAclRow(
                    employee = employee,
                    role = UserRole.UNIT_SUPERVISOR,
                    groupIds = listOf(testDaycareGroup.id),
                )
            ),
            getAclRows(),
        )

        deleteSupervisor(testDaycare.id)
        assertTrue(getAclRows().isEmpty())
    }

    @Test
    fun `add and delete daycare acl with occupancy coefficient`() {
        assertTrue(getAclRows().isEmpty())

        val aclUpdate = UnitAclController.AclUpdate(groupIds = null, hasStaffOccupancyEffect = true)

        insertEmployee(aclUpdate, UserRole.UNIT_SUPERVISOR, testDaycare.id, employee.id)
        assertEquals(
            listOf(
                DaycareAclRow(
                    employee =
                        DaycareAclRowEmployee(
                            id = employee.id,
                            email = employee.email,
                            firstName = employee.firstName,
                            lastName = employee.lastName,
                            temporary = employee.temporary,
                            hasStaffOccupancyEffect = true,
                            active = true,
                        ),
                    role = UserRole.UNIT_SUPERVISOR,
                    groupIds = emptyList(),
                )
            ),
            getAclRows(),
        )

        val coefficientsAfterInsert = getDaycareOccupancyCoefficients(testDaycare.id)

        assertEquals(BigDecimal("7.00"), coefficientsAfterInsert[employee.id])

        deleteSupervisor(testDaycare.id)

        val coefficientsAfterDelete = getDaycareOccupancyCoefficients(testDaycare.id)

        assertEquals(BigDecimal("7.00"), coefficientsAfterDelete[employee.id])

        assertTrue(getAclRows().isEmpty())
    }

    @Test
    fun `modify group acl and occupancy coefficient`() {
        assertTrue(getAclRows().isEmpty())

        val aclUpdate = UnitAclController.AclUpdate(groupIds = null, hasStaffOccupancyEffect = true)

        insertEmployee(aclUpdate, UserRole.UNIT_SUPERVISOR, testDaycare.id, employee.id)
        assertEquals(
            listOf(
                DaycareAclRow(
                    employee =
                        DaycareAclRowEmployee(
                            id = employee.id,
                            email = employee.email,
                            firstName = employee.firstName,
                            lastName = employee.lastName,
                            temporary = employee.temporary,
                            hasStaffOccupancyEffect = true,
                            active = true,
                        ),
                    role = UserRole.UNIT_SUPERVISOR,
                    groupIds = emptyList(),
                )
            ),
            getAclRows(),
        )

        val aclModification =
            UnitAclController.AclUpdate(
                groupIds = listOf(testDaycareGroup.id),
                hasStaffOccupancyEffect = false,
            )
        modifyEmployee(aclModification, testDaycare.id, employee.id)

        assertEquals(
            listOf(
                DaycareAclRow(
                    employee = employee,
                    role = UserRole.UNIT_SUPERVISOR,
                    groupIds = listOf(testDaycareGroup.id),
                )
            ),
            getAclRows(),
        )

        val coefficientsAfterModification = getDaycareOccupancyCoefficients(testDaycare.id)
        assertEquals(BigDecimal("0.00"), coefficientsAfterModification[employee.id])
    }

    @Test
    fun `supervisor message account`() {
        assertEquals(MessageAccountState.NO_ACCOUNT, employeeMessageAccountState())
        insertEmployee(
            UnitAclController.AclUpdate(groupIds = null, hasStaffOccupancyEffect = null),
            UserRole.UNIT_SUPERVISOR,
            testDaycare.id,
            employee.id,
        )

        assertEquals(MessageAccountState.ACTIVE_ACCOUNT, employeeMessageAccountState())
        insertEmployee(
            UnitAclController.AclUpdate(groupIds = null, hasStaffOccupancyEffect = null),
            UserRole.UNIT_SUPERVISOR,
            testDaycare2.id,
            employee.id,
        )

        assertEquals(MessageAccountState.ACTIVE_ACCOUNT, employeeMessageAccountState())
        deleteSupervisor(testDaycare.id)
        assertEquals(MessageAccountState.ACTIVE_ACCOUNT, employeeMessageAccountState())
        deleteSupervisor(testDaycare2.id)
        assertEquals(MessageAccountState.INACTIVE_ACCOUNT, employeeMessageAccountState())
    }

    @Test
    fun `personal mobile device is deleted in async job after supervisor role is removed`() {
        val supervisor1 = DevEmployee()
        val device1 = DevPersonalMobileDevice(employeeId = supervisor1.id)
        val supervisor2 = DevEmployee()
        val device2 = DevPersonalMobileDevice(employeeId = supervisor2.id)
        db.transaction { tx ->
            tx.insert(supervisor1, unitRoles = mapOf(testDaycare.id to UserRole.UNIT_SUPERVISOR))
            tx.insert(device1)
            tx.insert(
                supervisor2,
                unitRoles =
                    mapOf(
                        testDaycare.id to UserRole.UNIT_SUPERVISOR,
                        testDaycare2.id to UserRole.UNIT_SUPERVISOR,
                    ),
            )
            tx.insert(device2)
        }

        val now = HelsinkiDateTime.of(LocalDate.of(2024, 12, 13), LocalTime.of(12, 0))
        unitAclController.deleteUnitSupervisor(
            dbInstance(),
            admin,
            MockEvakaClock(now),
            testDaycare.id,
            supervisor1.id,
        )
        unitAclController.deleteUnitSupervisor(
            dbInstance(),
            admin,
            MockEvakaClock(now),
            testDaycare.id,
            supervisor2.id,
        )
        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now.plusHours(1)))

        db.read { tx ->
            assertEquals(emptyList(), tx.listPersonalDevices(employeeId = supervisor1.id))
            assertEquals(
                listOf(device2.id),
                tx.listPersonalDevices(employeeId = supervisor2.id).map { it.id },
            )
        }
    }

    @Test
    fun temporaryEmployeeCrud() {
        val dateTime = HelsinkiDateTime.of(LocalDate.of(2023, 3, 29), LocalTime.of(8, 37))
        val clock = MockEvakaClock(dateTime)
        assertThat(getTemporaryEmployees(clock, testDaycare.id)).isEmpty()
        assertThat(getTemporaryEmployees(clock, testDaycare2.id)).isEmpty()

        // create
        val createdTemporary =
            TemporaryEmployee(
                firstName = "Etu1",
                lastName = "Suku1",
                groupIds = emptySet(),
                hasStaffOccupancyEffect = false,
                pinCode = null,
            )
        val temporaryEmployeeId =
            unitAclController.createTemporaryEmployee(
                dbInstance(),
                admin,
                clock,
                testDaycare.id,
                createdTemporary,
            )
        assertThat(getTemporaryEmployees(clock, testDaycare.id))
            .extracting({ it.id }, { it.firstName }, { it.lastName }, { it.temporaryInUnitId })
            .containsExactly(Tuple(temporaryEmployeeId, "Etu1", "Suku1", testDaycare.id))
        assertThat(getTemporaryEmployees(clock, testDaycare2.id)).isEmpty()
        assertThat(getTemporaryEmployee(clock, testDaycare.id, temporaryEmployeeId))
            .isEqualTo(createdTemporary)
        assertThrows<NotFound> { getTemporaryEmployee(clock, testDaycare2.id, temporaryEmployeeId) }
        dbInstance().connect { dbc ->
            dbc.transaction { tx -> tx.deactivateInactiveEmployees(dateTime.plusMonths(1)) }
        }
        assertThat(getTemporaryEmployee(clock, testDaycare.id, temporaryEmployeeId))
            .isEqualTo(createdTemporary)

        // update
        val updatedTemporary =
            TemporaryEmployee(
                firstName = "Etu2",
                lastName = "Suku2",
                groupIds = setOf(testDaycareGroup.id),
                hasStaffOccupancyEffect = true,
                pinCode = PinCode("2537"),
            )
        assertThrows<NotFound> {
            unitAclController.updateTemporaryEmployee(
                dbInstance(),
                admin,
                clock,
                testDaycare2.id,
                temporaryEmployeeId,
                updatedTemporary,
            )
        }
        unitAclController.updateTemporaryEmployee(
            dbInstance(),
            admin,
            clock,
            testDaycare.id,
            temporaryEmployeeId,
            updatedTemporary,
        )
        assertThat(getTemporaryEmployees(clock, testDaycare.id))
            .extracting({ it.id }, { it.firstName }, { it.lastName }, { it.temporaryInUnitId })
            .containsExactly(Tuple(temporaryEmployeeId, "Etu2", "Suku2", testDaycare.id))
        assertThat(getTemporaryEmployee(clock, testDaycare.id, temporaryEmployeeId))
            .isEqualTo(updatedTemporary)
        dbInstance().connect { dbc ->
            dbc.transaction { tx -> tx.deactivateInactiveEmployees(dateTime.plusMonths(1)) }
        }
        assertThat(getTemporaryEmployee(clock, testDaycare.id, temporaryEmployeeId))
            .isEqualTo(updatedTemporary)

        // delete acl
        assertThrows<NotFound> {
            unitAclController.deleteTemporaryEmployeeAcl(
                dbInstance(),
                admin,
                clock,
                testDaycare2.id,
                temporaryEmployeeId,
            )
        }
        unitAclController.deleteTemporaryEmployeeAcl(
            dbInstance(),
            admin,
            clock,
            testDaycare.id,
            temporaryEmployeeId,
        )
        assertThat(getTemporaryEmployees(clock, testDaycare.id))
            .extracting({ it.id }, { it.firstName }, { it.lastName }, { it.temporaryInUnitId })
            .containsExactly(Tuple(temporaryEmployeeId, "Etu2", "Suku2", testDaycare.id))
        assertThat(getTemporaryEmployee(clock, testDaycare.id, temporaryEmployeeId))
            .isEqualTo(updatedTemporary.copy(groupIds = emptySet()))

        // delete
        assertThrows<NotFound> {
            unitAclController.deleteTemporaryEmployee(
                dbInstance(),
                admin,
                clock,
                testDaycare2.id,
                temporaryEmployeeId,
            )
        }
        unitAclController.deleteTemporaryEmployee(
            dbInstance(),
            admin,
            clock,
            testDaycare.id,
            temporaryEmployeeId,
        )
        assertThat(getTemporaryEmployees(clock, testDaycare.id)).isEmpty()
        assertThrows<NotFound> { getTemporaryEmployee(clock, testDaycare.id, temporaryEmployeeId) }
    }

    @Test
    fun temporaryEmployeeCannotBeUpdatedWithPermanentEmployeeApi() {
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2023, 3, 29), LocalTime.of(8, 37)))
        val temporaryEmployeeId =
            unitAclController.createTemporaryEmployee(
                dbInstance(),
                admin,
                clock,
                testDaycare.id,
                TemporaryEmployee(
                    firstName = "Etu1",
                    lastName = "Suku1",
                    groupIds = emptySet(),
                    hasStaffOccupancyEffect = false,
                    pinCode = null,
                ),
            )

        assertThrows<NotFound> {
            unitAclController.updateGroupAclWithOccupancyCoefficient(
                dbInstance(),
                admin,
                clock,
                testDaycare.id,
                temporaryEmployeeId,
                UnitAclController.AclUpdate(listOf(testDaycareGroup.id), false),
            )
        }
        assertThrows<NotFound> {
            unitAclController.addFullAclForRole(
                dbInstance(),
                admin,
                clock,
                testDaycare.id,
                temporaryEmployeeId,
                UnitAclController.FullAclInfo(
                    UserRole.STAFF,
                    UnitAclController.AclUpdate(listOf(testDaycareGroup.id), false),
                ),
            )
        }
        assertThrows<NotFound> {
            unitAclController.deleteUnitSupervisor(
                dbInstance(),
                admin,
                clock,
                testDaycare.id,
                temporaryEmployeeId,
            )
        }
        assertThrows<NotFound> {
            unitAclController.deleteSpecialEducationTeacher(
                dbInstance(),
                admin,
                clock,
                testDaycare.id,
                temporaryEmployeeId,
            )
        }
        assertThrows<NotFound> {
            unitAclController.deleteEarlyChildhoodEducationSecretary(
                dbInstance(),
                admin,
                clock,
                testDaycare.id,
                temporaryEmployeeId,
            )
        }
        assertThrows<NotFound> {
            unitAclController.deleteStaff(
                dbInstance(),
                admin,
                clock,
                testDaycare.id,
                temporaryEmployeeId,
            )
        }
    }

    @Test
    fun permanentEmployeeCannotBeUpdatedWithTemporaryEmployeeApi() {
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2023, 3, 29), LocalTime.of(8, 37)))
        insertEmployee(
            UnitAclController.AclUpdate(groupIds = null, hasStaffOccupancyEffect = null),
            UserRole.STAFF,
            testDaycare.id,
            employee.id,
        )

        assertThrows<NotFound> {
            unitAclController.getTemporaryEmployee(
                dbInstance(),
                admin,
                clock,
                testDaycare.id,
                employee.id,
            )
        }
        assertThrows<NotFound> {
            unitAclController.updateTemporaryEmployee(
                dbInstance(),
                admin,
                clock,
                testDaycare.id,
                employee.id,
                TemporaryEmployee(
                    firstName = "Etu1",
                    lastName = "Suku1",
                    groupIds = emptySet(),
                    hasStaffOccupancyEffect = false,
                    pinCode = null,
                ),
            )
        }
        assertThrows<NotFound> {
            unitAclController.deleteTemporaryEmployeeAcl(
                dbInstance(),
                admin,
                clock,
                testDaycare.id,
                employee.id,
            )
        }
        assertThrows<NotFound> {
            unitAclController.deleteTemporaryEmployee(
                dbInstance(),
                admin,
                clock,
                testDaycare.id,
                employee.id,
            )
        }
    }

    private fun getAclRows(): List<DaycareAclRow> {
        val (_, res, body) =
            http
                .get("/employee/daycares/${testDaycare.id}/acl")
                .asUser(admin)
                .responseObject<List<DaycareAclRow>>(jsonMapper)
        assertTrue(res.isSuccessful)
        return body.get()
    }

    private fun getTemporaryEmployees(clock: EvakaClock, unitId: DaycareId) =
        unitAclController.getTemporaryEmployees(dbInstance(), admin, clock, unitId)

    private fun getTemporaryEmployee(clock: EvakaClock, unitId: DaycareId, employeeId: EmployeeId) =
        unitAclController.getTemporaryEmployee(dbInstance(), admin, clock, unitId, employeeId)

    private fun getDaycareOccupancyCoefficients(unitId: DaycareId): Map<EmployeeId, BigDecimal> {
        return db.read { tx ->
            tx.getOccupancyCoefficientsByUnit(unitId).associate { it.employeeId to it.coefficient }
        }
    }

    private fun deleteSupervisor(daycareId: DaycareId) {
        val (_, res, _) =
            http
                .delete("/employee/daycares/$daycareId/supervisors/${employee.id}")
                .asUser(admin)
                .response()
        assertTrue(res.isSuccessful)
    }

    private fun insertEmployee(
        update: UnitAclController.AclUpdate,
        role: UserRole,
        daycareId: DaycareId,
        employeeId: EmployeeId,
    ) {
        val (_, res, _) =
            http
                .put("/employee/daycares/$daycareId/full-acl/$employeeId")
                .asUser(admin)
                .jsonBody(
                    getRoleBodyString(UnitAclController.FullAclInfo(update = update, role = role))
                )
                .response()
        assertTrue(res.isSuccessful)
    }

    private fun modifyEmployee(
        update: UnitAclController.AclUpdate,
        daycareId: DaycareId,
        employeeId: EmployeeId,
    ) {
        val (_, res, _) =
            http
                .put("/employee/daycares/$daycareId/staff/$employeeId/groups")
                .asUser(admin)
                .jsonBody(jsonMapper.writeValueAsString(update))
                .response()
        assertTrue(res.isSuccessful)
    }

    private fun deleteStaff() {
        val (_, res, _) =
            http
                .delete("/employee/daycares/${testDaycare.id}/staff/${employee.id}")
                .asUser(admin)
                .response()
        assertTrue(res.isSuccessful)
    }

    private enum class MessageAccountState {
        NO_ACCOUNT,
        ACTIVE_ACCOUNT,
        INACTIVE_ACCOUNT,
    }

    private fun employeeMessageAccountState(): MessageAccountState =
        db.read {
                it.createQuery {
                        sql(
                            "SELECT active FROM message_account WHERE employee_id = ${bind(employee.id)}"
                        )
                    }
                    .toList<Boolean>()
            }
            .let { accounts ->
                if (accounts.size == 1) {
                    if (accounts[0]) {
                        MessageAccountState.ACTIVE_ACCOUNT
                    } else {
                        MessageAccountState.INACTIVE_ACCOUNT
                    }
                } else if (accounts.isEmpty()) {
                    MessageAccountState.NO_ACCOUNT
                } else {
                    throw RuntimeException("Employee has more than one account")
                }
            }
}
