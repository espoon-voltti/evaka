// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.service.AbsenceCareType
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.note.child.daily.getChildDailyNotesInUnit
import fi.espoo.evaka.note.child.sticky.getChildStickyNotesForUnit
import fi.espoo.evaka.note.group.getGroupNotesForUnit
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.PlacementType.DAYCARE_FIVE_YEAR_OLDS
import fi.espoo.evaka.placement.PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS
import fi.espoo.evaka.placement.PlacementType.PREPARATORY
import fi.espoo.evaka.placement.PlacementType.PREPARATORY_DAYCARE
import fi.espoo.evaka.placement.PlacementType.PRESCHOOL
import fi.espoo.evaka.placement.PlacementType.PRESCHOOL_DAYCARE
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.utils.dateNow
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

val preschoolStart: LocalTime = LocalTime.of(9, 0)
val preschoolEnd: LocalTime = LocalTime.of(13, 0)
val preschoolMinimumDuration: Duration = Duration.ofHours(1)

val preparatoryStart: LocalTime = LocalTime.of(9, 0)
val preparatoryEnd: LocalTime = LocalTime.of(14, 0)
val preparatoryMinimumDuration: Duration = Duration.ofHours(1)

val connectedDaycareBuffer: Duration = Duration.ofMinutes(15)
val fiveYearOldFreeLimit: Duration = Duration.ofHours(4) + Duration.ofMinutes(15)

