// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.daycare.service.CareType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.transaction
import org.jdbi.v3.core.Jdbi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/attendances")
class ChildAttendanceController(
    private val jdbi: Jdbi,
    private val acl: AccessControlList
) {
    val authorizedRoles = arrayOf(
        UserRole.ADMIN,
        UserRole.SERVICE_WORKER,
        UserRole.UNIT_SUPERVISOR,
        UserRole.STAFF
    )

    @GetMapping
    fun getAttendances(
        user: AuthenticatedUser,
        @RequestParam unitId: UUID
    ): ResponseEntity<AttendanceResponse> {
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        val response = jdbi.transaction { h ->
            val unitInfo = h.fetchUnitInfo(unitId)
            val childrenBasics = h.fetchChildrenBasics(unitId)
            val childrenAttendances = h.fetchChildrenAttendances(unitId)
            val childrenAbsences = h.fetchChildrenAbsences(unitId)

            val children = childrenBasics.map { child ->
                val attendance = childrenAttendances.firstOrNull { it.childId == child.id }
                val absences = childrenAbsences.filter { it.childId == child.id }
                val status = getChildStatus(child.placementType, attendance, absences)

                Child(
                    id = child.id,
                    firstName = child.firstName,
                    lastName = child.lastName,
                    placementType = child.placementType,
                    groupId = child.groupId,
                    status = status,
                    attendance = attendance,
                    absences = absences
                )
            }

            AttendanceResponse(unitInfo, children)
        }

        return ResponseEntity.ok(response)
    }
}

fun getChildStatus(placementType: PlacementType, attendance: ChildAttendance?, absences: List<ChildAbsence>): AttendanceStatus {
    if (attendance != null) {
        return if (attendance.departed == null) AttendanceStatus.PRESENT else AttendanceStatus.DEPARTED
    }

    if (isAbsent(placementType, absences)) {
        return AttendanceStatus.ABSENT
    }

    return AttendanceStatus.COMING
}

fun isAbsent(placementType: PlacementType, absences: List<ChildAbsence>): Boolean {
    return when (placementType) {
        PlacementType.PRESCHOOL, PlacementType.PREPARATORY ->
            absences.any { it.careType == CareType.PRESCHOOL }
        PlacementType.PRESCHOOL_DAYCARE, PlacementType.PREPARATORY_DAYCARE ->
            absences.any { it.careType == CareType.PRESCHOOL } && absences.any { it.careType == CareType.PRESCHOOL_DAYCARE }
        PlacementType.DAYCARE, PlacementType.DAYCARE_PART_TIME ->
            absences.any { it.careType == CareType.DAYCARE }
        PlacementType.CLUB ->
            absences.any { it.careType == CareType.CLUB }
    }.exhaust()
}
