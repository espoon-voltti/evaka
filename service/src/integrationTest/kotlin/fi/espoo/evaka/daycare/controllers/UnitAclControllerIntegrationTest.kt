// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.attendance.getOccupancyCoefficientsByUnit
import fi.espoo.evaka.pairing.listPersonalDevices
import fi.espoo.evaka.pis.TemporaryEmployee
import fi.espoo.evaka.pis.controllers.PinCode
import fi.espoo.evaka.pis.deactivateInactiveEmployees
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.*
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPersonalMobileDevice
import fi.espoo.evaka.shared.dev.insert
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

    val now = HelsinkiDateTime.of(LocalDate.of(2023, 3, 29), LocalTime.of(8, 37))
    val clock = MockEvakaClock(now)

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
            UnitAclController.AclUpdate(
                groupIds = null,
                hasStaffOccupancyEffect = null,
                endDate = null,
            ),
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
                    endDate = null,
                )
            ),
            getAclRows(),
        )

        deleteSupervisor(testDaycare.id)
        assertTrue(getAclRows().isEmpty())

        val endDate = now.toLocalDate().plusDays(7)
        insertEmployee(
            UnitAclController.AclUpdate(
                groupIds = null,
                hasStaffOccupancyEffect = null,
                endDate = endDate,
            ),
            UserRole.STAFF,
            testDaycare.id,
            employee.id,
        )

        assertEquals(
            listOf(
                DaycareAclRow(
                    employee = employee,
                    role = UserRole.STAFF,
                    groupIds = emptyList(),
                    endDate = endDate,
                )
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
                endDate = null,
            )

        insertEmployee(aclUpdate, UserRole.UNIT_SUPERVISOR, testDaycare.id, employee.id)

        assertEquals(
            listOf(
                DaycareAclRow(
                    employee = employee,
                    role = UserRole.UNIT_SUPERVISOR,
                    groupIds = listOf(testDaycareGroup.id),
                    endDate = null,
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

        val aclUpdate =
            UnitAclController.AclUpdate(
                groupIds = null,
                hasStaffOccupancyEffect = true,
                endDate = null,
            )

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
                    endDate = null,
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
    fun `modify group acl, occupancy coefficient and end date`() {
        assertTrue(getAclRows().isEmpty())

        val endDate1 = now.toLocalDate().plusDays(7)
        val aclUpdate =
            UnitAclController.AclUpdate(
                groupIds = null,
                hasStaffOccupancyEffect = true,
                endDate = endDate1,
            )

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
                    endDate = endDate1,
                )
            ),
            getAclRows(),
        )

        val endDate2 = now.toLocalDate().plusDays(14)
        val aclModification =
            UnitAclController.AclUpdate(
                groupIds = listOf(testDaycareGroup.id),
                hasStaffOccupancyEffect = false,
                endDate = endDate2,
            )
        modifyEmployee(aclModification, testDaycare.id, employee.id)

        assertEquals(
            listOf(
                DaycareAclRow(
                    employee = employee,
                    role = UserRole.UNIT_SUPERVISOR,
                    groupIds = listOf(testDaycareGroup.id),
                    endDate = endDate2,
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
            UnitAclController.AclUpdate(
                groupIds = null,
                hasStaffOccupancyEffect = null,
                endDate = null,
            ),
            UserRole.UNIT_SUPERVISOR,
            testDaycare.id,
            employee.id,
        )

        assertEquals(MessageAccountState.ACTIVE_ACCOUNT, employeeMessageAccountState())
        insertEmployee(
            UnitAclController.AclUpdate(
                groupIds = null,
                hasStaffOccupancyEffect = null,
                endDate = null,
            ),
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

        assertThat(getTemporaryEmployees(testDaycare.id)).isEmpty()
        assertThat(getTemporaryEmployees(testDaycare2.id)).isEmpty()

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
        assertThat(getTemporaryEmployees(testDaycare.id))
            .extracting({ it.id }, { it.firstName }, { it.lastName }, { it.temporaryInUnitId })
            .containsExactly(Tuple(temporaryEmployeeId, "Etu1", "Suku1", testDaycare.id))
        assertThat(getTemporaryEmployees(testDaycare2.id)).isEmpty()
        assertThat(getTemporaryEmployee(testDaycare.id, temporaryEmployeeId))
            .isEqualTo(createdTemporary)
        assertThrows<NotFound> { getTemporaryEmployee(testDaycare2.id, temporaryEmployeeId) }
        dbInstance().connect { dbc ->
            dbc.transaction { tx -> tx.deactivateInactiveEmployees(now.plusMonths(1)) }
        }
        assertThat(getTemporaryEmployee(testDaycare.id, temporaryEmployeeId))
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
        assertThat(getTemporaryEmployees(testDaycare.id))
            .extracting({ it.id }, { it.firstName }, { it.lastName }, { it.temporaryInUnitId })
            .containsExactly(Tuple(temporaryEmployeeId, "Etu2", "Suku2", testDaycare.id))
        assertThat(getTemporaryEmployee(testDaycare.id, temporaryEmployeeId))
            .isEqualTo(updatedTemporary)
        dbInstance().connect { dbc ->
            dbc.transaction { tx -> tx.deactivateInactiveEmployees(now.plusMonths(1)) }
        }
        assertThat(getTemporaryEmployee(testDaycare.id, temporaryEmployeeId))
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
        assertThat(getTemporaryEmployees(testDaycare.id))
            .extracting({ it.id }, { it.firstName }, { it.lastName }, { it.temporaryInUnitId })
            .containsExactly(Tuple(temporaryEmployeeId, "Etu2", "Suku2", testDaycare.id))
        assertThat(getTemporaryEmployee(testDaycare.id, temporaryEmployeeId))
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
        assertThat(getTemporaryEmployees(testDaycare.id)).isEmpty()
        assertThrows<NotFound> { getTemporaryEmployee(testDaycare.id, temporaryEmployeeId) }
    }

    @Test
    fun temporaryEmployeeCannotBeUpdatedWithPermanentEmployeeApi() {
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
                UnitAclController.AclUpdate(listOf(testDaycareGroup.id), false, null),
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
                    UnitAclController.AclUpdate(listOf(testDaycareGroup.id), false, null),
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
    fun groupAccessCanBeAddedAndRemoved() {
        val group2 =
            DevDaycareGroup(
                daycareId = testDaycare.id,
                id = GroupId(UUID.randomUUID()),
                name = "Group2",
            )
        val unit2Group =
            DevDaycareGroup(daycareId = testDaycare2.id, id = GroupId(UUID.randomUUID()))
        db.transaction { tx ->
            tx.insert(group2)
            tx.insert(unit2Group)
            tx.insertDaycareAclRow(testDaycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareAclRow(testDaycare2.id, employee.id, UserRole.STAFF)
        }

        data class DaycareGroupAcl(
            val daycareGroupId: GroupId,
            val employeeId: EmployeeId,
            val created: HelsinkiDateTime,
            val updated: HelsinkiDateTime,
        )

        fun readAllGroupAcls() =
            db.read { tx ->
                tx.createQuery { sql("SELECT * FROM daycare_group_acl") }.toList<DaycareGroupAcl>()
            }

        // add access to two groups in daycare 1
        // daycare 2 has no groups
        val moment1 = clock.now()
        unitAclController.updateGroupAclWithOccupancyCoefficient(
            dbInstance(),
            admin,
            MockEvakaClock(moment1),
            testDaycare.id,
            employee.id,
            UnitAclController.AclUpdate(
                groupIds = listOf(testDaycareGroup.id, group2.id),
                hasStaffOccupancyEffect = null,
                endDate = null,
            ),
        )

        val step1Acls = readAllGroupAcls()
        assertThat(step1Acls)
            .containsExactlyInAnyOrder(
                DaycareGroupAcl(
                    daycareGroupId = testDaycareGroup.id,
                    employeeId = employee.id,
                    created = moment1,
                    updated = moment1,
                ),
                DaycareGroupAcl(
                    daycareGroupId = group2.id,
                    employeeId = employee.id,
                    created = moment1,
                    updated = moment1,
                ),
            )

        // add access to one group in daycare 2
        // daycare 1 still has two groups
        val moment2 = clock.now().plusMinutes(1)
        unitAclController.updateGroupAclWithOccupancyCoefficient(
            dbInstance(),
            admin,
            MockEvakaClock(moment2),
            testDaycare2.id,
            employee.id,
            UnitAclController.AclUpdate(
                groupIds = listOf(unit2Group.id),
                hasStaffOccupancyEffect = null,
                endDate = null,
            ),
        )

        val step2Acls = readAllGroupAcls()
        assertThat(step2Acls)
            .containsExactlyInAnyOrder(
                DaycareGroupAcl(
                    daycareGroupId = testDaycareGroup.id,
                    employeeId = employee.id,
                    created = moment1,
                    updated = moment1,
                ),
                DaycareGroupAcl(
                    daycareGroupId = group2.id,
                    employeeId = employee.id,
                    created = moment1,
                    updated = moment1,
                ),
                DaycareGroupAcl(
                    daycareGroupId = unit2Group.id,
                    employeeId = employee.id,
                    created = moment2,
                    updated = moment2,
                ),
            )

        // remove access to group 2 in daycare 1
        // daycare 2 still has one group
        val moment3 = clock.now().plusMinutes(2)
        unitAclController.updateGroupAclWithOccupancyCoefficient(
            dbInstance(),
            admin,
            MockEvakaClock(moment3),
            testDaycare.id,
            employee.id,
            UnitAclController.AclUpdate(
                groupIds = listOf(testDaycareGroup.id),
                hasStaffOccupancyEffect = null,
                endDate = null,
            ),
        )

        val step3Acls = readAllGroupAcls()
        assertThat(step3Acls)
            .containsExactlyInAnyOrder(
                DaycareGroupAcl(
                    daycareGroupId = testDaycareGroup.id,
                    employeeId = employee.id,
                    created = moment1,
                    updated = moment1,
                ),
                DaycareGroupAcl(
                    daycareGroupId = unit2Group.id,
                    employeeId = employee.id,
                    created = moment2,
                    updated = moment2,
                ),
            )
    }

    @Test
    fun permanentEmployeeCannotBeUpdatedWithTemporaryEmployeeApi() {
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2023, 3, 29), LocalTime.of(8, 37)))
        insertEmployee(
            UnitAclController.AclUpdate(
                groupIds = null,
                hasStaffOccupancyEffect = null,
                endDate = null,
            ),
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

    private fun getAclRows(): List<DaycareAclRow> =
        unitAclController.getDaycareAcl(dbInstance(), admin, clock, testDaycare.id)

    private fun getTemporaryEmployees(unitId: DaycareId) =
        unitAclController.getTemporaryEmployees(dbInstance(), admin, clock, unitId)

    private fun getTemporaryEmployee(unitId: DaycareId, employeeId: EmployeeId) =
        unitAclController.getTemporaryEmployee(dbInstance(), admin, clock, unitId, employeeId)

    private fun getDaycareOccupancyCoefficients(unitId: DaycareId): Map<EmployeeId, BigDecimal> {
        return db.read { tx ->
            tx.getOccupancyCoefficientsByUnit(unitId).associate { it.employeeId to it.coefficient }
        }
    }

    private fun deleteSupervisor(daycareId: DaycareId) =
        unitAclController.deleteUnitSupervisor(dbInstance(), admin, clock, daycareId, employee.id)

    private fun insertEmployee(
        update: UnitAclController.AclUpdate,
        role: UserRole,
        daycareId: DaycareId,
        employeeId: EmployeeId,
    ) =
        unitAclController.addFullAclForRole(
            dbInstance(),
            admin,
            clock,
            daycareId,
            employeeId,
            UnitAclController.FullAclInfo(role, update),
        )

    private fun modifyEmployee(
        update: UnitAclController.AclUpdate,
        daycareId: DaycareId,
        employeeId: EmployeeId,
    ) =
        unitAclController.updateGroupAclWithOccupancyCoefficient(
            dbInstance(),
            admin,
            clock,
            daycareId,
            employeeId,
            update,
        )

    private fun deleteStaff() =
        unitAclController.deleteStaff(dbInstance(), admin, clock, testDaycare.id, employee.id)

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
