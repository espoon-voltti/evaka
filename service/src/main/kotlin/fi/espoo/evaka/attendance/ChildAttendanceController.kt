// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.daycare.service.CareType
import fi.espoo.evaka.pis.dao.mapPSQLException
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.domain.BadRequest
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

    @GetMapping("/units/{unitId}")
    fun getAttendances(
        user: AuthenticatedUser,
        @PathVariable unitId: UUID
    ): ResponseEntity<AttendanceResponse> {
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        val response = jdbi.transaction { h -> getAttendancesResponse(h, unitId) }
        return ResponseEntity.ok(response)
    }

    data class ArrivalRequest(
        val arrived: Instant
    )
    @PostMapping("/units/{unitId}/children/{childId}/arrival")
    fun postArrival(
        user: AuthenticatedUser,
        @PathVariable unitId: UUID,
        @PathVariable childId: UUID,
        @RequestBody body: ArrivalRequest
    ): ResponseEntity<AttendanceResponse> {
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        val response = jdbi.transaction { h ->
            assertChildPlacement(h, childId, unitId, LocalDate.ofInstant(body.arrived, ZoneId.of("Europe/Helsinki")))

            // todo: handle absence clearing

            try {
                h.insertAttendance(
                    childId = childId,
                    arrived = body.arrived,
                    departed = null
                )
            } catch (e: Exception) {
                throw mapPSQLException(e)
            }

            getAttendancesResponse(h, unitId)
        }
        return ResponseEntity.ok(response)
    }
}

fun assertChildPlacement(h: Handle, childId: UUID, unitId: UUID, date: LocalDate){
    // language=sql
    val sql = """
        SELECT id FROM placement
        WHERE child_id = :childId AND unit_id = :unitId AND daterange(start_date, end_date, '[]') @> :date
        
        UNION ALL
        
        SELECT id FROM backup_care
        WHERE child_id = :childId AND unit_id = :unitId AND daterange(start_date, end_date, '[]') @> :date
    """.trimIndent()

    val placementMissing = h.createQuery(sql)
        .bind("childId", childId)
        .bind("unitId", unitId)
        .bind("date", date)
        .mapTo<UUID>()
        .list()
        .isEmpty()

    if(placementMissing){
        throw BadRequest("Child $childId has no placement in unit $unitId on the given day")
    }
}

fun getAttendancesResponse(h: Handle, unitId: UUID): AttendanceResponse{
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

    return AttendanceResponse(unitInfo, children)
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
