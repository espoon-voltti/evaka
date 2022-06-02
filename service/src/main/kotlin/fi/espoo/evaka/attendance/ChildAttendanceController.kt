// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.service.AbsenceCategory
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
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
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
    private val accessControl: AccessControl,
    private val acl: AccessControlList
) {
    @GetMapping("/units/{unitId}")
    fun getAttendances(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @PathVariable unitId: DaycareId
    ): AttendanceResponse {
        Audit.ChildAttendancesRead.log(targetId = unitId)
        accessControl.requirePermissionFor(user, Action.Unit.READ_CHILD_ATTENDANCES, unitId)

        return db.connect { dbc -> dbc.read { it.getAttendancesResponse(unitId, evakaClock.now()) } }
    }

    data class AttendancesRequest(
        val date: LocalDate,
        val attendances: List<AttendanceTimeRange>
    )

    data class AttendanceTimeRange(
        val startTime: LocalTime,
        val endTime: LocalTime?
    )

    @PostMapping("/units/{unitId}/children/{childId}")
    fun upsertAttendances(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: ChildId,
        @RequestBody body: AttendancesRequest
    ) {
        Audit.ChildAttendancesUpsert.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Unit.UPDATE_CHILD_ATTENDANCES, unitId)

        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.deleteAbsencesByDate(childId, evakaClock.today())
                tx.deleteAttendancesByDate(childId, body.date)
                try {
                    body.attendances.forEach {
                        tx.insertAttendance(
                            childId = childId,
                            unitId = unitId,
                            date = body.date,
                            startTime = it.startTime,
                            endTime = it.endTime
                        )
                    }
                } catch (e: Exception) {
                    throw mapPSQLException(e)
                }
            }
        }
    }

    data class ArrivalRequest(
        @DateTimeFormat(pattern = "HH:mm") val arrived: LocalTime
    )

    @PostMapping("/units/{unitId}/children/{childId}/arrival")
    fun postArrival(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: ChildId,
        @RequestBody body: ArrivalRequest
    ) {
        Audit.ChildAttendancesArrivalCreate.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Unit.UPDATE_CHILD_ATTENDANCES, unitId)

        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.fetchChildPlacementBasics(childId, unitId, evakaClock.today())

                tx.deleteAbsencesByDate(childId, evakaClock.today())
                try {
                    tx.insertAttendance(
                        childId = childId,
                        unitId = unitId,
                        date = evakaClock.today(),
                        startTime = body.arrived
                    )
                } catch (e: Exception) {
                    throw mapPSQLException(e)
                }
            }
        }
    }

    @PostMapping("/units/{unitId}/children/{childId}/return-to-coming")
    fun returnToComing(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: ChildId
    ) {
        Audit.ChildAttendancesReturnToComing.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Unit.UPDATE_CHILD_ATTENDANCES, unitId)

        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.fetchChildPlacementBasics(childId, unitId, evakaClock.today())
                tx.deleteAbsencesByDate(childId, evakaClock.today())

                val attendance = tx.getChildOngoingAttendance(childId, unitId)
                if (attendance != null) tx.deleteAttendance(attendance.id)
            }
        }
    }

    @GetMapping("/units/{unitId}/children/{childId}/departure")
    fun getChildDeparture(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: ChildId
    ): List<AbsenceThreshold> {
        Audit.ChildAttendancesDepartureRead.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Unit.READ_CHILD_ATTENDANCES, unitId)

        return db.connect { dbc ->
            dbc.read { tx ->
                val placementBasics = tx.fetchChildPlacementBasics(childId, unitId, evakaClock.today())
                val attendance = tx.getChildOngoingAttendance(childId, unitId)
                    ?: throw Conflict("Cannot depart, has not yet arrived")
                val childHasPaidServiceNeedToday = tx.childHasPaidServiceNeedToday(childId, evakaClock.today())
                getPartialAbsenceThresholds(placementBasics, attendance.startTime, childHasPaidServiceNeedToday)
            }
        }
    }

    data class DepartureRequest(
        @DateTimeFormat(pattern = "HH:mm") val departed: LocalTime,
        val absenceType: AbsenceType?
    )

    @PostMapping("/units/{unitId}/children/{childId}/departure")
    fun postDeparture(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: ChildId,
        @RequestBody body: DepartureRequest
    ) {
        Audit.ChildAttendancesDepartureCreate.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Unit.UPDATE_CHILD_ATTENDANCES, unitId)

        db.connect { dbc ->
            dbc.transaction { tx ->
                val today = evakaClock.today()
                val placementBasics = tx.fetchChildPlacementBasics(childId, unitId, today)

                val attendance = tx.getChildOngoingAttendance(childId, unitId)
                    ?: throw Conflict("Cannot depart, has not yet arrived")

                val childHasPaidServiceNeedToday = tx.childHasPaidServiceNeedToday(childId, today)

                val absentFrom = getPartialAbsenceThresholds(placementBasics, attendance.startTime, childHasPaidServiceNeedToday)
                    .filter { body.departed <= it.time }
                tx.deleteAbsencesByDate(childId, today)
                if (absentFrom.isNotEmpty()) {
                    if (body.absenceType == null) {
                        throw BadRequest("Request had no absenceType but child was absent from ${absentFrom.joinToString(", ")}.")
                    }

                    absentFrom.forEach { (careType, _) ->
                        tx.insertAbsence(user, childId, today, careType, body.absenceType)
                    }
                } else if (body.absenceType != null) {
                    throw BadRequest("Request defines absenceType but child was not absent.")
                }

                try {
                    if (attendance.date == today) {
                        tx.updateAttendanceEnd(
                            attendanceId = attendance.id,
                            endTime = body.departed
                        )
                    } else {
                        tx.updateAttendanceEnd(
                            attendanceId = attendance.id,
                            endTime = LocalTime.of(23, 59)
                        )
                        generateSequence(attendance.date.plusDays(1)) { it.plusDays(1) }
                            .takeWhile { it <= today }
                            .map { date ->
                                Triple(
                                    date,
                                    LocalTime.of(0, 0),
                                    if (date < today) LocalTime.of(23, 59) else body.departed
                                )
                            }
                            .filter { (_, startTime, endTime) -> startTime != endTime }
                            .forEach { (date, startTime, endTime) ->
                                tx.insertAttendance(childId, unitId, date, startTime, endTime)
                            }
                    }
                } catch (e: Exception) {
                    throw mapPSQLException(e)
                }
            }
        }
    }

    @PostMapping("/units/{unitId}/children/{childId}/return-to-present")
    fun returnToPresent(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: ChildId
    ) {
        Audit.ChildAttendancesReturnToPresent.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Unit.UPDATE_CHILD_ATTENDANCES, unitId)

        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.fetchChildPlacementBasics(childId, unitId, evakaClock.today())
                tx.deleteAbsencesByDate(childId, evakaClock.today())

                tx.getChildAttendance(childId, unitId, evakaClock.now())?.let { attendance ->
                    tx.unsetAttendanceEndTime(attendance.id)
                }
            }
        }
    }

    data class FullDayAbsenceRequest(
        val absenceType: AbsenceType
    )

    @PostMapping("/units/{unitId}/children/{childId}/full-day-absence")
    fun postFullDayAbsence(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: ChildId,
        @RequestBody body: FullDayAbsenceRequest
    ) {
        Audit.ChildAttendancesFullDayAbsenceCreate.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Unit.UPDATE_CHILD_ATTENDANCES, unitId)

        db.connect { dbc ->
            dbc.transaction { tx ->
                val placementBasics = tx.fetchChildPlacementBasics(childId, unitId, evakaClock.today())

                val attendance = tx.getChildAttendance(childId, unitId, evakaClock.now())
                if (attendance != null) {
                    throw Conflict("Cannot add full day absence, child already has attendance")
                }

                try {
                    tx.deleteAbsencesByDate(childId, evakaClock.today())
                    placementBasics.placementType.absenceCategories().forEach { category ->
                        tx.insertAbsence(user, childId, evakaClock.today(), category, body.absenceType)
                    }
                } catch (e: Exception) {
                    throw mapPSQLException(e)
                }
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
        db: Database,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: ChildId,
        @RequestBody body: AbsenceRangeRequest
    ) {
        Audit.ChildAttendancesAbsenceRangeCreate.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Unit.UPDATE_CHILD_ATTENDANCES, unitId)

        db.connect { dbc ->
            dbc.transaction { tx ->
                val typeOnDates = tx.fetchChildPlacementTypeDates(childId, unitId, body.startDate, body.endDate)

                try {
                    for ((date, placementType) in typeOnDates) {
                        tx.deleteAbsencesByDate(childId, date)
                        placementType.absenceCategories().forEach { category ->
                            tx.insertAbsence(user, childId, date, category, body.absenceType)
                        }
                    }
                } catch (e: Exception) {
                    throw mapPSQLException(e)
                }
            }
        }
    }

    @DeleteMapping("/units/{unitId}/children/{childId}/absence-range")
    fun deleteAbsenceRange(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ) {
        Audit.AbsenceDeleteRange.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.DELETE_ABSENCE_RANGE, childId)
        return db.connect { dbc -> dbc.transaction { tx -> tx.deleteAbsencesByFiniteDateRange(childId, FiniteDateRange(from, to)) } }
    }
}

