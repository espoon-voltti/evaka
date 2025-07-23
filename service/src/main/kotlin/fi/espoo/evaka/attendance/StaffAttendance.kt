// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.ConstList
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.StaffAttendanceExternalId
import fi.espoo.evaka.shared.StaffAttendanceRealtimeId
import fi.espoo.evaka.shared.data.DateTimeSet
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import fi.espoo.evaka.user.EvakaUser
import java.math.BigDecimal
import java.time.LocalDate
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
    val extraAttendances: List<ExternalStaffMember>,
    val operationalDays: List<LocalDate>,
)

data class ExternalStaffMember(
    val id: StaffAttendanceExternalId,
    val name: String,
    val groupId: GroupId,
    val arrived: HelsinkiDateTime,
    val occupancyEffect: Boolean,
)

data class StaffMember(
    val employeeId: EmployeeId,
    val firstName: String,
    val lastName: String,
    val unitIds: List<DaycareId>,
    val groupIds: List<GroupId>,
    val occupancyEffect: Boolean,
    @Nested("attendance") val latestCurrentDayAttendance: StaffMemberAttendance?,
    @Json val attendances: List<StaffMemberAttendance>,
    @Json val plannedAttendances: List<PlannedStaffAttendance>,
    val hasFutureAttendances: Boolean,
) {
    val present: GroupId?
        get() =
            latestCurrentDayAttendance
                ?.takeIf { it.departed == null && it.type.presentInGroup() }
                ?.groupId

    val spanningPlans: List<HelsinkiDateTimeRange>
        get() =
            plannedAttendances
                .map { HelsinkiDateTimeRange(it.start, it.end) }
                .let { DateTimeSet.of(it) } // merge adjacent
                .ranges()
                .toList()
}

data class StaffMemberWithOperationalDays(
    val staffMember: StaffMember,
    val operationalDays: List<LocalDate>,
)

data class StaffMemberAttendance(
    @PropagateNull val id: StaffAttendanceRealtimeId,
    val employeeId: EmployeeId,
    val groupId: GroupId?,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?,
    val type: StaffAttendanceType,
    val departedAutomatically: Boolean,
    val occupancyCoefficient: BigDecimal,
)

data class ExternalAttendance(
    val id: StaffAttendanceExternalId,
    val name: String,
    val groupId: GroupId,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?,
    val occupancyCoefficient: BigDecimal,
    val departedAutomatically: Boolean,
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
    val departedAutomatically: Boolean,
    val arrivedAddedAt: HelsinkiDateTime?,
    val arrivedAddedBy: EvakaUser?,
    val arrivedModifiedAt: HelsinkiDateTime?,
    val arrivedModifiedBy: EvakaUser?,
    val departedAddedAt: HelsinkiDateTime?,
    val departedAddedBy: EvakaUser?,
    val departedModifiedAt: HelsinkiDateTime?,
    val departedModifiedBy: EvakaUser?,
)

data class EmployeeAttendance(
    val employeeId: EmployeeId,
    val groups: List<GroupId>,
    val firstName: String,
    val lastName: String,
    val currentOccupancyCoefficient: BigDecimal,
    val attendances: List<Attendance>,
    val plannedAttendances: List<PlannedStaffAttendance>,
    val allowedToEdit: Boolean,
)

data class StaffAttendanceResponse(
    val staff: List<EmployeeAttendance>,
    val extraAttendances: List<ExternalAttendance>,
)

data class PlannedStaffAttendance(
    val start: HelsinkiDateTime,
    val end: HelsinkiDateTime,
    val type: StaffAttendanceType,
    val description: String?,
)
