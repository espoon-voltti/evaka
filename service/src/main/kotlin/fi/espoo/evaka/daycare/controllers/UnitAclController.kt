// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.absence.getDaycareIdByGroup
import fi.espoo.evaka.attendance.OccupancyCoefficientUpsert
import fi.espoo.evaka.attendance.getOccupancyCoefficientForEmployeeInUnit
import fi.espoo.evaka.attendance.upsertOccupancyCoefficient
import fi.espoo.evaka.daycare.removeDaycareAclForRole
import fi.espoo.evaka.messaging.deactivateEmployeeMessageAccount
import fi.espoo.evaka.messaging.upsertEmployeeMessageAccount
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.NewEmployee
import fi.espoo.evaka.pis.TemporaryEmployee
import fi.espoo.evaka.pis.createEmployee
import fi.espoo.evaka.pis.getEmployee
import fi.espoo.evaka.pis.getPinCode
import fi.espoo.evaka.pis.getTemporaryEmployees
import fi.espoo.evaka.pis.updateEmployee
import fi.espoo.evaka.pis.updateEmployeeActive
import fi.espoo.evaka.pis.upsertPinCode
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.DaycareAclRow
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.clearDaycareGroupAcl
import fi.espoo.evaka.shared.auth.deleteDaycareAclRow
import fi.espoo.evaka.shared.auth.getDaycareAclRows
import fi.espoo.evaka.shared.auth.hasAnyDaycareAclRow
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.auth.insertDaycareGroupAcl
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.math.BigDecimal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UnitAclController(private val accessControl: AccessControl) {

    val coefficientPositiveValue = BigDecimal("7.00")
    val coefficientNegativeValue = BigDecimal("0.00")

    @GetMapping("/daycares/{daycareId}/acl")
    fun getAcl(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId
    ): DaycareAclResponse {
        return DaycareAclResponse(
            db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_ACL,
                        daycareId
                    )
                    val hasOccupancyPermission =
                        accessControl.hasPermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Unit.READ_STAFF_OCCUPANCY_COEFFICIENTS,
                            daycareId
                        )

                    val aclRows =
                        tx.getDaycareAclRows(daycareId, hasOccupancyPermission).map {
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
                        targetId = daycareId,
                        meta = mapOf("count" to aclRows.size)
                    )
                    if (hasOccupancyPermission) {
                        Audit.StaffOccupancyCoefficientRead.log(
                            targetId = daycareId,
                            meta = mapOf("count" to aclRows.size)
                        )
                    }
                    aclRows
                }
            }
        )
    }

    @DeleteMapping("/daycares/{daycareId}/supervisors/{employeeId}")
    fun deleteUnitSupervisor(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Unit.DELETE_ACL_UNIT_SUPERVISOR,
                    daycareId
                )
                validateIsPermanentEmployee(it, employeeId)
                removeDaycareAclForRole(it, daycareId, employeeId, UserRole.UNIT_SUPERVISOR)
            }
        }
        Audit.UnitAclDelete.log(targetId = daycareId, objectId = employeeId)
    }

    @DeleteMapping("/daycares/{daycareId}/specialeducationteacher/{employeeId}")
    fun deleteSpecialEducationTeacher(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Unit.DELETE_ACL_SPECIAL_EDUCATION_TEACHER,
                    daycareId
                )
                validateIsPermanentEmployee(it, employeeId)
                removeDaycareAclForRole(
                    it,
                    daycareId,
                    employeeId,
                    UserRole.SPECIAL_EDUCATION_TEACHER
                )
            }
        }
        Audit.UnitAclDelete.log(targetId = daycareId, objectId = employeeId)
    }

    @DeleteMapping("/daycares/{daycareId}/earlychildhoodeducationsecretary/{employeeId}")
    fun deleteEarlyChildhoodEducationSecretary(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Unit.DELETE_ACL_EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                    daycareId
                )
                validateIsPermanentEmployee(it, employeeId)
                removeDaycareAclForRole(
                    it,
                    daycareId,
                    employeeId,
                    UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY
                )
            }
        }
        Audit.UnitAclDelete.log(targetId = daycareId, objectId = employeeId)
    }

    @DeleteMapping("/daycares/{daycareId}/staff/{employeeId}")
    fun deleteStaff(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Unit.DELETE_ACL_STAFF,
                    daycareId
                )
                validateIsPermanentEmployee(it, employeeId)
                removeDaycareAclForRole(it, daycareId, employeeId, UserRole.STAFF)
            }
        }
        Audit.UnitAclDelete.log(targetId = daycareId, objectId = employeeId)
    }

    @PutMapping("/daycares/{daycareId}/staff/{employeeId}/groups")
    fun updateGroupAclWithOccupancyCoefficient(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId,
        @RequestBody update: AclUpdate
    ) {
        if (update.groupIds == null && update.hasStaffOccupancyEffect == null) {
            throw BadRequest("Request is missing all update content")
        }
        val occupancyCoefficientId =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    update.groupIds?.let {
                        accessControl.requirePermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Unit.UPDATE_STAFF_GROUP_ACL,
                            daycareId
                        )
                        validateIsPermanentEmployee(tx, employeeId)
                        tx.clearDaycareGroupAcl(daycareId, employeeId)
                        tx.insertDaycareGroupAcl(daycareId, employeeId, it)
                    }

                    val occupancyCoefficientId =
                        update.hasStaffOccupancyEffect?.let {
                            accessControl.requirePermissionFor(
                                tx,
                                user,
                                clock,
                                Action.Unit.UPSERT_STAFF_OCCUPANCY_COEFFICIENTS,
                                daycareId
                            )
                            tx.upsertOccupancyCoefficient(
                                OccupancyCoefficientUpsert(
                                    unitId = daycareId,
                                    employeeId = employeeId,
                                    coefficient = parseCoefficientValue(it)
                                )
                            )
                        }
                    occupancyCoefficientId
                }
            }
        if (update.groupIds != null) {
            Audit.UnitGroupAclUpdate.log(targetId = daycareId, objectId = employeeId)
        }

        if (update.hasStaffOccupancyEffect != null) {
            Audit.StaffOccupancyCoefficientUpsert.log(
                targetId = listOf(daycareId, employeeId),
                objectId = occupancyCoefficientId
            )
        }
    }

    @PutMapping("/daycares/{daycareId}/full-acl/{employeeId}")
    fun addFullAclForRole(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId,
        @RequestBody aclInfo: FullAclInfo
    ) {
        val occupancyCoefficientId =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    val roleAction = getRoleAddAction(aclInfo.role)
                    accessControl.requirePermissionFor(tx, user, clock, roleAction, daycareId)
                    validateIsPermanentEmployee(tx, employeeId)
                    tx.clearDaycareGroupAcl(daycareId, employeeId)
                    tx.insertDaycareAclRow(daycareId, employeeId, aclInfo.role)
                    tx.upsertEmployeeMessageAccount(employeeId)
                    aclInfo.update.groupIds?.let {
                        accessControl.requirePermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Unit.UPDATE_STAFF_GROUP_ACL,
                            daycareId
                        )
                        tx.insertDaycareGroupAcl(daycareId, employeeId, it)
                    }
                    val occupancyCoefficientId =
                        aclInfo.update.hasStaffOccupancyEffect?.let {
                            accessControl.requirePermissionFor(
                                tx,
                                user,
                                clock,
                                Action.Unit.UPSERT_STAFF_OCCUPANCY_COEFFICIENTS,
                                daycareId
                            )
                            tx.upsertOccupancyCoefficient(
                                OccupancyCoefficientUpsert(
                                    unitId = daycareId,
                                    employeeId = employeeId,
                                    coefficient = parseCoefficientValue(it)
                                )
                            )
                        }
                    occupancyCoefficientId
                }
            }
        Audit.UnitAclCreate.log(targetId = daycareId, objectId = employeeId)
        if (aclInfo.update.groupIds != null) {
            Audit.UnitGroupAclUpdate.log(targetId = daycareId, objectId = employeeId)
        }
        if (aclInfo.update.hasStaffOccupancyEffect != null) {
            Audit.StaffOccupancyCoefficientUpsert.log(
                targetId = listOf(daycareId, employeeId),
                objectId = occupancyCoefficientId
            )
        }
    }

    data class FullAclInfo(val role: UserRole, val update: AclUpdate)

    data class AclUpdate(val groupIds: List<GroupId>?, val hasStaffOccupancyEffect: Boolean?)

    data class DaycareAclResponse(val aclRows: List<DaycareAclRow>)

    fun getRoleAddAction(role: UserRole): Action.Unit =
        when (role) {
            UserRole.STAFF -> Action.Unit.INSERT_ACL_STAFF
            UserRole.UNIT_SUPERVISOR -> Action.Unit.INSERT_ACL_UNIT_SUPERVISOR
            UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY ->
                Action.Unit.INSERT_ACL_EARLY_CHILDHOOD_EDUCATION_SECRETARY
            UserRole.SPECIAL_EDUCATION_TEACHER -> Action.Unit.INSERT_ACL_SPECIAL_EDUCATION_TEACHER
            else -> throw BadRequest("Invalid daycare acl role: $role")
        }

    @GetMapping("/daycares/{unitId}/temporary")
    fun getTemporaryEmployees(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId
    ): List<Employee> {
        val employees =
            db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_TEMPORARY_EMPLOYEE,
                        unitId
                    )
                    tx.getTemporaryEmployees(unitId).filter { it.active }
                }
            }
        Audit.TemporaryEmployeesRead.log(meta = mapOf("unitId" to unitId))
        return employees
    }

    @PostMapping("/daycares/{unitId}/temporary")
    fun createTemporaryEmployee(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @RequestBody input: TemporaryEmployee
    ): EmployeeId {
        val employeeId =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.CREATE_TEMPORARY_EMPLOYEE,
                        unitId
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
                                active = true
                            )
                        )
                    setTemporaryEmployeeDetails(tx, unitId, employee.id, input)
                    employee.id
                }
            }
        Audit.TemporaryEmployeeCreate.log(meta = mapOf("unitId" to unitId), targetId = employeeId)
        return employeeId
    }

    @GetMapping("/daycares/{unitId}/temporary/{employeeId}")
    fun getTemporaryEmployee(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable employeeId: EmployeeId
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
                        unitId
                    )
                    val groupIds =
                        tx.getDaycareAclRows(daycareId = unitId, false)
                            .filter { it.employee.id == employee.id && it.role == UserRole.STAFF }
                            .flatMap { it.groupIds }
                            .toSet()
                    val occupancyCoefficient =
                        tx.getOccupancyCoefficientForEmployeeInUnit(
                            employeeId = employeeId,
                            unitId = unitId
                        ) ?: BigDecimal.ZERO
                    val pinCode = tx.getPinCode(employee.id)
                    TemporaryEmployee(
                        firstName = employee.firstName,
                        lastName = employee.lastName,
                        groupIds = groupIds,
                        hasStaffOccupancyEffect = occupancyCoefficient > BigDecimal.ZERO,
                        pinCode = pinCode
                    )
                }
            }
        Audit.TemporaryEmployeeRead.log(meta = mapOf("unitId" to unitId), targetId = employeeId)
        return employee
    }

    @PutMapping("/daycares/{unitId}/temporary/{employeeId}")
    fun updateTemporaryEmployee(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable employeeId: EmployeeId,
        @RequestBody input: TemporaryEmployee
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                val employee = getTemporaryEmployee(tx, unitId, employeeId)
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.UPDATE_TEMPORARY_EMPLOYEE,
                    unitId
                )
                tx.updateEmployee(
                    id = employee.id,
                    firstName = input.firstName,
                    lastName = input.lastName
                )
                setTemporaryEmployeeDetails(tx, unitId, employee.id, input)
            }
        }
        Audit.TemporaryEmployeeUpdate.log(meta = mapOf("unitId" to unitId), targetId = employeeId)
    }

    @DeleteMapping("/daycares/{unitId}/temporary/{employeeId}/acl")
    fun deleteTemporaryEmployeeAcl(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable employeeId: EmployeeId
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                val employee = getTemporaryEmployee(tx, unitId, employeeId)
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.UPDATE_TEMPORARY_EMPLOYEE,
                    unitId
                )
                deleteTemporaryEmployeeAcl(tx, unitId, employee)
            }
        }
        Audit.TemporaryEmployeeDeleteAcl.log(
            meta = mapOf("unitId" to unitId),
            targetId = employeeId
        )
    }

    @DeleteMapping("/daycares/{unitId}/temporary/{employeeId}")
    fun deleteTemporaryEmployee(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable employeeId: EmployeeId
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                val employee = getTemporaryEmployee(tx, unitId, employeeId)
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.DELETE_TEMPORARY_EMPLOYEE,
                    unitId
                )
                tx.updateEmployeeActive(employeeId, active = false)
                deleteTemporaryEmployeeAcl(tx, unitId, employee)
            }
        }
        Audit.TemporaryEmployeeDelete.log(meta = mapOf("unitId" to unitId), targetId = employeeId)
    }

    private fun setTemporaryEmployeeDetails(
        tx: Database.Transaction,
        unitId: DaycareId,
        employeeId: EmployeeId,
        input: TemporaryEmployee
    ) {
        if (
            input.groupIds.isNotEmpty() &&
                input.groupIds.any { groupId -> tx.getDaycareIdByGroup(groupId) != unitId }
        ) {
            throw Forbidden("All groups must be in unit")
        }

        tx.clearDaycareGroupAcl(unitId, employeeId)
        tx.insertDaycareAclRow(unitId, employeeId, UserRole.STAFF)
        tx.insertDaycareGroupAcl(unitId, employeeId, input.groupIds.toList())

        tx.upsertEmployeeMessageAccount(employeeId)
        tx.upsertOccupancyCoefficient(
            OccupancyCoefficientUpsert(
                unitId,
                employeeId,
                parseCoefficientValue(input.hasStaffOccupancyEffect)
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
        employeeId: EmployeeId
    ): Employee =
        tx.getEmployee(employeeId).takeIf { it?.temporaryInUnitId == unitId && it.active }
            ?: throw NotFound()

    private fun deleteTemporaryEmployeeAcl(
        tx: Database.Transaction,
        unitId: DaycareId,
        employee: Employee
    ) {
        tx.clearDaycareGroupAcl(unitId, employee.id)
        tx.deleteDaycareAclRow(unitId, employee.id, UserRole.STAFF)
        if (!tx.hasAnyDaycareAclRow(employee.id)) {
            tx.deactivateEmployeeMessageAccount(employee.id)
        }
    }

    fun parseCoefficientValue(bool: Boolean) =
        if (bool) coefficientPositiveValue else coefficientNegativeValue
}
