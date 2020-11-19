// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.CareType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.utils.dateNow
import fi.espoo.evaka.shared.utils.zoneId
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID

@RestController
@RequestMapping("/attendances")
class ChildAttendanceController(
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
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: UUID
    ): ResponseEntity<AttendanceResponse> {
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        return db.read { tx ->
            tx.getAttendancesResponse(unitId)
        }.let { ResponseEntity.ok(it) }
    }

    data class ArrivalInfoResponse(
        val absentFromPreschool: Boolean
    )
    @GetMapping("/units/{unitId}/children/{childId}/arrival")
    fun getChildArrival(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: UUID,
        @PathVariable childId: UUID,
        @RequestParam @DateTimeFormat(pattern = "HH:mm") time: LocalTime
    ): ResponseEntity<ArrivalInfoResponse> {
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        return db.read { tx ->
            tx.assertChildPlacement(childId, unitId)

            val attendance = tx.getChildCurrentDayAttendance(childId, unitId)
            if (attendance != null)
                throw Conflict("Cannot arrive, already arrived today")

            ArrivalInfoResponse(
                absentFromPreschool = false // todo
            )
        }.let { ResponseEntity.ok(it) }
    }

    data class ArrivalRequest(
        @DateTimeFormat(pattern = "HH:mm") val arrived: LocalTime
    )
    @PostMapping("/units/{unitId}/children/{childId}/arrival")
    fun postArrival(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: UUID,
        @PathVariable childId: UUID,
        @RequestBody body: ArrivalRequest
    ): ResponseEntity<AttendanceResponse> {
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        return db.transaction { tx ->
            tx.assertChildPlacement(childId, unitId)

            val attendance = tx.getChildCurrentDayAttendance(childId, unitId)
            if (attendance != null)
                throw Conflict("Cannot arrive, already arrived today")

            val arrived = ZonedDateTime.of(LocalDate.now(zoneId).atTime(body.arrived), zoneId).toInstant()

            try {
                tx.insertAttendance(
                    childId = childId,
                    unitId = unitId,
                    arrived = arrived,
                    departed = null
                )
                // todo: handle absence clearing? (EVAKA-4004)
            } catch (e: Exception) {
                throw mapPSQLException(e)
            }

            tx.getAttendancesResponse(unitId)
        }.let { ResponseEntity.ok(it) }
    }

    @PostMapping("/units/{unitId}/children/{childId}/return-to-coming")
    fun returnToComing(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: UUID,
        @PathVariable childId: UUID
    ): ResponseEntity<AttendanceResponse> {
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        return db.transaction { tx ->
            tx.assertChildPlacement(childId, unitId)

            val attendance = tx.getChildCurrentDayAttendance(childId, unitId)

            if (attendance == null) {
                tx.deleteCurrentDayAbsences(childId)
            } else {
                if (attendance.departed == null) {
                    try {
                        tx.deleteAttendance(attendance.id)
                    } catch (e: Exception) {
                        throw mapPSQLException(e)
                    }
                } else {
                    throw Conflict("Already departed, did you mean return-to-present?")
                }
            }

            tx.getAttendancesResponse(unitId)
        }.let { ResponseEntity.ok(it) }
    }

    data class DepartureInfoResponse(
        val absentFrom: Set<CareType>
    )
    @GetMapping("/units/{unitId}/children/{childId}/departure")
    fun getChildDeparture(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: UUID,
        @PathVariable childId: UUID,
        @RequestParam @DateTimeFormat(pattern = "HH:mm") time: LocalTime
    ): ResponseEntity<DepartureInfoResponse> {
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        return db.read { tx ->
            tx.assertChildPlacement(childId, unitId)

            val attendance = tx.getChildCurrentDayAttendance(childId, unitId)
            if (attendance == null) {
                throw Conflict("Cannot depart, has not yet arrived")
            } else if (attendance.departed != null) {
                throw Conflict("Cannot depart, already departed")
            }

            DepartureInfoResponse(
                absentFrom = emptySet() // todo
            )
        }.let { ResponseEntity.ok(it) }
    }

    data class DepartureRequest(
        @DateTimeFormat(pattern = "HH:mm") val departed: LocalTime
    )
    @PostMapping("/units/{unitId}/children/{childId}/departure")
    fun postDeparture(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: UUID,
        @PathVariable childId: UUID,
        @RequestBody body: DepartureRequest
    ): ResponseEntity<AttendanceResponse> {
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        return db.transaction { tx ->
            tx.assertChildPlacement(childId, unitId)

            val attendance = tx.getChildCurrentDayAttendance(childId, unitId)
            if (attendance == null) {
                throw Conflict("Cannot depart, has not yet arrived")
            } else if (attendance.departed != null) {
                throw Conflict("Cannot depart, already departed")
            }

            val departed = ZonedDateTime.of(LocalDate.now(zoneId).atTime(body.departed), zoneId).toInstant()

            try {
                tx.updateAttendanceEnd(
                    attendanceId = attendance.id,
                    departed = departed
                )
                // todo: handle absence clearing? (EVAKA-4004)
            } catch (e: Exception) {
                throw mapPSQLException(e)
            }

            tx.getAttendancesResponse(unitId)
        }.let { ResponseEntity.ok(it) }
    }

    @PostMapping("/units/{unitId}/children/{childId}/return-to-present")
    fun returnToPresent(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: UUID,
        @PathVariable childId: UUID
    ): ResponseEntity<AttendanceResponse> {
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        return db.transaction { tx ->
            tx.assertChildPlacement(childId, unitId)

            val attendance = tx.getChildCurrentDayAttendance(childId, unitId)

            if (attendance?.departed == null) {
                throw Conflict("Can not return to present since not yet departed")
            } else {
                try {
                    tx.updateAttendance(attendance.id, attendance.arrived, null)
                } catch (e: Exception) {
                    throw mapPSQLException(e)
                }
            }

            tx.getAttendancesResponse(unitId)
        }.let { ResponseEntity.ok(it) }
    }

    data class FullDayAbsenceRequest(
        val absenceType: AbsenceType
    )
    @PostMapping("/units/{unitId}/children/{childId}/full-day-absence")
    fun postFullDayAbsence(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: UUID,
        @PathVariable childId: UUID,
        @RequestBody body: FullDayAbsenceRequest
    ): ResponseEntity<AttendanceResponse> {
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        return db.transaction { tx ->
            tx.assertChildPlacement(childId, unitId)

            val attendance = tx.getChildCurrentDayAttendance(childId, unitId)
            if (attendance != null) {
                throw Conflict("Cannot add full day absence, child already has attendance")
            }

            try {
                tx.deleteCurrentDayAbsences(childId)

                val placementType = tx.fetchChildPlacementType(childId, unitId, dateNow())
                getCareTypes(placementType).forEach { careType ->
                    tx.insertAbsence(user, childId, LocalDate.now(), careType, body.absenceType)
                }
            } catch (e: Exception) {
                throw mapPSQLException(e)
            }

            tx.getAttendancesResponse(unitId)
        }.let { ResponseEntity.ok(it) }
    }
}

