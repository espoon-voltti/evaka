// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.CareType
import fi.espoo.evaka.placement.PlacementType
import java.time.Instant
import java.util.UUID

data class AttendanceResponse(
    val unit: UnitInfo,
    val children: List<Child>
)

data class UnitInfo(
    val id: UUID,
    val name: String,
    val groups: List<GroupInfo>
)

data class GroupInfo(
    val id: UUID,
    val name: String
)

open class ChildBasics(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val placementType: PlacementType,
    val groupId: UUID
)

class Child(
    id: UUID,
    firstName: String,
    lastName: String,
    placementType: PlacementType,
    groupId: UUID,
    val status: AttendanceStatus,
    val attendance: ChildAttendance?,
    val absences: List<ChildAbsence>
) : ChildBasics(id, firstName, lastName, placementType, groupId)

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
    val absenceType: AbsenceType,
    val careType: CareType
)
