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
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/staff-attendances")
class StaffAttendanceController(
    private val staffAttendanceService: StaffAttendanceService,
    private val accessControl: AccessControl
) {
    @GetMapping("/unit/{unitId}")
    fun getAttendancesByUnit(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId
    ): ResponseEntity<UnitStaffAttendance> {
        Audit.UnitStaffAttendanceRead.log(targetId = unitId)
        accessControl.requirePermissionFor(user, Action.Unit.READ_STAFF_ATTENDANCES, unitId)
        val result = db.connect { dbc -> staffAttendanceService.getUnitAttendancesForDate(dbc, unitId, LocalDate.now()) }
        return ResponseEntity.ok(result)
    }

    @GetMapping("/group/{groupId}")
    fun getAttendancesByGroup(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @PathVariable groupId: GroupId
    ): ResponseEntity<Wrapper<StaffAttendanceForDates>> {
        Audit.StaffAttendanceRead.log(targetId = groupId)
        accessControl.requirePermissionFor(user, Action.Group.READ_STAFF_ATTENDANCES, groupId)
        val result = db.connect { dbc -> staffAttendanceService.getGroupAttendancesByMonth(dbc, year, month, groupId) }
        return ResponseEntity.ok(Wrapper(result))
    }

    @PostMapping("/group/{groupId}")
    fun upsertStaffAttendance(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody staffAttendance: StaffAttendanceUpdate,
        @PathVariable groupId: GroupId
    ): ResponseEntity<Unit> {
        Audit.StaffAttendanceUpdate.log(targetId = groupId)
        accessControl.requirePermissionFor(user, Action.Group.UPDATE_STAFF_ATTENDANCES, groupId)
        if (staffAttendance.count == null) {
            throw BadRequest("Count can't be null")
        }
        db.connect { dbc -> staffAttendanceService.upsertStaffAttendance(dbc, staffAttendance.copy(groupId = groupId)) }
        return ResponseEntity.noContent().build()
    }
}
