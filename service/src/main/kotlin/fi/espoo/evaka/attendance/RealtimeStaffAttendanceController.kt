// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/staff-attendances/realtime")
class RealtimeStaffAttendanceController(
    private val ac: AccessControl
) {
    @GetMapping
    fun getAttendances(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate
    ): StaffAttendanceResponse {
        Audit.StaffAttendanceRead.log(targetId = unitId)

        // TODO AccessControl

        return db.connect { dbc ->
            dbc.read {
                val range = FiniteDateRange(start, end)
                val rawAttendances = it.getStaffAttendancesForDateRange(unitId, range)
                val attendancesByEmployee = rawAttendances.groupBy { raw -> raw.employeeId }
                val staffWithoutAttendance = it.getCurrentStaffForAttendanceCalendar(unitId)
                    .filter { emp -> !attendancesByEmployee.keys.contains(emp.id) }
                    .map { emp -> EmployeeAttendance(emp.id, emp.firstName, emp.lastName, listOf()) }
                val staffWithAttendance = attendancesByEmployee.entries.map { (employeeId, data) ->
                    EmployeeAttendance(
                        employeeId = employeeId,
                        firstName = data[0].firstName,
                        lastName = data[0].lastName,
                        attendances = data.map { att -> Attendance(att.id, att.groupId, att.arrived, att.departed) }
                    )
                }
                StaffAttendanceResponse(
                    staff = staffWithAttendance + staffWithoutAttendance,
                    extraAttendances = it.getExternalStaffAttendancesByDateRange(unitId, range)
                )
            }
        }
    }
}
