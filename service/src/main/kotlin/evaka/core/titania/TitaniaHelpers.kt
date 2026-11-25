// SPDX-FileCopyrightText: 2021-2022 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.titania

import evaka.core.attendance.RawAttendance
import evaka.core.attendance.StaffAttendanceType
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.FiniteDateTimeRange
import java.time.LocalTime

fun splitOvernight(attendance: RawAttendance): List<RawAttendance> {
    val range = attendance.period.asFiniteDateTimeRange() ?: return listOf(attendance)
    return FiniteDateRange(range.start.toLocalDate(), range.end.toLocalDate())
        .dates()
        .mapNotNull { date ->
            range.intersection(FiniteDateTimeRange.of(date, LocalTime.MIN, LocalTime.MAX))?.let {
                attendance.copy(period = it.asDateTimeRange())
            }
        }
        .toList()
}

fun staffAttendanceTypeFromTitaniaEventCode(code: String): StaffAttendanceType =
    when (code) {
        "U" -> StaffAttendanceType.PRESENT
        "K" -> StaffAttendanceType.TRAINING
        else -> StaffAttendanceType.PRESENT
    }
