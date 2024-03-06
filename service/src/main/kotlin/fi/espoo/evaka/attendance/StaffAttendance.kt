// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.ConstList
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.StaffAttendanceExternalId
import fi.espoo.evaka.shared.StaffAttendanceRealtimeId
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import java.math.BigDecimal
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull
import org.jdbi.v3.json.Json

@ConstList("staffAttendanceTypes")
enum class StaffAttendanceType : DatabaseEnum {
    PRESENT,
    OTHER_WORK,
    TRAINING,
    OVERTIME,
    JUSTIFIED_CHANGE;

    override val sqlType: String = "staff_attendance_type"

    fun presentInGroup() =
        when (this) {
            OTHER_WORK,
            TRAINING -> false
            else -> true
        }
}

data class CurrentDayStaffAttendanceResponse(
    val staff: List<StaffMember>,
    val extraAttendances: List<ExternalStaffMember>
)

data class ExternalStaffMember(
    val id: StaffAttendanceExternalId,
    val name: String,
    val groupId: GroupId,
    val arrived: HelsinkiDateTime,
)

data class StaffMember(
    val employeeId: EmployeeId,
    val firstName: String,
    val lastName: String,
    val groupIds: List<GroupId>,
    val occupancyEffect: Boolean,
    @Nested("attendance") val latestCurrentDayAttendance: StaffMemberAttendance?,
    @Json val attendances: List<StaffMemberAttendance>,
    @Json val plannedAttendances: List<PlannedStaffAttendance>,
    val hasFutureAttendances: Boolean
) {
    val present: GroupId?
        get() =
            latestCurrentDayAttendance
                ?.takeIf { it.departed == null && it.type.presentInGroup() }
                ?.groupId

    val spanningPlan: HelsinkiDateTimeRange?
        get() =
            if (plannedAttendances.isEmpty()) {
                null
            } else {
                HelsinkiDateTimeRange(
                    plannedAttendances.minOf { it.start },
                    plannedAttendances.maxOf { it.end }
                )
            }
}

data class StaffMemberAttendance(
    @PropagateNull val id: StaffAttendanceRealtimeId,
    val employeeId: EmployeeId,
    val groupId: GroupId?,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?,
    val type: StaffAttendanceType,
    val departedAutomatically: Boolean
)

data class ExternalAttendance(
    val id: StaffAttendanceExternalId,
    val name: String,
    val groupId: GroupId,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?,
    val occupancyCoefficient: BigDecimal,
    val departedAutomatically: Boolean
) {
    val type = StaffAttendanceType.PRESENT
}

data class Attendance(
    val id: StaffAttendanceRealtimeId,
    val groupId: GroupId?,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?,
    val occupancyCoefficient: BigDecimal,
    val type: StaffAttendanceType,
    val departedAutomatically: Boolean
)

data class EmployeeAttendance(
    val employeeId: EmployeeId,
    val groups: List<GroupId>,
    val firstName: String,
    val lastName: String,
    val currentOccupancyCoefficient: BigDecimal,
    val attendances: List<Attendance>,
    val plannedAttendances: List<PlannedStaffAttendance>
)

data class StaffAttendanceResponse(
    val staff: List<EmployeeAttendance>,
    val extraAttendances: List<ExternalAttendance>
)

data class PlannedStaffAttendance(
    val start: HelsinkiDateTime,
    val end: HelsinkiDateTime,
    val type: StaffAttendanceType
)
