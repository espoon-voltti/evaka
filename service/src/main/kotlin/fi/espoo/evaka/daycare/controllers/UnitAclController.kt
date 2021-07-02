// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.addSpecialEducationTeacher
import fi.espoo.evaka.daycare.addStaffMember
import fi.espoo.evaka.daycare.addUnitSupervisor
import fi.espoo.evaka.daycare.getDaycareAclRows
import fi.espoo.evaka.daycare.removeSpecialEducationTeacher
import fi.espoo.evaka.daycare.removeStaffMember
import fi.espoo.evaka.daycare.removeUnitSupervisor
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.DaycareAclRow
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.clearDaycareGroupAcl
import fi.espoo.evaka.shared.auth.insertDaycareGroupAcl
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UnitAclController(private val acl: AccessControlList) {
    @GetMapping("/daycares/{daycareId}/acl")
    fun getAcl(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable daycareId: DaycareId
    ): ResponseEntity<DaycareAclResponse> {
        Audit.UnitAclRead.log()
        acl.getRolesForUnit(user, daycareId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)
        val acls = getDaycareAclRows(db, daycareId)
        return ResponseEntity.ok(DaycareAclResponse(acls))
    }

    @PutMapping("/daycares/{daycareId}/supervisors/{employeeId}")
    fun insertUnitSupervisor(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId
    ): ResponseEntity<Unit> {
        Audit.UnitAclCreate.log(targetId = daycareId, objectId = employeeId)
        acl.getRolesForUnit(user, daycareId)
            .requireOneOfRoles(UserRole.ADMIN)
        addUnitSupervisor(db, daycareId, employeeId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/daycares/{daycareId}/supervisors/{employeeId}")
    fun deleteUnitSupervisor(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId
    ): ResponseEntity<Unit> {
        Audit.UnitAclDelete.log(targetId = daycareId, objectId = employeeId)
        acl.getRolesForUnit(user, daycareId)
            .requireOneOfRoles(UserRole.ADMIN)
        removeUnitSupervisor(db, daycareId, employeeId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/daycares/{daycareId}/specialeducationteacher/{employeeId}")
    fun insertSpecialEducationTeacher(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId
    ): ResponseEntity<Unit> {
        Audit.UnitAclCreate.log(targetId = daycareId, objectId = employeeId)
        acl.getRolesForUnit(user, daycareId)
            .requireOneOfRoles(UserRole.ADMIN)
        addSpecialEducationTeacher(db, daycareId, employeeId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/daycares/{daycareId}/specialeducationteacher/{employeeId}")
    fun deleteSpecialEducationTeacher(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId
    ): ResponseEntity<Unit> {
        Audit.UnitAclDelete.log(targetId = daycareId, objectId = employeeId)
        acl.getRolesForUnit(user, daycareId)
            .requireOneOfRoles(UserRole.ADMIN)
        removeSpecialEducationTeacher(db, daycareId, employeeId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/daycares/{daycareId}/staff/{employeeId}")
    fun insertStaff(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId
    ): ResponseEntity<Unit> {
        Audit.UnitAclCreate.log(targetId = daycareId, objectId = employeeId)
        acl.getRolesForUnit(user, daycareId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)
        addStaffMember(db, daycareId, employeeId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/daycares/{daycareId}/staff/{employeeId}")
    fun deleteStaff(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId
    ): ResponseEntity<Unit> {
        Audit.UnitAclDelete.log(targetId = daycareId, objectId = employeeId)
        acl.getRolesForUnit(user, daycareId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)
        removeStaffMember(db, daycareId, employeeId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/daycares/{daycareId}/staff/{employeeId}/groups")
    fun updateStaffGroupAcl(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable daycareId: DaycareId,
        @PathVariable employeeId: EmployeeId,
        @RequestBody update: GroupAclUpdate
    ) {
        Audit.UnitGroupAclUpdate.log(targetId = daycareId, objectId = employeeId)
        acl.getRolesForUnit(user, daycareId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)

        db.transaction {
            it.clearDaycareGroupAcl(daycareId, employeeId)
            it.insertDaycareGroupAcl(daycareId, employeeId, update.groupIds)
        }
    }

    data class GroupAclUpdate(val groupIds: List<GroupId>)
}

data class DaycareAclResponse(val rows: List<DaycareAclRow>)
