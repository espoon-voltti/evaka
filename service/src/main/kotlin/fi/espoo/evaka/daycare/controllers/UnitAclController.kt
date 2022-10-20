// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.addEarlyChildhoodEducationSecretary
import fi.espoo.evaka.daycare.addSpecialEducationTeacher
import fi.espoo.evaka.daycare.addStaffMember
import fi.espoo.evaka.daycare.addUnitSupervisor
import fi.espoo.evaka.daycare.getDaycareAclRows
import fi.espoo.evaka.daycare.removeEarlyChildhoodEducationSecretary
import fi.espoo.evaka.daycare.removeSpecialEducationTeacher
import fi.espoo.evaka.daycare.removeStaffMember
import fi.espoo.evaka.daycare.removeUnitSupervisor
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.DaycareAclRow
import fi.espoo.evaka.shared.auth.clearDaycareGroupAcl
import fi.espoo.evaka.shared.auth.insertDaycareGroupAcl
import fi.espoo.evaka.shared.db.Database
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
        accessControl.requirePermissionFor(user, clock, Action.Unit.READ_ACL, daycareId)
        return DaycareAclResponse(
            db.connect { dbc -> getDaycareAclRows(dbc, daycareId) }
                .also {
                    Audit.UnitAclRead.log(targetId = daycareId, args = mapOf("count" to it.size))
                }
        )
    }

    @PutMapping("/daycares/{daycareId}/supervisors/{employeeId}")
    fun insertUnitSupervisor(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId
    ) {
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Unit.INSERT_ACL_UNIT_SUPERVISOR,
            daycareId
        )
        db.connect { dbc -> addUnitSupervisor(dbc, daycareId, employeeId) }
        Audit.UnitAclCreate.log(targetId = daycareId, objectId = employeeId)
    }

    @DeleteMapping("/daycares/{daycareId}/supervisors/{employeeId}")
    fun deleteUnitSupervisor(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId
    ) {
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Unit.DELETE_ACL_UNIT_SUPERVISOR,
            daycareId
        )
        db.connect { dbc -> removeUnitSupervisor(dbc, daycareId, employeeId) }
        Audit.UnitAclDelete.log(targetId = daycareId, objectId = employeeId)
    }

    @PutMapping("/daycares/{daycareId}/specialeducationteacher/{employeeId}")
    fun insertSpecialEducationTeacher(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId
    ) {
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Unit.INSERT_ACL_SPECIAL_EDUCATION_TEACHER,
            daycareId
        )
        db.connect { dbc -> addSpecialEducationTeacher(dbc, daycareId, employeeId) }
        Audit.UnitAclCreate.log(targetId = daycareId, objectId = employeeId)
    }

    @DeleteMapping("/daycares/{daycareId}/specialeducationteacher/{employeeId}")
    fun deleteSpecialEducationTeacher(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId
    ) {
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Unit.DELETE_ACL_SPECIAL_EDUCATION_TEACHER,
            daycareId
        )
        db.connect { dbc -> removeSpecialEducationTeacher(dbc, daycareId, employeeId) }
        Audit.UnitAclDelete.log(targetId = daycareId, objectId = employeeId)
    }

    @PutMapping("/daycares/{daycareId}/earlychildhoodeducationsecretary/{employeeId}")
    fun insertEarlyChildhoodEducationSecretary(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId
    ) {
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Unit.INSERT_ACL_EARLY_CHILDHOOD_EDUCATION_SECRETARY,
            daycareId
        )
        db.connect { dbc -> addEarlyChildhoodEducationSecretary(dbc, daycareId, employeeId) }
        Audit.UnitAclCreate.log(targetId = daycareId, objectId = employeeId)
    }

    @DeleteMapping("/daycares/{daycareId}/earlychildhoodeducationsecretary/{employeeId}")
    fun deleteEarlyChildhoodEducationSecretary(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId
    ) {
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Unit.DELETE_ACL_EARLY_CHILDHOOD_EDUCATION_SECRETARY,
            daycareId
        )
        db.connect { dbc -> removeEarlyChildhoodEducationSecretary(dbc, daycareId, employeeId) }
        Audit.UnitAclDelete.log(targetId = daycareId, objectId = employeeId)
    }

    @PutMapping("/daycares/{daycareId}/staff/{employeeId}")
    fun insertStaff(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId
    ) {
        accessControl.requirePermissionFor(user, clock, Action.Unit.INSERT_ACL_STAFF, daycareId)
        db.connect { dbc -> addStaffMember(dbc, daycareId, employeeId) }
        Audit.UnitAclCreate.log(targetId = daycareId, objectId = employeeId)
    }

    @DeleteMapping("/daycares/{daycareId}/staff/{employeeId}")
    fun deleteStaff(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId
    ) {
        accessControl.requirePermissionFor(user, clock, Action.Unit.DELETE_ACL_STAFF, daycareId)
        db.connect { dbc -> removeStaffMember(dbc, daycareId, employeeId) }
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
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Unit.UPDATE_STAFF_GROUP_ACL,
            daycareId
        )

        db.connect { dbc ->
            dbc.transaction {
                it.clearDaycareGroupAcl(daycareId, employeeId)
                it.insertDaycareGroupAcl(daycareId, employeeId, update.groupIds)
            }
        }
        Audit.UnitGroupAclUpdate.log(targetId = daycareId, objectId = employeeId)
    }

    data class GroupAclUpdate(val groupIds: List<GroupId>)
}

data class DaycareAclResponse(val rows: List<DaycareAclRow>)
