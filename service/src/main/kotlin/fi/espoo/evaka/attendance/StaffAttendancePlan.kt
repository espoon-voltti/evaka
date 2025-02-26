// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

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