data class ChildPlacementBasics(
    val placementType: PlacementType,
    val dateOfBirth: LocalDate
)

private fun Database.Read.fetchChildPlacementBasics(childId: ChildId, unitId: DaycareId, today: LocalDate): ChildPlacementBasics {
    // language=sql
    val sql =
        """
        SELECT p.type AS placement_type, c.date_of_birth
        FROM person c 
        JOIN placement p 
            ON p.child_id = c.id AND daterange(p.start_date, p.end_date, '[]') @> :today
        LEFT JOIN backup_care bc
            ON bc.child_id = c.id AND daterange(bc.start_date, bc.end_date, '[]') @> :today
        WHERE c.id = :childId AND (p.unit_id = :unitId OR bc.unit_id = :unitId)
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("unitId", unitId)
        .bind("today", today)
        .mapTo<ChildPlacementBasics>()
        .list()
        .firstOrNull() ?: throw BadRequest("Child $childId has no placement in unit $unitId on date $today")
}

data class PlacementTypeDate(
    val date: LocalDate,
    val placementType: PlacementType
)

private fun Database.Read.fetchChildPlacementTypeDates(
    childId: ChildId,
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
    val dailyNotesForChildrenInUnit = getChildDailyNotesInUnit(unitId, instant.toLocalDate())
    val stickyNotesForChildrenInUnit = getChildStickyNotesForUnit(unitId, instant.toLocalDate())
    val attendanceReservations = fetchAttendanceReservations(unitId, instant)

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
            reservations = attendanceReservations[child.id] ?.sortedBy { it.startTime } ?: listOf()
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
    val absenceCategories = absences.asSequence().map { it.category }.toSet()
    return placementBasics.placementType.absenceCategories() == absenceCategories
}

data class AbsenceThreshold(
    val category: AbsenceCategory,
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
        return AbsenceThreshold(AbsenceCategory.NONBILLABLE, threshold)
    }

    if (placementType in listOf(PREPARATORY, PREPARATORY_DAYCARE)) {
        val threshold =
            if (arrived > preparatoryEnd || Duration.between(arrived, preparatoryEnd) < preparatoryMinimumDuration)
                LocalTime.of(23, 59)
            else minOf(maxOf(arrived, preschoolStart).plus(preparatoryMinimumDuration), preparatoryEnd)
        return AbsenceThreshold(AbsenceCategory.NONBILLABLE, threshold)
    }

    return null
}

private fun preschoolDaycareAbsenceThreshold(placementType: PlacementType, arrived: LocalTime): AbsenceThreshold? {
    if (placementType == PRESCHOOL_DAYCARE) {
        if (Duration.between(arrived, preschoolStart) > connectedDaycareBuffer) return null

        val threshold = preschoolEnd.plus(connectedDaycareBuffer)
        return AbsenceThreshold(AbsenceCategory.BILLABLE, threshold)
    }

    if (placementType == PREPARATORY_DAYCARE) {
        if (Duration.between(arrived, preparatoryStart) > connectedDaycareBuffer) return null

        val threshold = preparatoryEnd.plus(connectedDaycareBuffer)
        return AbsenceThreshold(AbsenceCategory.BILLABLE, threshold)
    }

    return null
}

private fun daycareAbsenceThreshold(placementType: PlacementType, arrived: LocalTime, childHasPaidServiceNeedToday: Boolean): AbsenceThreshold? {
    if (placementType in listOf(DAYCARE_FIVE_YEAR_OLDS, DAYCARE_PART_TIME_FIVE_YEAR_OLDS) && childHasPaidServiceNeedToday) {
        val threshold = arrived.plus(fiveYearOldFreeLimit)
        return AbsenceThreshold(AbsenceCategory.BILLABLE, threshold)
    }

    return null
}

private fun Database.Read.childHasPaidServiceNeedToday(
    childId: ChildId,
    today: LocalDate
): Boolean = createQuery(
    """
SELECT EXISTS(
    SELECT 1
    FROM placement p  
        LEFT JOIN service_need sn ON p.id = sn.placement_id AND daterange(sn.start_date, sn.end_date, '[]') @> :today
        LEFT JOIN service_need_option sno ON sn.option_id = sno.id
    WHERE
        p.child_id = :childId
        AND daterange(p.start_date, p.end_date, '[]') @> :today
        AND sno.fee_coefficient > 0.0)
    """.trimIndent()
)
    .bind("childId", childId)
    .bind("today", today)
    .mapTo<Boolean>()
    .first()
