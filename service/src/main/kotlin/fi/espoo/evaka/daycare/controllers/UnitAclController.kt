// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.absence.getDaycareIdByGroup
import fi.espoo.evaka.attendance.OccupancyCoefficientUpsert
import fi.espoo.evaka.attendance.getOccupancyCoefficientForEmployeeInUnit
import fi.espoo.evaka.attendance.upsertOccupancyCoefficient
import fi.espoo.evaka.daycare.deactivatePersonalMessageAccountIfNeeded
import fi.espoo.evaka.messaging.deactivateEmployeeMessageAccount
import fi.espoo.evaka.messaging.upsertEmployeeMessageAccount
import fi.espoo.evaka.pairing.deletePersonalDevices
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.NewEmployee
import fi.espoo.evaka.pis.TemporaryEmployee
import fi.espoo.evaka.pis.createEmployee
import fi.espoo.evaka.pis.getEmployee
import fi.espoo.evaka.pis.getEmployeeRoles
import fi.espoo.evaka.pis.getPinCode
import fi.espoo.evaka.pis.getTemporaryEmployees
import fi.espoo.evaka.pis.updateEmployee
import fi.espoo.evaka.pis.updateEmployeeActive
import fi.espoo.evaka.pis.upsertPinCode
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.*
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.*
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.math.BigDecimal
import java.time.LocalDate
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UnitAclController(
    private val accessControl: AccessControl,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
    val coefficientPositiveValue = BigDecimal("7.00")
    val coefficientNegativeValue = BigDecimal("0.00")

    @GetMapping("/employee/daycares/{unitId}/acl")
    fun getDaycareAcl(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
    ): List<DaycareAclRow> {
        return db.connect { dbc ->
            dbc.read { tx ->
                accessControl.requirePermissionFor(tx, user, clock, Action.Unit.READ_ACL, unitId)
                val hasOccupancyPermission =
                    accessControl.hasPermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_STAFF_OCCUPANCY_COEFFICIENTS,
                        unitId,
                    )

                val aclRows =
                    tx.getDaycareAclRows(unitId, hasOccupancyPermission).map {
                        if (it.employee.active) it
                        else
                            it.copy(
                                employee =
                                    it.employee.copy(
                                        lastName = "${it.employee.lastName} (deaktivoitu)"
                                    )
                            )
                    }

                Audit.UnitAclRead.log(
                    targetId = AuditId(unitId),
                    meta = mapOf("count" to aclRows.size),
                )
                if (hasOccupancyPermission) {
                    Audit.StaffOccupancyCoefficientRead.log(
                        targetId = AuditId(unitId),
                        meta = mapOf("count" to aclRows.size),
                    )
                }
                aclRows
            }
        }
    }

    @GetMapping("/employee/daycares/{unitId}/scheduled-acl")
    fun getScheduledDaycareAcl(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
    ): List<ScheduledDaycareAclRow> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_ACL,
                        unitId,
                    )
                    tx.getScheduledDaycareAclRows(unitId)
                }
            }
            .also {
                Audit.UnitScheduledAclRead.log(
                    targetId = AuditId(unitId),
                    meta = mapOf("count" to it.size),
                )
            }
    }

    @DeleteMapping("/employee/daycares/{unitId}/supervisors/{employeeId}")
    fun deleteUnitSupervisor(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable employeeId: EmployeeId,
    ) {
        if (user.id == employeeId) throw Forbidden("Cannot modify own roles")
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Unit.DELETE_ACL_UNIT_SUPERVISOR,
                    unitId,
                )
                validateIsPermanentEmployee(it, employeeId)
                removeDaycareAclForRole(
                    it,
                    asyncJobRunner,
                    clock.now(),
                    unitId,
                    employeeId,
                    UserRole.UNIT_SUPERVISOR,
                )
            }
        }
        Audit.UnitAclDelete.log(targetId = AuditId(unitId), objectId = AuditId(employeeId))
    }

    @DeleteMapping("/employee/daycares/{unitId}/specialeducationteacher/{employeeId}")
    fun deleteSpecialEducationTeacher(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable employeeId: EmployeeId,
    ) {
        if (user.id == employeeId) throw Forbidden("Cannot modify own roles")
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Unit.DELETE_ACL_SPECIAL_EDUCATION_TEACHER,
                    unitId,
                )
                validateIsPermanentEmployee(it, employeeId)
                removeDaycareAclForRole(
                    it,
                    asyncJobRunner,
                    clock.now(),
                    unitId,
                    employeeId,
                    UserRole.SPECIAL_EDUCATION_TEACHER,
                )
            }
        }
        Audit.UnitAclDelete.log(targetId = AuditId(unitId), objectId = AuditId(employeeId))
    }

    @DeleteMapping("/employee/daycares/{unitId}/earlychildhoodeducationsecretary/{employeeId}")
    fun deleteEarlyChildhoodEducationSecretary(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable employeeId: EmployeeId,
    ) {
        if (user.id == employeeId) throw Forbidden("Cannot modify own roles")
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Unit.DELETE_ACL_EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                    unitId,
                )
                validateIsPermanentEmployee(it, employeeId)
                removeDaycareAclForRole(
                    it,
                    asyncJobRunner,
                    clock.now(),
                    unitId,
                    employeeId,
                    UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
            }
        }
        Audit.UnitAclDelete.log(targetId = AuditId(unitId), objectId = AuditId(employeeId))
    }

    @DeleteMapping("/employee/daycares/{unitId}/staff/{employeeId}")
    fun deleteStaff(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable employeeId: EmployeeId,
    ) {
        if (user.id == employeeId) throw Forbidden("Cannot modify own roles")
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Unit.DELETE_ACL_STAFF,
                    unitId,
                )
                validateIsPermanentEmployee(it, employeeId)
                removeDaycareAclForRole(
                    it,
                    asyncJobRunner,
                    clock.now(),
                    unitId,
                    employeeId,
                    UserRole.STAFF,
                )
            }
        }
        Audit.UnitAclDelete.log(targetId = AuditId(unitId), objectId = AuditId(employeeId))
    }

    @DeleteMapping("/employee/daycares/{unitId}/scheduled/{employeeId}")
    fun deleteScheduledAcl(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable employeeId: EmployeeId,
    ) {
        if (user.id == employeeId) throw Forbidden("Cannot modify own roles")
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Unit.DELETE_ACL_SCHEDULED,
                    unitId,
                )
                validateIsPermanentEmployee(it, employeeId)
                it.deleteScheduledDaycareAclRow(employeeId, unitId)
            }
        }
        Audit.UnitAclDeleteScheduled.log(targetId = AuditId(unitId), objectId = AuditId(employeeId))
    }

    @PutMapping("/employee/daycares/{unitId}/staff/{employeeId}/groups")
    fun updateGroupAclWithOccupancyCoefficient(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable employeeId: EmployeeId,
        @RequestBody update: AclUpdate,
    ) {
        if (update.groupIds == null && update.hasStaffOccupancyEffect == null) {
            throw BadRequest("Request is missing all update content")
        }
        val occupancyCoefficientId =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.UPDATE_STAFF_GROUP_ACL,
                        unitId,
                    )
                    validateIsPermanentEmployee(tx, employeeId)

                    if (update.endDate?.isBefore(clock.today()) == true) {
                        throw BadRequest("End date cannot be in the past")
                    }
                    tx.updateAclRowEndDate(unitId, employeeId, update.endDate)

                    update.groupIds?.let {
                        tx.syncDaycareGroupAcl(unitId, employeeId, it, clock.now())
                    }

                    val occupancyCoefficientId =
                        update.hasStaffOccupancyEffect?.let {
                            accessControl.requirePermissionFor(
                                tx,
                                user,
                                clock,
                                Action.Unit.UPSERT_STAFF_OCCUPANCY_COEFFICIENTS,
                                unitId,
                            )
                            tx.upsertOccupancyCoefficient(
                                OccupancyCoefficientUpsert(
                                    unitId = unitId,
                                    employeeId = employeeId,
                                    coefficient = parseCoefficientValue(it),
                                )
                            )
                        }
                    occupancyCoefficientId
                }
            }
        if (update.groupIds != null) {
            Audit.UnitGroupAclUpdate.log(targetId = AuditId(unitId), objectId = AuditId(employeeId))
        }

        if (update.hasStaffOccupancyEffect != null) {
            Audit.StaffOccupancyCoefficientUpsert.log(
                targetId = AuditId(listOf(unitId, employeeId)),
                objectId = occupancyCoefficientId?.let(AuditId::invoke),
            )
        }
    }

    @PutMapping("/employee/daycares/{unitId}/full-acl/{employeeId}")
    fun addFullAclForRole(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable employeeId: EmployeeId,
        @RequestBody aclInfo: FullAclInfo,
    ) {
        if (user.id == employeeId) throw Forbidden("Cannot modify own roles")
        val occupancyCoefficientId =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    val roleAction = getRoleAddAction(aclInfo.role)
                    accessControl.requirePermissionFor(tx, user, clock, roleAction, unitId)
                    validateIsPermanentEmployee(tx, employeeId)
                    tx.deleteScheduledDaycareAclRow(employeeId, unitId)
                    tx.insertDaycareAclRow(unitId, employeeId, aclInfo.role, aclInfo.update.endDate)
                    tx.upsertEmployeeMessageAccount(employeeId)
                    aclInfo.update.groupIds?.let {
                        accessControl.requirePermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Unit.UPDATE_STAFF_GROUP_ACL,
                            unitId,
                        )
                        tx.syncDaycareGroupAcl(unitId, employeeId, it, clock.now())
                    }
                    val occupancyCoefficientId =
                        aclInfo.update.hasStaffOccupancyEffect?.let {
                            accessControl.requirePermissionFor(
                                tx,
                                user,
                                clock,
                                Action.Unit.UPSERT_STAFF_OCCUPANCY_COEFFICIENTS,
                                unitId,
                            )
                            tx.upsertOccupancyCoefficient(
                                OccupancyCoefficientUpsert(
                                    unitId = unitId,
                                    employeeId = employeeId,
                                    coefficient = parseCoefficientValue(it),
                                )
                            )
                        }
                    occupancyCoefficientId
                }
            }
        Audit.UnitAclCreate.log(targetId = AuditId(unitId), objectId = AuditId(employeeId))
        if (aclInfo.update.groupIds != null) {
            Audit.UnitGroupAclUpdate.log(targetId = AuditId(unitId), objectId = AuditId(employeeId))
        }
        if (aclInfo.update.hasStaffOccupancyEffect != null) {
            Audit.StaffOccupancyCoefficientUpsert.log(
                targetId = AuditId(listOf(unitId, employeeId)),
                objectId = occupancyCoefficientId?.let(AuditId::invoke),
            )
        }
    }

    data class FullAclInfo(val role: UserRole, val update: AclUpdate)

    data class AclUpdate(
        val groupIds: List<GroupId>?,
        val hasStaffOccupancyEffect: Boolean?,
        val endDate: LocalDate?,
    )

    fun getRoleAddAction(role: UserRole): Action.Unit =
        when (role) {
            UserRole.STAFF -> Action.Unit.INSERT_ACL_STAFF
            UserRole.UNIT_SUPERVISOR -> Action.Unit.INSERT_ACL_UNIT_SUPERVISOR
            UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY ->
                Action.Unit.INSERT_ACL_EARLY_CHILDHOOD_EDUCATION_SECRETARY
            UserRole.SPECIAL_EDUCATION_TEACHER -> Action.Unit.INSERT_ACL_SPECIAL_EDUCATION_TEACHER
            else -> throw BadRequest("Invalid daycare acl role: $role")
        }

    @GetMapping("/employee/daycares/{unitId}/temporary")
    fun getTemporaryEmployees(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
    ): List<Employee> {
        val employees =
            db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_TEMPORARY_EMPLOYEE,
                        unitId,
                    )
                    tx.getTemporaryEmployees(unitId).filter { it.active }
                }
            }
        Audit.TemporaryEmployeesRead.log(meta = mapOf("unitId" to unitId))
        return employees
    }

    @PostMapping("/employee/daycares/{unitId}/temporary")
    fun createTemporaryEmployee(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @RequestBody input: TemporaryEmployee,
    ): EmployeeId {
        val employeeId =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.CREATE_TEMPORARY_EMPLOYEE,
                        unitId,
                    )
                    val employee =
                        tx.createEmployee(
                            NewEmployee(
                                firstName = input.firstName,
                                lastName = input.lastName,
                                email = null,
                                externalId = null,
                                employeeNumber = null,
                                temporaryInUnitId = unitId,
                                active = true,
                            )
                        )
                    setTemporaryEmployeeDetails(tx, unitId, employee.id, input, clock.now())
                    employee.id
                }
            }
        Audit.TemporaryEmployeeCreate.log(
            targetId = AuditId(employeeId),
            meta = mapOf("unitId" to unitId),
        )
        return employeeId
    }

    @GetMapping("/employee/daycares/{unitId}/temporary/{employeeId}")
    fun getTemporaryEmployee(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable employeeId: EmployeeId,
    ): TemporaryEmployee {
        val employee =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    val employee = getTemporaryEmployee(tx, unitId, employeeId)
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_TEMPORARY_EMPLOYEE,
                        unitId,
                    )
                    val groupIds =
                        tx.getDaycareAclRows(daycareId = unitId, false)
                            .filter { it.employee.id == employee.id && it.role == UserRole.STAFF }
                            .flatMap { it.groupIds }
                            .toSet()
                    val occupancyCoefficient =
                        tx.getOccupancyCoefficientForEmployeeInUnit(
                            employeeId = employeeId,
                            unitId = unitId,
                        ) ?: BigDecimal.ZERO
                    val pinCode = tx.getPinCode(employee.id)
                    TemporaryEmployee(
                        firstName = employee.firstName,
                        lastName = employee.lastName,
                        groupIds = groupIds,
                        hasStaffOccupancyEffect = occupancyCoefficient > BigDecimal.ZERO,
                        pinCode = pinCode,
                    )
                }
            }
        Audit.TemporaryEmployeeRead.log(
            targetId = AuditId(employeeId),
            meta = mapOf("unitId" to unitId),
        )
        return employee
    }

    @PutMapping("/employee/daycares/{unitId}/temporary/{employeeId}")
    fun updateTemporaryEmployee(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable employeeId: EmployeeId,
        @RequestBody input: TemporaryEmployee,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                val employee = getTemporaryEmployee(tx, unitId, employeeId)
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.UPDATE_TEMPORARY_EMPLOYEE,
                    unitId,
                )
                tx.updateEmployee(
                    id = employee.id,
                    firstName = input.firstName,
                    lastName = input.lastName,
                )
                setTemporaryEmployeeDetails(tx, unitId, employee.id, input, clock.now())
            }
        }
        Audit.TemporaryEmployeeUpdate.log(
            targetId = AuditId(employeeId),
            meta = mapOf("unitId" to unitId),
        )
    }

    @PutMapping("/employee/daycares/{unitId}/temporary/{employeeId}/reactivate")
    fun reactivateTemporaryEmployee(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable employeeId: EmployeeId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.UPDATE_TEMPORARY_EMPLOYEE,
                    unitId,
                )
                tx.insertDaycareAclRow(unitId, employeeId, UserRole.STAFF, endDate = null)
                tx.upsertEmployeeMessageAccount(employeeId)
            }
        }
        Audit.TemporaryEmployeeUpdate.log(
            targetId = AuditId(employeeId),
            meta = mapOf("unitId" to unitId),
        )
    }

    @DeleteMapping("/employee/daycares/{unitId}/temporary/{employeeId}/acl")
    fun deleteTemporaryEmployeeAcl(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable employeeId: EmployeeId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                val employee = getTemporaryEmployee(tx, unitId, employeeId)
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.UPDATE_TEMPORARY_EMPLOYEE,
                    unitId,
                )
                deleteTemporaryEmployeeAcl(tx, unitId, employee)
            }
        }
        Audit.TemporaryEmployeeDeleteAcl.log(
            targetId = AuditId(employeeId),
            meta = mapOf("unitId" to unitId),
        )
    }

    @DeleteMapping("/employee/daycares/{unitId}/temporary/{employeeId}")
    fun deleteTemporaryEmployee(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable employeeId: EmployeeId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                val employee = getTemporaryEmployee(tx, unitId, employeeId)
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.DELETE_TEMPORARY_EMPLOYEE,
                    unitId,
                )
                tx.updateEmployeeActive(employeeId, active = false)
                deleteTemporaryEmployeeAcl(tx, unitId, employee)
            }
        }
        Audit.TemporaryEmployeeDelete.log(
            targetId = AuditId(employeeId),
            meta = mapOf("unitId" to unitId),
        )
    }

    private fun setTemporaryEmployeeDetails(
        tx: Database.Transaction,
        unitId: DaycareId,
        employeeId: EmployeeId,
        input: TemporaryEmployee,
        now: HelsinkiDateTime,
    ) {
        if (
            input.groupIds.isNotEmpty() &&
                input.groupIds.any { groupId -> tx.getDaycareIdByGroup(groupId) != unitId }
        ) {
            throw Forbidden("All groups must be in unit")
        }

        tx.insertDaycareAclRow(unitId, employeeId, UserRole.STAFF, endDate = null)
        tx.syncDaycareGroupAcl(unitId, employeeId, input.groupIds, now)

        tx.upsertEmployeeMessageAccount(employeeId)
        tx.upsertOccupancyCoefficient(
            OccupancyCoefficientUpsert(
                unitId,
                employeeId,
                parseCoefficientValue(input.hasStaffOccupancyEffect),
            )
        )
        if (input.pinCode != null) {
            tx.upsertPinCode(employeeId, input.pinCode)
        }
    }

    private fun validateIsPermanentEmployee(tx: Database.Read, employeeId: EmployeeId) {
        val employee = tx.getEmployee(employeeId)
        if (employee == null || employee.temporaryInUnitId != null) {
            throw NotFound()
        }
    }

    private fun getTemporaryEmployee(
        tx: Database.Read,
        unitId: DaycareId,
        employeeId: EmployeeId,
    ): Employee =
        tx.getEmployee(employeeId).takeIf { it?.temporaryInUnitId == unitId && it.active }
            ?: throw NotFound()

    private fun deleteTemporaryEmployeeAcl(
        tx: Database.Transaction,
        unitId: DaycareId,
        employee: Employee,
    ) {
        tx.syncDaycareGroupAcl(unitId, employee.id, emptyList())
        tx.deleteDaycareAclRow(unitId, employee.id, UserRole.STAFF)
        if (!tx.hasAnyDaycareAclRow(employee.id)) {
            tx.deactivateEmployeeMessageAccount(employee.id)
        }
    }

    fun parseCoefficientValue(bool: Boolean) =
        if (bool) coefficientPositiveValue else coefficientNegativeValue

    init {
        asyncJobRunner.registerHandler(::deletePersonalMobileDevicesIfNeeded)
    }

    private fun deletePersonalMobileDevicesIfNeeded(
        db: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.DeletePersonalDevicesIfNeeded,
    ) {
        db.transaction { tx ->
            if (
                !accessControl.hasPermissionFor(
                    tx,
                    AuthenticatedUser.Employee(job.employeeId, tx.getEmployeeRoles(job.employeeId)),
                    clock,
                    Action.Global.CREATE_PERSONAL_MOBILE_DEVICE_PAIRING,
                )
            ) {
                tx.deletePersonalDevices(job.employeeId)
            }
        }
    }
}

fun removeDaycareAclForRole(
    tx: Database.Transaction,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    now: HelsinkiDateTime,
    unitId: DaycareId,
    employeeId: EmployeeId,
    role: UserRole,
) {
    tx.syncDaycareGroupAcl(unitId, employeeId, emptyList())
    tx.deleteDaycareAclRow(unitId, employeeId, role)
    deactivatePersonalMessageAccountIfNeeded(tx, employeeId)

    // Delete personal mobile devices after a while, in case the employee is added back to this
    // or some other unit
    asyncJobRunner.plan(
        tx,
        listOf(AsyncJob.DeletePersonalDevicesIfNeeded(employeeId)),
        runAt = now.plusHours(1),
    )
}
