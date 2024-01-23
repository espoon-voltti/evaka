// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesValue
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.note.child.daily.ChildDailyNote
import fi.espoo.evaka.note.child.sticky.ChildStickyNote
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.ScheduleType
import fi.espoo.evaka.reservations.ReservationResponse
import fi.espoo.evaka.shared.AttendanceId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate

data class ContactInfo(
    val id: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val backupPhone: String,
    val email: String,
    val priority: Int?
)

data class AttendanceChild(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val preferredName: String,
    val dateOfBirth: LocalDate,
    val placementType: PlacementType,
    val scheduleType: ScheduleType,
    val groupId: GroupId?,
    val backup: Boolean,
    val dailyServiceTimes: DailyServiceTimesValue?,
    val dailyNote: ChildDailyNote?,
    val stickyNotes: List<ChildStickyNote>,
    val imageUrl: String?,
    val reservations: List<ReservationResponse>
)

enum class AttendanceStatus {
    COMING,
    PRESENT,
    DEPARTED,
    ABSENT
}

data class ChildAttendance(
    val id: AttendanceId,
    val childId: ChildId,
    val unitId: DaycareId,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?
)

data class AttendanceTimes(val arrived: HelsinkiDateTime, val departed: HelsinkiDateTime?)

data class ChildAbsence(val category: AbsenceCategory)