@RestController
@RequestMapping("/attendances")
class ChildAttendanceController(
    private val acl: AccessControlList
) {
    val authorizedRoles = arrayOf(
        UserRole.ADMIN,
        UserRole.SERVICE_WORKER,
        UserRole.UNIT_SUPERVISOR,
        UserRole.STAFF,
        UserRole.MOBILE
    )

    @GetMapping("/units/{unitId}")
    fun getAttendances(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId
    ): AttendanceResponse {
        Audit.ChildAttendancesRead.log(targetId = unitId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        return db.read { it.getAttendancesResponse(unitId, HelsinkiDateTime.now()) }
    }

    data class ArrivalRequest(
        @DateTimeFormat(pattern = "HH:mm") val arrived: LocalTime
    )

    @PostMapping("/units/{unitId}/children/{childId}/arrival")
    fun postArrival(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: UUID,
        @RequestBody body: ArrivalRequest
    ) {
        Audit.ChildAttendancesArrivalCreate.log(targetId = childId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        db.transaction { tx ->
            tx.fetchChildPlacementBasics(childId, unitId)

            tx.deleteAbsencesByDate(childId, dateNow())
            try {
                tx.insertAttendance(
                    childId = childId,
                    unitId = unitId,
                    arrived = HelsinkiDateTime.now().withTime(body.arrived),
                    departed = null
                )
            } catch (e: Exception) {
                throw mapPSQLException(e)
            }
        }
    }

    @PostMapping("/units/{unitId}/children/{childId}/return-to-coming")
    fun returnToComing(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: UUID
    ) {
        Audit.ChildAttendancesReturnToComing.log(targetId = childId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        db.transaction { tx ->
            tx.fetchChildPlacementBasics(childId, unitId)
            tx.deleteAbsencesByDate(childId, dateNow())

            val attendance = tx.getChildAttendance(childId, unitId, HelsinkiDateTime.now())
            if (attendance != null) {
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
        }
    }

    @GetMapping("/units/{unitId}/children/{childId}/departure")
    fun getChildDeparture(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: UUID
    ): List<AbsenceThreshold> {
        Audit.ChildAttendancesDepartureRead.log(targetId = childId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        return db.read { tx ->
            val placementBasics = tx.fetchChildPlacementBasics(childId, unitId)

            val attendance = tx.getChildOngoingAttendance(childId, unitId)
            if (attendance == null) {
                throw Conflict("Cannot depart, has not yet arrived")
            } else if (attendance.departed != null) {
                throw Conflict("Cannot depart, already departed")
            }

            val arrived = attendance.arrived.toLocalTime()

            // temporary hotfix for case where scheduled job was missing and child arrived yesterday
            val forgottenToDepart = attendance.arrived.toLocalDate() != dateNow()

            val childHasPaidServiceNeedToday = tx.childHasPaidServiceNeedToday(childId)

            if (forgottenToDepart) listOf()
            else getPartialAbsenceThresholds(placementBasics, arrived, childHasPaidServiceNeedToday)
        }
    }

    data class DepartureRequest(
        @DateTimeFormat(pattern = "HH:mm") val departed: LocalTime,
        val absenceType: AbsenceType?
    )

    @PostMapping("/units/{unitId}/children/{childId}/departure")
    fun postDeparture(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: UUID,
        @RequestBody body: DepartureRequest
    ) {
        Audit.ChildAttendancesDepartureCreate.log(targetId = childId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        db.transaction { tx ->
            val placementBasics = tx.fetchChildPlacementBasics(childId, unitId)

            val attendance = tx.getChildAttendance(childId, unitId, HelsinkiDateTime.now())
            if (attendance == null) {
                // temporary hotfix for case where scheduled job was missing and child arrived yesterday
                val forgottenAttendance: ChildAttendance = tx.getChildOngoingAttendance(childId, unitId)
                    ?: throw Conflict("Cannot depart, has not yet arrived")
                tx.updateAttendanceEnd(
                    attendanceId = forgottenAttendance.id,
                    departed = forgottenAttendance.arrived.withTime(LocalTime.of(23, 59))
                )
                return@transaction tx.getAttendancesResponse(unitId, HelsinkiDateTime.now())
            } else if (attendance.departed != null) {
                throw Conflict("Cannot depart, already departed")
            }

            val childHasPaidServiceNeedToday = tx.childHasPaidServiceNeedToday(childId)

            val absentFrom = getPartialAbsenceThresholds(placementBasics, attendance.arrived.toLocalTime(), childHasPaidServiceNeedToday)
                .filter { body.departed <= it.time }
            tx.deleteAbsencesByDate(childId, dateNow())
            if (absentFrom.isNotEmpty()) {
                if (body.absenceType == null) {
                    throw BadRequest("Request had no absenceType but child was absent from ${absentFrom.joinToString(", ")}.")
                }

                absentFrom.forEach { (careType, _) ->
                    tx.insertAbsence(user, childId, dateNow(), careType, body.absenceType)
                }
            } else if (body.absenceType != null) {
                throw BadRequest("Request defines absenceType but child was not absent.")
            }

            try {
                tx.updateAttendanceEnd(
                    attendanceId = attendance.id,
                    departed = HelsinkiDateTime.now().withTime(body.departed)
                )
            } catch (e: Exception) {
                throw mapPSQLException(e)
            }
        }
    }

    @PostMapping("/units/{unitId}/children/{childId}/return-to-present")
    fun returnToPresent(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: UUID
    ) {
        Audit.ChildAttendancesReturnToPresent.log(targetId = childId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        db.transaction { tx ->
            tx.fetchChildPlacementBasics(childId, unitId)
            tx.deleteAbsencesByDate(childId, dateNow())

            val attendance = tx.getChildAttendance(childId, unitId, HelsinkiDateTime.now())

            if (attendance?.departed == null) {
                throw Conflict("Can not return to present since not yet departed")
            } else {
                try {
                    tx.updateAttendance(attendance.id, attendance.arrived, null)
                } catch (e: Exception) {
                    throw mapPSQLException(e)
                }
            }
        }
    }

    data class FullDayAbsenceRequest(
        val absenceType: AbsenceType
    )

    @PostMapping("/units/{unitId}/children/{childId}/full-day-absence")
    fun postFullDayAbsence(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: UUID,
        @RequestBody body: FullDayAbsenceRequest
    ) {
        Audit.ChildAttendancesFullDayAbsenceCreate.log(targetId = childId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        db.transaction { tx ->
            val placementBasics = tx.fetchChildPlacementBasics(childId, unitId)

            val attendance = tx.getChildAttendance(childId, unitId, HelsinkiDateTime.now())
            if (attendance != null) {
                throw Conflict("Cannot add full day absence, child already has attendance")
            }

            try {
                tx.deleteAbsencesByDate(childId, dateNow())
                getCareTypes(placementBasics.placementType).forEach { careType ->
                    tx.insertAbsence(user, childId, LocalDate.now(), careType, body.absenceType)
                }
            } catch (e: Exception) {
                throw mapPSQLException(e)
            }
        }
    }

    data class AbsenceRangeRequest(
        val absenceType: AbsenceType,
        val startDate: LocalDate,
        val endDate: LocalDate
    )

    @PostMapping("/units/{unitId}/children/{childId}/absence-range")
    fun postAbsenceRange(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: UUID,
        @RequestBody body: AbsenceRangeRequest
    ) {
        Audit.ChildAttendancesAbsenceRangeCreate.log(targetId = childId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        db.transaction { tx ->
            val typeOnDates = tx.fetchChildPlacementTypeDates(childId, unitId, body.startDate, body.endDate)

            try {
                for ((date, placementType) in typeOnDates) {
                    tx.deleteAbsencesByDate(childId, date)
                    getCareTypes(placementType).forEach { careType ->
                        tx.insertAbsence(user, childId, date, careType, body.absenceType)
                    }
                }
            } catch (e: Exception) {
                throw mapPSQLException(e)
            }
        }
    }

    @DeleteMapping("/units/{unitId}/children/{childId}/absence-range")
    fun deleteAbsenceRange(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ) {
        Audit.AbsenceDeleteRange.log(targetId = childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(UserRole.MOBILE)
        return db.transaction { tx -> tx.deleteAbsencesByFiniteDateRange(childId, FiniteDateRange(from, to)) }
    }
}

data class ChildPlacementBasics(
    val placementType: PlacementType,
    val dateOfBirth: LocalDate
)

private fun Database.Read.fetchChildPlacementBasics(childId: UUID, unitId: DaycareId): ChildPlacementBasics {
    // language=sql
    val sql =
        """
        SELECT p.type AS placement_type, c.date_of_birth
        FROM person c 
        JOIN placement p 
            ON p.child_id = c.id AND daterange(p.start_date, p.end_date, '[]') @> :date
        LEFT JOIN backup_care bc
            ON bc.child_id = c.id AND daterange(bc.start_date, bc.end_date, '[]') @> :date
        WHERE c.id = :childId AND (p.unit_id = :unitId OR bc.unit_id = :unitId)
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("unitId", unitId)
        .bind("date", dateNow())
        .mapTo<ChildPlacementBasics>()
        .list()
        .firstOrNull() ?: throw BadRequest("Child $childId has no placement in unit $unitId on the given day")
}

data class PlacementTypeDate(
    val date: LocalDate,
    val placementType: PlacementType
)

private fun Database.Read.fetchChildPlacementTypeDates(
    childId: UUID,
    unitId: DaycareId,
    startDate: LocalDate,
    endDate: LocalDate
): List<PlacementTypeDate> {

    // language=sql
    val sql = """
        SELECT DISTINCT d::date AS date, p.type AS placement_type
        FROM generate_series(:startDate, :endDate, '1 day') d
        JOIN placement p ON daterange(p.start_date, p.end_date, '[]') @> d::date
        LEFT JOIN backup_care bc
            ON bc.child_id = p.child_id AND daterange(bc.start_date, bc.end_date, '[]') @> d::date
        WHERE p.child_id = :childId AND (p.unit_id = :unitId OR bc.unit_id = :unitId)
    """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("unitId", unitId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .mapTo<PlacementTypeDate>()
        .list()
}

private fun Database.Read.getAttendancesResponse(unitId: DaycareId, instant: HelsinkiDateTime): AttendanceResponse {
    val childrenBasics = fetchChildrenBasics(unitId, instant)
    val dailyNotesForChildrenInUnit = getChildDailyNotesInUnit(unitId)
    val stickyNotesForChildrenInUnit = getChildStickyNotesForUnit(unitId)
    val attendanceReservations = fetchAttendanceReservations(unitId, instant.toLocalDate())

    val children = childrenBasics.map { child ->
        val placementBasics = ChildPlacementBasics(child.placementType, child.dateOfBirth)
        val status = getChildAttendanceStatus(placementBasics, child.attendance, child.absences)
        val dailyNote = dailyNotesForChildrenInUnit.firstOrNull { it.childId == child.id }
        val stickyNotes = stickyNotesForChildrenInUnit.filter { it.childId == child.id }

        Child(
            id = child.id,
            firstName = child.firstName,
            lastName = child.lastName,
            preferredName = child.preferredName,
            placementType = child.placementType,
            groupId = child.groupId,
            backup = child.backup,
            status = status,
            attendance = child.attendance,
            absences = child.absences,
            dailyServiceTimes = child.dailyServiceTimes,
            dailyNote = dailyNote,
            stickyNotes = stickyNotes,
            imageUrl = child.imageUrl,
            reservations = attendanceReservations[child.id] ?: listOf()
        )
    }

    val groupNotes = getGroupNotesForUnit(unitId)
    return AttendanceResponse(children, groupNotes)
}

private fun getChildAttendanceStatus(
    placementBasics: ChildPlacementBasics,
    attendance: AttendanceTimes?,
    absences: List<ChildAbsence>
): AttendanceStatus {
    if (attendance != null) {
        return if (attendance.departed == null) AttendanceStatus.PRESENT else AttendanceStatus.DEPARTED
    }

    if (isFullyAbsent(placementBasics, absences)) {
        return AttendanceStatus.ABSENT
    }

    return AttendanceStatus.COMING
}

private fun isFullyAbsent(placementBasics: ChildPlacementBasics, absences: List<ChildAbsence>): Boolean {
    return getCareTypes(placementBasics.placementType).all { type -> absences.map { it.careType }.contains(type) }
}

private fun getCareTypes(placementType: PlacementType): List<AbsenceCareType> =
    when (placementType) {
        PlacementType.SCHOOL_SHIFT_CARE ->
            listOf(AbsenceCareType.SCHOOL_SHIFT_CARE)
        PRESCHOOL, PREPARATORY ->
            listOf(AbsenceCareType.PRESCHOOL)
        PRESCHOOL_DAYCARE, PREPARATORY_DAYCARE ->
            listOf(AbsenceCareType.PRESCHOOL, AbsenceCareType.PRESCHOOL_DAYCARE)
        DAYCARE_FIVE_YEAR_OLDS,
        DAYCARE_PART_TIME_FIVE_YEAR_OLDS ->
            listOf(AbsenceCareType.DAYCARE_5YO_FREE, AbsenceCareType.DAYCARE)
        PlacementType.DAYCARE, PlacementType.DAYCARE_PART_TIME,
        PlacementType.TEMPORARY_DAYCARE, PlacementType.TEMPORARY_DAYCARE_PART_DAY ->
            listOf(AbsenceCareType.DAYCARE)
        PlacementType.CLUB ->
            listOf(AbsenceCareType.CLUB)
    }

data class AbsenceThreshold(
    val type: AbsenceCareType,
    val time: LocalTime
)

private fun getPartialAbsenceThresholds(
    placementBasics: ChildPlacementBasics,
    arrived: LocalTime,
    childHasPaidServiceNeedToday: Boolean
): List<AbsenceThreshold> {
    val placementType = placementBasics.placementType

    return listOfNotNull(
        preschoolAbsenceThreshold(placementType, arrived),
        preschoolDaycareAbsenceThreshold(placementType, arrived),
        daycareAbsenceThreshold(placementType, arrived, childHasPaidServiceNeedToday)
    )
}

private fun preschoolAbsenceThreshold(placementType: PlacementType, arrived: LocalTime): AbsenceThreshold? {
    if (placementType in listOf(PRESCHOOL, PRESCHOOL_DAYCARE)) {
        val threshold =
            if (arrived > preschoolEnd || Duration.between(arrived, preschoolEnd) < preschoolMinimumDuration)
                LocalTime.of(23, 59)
            else minOf(maxOf(arrived, preschoolStart).plus(preschoolMinimumDuration), preschoolEnd)
        return AbsenceThreshold(AbsenceCareType.PRESCHOOL, threshold)
    }

    if (placementType in listOf(PREPARATORY, PREPARATORY_DAYCARE)) {
        val threshold =
            if (arrived > preparatoryEnd || Duration.between(arrived, preparatoryEnd) < preparatoryMinimumDuration)
                LocalTime.of(23, 59)
            else minOf(maxOf(arrived, preschoolStart).plus(preparatoryMinimumDuration), preparatoryEnd)
        return AbsenceThreshold(AbsenceCareType.PRESCHOOL, threshold)
    }

    return null
}

private fun preschoolDaycareAbsenceThreshold(placementType: PlacementType, arrived: LocalTime): AbsenceThreshold? {
    if (placementType == PRESCHOOL_DAYCARE) {
        if (Duration.between(arrived, preschoolStart) > connectedDaycareBuffer) return null

        val threshold = preschoolEnd.plus(connectedDaycareBuffer)
        return AbsenceThreshold(AbsenceCareType.PRESCHOOL_DAYCARE, threshold)
    }

    if (placementType == PREPARATORY_DAYCARE) {
        if (Duration.between(arrived, preparatoryStart) > connectedDaycareBuffer) return null

        val threshold = preparatoryEnd.plus(connectedDaycareBuffer)
        return AbsenceThreshold(AbsenceCareType.PRESCHOOL_DAYCARE, threshold)
    }

    return null
}

private fun daycareAbsenceThreshold(placementType: PlacementType, arrived: LocalTime, childHasPaidServiceNeedToday: Boolean): AbsenceThreshold? {
    if (placementType in listOf(DAYCARE_FIVE_YEAR_OLDS, DAYCARE_PART_TIME_FIVE_YEAR_OLDS) && childHasPaidServiceNeedToday) {
        val threshold = arrived.plus(fiveYearOldFreeLimit)
        return AbsenceThreshold(AbsenceCareType.DAYCARE, threshold)
    }

    return null
}

private fun Database.Read.childHasPaidServiceNeedToday(
    childId: UUID
): Boolean = createQuery(
    """
SELECT EXISTS(
    SELECT 1
    FROM placement p  
        LEFT JOIN service_need sn ON p.id = sn.placement_id AND daterange(sn.start_date, sn.end_date, '[]') @> current_date
        LEFT JOIN service_need_option sno ON sn.option_id = sno.id
    WHERE
        p.child_id = :childId
        AND daterange(p.start_date, p.end_date, '[]') @> current_date
        AND sno.fee_coefficient > 0.0)
    """.trimIndent()
).bind("childId", childId)
    .mapTo<Boolean>()
    .first()
