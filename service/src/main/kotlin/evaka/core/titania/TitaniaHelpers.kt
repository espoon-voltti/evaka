// SPDX-FileCopyrightText: 2021-2022 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.titania

import evaka.core.attendance.RawAttendance
import evaka.core.attendance.StaffAttendanceType
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalTime

fun splitOvernight(attendance: RawAttendance): List<RawAttendance> {
    if (attendance.period.end == null) {
        return listOf(attendance)
    }
    val arrivedDate = attendance.period.start.toLocalDate()
    val departedDate = attendance.period.end.toLocalDate()
    if (arrivedDate == departedDate) {
        return listOf(attendance)
    }
    return FiniteDateRange(arrivedDate, departedDate)
        .dates()
        .map { date ->
            attendance.copy(
                period =
                    attendance.period.copy(
                        start =
                            if (date == arrivedDate) attendance.period.start
                            else HelsinkiDateTime.of(date, LocalTime.MIN),
                        end =
                            if (date == departedDate) attendance.period.end
                            else HelsinkiDateTime.of(date, LocalTime.MAX),
                    ),
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
