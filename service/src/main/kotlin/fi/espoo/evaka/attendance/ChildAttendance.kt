// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.dailyservicetimes.DailyServiceTimes
import fi.espoo.evaka.daycare.service.AbsenceCareType
import fi.espoo.evaka.note.child.daily.ChildDailyNote
import fi.espoo.evaka.note.child.sticky.ChildStickyNote
import fi.espoo.evaka.note.group.GroupNote
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AttendanceId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.util.UUID

data class ContactInfo(
    val id: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val backupPhone: String,
    val email: String,
    val priority: Int?
)

data class AttendanceResponse(
    val children: List<Child>,
    val groupNotes: List<GroupNote>
)

data class Child(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val preferredName: String?,
    val placementType: PlacementType,
    val groupId: GroupId,
    val backup: Boolean,
    val status: AttendanceStatus,
    val attendance: AttendanceTimes?,
    val absences: List<ChildAbsence>,
    val dailyServiceTimes: DailyServiceTimes?,
    val dailyNote: ChildDailyNote?,
    val stickyNotes: List<ChildStickyNote>,
    val imageUrl: String?,
    val reservations: List<AttendanceReservation>
) {
    // Added for backwards compatibility. Remove when employee mobile clients are guaranteed to be recent enough.
    val reservation = reservations.firstOrNull()
}

enum class AttendanceStatus {
    COMING, PRESENT, DEPARTED, ABSENT
}

data class ChildAttendance(
    val id: AttendanceId,
    val childId: UUID,
    val unitId: DaycareId,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?
)

data class AttendanceTimes(
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?
)

data class ChildAbsence(
    val careType: AbsenceCareType
)

data class AttendanceReservation(val startTime: String, val endTime: String)
