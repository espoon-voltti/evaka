// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.daycare.controllers

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.daycare.service.StaffAttendanceForDates
import evaka.core.daycare.service.StaffAttendanceService
import evaka.core.daycare.service.StaffAttendanceUpdate
import evaka.core.shared.GroupId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class StaffAttendanceController(
    private val staffAttendanceService: StaffAttendanceService,
    private val accessControl: AccessControl,
) {
    @GetMapping("/employee/staff-attendances/group/{groupId}")
    fun getStaffAttendancesByGroup(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @PathVariable groupId: GroupId,
    ): StaffAttendanceForDates {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Group.READ_STAFF_ATTENDANCES,
                        groupId,
                    )
                }
                staffAttendanceService.getGroupAttendancesByMonth(dbc, year, month, groupId)
            }
            .also { Audit.StaffAttendanceRead.log(targetId = AuditId(groupId)) }
    }

    @PostMapping("/employee/staff-attendances/group/{groupId}")
    fun upsertStaffAttendance(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody staffAttendance: StaffAttendanceUpdate,
        @PathVariable groupId: GroupId,
    ) {
        db.connect { dbc ->
            dbc.read {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Group.UPDATE_STAFF_ATTENDANCES,
                    groupId,
                )
            }
            if (staffAttendance.count == null) {
                staffAttendanceService.clearStaffAttendance(dbc, groupId, staffAttendance.date)
            } else {
                staffAttendanceService.upsertStaffAttendance(
                    dbc,
                    staffAttendance.copy(groupId = groupId),
                )
            }
        }
        Audit.StaffAttendanceUpdate.log(targetId = AuditId(groupId))
    }
}
