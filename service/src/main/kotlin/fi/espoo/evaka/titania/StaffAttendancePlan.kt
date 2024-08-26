// SPDX-FileCopyrightText: 2021-2022 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.titania

import fi.espoo.evaka.attendance.StaffAttendanceType
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.Duration

data class StaffAttendancePlan(
    val employeeId: EmployeeId,
    val type: StaffAttendanceType,
    val startTime: HelsinkiDateTime,
    val endTime: HelsinkiDateTime,
    val description: String?,
) {
    fun canMerge(other: StaffAttendancePlan) =
        this.employeeId == other.employeeId &&
            this.type == other.type &&
            this.endTime.plusMinutes(1).durationSince(other.startTime).abs() <
                Duration.ofMinutes(1) &&
            this.description == other.description
}
