// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.dailyservicetimes.DailyServiceTimes
import fi.espoo.evaka.daycare.service.CareType
import fi.espoo.evaka.messaging.daycarydailynote.DaycareDailyNote
import fi.espoo.evaka.placement.PlacementType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ChildResult(
    val status: ChildResultStatus,
    val child: ChildSensitiveInformation? = null
)

enum class ChildResultStatus {
    SUCCESS, WRONG_PIN, PIN_LOCKED, NOT_FOUND
}

data class ChildSensitiveInformation(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val preferredName: String,
    val ssn: String,
    val childAddress: String,
    val placementTypes: List<PlacementType>,
    val allergies: String,
    val diet: String,
    val medication: String,
    val contacts: List<ContactInfo>,
    val backupPickups: List<ContactInfo>
)

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
    val unit: UnitInfo,
    val children: List<Child>
)

data class UnitInfo(
    val id: UUID,
    val name: String,
    val groups: List<GroupInfo>,
    val staff: List<Staff>
)

data class GroupInfo(
    val id: UUID,
    val name: String,
    val dailyNote: DaycareDailyNote?
)

data class ChildBasics(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val preferredName: String?,
    val dateOfBirth: LocalDate,
    val dailyServiceTimes: DailyServiceTimes?,
    val placementType: PlacementType,
    val groupId: UUID,
    val backup: Boolean,
    val imageUrl: String?
)

data class Child(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val preferredName: String?,
    val placementType: PlacementType,
    val groupId: UUID,
    val backup: Boolean,
    val status: AttendanceStatus,
    val attendance: ChildAttendance?,
    val absences: List<ChildAbsence>,
    val dailyServiceTimes: DailyServiceTimes?,
    val dailyNote: DaycareDailyNote?,
    val imageUrl: String?
)

enum class AttendanceStatus {
    COMING, PRESENT, DEPARTED, ABSENT
}

data class ChildAttendance(
    val id: UUID,
    val childId: UUID,
    val unitId: UUID,
    val arrived: Instant,
    val departed: Instant?
)

data class ChildAbsence(
    val id: UUID,
    val childId: UUID,
    val careType: CareType
)

data class Staff(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val pinSet: Boolean = false,
    val groups: List<UUID>
)