private fun Database.Read.assertChildPlacement(childId: UUID, unitId: UUID) {
    // language=sql
    val sql =
        """
        SELECT id FROM placement
        WHERE child_id = :childId AND unit_id = :unitId AND daterange(start_date, end_date, '[]') @> :date
        
        UNION ALL
        
        SELECT id FROM backup_care
        WHERE child_id = :childId AND unit_id = :unitId AND daterange(start_date, end_date, '[]') @> :date
        """.trimIndent()

    val placementMissing = createQuery(sql)
        .bind("childId", childId)
        .bind("unitId", unitId)
        .bind("date", dateNow())
        .mapTo<UUID>()
        .list()
        .isEmpty()

    if (placementMissing) {
        throw BadRequest("Child $childId has no placement in unit $unitId on the given day")
    }
}

private fun Database.Read.fetchChildPlacementType(childId: UUID, unitId: UUID, date: LocalDate): PlacementType {
    // language=sql
    val sql =
        """
        SELECT type FROM placement
        WHERE child_id = :childId AND unit_id = :unitId AND daterange(start_date, end_date, '[]') @> :date
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("unitId", unitId)
        .bind("date", date)
        .mapTo<PlacementType>()
        .list()
        .first()
}

private fun Database.Read.getAttendancesResponse(unitId: UUID): AttendanceResponse {
    val unitInfo = fetchUnitInfo(unitId)
    val childrenBasics = fetchChildrenBasics(unitId)
    val childrenAttendances = fetchChildrenAttendances(unitId)
    val childrenAbsences = fetchChildrenAbsences(unitId)

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

private fun getChildStatus(placementType: PlacementType, attendance: ChildAttendance?, absences: List<ChildAbsence>): AttendanceStatus {
    if (attendance != null) {
        return if (attendance.departed == null) AttendanceStatus.PRESENT else AttendanceStatus.DEPARTED
    }

    if (isAbsent(placementType, absences)) {
        return AttendanceStatus.ABSENT
    }

    return AttendanceStatus.COMING
}

private fun isAbsent(placementType: PlacementType, absences: List<ChildAbsence>): Boolean {
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

private fun getCareTypes(placementType: PlacementType): List<CareType> {
    return when (placementType) {
        PlacementType.PRESCHOOL, PlacementType.PREPARATORY ->
            listOf(CareType.PRESCHOOL)
        PlacementType.PRESCHOOL_DAYCARE, PlacementType.PREPARATORY_DAYCARE ->
            listOf(CareType.PRESCHOOL, CareType.PRESCHOOL_DAYCARE)
        PlacementType.DAYCARE, PlacementType.DAYCARE_PART_TIME ->
            listOf(CareType.DAYCARE)
        PlacementType.CLUB ->
            listOf(CareType.CLUB)
    }.exhaust()
}
