// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.StaffAttendanceExternalId
import fi.espoo.evaka.shared.StaffAttendanceId
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull
import org.jdbi.v3.json.Json
import java.math.BigDecimal
import java.util.UUID

enum class StaffAttendanceType : DatabaseEnum {
    PRESENT,
    OTHER_WORK,
    TRAINING,
    OVERTIME,
    JUSTIFIED_CHANGE,
    ;

    override val sqlType: String = "staff_attendance_type"
}

data class CurrentDayStaffAttendanceResponse(
    val staff: List<StaffMember>,
    val extraAttendances: List<ExternalStaffMember>,
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
    @Nested("attendance")
    val latestCurrentDayAttendance: StaffMemberAttendance?,
    @Json
    val plannedAttendances: List<PlannedStaffAttendance>
) {
    val present: GroupId?
        get() = latestCurrentDayAttendance?.takeIf { it.departed == null }?.groupId
}

data class StaffMemberAttendance(
    @PropagateNull
    val id: StaffAttendanceId,
    val employeeId: EmployeeId,
    val groupId: GroupId,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?
)

data class ExternalAttendance(
    val id: StaffAttendanceExternalId,
    val name: String,
    val groupId: GroupId,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?,
    val occupancyCoefficient: BigDecimal
)

data class Attendance(
    val id: UUID,
    val groupId: GroupId,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?,
    val occupancyCoefficient: BigDecimal
)

data class EmployeeAttendance(
    val employeeId: EmployeeId,
    val groups: List<GroupId>,
    val firstName: String,
    val lastName: String,
    val currentOccupancyCoefficient: BigDecimal,
    val attendances: List<Attendance>
)

data class StaffAttendanceResponse(
    val staff: List<EmployeeAttendance>,
    val extraAttendances: List<ExternalAttendance>,
)

data class PlannedStaffAttendance(
    val start: HelsinkiDateTime,
    val end: HelsinkiDateTime,
    val type: StaffAttendanceType
)
