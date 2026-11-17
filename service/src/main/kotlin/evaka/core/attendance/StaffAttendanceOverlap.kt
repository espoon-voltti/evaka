// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.attendance

import evaka.core.shared.EmployeeId
import evaka.core.shared.StaffAttendanceRealtimeId
import evaka.core.shared.db.Database
import evaka.core.shared.db.mapPSQLException
import evaka.core.shared.db.psqlCause
import evaka.core.shared.domain.BadRequest
import org.jdbi.v3.core.JdbiException

const val OVERLAPPING_STAFF_ATTENDANCE = "overlappingAttendance"

fun requireNoOverlapsBetween(
    entries: List<RealtimeStaffAttendanceController.StaffAttendanceUpsert>,
) {
    if (entries.withIndex().any { (i, a) -> entries.drop(i + 1).any { b -> a.overlaps(b) } }) {
        throw BadRequest("Attendances overlap", OVERLAPPING_STAFF_ATTENDANCE)
    }
}

fun requireNoOverlapsWithExistingStaffAttendances(
    tx: Database.Read,
    employeeId: EmployeeId,
    entries: List<RealtimeStaffAttendanceController.StaffAttendanceUpsert>,
    excludeIds: Set<StaffAttendanceRealtimeId>,
) {
    if (
        entries.any {
            tx.hasOverlappingStaffAttendance(employeeId, it.arrived, it.departed, excludeIds)
        }
    ) {
        throw BadRequest(
            "Attendance overlaps with an existing attendance",
            OVERLAPPING_STAFF_ATTENDANCE,
        )
    }
}

fun mapStaffAttendanceWriteException(e: JdbiException): Exception =
    if (e.psqlCause()?.serverErrorMessage?.constraint == "staff_attendance_no_overlaps") {
        BadRequest(
            "Attendance overlaps with an existing attendance",
            OVERLAPPING_STAFF_ATTENDANCE,
            e,
        )
    } else {
        mapPSQLException(e)
    }
