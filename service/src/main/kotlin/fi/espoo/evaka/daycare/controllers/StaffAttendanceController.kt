// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.daycare.StaffAttendanceForDates
import fi.espoo.evaka.daycare.StaffAttendanceUpdate
import fi.espoo.evaka.daycare.deleteStaffAttendance
import fi.espoo.evaka.daycare.getGroupInfo
import fi.espoo.evaka.daycare.getStaffAttendanceByRange
import fi.espoo.evaka.daycare.isValidStaffAttendanceDate
import fi.espoo.evaka.daycare.upsertStaffAttendance
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.Month
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class StaffAttendanceController(private val accessControl: AccessControl) {
    @GetMapping("/employee/staff-attendances/group/{groupId}")
    fun getStaffAttendancesByGroup(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @PathVariable groupId: GroupId,
    ): StaffAttendanceForDates {
        val range = FiniteDateRange.ofMonth(year, Month.of(month))
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Group.READ_STAFF_ATTENDANCES,
                        groupId,
                    )
                    val groupInfo =
                        tx.getGroupInfo(groupId)
                            ?: throw BadRequest("Couldn't find group info with id $groupId")
                    val attendanceMap =
                        tx.getStaffAttendanceByRange(range, groupId).associateBy { it.date }
                    StaffAttendanceForDates(
                        groupId,
                        groupInfo.groupName,
                        groupInfo.startDate,
                        groupInfo.endDate,
                        attendanceMap,
                    )
                }
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
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Group.UPDATE_STAFF_ATTENDANCES,
                    groupId,
                )
                if (staffAttendance.count == null) {
                    tx.deleteStaffAttendance(groupId, staffAttendance.date)
                } else {
                    val sa = staffAttendance.copy(groupId = groupId)
                    if (!tx.isValidStaffAttendanceDate(sa)) {
                        throw BadRequest(
                            "Error: Upserting staff count failed. Group is not operating in given date"
                        )
                    }
                    tx.upsertStaffAttendance(sa)
                }
            }
        }
        Audit.StaffAttendanceUpdate.log(targetId = AuditId(groupId))
    }
}
