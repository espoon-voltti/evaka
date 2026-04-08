// SPDX-FileCopyrightText: 2021-2022 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.titania

import evaka.core.attendance.RawAttendance
import evaka.core.attendance.StaffAttendanceType
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalTime

fun splitOvernight(attendance: RawAttendance): Iterable<RawAttendance> {
    if (attendance.departed == null) {
        return listOf(attendance)
    }
    val arrivedDate = attendance.arrived.toLocalDate()
    val departedDate = attendance.departed.toLocalDate()
    if (arrivedDate == departedDate) {
        return listOf(attendance)
    }
    return FiniteDateRange(arrivedDate, departedDate)
        .dates()
        .map { date ->
            attendance.copy(
                arrived =
                    if (date == arrivedDate) attendance.arrived
                    else HelsinkiDateTime.of(date, LocalTime.MIN),
                departed =
                    if (date == departedDate) attendance.departed
                    else HelsinkiDateTime.of(date, LocalTime.MAX),
            )
        }
        .toList()
}

fun staffAttendanceTypeFromTitaniaEventCode(code: String): StaffAttendanceType =
    when (code) {
        "U" -> StaffAttendanceType.PRESENT
        "K" -> StaffAttendanceType.TRAINING
        else -> StaffAttendanceType.PRESENT
    }
