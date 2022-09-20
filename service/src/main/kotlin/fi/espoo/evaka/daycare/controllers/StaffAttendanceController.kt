// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.service.StaffAttendanceForDates
import fi.espoo.evaka.daycare.service.StaffAttendanceService
import fi.espoo.evaka.daycare.service.StaffAttendanceUpdate
import fi.espoo.evaka.daycare.service.UnitStaffAttendance
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.controllers.Wrapper
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
        clock: EvakaClock,
        @PathVariable unitId: DaycareId
    ): UnitStaffAttendance {
        Audit.UnitStaffAttendanceRead.log(targetId = unitId)
        accessControl.requirePermissionFor(user, clock, Action.Unit.READ_STAFF_ATTENDANCES, unitId)
        return db.connect { dbc ->
            staffAttendanceService.getUnitAttendancesForDate(dbc, unitId, clock.today())
        }
    }

    @GetMapping("/group/{groupId}")
    fun getAttendancesByGroup(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @PathVariable groupId: GroupId
    ): Wrapper<StaffAttendanceForDates> {
        Audit.StaffAttendanceRead.log(targetId = groupId)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Group.READ_STAFF_ATTENDANCES,
            groupId
        )
        val result =
            db.connect { dbc ->
                staffAttendanceService.getGroupAttendancesByMonth(dbc, year, month, groupId)
            }
        return Wrapper(result)
    }

    @PostMapping("/group/{groupId}")
    fun upsertStaffAttendance(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody staffAttendance: StaffAttendanceUpdate,
        @PathVariable groupId: GroupId
    ) {
        Audit.StaffAttendanceUpdate.log(targetId = groupId)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Group.UPDATE_STAFF_ATTENDANCES,
            groupId
        )
        if (staffAttendance.count == null) {
            throw BadRequest("Count can't be null")
        }
        db.connect { dbc ->
            staffAttendanceService.upsertStaffAttendance(
                dbc,
                staffAttendance.copy(groupId = groupId)
            )
        }
    }
}
