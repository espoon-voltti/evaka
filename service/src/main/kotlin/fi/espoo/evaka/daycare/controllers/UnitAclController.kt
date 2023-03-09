// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.getDaycareAclRows
import fi.espoo.evaka.daycare.removeEarlyChildhoodEducationSecretary
import fi.espoo.evaka.daycare.removeSpecialEducationTeacher
import fi.espoo.evaka.daycare.removeStaffMember
import fi.espoo.evaka.daycare.removeUnitSupervisor
import fi.espoo.evaka.messaging.upsertEmployeeMessageAccount
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.DaycareAclRow
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.clearDaycareGroupAcl
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.auth.insertDaycareGroupAcl
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UnitAclController(private val accessControl: AccessControl) {
    @GetMapping("/daycares/{daycareId}/acl")
    fun getAcl(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId
    ): DaycareAclResponse {
        return DaycareAclResponse(
            db.connect { dbc ->
                    dbc.read {
                        accessControl.requirePermissionFor(
                            it,
                            user,
                            clock,
                            Action.Unit.READ_ACL,
                            daycareId
                        )
                    }
                    getDaycareAclRows(dbc, daycareId)
                }
                .also {
                    Audit.UnitAclRead.log(targetId = daycareId, meta = mapOf("count" to it.size))
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
            dbc.read {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Unit.DELETE_ACL_UNIT_SUPERVISOR,
                    daycareId
                )
            }
            removeUnitSupervisor(dbc, daycareId, employeeId)
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
            dbc.read {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Unit.DELETE_ACL_SPECIAL_EDUCATION_TEACHER,
                    daycareId
                )
            }
            removeSpecialEducationTeacher(dbc, daycareId, employeeId)
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
            dbc.read {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Unit.DELETE_ACL_EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                    daycareId
                )
            }
            removeEarlyChildhoodEducationSecretary(dbc, daycareId, employeeId)
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
            dbc.read {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Unit.DELETE_ACL_STAFF,
                    daycareId
                )
            }
            removeStaffMember(dbc, daycareId, employeeId)
        }
        Audit.UnitAclDelete.log(targetId = daycareId, objectId = employeeId)
    }

    @PutMapping("/daycares/{daycareId}/staff/{employeeId}/groups")
    fun updateStaffGroupAcl(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId,
        @RequestBody update: GroupAclUpdate
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Unit.UPDATE_STAFF_GROUP_ACL,
                    daycareId
                )
                it.clearDaycareGroupAcl(daycareId, employeeId)
                it.insertDaycareGroupAcl(daycareId, employeeId, update.groupIds)
            }
        }
        Audit.UnitGroupAclUpdate.log(targetId = daycareId, objectId = employeeId)
    }

    data class GroupAclUpdate(val groupIds: List<GroupId>)

    @PutMapping("/daycares/{daycareId}/full-acl/{employeeId}")
    fun addDaycareAclWithGroupsForRole(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId,
        @RequestBody aclUpdate: FullAclUpdate
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                val roleAction = getRoleAddAction(aclUpdate.role)
                accessControl.requirePermissionFor(tx, user, clock, roleAction, daycareId)
                tx.clearDaycareGroupAcl(daycareId, employeeId)
                tx.insertDaycareAclRow(daycareId, employeeId, aclUpdate.role)
                tx.upsertEmployeeMessageAccount(employeeId)
                aclUpdate.groupIds?.let {
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.UPDATE_STAFF_GROUP_ACL,
                        daycareId
                    )
                    tx.insertDaycareGroupAcl(daycareId, employeeId, it)
                }
            }
        }
        Audit.UnitAclCreate.log(targetId = daycareId, objectId = employeeId)
        if (aclUpdate.groupIds != null) {
            Audit.UnitGroupAclUpdate.log(targetId = daycareId, objectId = employeeId)
        }
    }

    data class FullAclUpdate(val groupIds: List<GroupId>?, val role: UserRole)

    data class DaycareAclResponse(val rows: List<DaycareAclRow>)

    fun getRoleAddAction(role: UserRole): Action.Unit =
        when (role) {
            UserRole.STAFF -> Action.Unit.INSERT_ACL_STAFF
            UserRole.UNIT_SUPERVISOR -> Action.Unit.INSERT_ACL_UNIT_SUPERVISOR
            UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY ->
                Action.Unit.INSERT_ACL_EARLY_CHILDHOOD_EDUCATION_SECRETARY
            UserRole.SPECIAL_EDUCATION_TEACHER -> Action.Unit.INSERT_ACL_SPECIAL_EDUCATION_TEACHER
            else -> throw BadRequest("Invalid daycare acl role: $role")
        }
}
