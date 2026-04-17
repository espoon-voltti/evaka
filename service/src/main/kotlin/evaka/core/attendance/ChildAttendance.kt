// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.attendance

import evaka.core.absence.AbsenceCategory
import evaka.core.absence.AbsenceType
import evaka.core.dailyservicetimes.DailyServiceTimesValue
import evaka.core.note.child.daily.ChildDailyNote
import evaka.core.note.child.sticky.ChildStickyNote
import evaka.core.placement.PlacementType
import evaka.core.placement.ScheduleType
import evaka.core.reservations.ReservationResponse
import evaka.core.serviceneed.ShiftCareType
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.GroupId
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.TimeInterval
import evaka.core.user.EvakaUser
import java.time.LocalDate
import java.time.LocalTime
import org.jdbi.v3.core.mapper.Nested

data class ContactInfo(
    val id: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val backupPhone: String,
    val email: String,
    val priority: Int?,
)

data class AttendanceChild(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val preferredName: String,
    val dateOfBirth: LocalDate,
    val placementType: PlacementType,
    val scheduleType: ScheduleType,
    val operationalDates: Set<LocalDate>,
    val groupId: GroupId?,
    val backup: Boolean,
    val dailyServiceTimes: DailyServiceTimesValue?,
    val dailyNote: ChildDailyNote?,
    val stickyNotes: List<ChildStickyNote>,
    val imageUrl: String?,
    val reservations: List<ReservationResponse>,
    val hasGuardian: Boolean,
    val shiftCare: ShiftCareType?,
)

enum class AttendanceStatus {
    COMING,
    PRESENT,
    DEPARTED,
    ABSENT,
}

data class ChildAttendanceRow(
    val childId: ChildId,
    val unitId: DaycareId,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime?,
    val modifiedAt: HelsinkiDateTime,
    @Nested("modified_by") val modifiedBy: EvakaUser,
) {
    fun asTimeInterval(): TimeInterval = TimeInterval(startTime, endTime)
}

data class AttendanceTimes(
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?,
    val modifiedAt: HelsinkiDateTime,
    @Nested("modified_by") val modifiedBy: EvakaUser,
)

data class ChildAbsence(val category: AbsenceCategory, val type: AbsenceType)
