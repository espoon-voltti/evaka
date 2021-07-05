// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.utils.Wrapper
import fi.espoo.evaka.daycare.service.StaffAttendanceForDates
import fi.espoo.evaka.daycare.service.StaffAttendanceService
import fi.espoo.evaka.daycare.service.StaffAttendanceUpdate
import fi.espoo.evaka.daycare.service.UnitStaffAttendance
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/staff-attendances")
class StaffAttendanceController(
    private val staffAttendanceService: StaffAttendanceService,
    private val acl: AccessControlList
) {
    @GetMapping("/unit/{unitId}")
    fun getAttendancesByUnit(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: UUID
    ): ResponseEntity<UnitStaffAttendance> {
        Audit.UnitStaffAttendanceRead.log(targetId = unitId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(UserRole.MOBILE)
        val result = staffAttendanceService.getUnitAttendancesForDate(db, unitId, LocalDate.now())
        return ResponseEntity.ok(result)
    }

    @GetMapping("/group/{groupId}")
    fun getAttendancesByGroup(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @PathVariable groupId: GroupId
    ): ResponseEntity<Wrapper<StaffAttendanceForDates>> {
        Audit.StaffAttendanceRead.log(targetId = groupId)
        acl.getRolesForUnitGroup(user, groupId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.MOBILE, UserRole.SPECIAL_EDUCATION_TEACHER)
        val result = staffAttendanceService.getGroupAttendancesByMonth(db, year, month, groupId)
        return ResponseEntity.ok(Wrapper(result))
    }

    @PostMapping("/group/{groupId}")
    fun upsertStaffAttendance(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody staffAttendance: StaffAttendanceUpdate,
        @PathVariable groupId: GroupId
    ): ResponseEntity<Unit> {
        Audit.StaffAttendanceUpdate.log(targetId = groupId)
        acl.getRolesForUnitGroup(user, groupId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.MOBILE)
        if (staffAttendance.count == null) {
            throw BadRequest("Count can't be null")
        }
        staffAttendanceService.upsertStaffAttendance(db, staffAttendance.copy(groupId = groupId))
        return ResponseEntity.noContent().build()
    }
}
