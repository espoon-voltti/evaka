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
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPersonalMobileDevice
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
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

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val daycare2 = DevDaycare(areaId = area.id, name = "Test Daycare 2")
    private val daycareGroup = DevDaycareGroup(daycareId = daycare.id)
    private val adminEmployee = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val admin = adminEmployee.user
    private val devEmployee =
        DevEmployee(firstName = "First", lastName = "Last", email = "test@example.com")
    private val employee =
        DaycareAclRowEmployee(
            id = devEmployee.id,
            firstName = devEmployee.firstName,
            lastName = devEmployee.lastName,
            email = devEmployee.email,
            employeeNumber = null,
            temporary = false,
            hasStaffOccupancyEffect = false,
            active = true,
        )

    val now = HelsinkiDateTime.of(LocalDate.of(2023, 3, 29), LocalTime.of(8, 37))
    val clock = MockEvakaClock(now)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(adminEmployee)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(daycare2)
            tx.insert(daycareGroup)
            tx.insert(devEmployee)
        }
    }

    @Test
    fun `add and delete daycare acl for different roles`() {
        assertTrue(getAclRows().isEmpty())

        insertEmployee(
            UnitAclController.AclUpdate(
                role = UserRole.UNIT_SUPERVISOR,
                groupIds = null,
                hasStaffOccupancyEffect = null,
                endDate = null,
            ),
            daycare.id,
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

        deleteSupervisor(daycare.id)
        assertTrue(getAclRows().isEmpty())

        val endDate = now.toLocalDate().plusDays(7)
        insertEmployee(
            UnitAclController.AclUpdate(
                role = UserRole.STAFF,
                groupIds = null,
                hasStaffOccupancyEffect = null,
                endDate = endDate,
            ),
            daycare.id,
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
                role = UserRole.UNIT_SUPERVISOR,
                groupIds = listOf(daycareGroup.id),
                hasStaffOccupancyEffect = null,
                endDate = null,
            )

        insertEmployee(aclUpdate, daycare.id, employee.id)

        assertEquals(
            listOf(
                DaycareAclRow(
                    employee = employee,
                    role = UserRole.UNIT_SUPERVISOR,
                    groupIds = listOf(daycareGroup.id),
                    endDate = null,
                )
            ),
            getAclRows(),
        )

        deleteSupervisor(daycare.id)
        assertTrue(getAclRows().isEmpty())
    }

    @Test
    fun `add and delete daycare acl with occupancy coefficient`() {
        assertTrue(getAclRows().isEmpty())

        val aclUpdate =
            UnitAclController.AclUpdate(
                role = UserRole.UNIT_SUPERVISOR,
                groupIds = null,
                hasStaffOccupancyEffect = true,
                endDate = null,
            )

        insertEmployee(aclUpdate, daycare.id, employee.id)
        assertEquals(
            listOf(
                DaycareAclRow(
                    employee =
                        DaycareAclRowEmployee(
                            id = employee.id,
                            email = employee.email,
                            employeeNumber = employee.employeeNumber,
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

        val coefficientsAfterInsert = getDaycareOccupancyCoefficients(daycare.id)

        assertEquals(BigDecimal("7.00"), coefficientsAfterInsert[employee.id])

        deleteSupervisor(daycare.id)

        val coefficientsAfterDelete = getDaycareOccupancyCoefficients(daycare.id)

        assertEquals(BigDecimal("7.00"), coefficientsAfterDelete[employee.id])

        assertTrue(getAclRows().isEmpty())
    }

    @Test
    fun `modify group acl, occupancy coefficient and end date`() {
        assertTrue(getAclRows().isEmpty())

        val endDate1 = now.toLocalDate().plusDays(7)
        val aclUpdate =
            UnitAclController.AclUpdate(
                role = UserRole.UNIT_SUPERVISOR,
                groupIds = null,
                hasStaffOccupancyEffect = true,
                endDate = endDate1,
            )

        insertEmployee(aclUpdate, daycare.id, employee.id)
        assertEquals(
            listOf(
                DaycareAclRow(
                    employee =
                        DaycareAclRowEmployee(
                            id = employee.id,
                            email = employee.email,
                            employeeNumber = employee.employeeNumber,
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
                role = UserRole.UNIT_SUPERVISOR,
                groupIds = listOf(daycareGroup.id),
                hasStaffOccupancyEffect = false,
                endDate = endDate2,
            )
        modifyEmployee(aclModification, daycare.id, employee.id)

        assertEquals(
            listOf(
                DaycareAclRow(
                    employee = employee,
                    role = UserRole.UNIT_SUPERVISOR,
                    groupIds = listOf(daycareGroup.id),
                    endDate = endDate2,
                )
            ),
            getAclRows(),
        )

        val coefficientsAfterModification = getDaycareOccupancyCoefficients(daycare.id)
        assertEquals(BigDecimal("0.00"), coefficientsAfterModification[employee.id])
    }

    @Test
    fun `supervisor message account`() {
        assertEquals(MessageAccountState.NO_ACCOUNT, employeeMessageAccountState())
        insertEmployee(
            UnitAclController.AclUpdate(
                role = UserRole.UNIT_SUPERVISOR,
                groupIds = null,
                hasStaffOccupancyEffect = null,
                endDate = null,
            ),
            daycare.id,
            employee.id,
        )

        assertEquals(MessageAccountState.ACTIVE_ACCOUNT, employeeMessageAccountState())
        insertEmployee(
            UnitAclController.AclUpdate(
                role = UserRole.UNIT_SUPERVISOR,
                groupIds = null,
                hasStaffOccupancyEffect = null,
                endDate = null,
            ),
            daycare2.id,
            employee.id,
        )

        assertEquals(MessageAccountState.ACTIVE_ACCOUNT, employeeMessageAccountState())
        deleteSupervisor(daycare.id)
        assertEquals(MessageAccountState.ACTIVE_ACCOUNT, employeeMessageAccountState())
        deleteSupervisor(daycare2.id)
        assertEquals(MessageAccountState.INACTIVE_ACCOUNT, employeeMessageAccountState())
    }

    @Test
    fun `personal mobile device is deleted in async job after supervisor role is removed`() {
        val supervisor1 = DevEmployee()
        val device1 = DevPersonalMobileDevice(employeeId = supervisor1.id)
        val supervisor2 = DevEmployee()
        val device2 = DevPersonalMobileDevice(employeeId = supervisor2.id)
        db.transaction { tx ->
            tx.insert(supervisor1, unitRoles = mapOf(daycare.id to UserRole.UNIT_SUPERVISOR))
            tx.insert(device1)
            tx.insert(
                supervisor2,
                unitRoles =
                    mapOf(
                        daycare.id to UserRole.UNIT_SUPERVISOR,
                        daycare2.id to UserRole.UNIT_SUPERVISOR,
                    ),
            )
            tx.insert(device2)
        }

        val now = HelsinkiDateTime.of(LocalDate.of(2024, 12, 13), LocalTime.of(12, 0))
        unitAclController.deleteUnitSupervisor(
            dbInstance(),
            admin,
            MockEvakaClock(now),
            daycare.id,
            supervisor1.id,
        )
        unitAclController.deleteUnitSupervisor(
            dbInstance(),
            admin,
            MockEvakaClock(now),
            daycare.id,
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

        assertThat(getTemporaryEmployees(daycare.id)).isEmpty()
        assertThat(getTemporaryEmployees(daycare2.id)).isEmpty()

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
                daycare.id,
                createdTemporary,
            )
        assertThat(getTemporaryEmployees(daycare.id))
            .extracting({ it.id }, { it.firstName }, { it.lastName }, { it.temporaryInUnitId })
            .containsExactly(Tuple(temporaryEmployeeId, "Etu1", "Suku1", daycare.id))
        assertThat(getTemporaryEmployees(daycare2.id)).isEmpty()
        assertThat(getTemporaryEmployee(daycare.id, temporaryEmployeeId))
            .isEqualTo(createdTemporary)
        assertThrows<NotFound> { getTemporaryEmployee(daycare2.id, temporaryEmployeeId) }
        dbInstance().connect { dbc ->
            dbc.transaction { tx -> tx.deactivateInactiveEmployees(now.plusMonths(1)) }
        }
        assertThat(getTemporaryEmployee(daycare.id, temporaryEmployeeId))
            .isEqualTo(createdTemporary)

        // update
        val updatedTemporary =
            TemporaryEmployee(
                firstName = "Etu2",
                lastName = "Suku2",
                groupIds = setOf(daycareGroup.id),
                hasStaffOccupancyEffect = true,
                pinCode = PinCode("2537"),
            )
        assertThrows<NotFound> {
            unitAclController.updateTemporaryEmployee(
                dbInstance(),
                admin,
                clock,
                daycare2.id,
                temporaryEmployeeId,
                updatedTemporary,
            )
        }
        unitAclController.updateTemporaryEmployee(
            dbInstance(),
            admin,
            clock,
            daycare.id,
            temporaryEmployeeId,
            updatedTemporary,
        )
        assertThat(getTemporaryEmployees(daycare.id))
            .extracting({ it.id }, { it.firstName }, { it.lastName }, { it.temporaryInUnitId })
            .containsExactly(Tuple(temporaryEmployeeId, "Etu2", "Suku2", daycare.id))
        assertThat(getTemporaryEmployee(daycare.id, temporaryEmployeeId))
            .isEqualTo(updatedTemporary)
        dbInstance().connect { dbc ->
            dbc.transaction { tx -> tx.deactivateInactiveEmployees(now.plusMonths(1)) }
        }
        assertThat(getTemporaryEmployee(daycare.id, temporaryEmployeeId))
            .isEqualTo(updatedTemporary)

        // delete acl
        assertThrows<NotFound> {
            unitAclController.deleteTemporaryEmployeeAcl(
                dbInstance(),
                admin,
                clock,
                daycare2.id,
                temporaryEmployeeId,
            )
        }
        unitAclController.deleteTemporaryEmployeeAcl(
            dbInstance(),
            admin,
            clock,
            daycare.id,
            temporaryEmployeeId,
        )
        assertThat(getTemporaryEmployees(daycare.id))
            .extracting({ it.id }, { it.firstName }, { it.lastName }, { it.temporaryInUnitId })
            .containsExactly(Tuple(temporaryEmployeeId, "Etu2", "Suku2", daycare.id))
        assertThat(getTemporaryEmployee(daycare.id, temporaryEmployeeId))
            .isEqualTo(updatedTemporary.copy(groupIds = emptySet()))

        // delete
        assertThrows<NotFound> {
            unitAclController.deleteTemporaryEmployee(
                dbInstance(),
                admin,
                clock,
                daycare2.id,
                temporaryEmployeeId,
            )
        }
        unitAclController.deleteTemporaryEmployee(
            dbInstance(),
            admin,
            clock,
            daycare.id,
            temporaryEmployeeId,
        )
        assertThat(getTemporaryEmployees(daycare.id)).isEmpty()
        assertThrows<NotFound> { getTemporaryEmployee(daycare.id, temporaryEmployeeId) }
    }

    @Test
    fun temporaryEmployeeCannotBeUpdatedWithPermanentEmployeeApi() {
        val temporaryEmployeeId =
            unitAclController.createTemporaryEmployee(
                dbInstance(),
                admin,
                clock,
                daycare.id,
                TemporaryEmployee(
                    firstName = "Etu1",
                    lastName = "Suku1",
                    groupIds = emptySet(),
                    hasStaffOccupancyEffect = false,
                    pinCode = null,
                ),
            )

        assertThrows<NotFound> {
            unitAclController.updateGroupAcl(
                dbInstance(),
                admin,
                clock,
                daycare.id,
                temporaryEmployeeId,
                UnitAclController.AclUpdate(
                    role = UserRole.STAFF,
                    groupIds = listOf(daycareGroup.id),
                    hasStaffOccupancyEffect = false,
                    endDate = null,
                ),
            )
        }
        assertThrows<NotFound> {
            unitAclController.addFullAclForRole(
                dbInstance(),
                admin,
                clock,
                daycare.id,
                temporaryEmployeeId,
                UnitAclController.AclUpdate(
                    role = UserRole.STAFF,
                    groupIds = listOf(daycareGroup.id),
                    hasStaffOccupancyEffect = false,
                    endDate = null,
                ),
            )
        }
        assertThrows<NotFound> {
            unitAclController.deleteUnitSupervisor(
                dbInstance(),
                admin,
                clock,
                daycare.id,
                temporaryEmployeeId,
            )
        }
        assertThrows<NotFound> {
            unitAclController.deleteSpecialEducationTeacher(
                dbInstance(),
                admin,
                clock,
                daycare.id,
                temporaryEmployeeId,
            )
        }
        assertThrows<NotFound> {
            unitAclController.deleteEarlyChildhoodEducationSecretary(
                dbInstance(),
                admin,
                clock,
                daycare.id,
                temporaryEmployeeId,
            )
        }
        assertThrows<NotFound> {
            unitAclController.deleteStaff(
                dbInstance(),
                admin,
                clock,
                daycare.id,
                temporaryEmployeeId,
            )
        }
    }

    @Test
    fun groupAccessCanBeAddedAndRemoved() {
        val group2 = DevDaycareGroup(daycareId = daycare.id, name = "Group 2")
        val unit2Group = DevDaycareGroup(daycareId = daycare2.id)
        db.transaction { tx ->
            tx.insert(group2)
            tx.insert(unit2Group)
            tx.insertDaycareAclRow(daycare.id, employee.id, UserRole.STAFF)
            tx.insertDaycareAclRow(daycare2.id, employee.id, UserRole.STAFF)
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
        unitAclController.updateGroupAcl(
            dbInstance(),
            admin,
            MockEvakaClock(moment1),
            daycare.id,
            employee.id,
            UnitAclController.AclUpdate(
                role = UserRole.STAFF,
                groupIds = listOf(daycareGroup.id, group2.id),
                hasStaffOccupancyEffect = null,
                endDate = null,
            ),
        )

        val step1Acls = readAllGroupAcls()
        assertThat(step1Acls)
            .containsExactlyInAnyOrder(
                DaycareGroupAcl(
                    daycareGroupId = daycareGroup.id,
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
        unitAclController.updateGroupAcl(
            dbInstance(),
            admin,
            MockEvakaClock(moment2),
            daycare2.id,
            employee.id,
            UnitAclController.AclUpdate(
                role = UserRole.STAFF,
                groupIds = listOf(unit2Group.id),
                hasStaffOccupancyEffect = null,
                endDate = null,
            ),
        )

        val step2Acls = readAllGroupAcls()
        assertThat(step2Acls)
            .containsExactlyInAnyOrder(
                DaycareGroupAcl(
                    daycareGroupId = daycareGroup.id,
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
        unitAclController.updateGroupAcl(
            dbInstance(),
            admin,
            MockEvakaClock(moment3),
            daycare.id,
            employee.id,
            UnitAclController.AclUpdate(
                role = UserRole.STAFF,
                groupIds = listOf(daycareGroup.id),
                hasStaffOccupancyEffect = null,
                endDate = null,
            ),
        )

        val step3Acls = readAllGroupAcls()
        assertThat(step3Acls)
            .containsExactlyInAnyOrder(
                DaycareGroupAcl(
                    daycareGroupId = daycareGroup.id,
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
                role = UserRole.STAFF,
                groupIds = null,
                hasStaffOccupancyEffect = null,
                endDate = null,
            ),
            daycare.id,
            employee.id,
        )

        assertThrows<NotFound> {
            unitAclController.getTemporaryEmployee(
                dbInstance(),
                admin,
                clock,
                daycare.id,
                employee.id,
            )
        }
        assertThrows<NotFound> {
            unitAclController.updateTemporaryEmployee(
                dbInstance(),
                admin,
                clock,
                daycare.id,
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
                daycare.id,
                employee.id,
            )
        }
        assertThrows<NotFound> {
            unitAclController.deleteTemporaryEmployee(
                dbInstance(),
                admin,
                clock,
                daycare.id,
                employee.id,
            )
        }
    }

    @Test
    fun `unit supervisor can update staff end date but not supervisor end date`() {
        val supervisor = DevEmployee()
        val staffMember = DevEmployee()
        val anotherSupervisor = DevEmployee()
        db.transaction { tx ->
            tx.insert(supervisor, unitRoles = mapOf(daycare.id to UserRole.UNIT_SUPERVISOR))
            tx.insert(staffMember, unitRoles = mapOf(daycare.id to UserRole.STAFF))
            tx.insert(anotherSupervisor, unitRoles = mapOf(daycare.id to UserRole.UNIT_SUPERVISOR))
        }

        val supervisorUser = AuthenticatedUser.Employee(supervisor.id, roles = emptySet())

        val endDate = clock.today().plusDays(30)

        // Supervisor can update staff end date
        unitAclController.updateGroupAcl(
            dbInstance(),
            supervisorUser,
            clock,
            daycare.id,
            staffMember.id,
            UnitAclController.AclUpdate(
                role = UserRole.STAFF,
                groupIds = emptyList(),
                hasStaffOccupancyEffect = null,
                endDate = endDate,
            ),
        )
        val staffAcl =
            db.read { tx -> tx.getDaycareAclRows(daycare.id, false, false, UserRole.STAFF) }
        assertEquals(endDate, staffAcl.first().endDate)

        // Supervisor can edit another supervisor's groups without changing end date
        unitAclController.updateGroupAcl(
            dbInstance(),
            supervisorUser,
            clock,
            daycare.id,
            anotherSupervisor.id,
            UnitAclController.AclUpdate(
                role = UserRole.UNIT_SUPERVISOR,
                groupIds = listOf(daycareGroup.id),
                hasStaffOccupancyEffect = null,
                endDate = null,
            ),
        )

        // Supervisor cannot update another supervisor's end date
        assertThrows<Forbidden> {
            unitAclController.updateGroupAcl(
                dbInstance(),
                supervisorUser,
                clock,
                daycare.id,
                anotherSupervisor.id,
                UnitAclController.AclUpdate(
                    role = UserRole.UNIT_SUPERVISOR,
                    groupIds = emptyList(),
                    hasStaffOccupancyEffect = null,
                    endDate = endDate,
                ),
            )
        }

        // Supervisor cannot delete another supervisor
        assertThrows<Forbidden> {
            unitAclController.deleteUnitSupervisor(
                dbInstance(),
                supervisorUser,
                clock,
                daycare.id,
                anotherSupervisor.id,
            )
        }

        // Supervisor cannot bypass permission check by passing wrong role
        assertThrows<BadRequest> {
            unitAclController.updateGroupAcl(
                dbInstance(),
                supervisorUser,
                clock,
                daycare.id,
                anotherSupervisor.id,
                UnitAclController.AclUpdate(
                    role = UserRole.STAFF,
                    groupIds = emptyList(),
                    hasStaffOccupancyEffect = null,
                    endDate = endDate,
                ),
            )
        }
    }

    @Test
    fun `user cannot modify own end date`() {
        val supervisor = DevEmployee()
        db.transaction { tx ->
            tx.insert(supervisor, unitRoles = mapOf(daycare.id to UserRole.UNIT_SUPERVISOR))
        }

        val supervisorUser = AuthenticatedUser.Employee(supervisor.id, roles = emptySet())

        // Cannot update own end date
        assertThrows<Forbidden> {
            unitAclController.updateGroupAcl(
                dbInstance(),
                supervisorUser,
                clock,
                daycare.id,
                supervisor.id,
                UnitAclController.AclUpdate(
                    role = UserRole.UNIT_SUPERVISOR,
                    groupIds = emptyList(),
                    hasStaffOccupancyEffect = null,
                    endDate = clock.today().plusDays(30),
                ),
            )
        }

        // Cannot delete own ACL
        assertThrows<Forbidden> {
            unitAclController.deleteUnitSupervisor(
                dbInstance(),
                supervisorUser,
                clock,
                daycare.id,
                supervisor.id,
            )
        }
    }

    private fun getAclRows(): List<DaycareAclRow> =
        unitAclController.getDaycareAcl(dbInstance(), admin, clock, daycare.id)

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
        daycareId: DaycareId,
        employeeId: EmployeeId,
    ) =
        unitAclController.addFullAclForRole(
            dbInstance(),
            admin,
            clock,
            daycareId,
            employeeId,
            update,
        )

    private fun modifyEmployee(
        update: UnitAclController.AclUpdate,
        daycareId: DaycareId,
        employeeId: EmployeeId,
        user: AuthenticatedUser.Employee = admin,
    ) = unitAclController.updateGroupAcl(dbInstance(), user, clock, daycareId, employeeId, update)

    private fun deleteStaff() =
        unitAclController.deleteStaff(dbInstance(), admin, clock, daycare.id, employee.id)

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
