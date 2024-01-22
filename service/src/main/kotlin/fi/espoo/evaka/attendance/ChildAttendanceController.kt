// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.Audit
import fi.espoo.evaka.ForceCodeGenType
import fi.espoo.evaka.daycare.getClubTerms
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.AbsenceUpsert
import fi.espoo.evaka.daycare.service.insertAbsences
import fi.espoo.evaka.note.child.daily.getChildDailyNotesForChildren
import fi.espoo.evaka.note.child.sticky.getChildStickyNotesForChildren
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.PlacementType.DAYCARE_FIVE_YEAR_OLDS
import fi.espoo.evaka.placement.PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS
import fi.espoo.evaka.placement.PlacementType.PREPARATORY
import fi.espoo.evaka.placement.PlacementType.PREPARATORY_DAYCARE
import fi.espoo.evaka.placement.PlacementType.PRESCHOOL
import fi.espoo.evaka.placement.PlacementType.PRESCHOOL_CLUB
import fi.espoo.evaka.placement.PlacementType.PRESCHOOL_DAYCARE
import fi.espoo.evaka.reservations.clearOldReservations
import fi.espoo.evaka.reservations.getReservableRange
import fi.espoo.evaka.reservations.getUnitReservations
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
    private val featureConfig: FeatureConfig
) {
    @GetMapping("/units/{unitId}/children")
    fun getChildren(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId
    ): List<AttendanceChild> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_CHILD_ATTENDANCES,
                        unitId
                    )
                    val now = clock.now()
                    val today = now.toLocalDate()

                    val clubTerms = tx.getClubTerms()
                    val preschoolTerms = tx.getPreschoolTerms()

                    val childrenBasics = tx.fetchChildrenBasics(unitId, now)
                    val childIds = childrenBasics.asSequence().map { it.id }.toSet()

                    val dailyNotes =
                        tx.getChildDailyNotesForChildren(childIds).associateBy { it.childId }
                    val stickyNotes =
                        tx.getChildStickyNotesForChildren(childIds).groupBy { it.childId }
                    val reservations = tx.getUnitReservations(unitId, today)

                    childrenBasics.map { child ->
                        AttendanceChild(
                            id = child.id,
                            firstName = child.firstName,
                            lastName = child.lastName,
                            preferredName = child.preferredName,
                            dateOfBirth = child.dateOfBirth,
                            placementType = child.placementType,
                            scheduleType =
                                child.placementType.scheduleType(today, clubTerms, preschoolTerms),
                            groupId = child.groupId,
                            backup = child.backup,
                            dailyServiceTimes = child.dailyServiceTimes?.times,
                            dailyNote = dailyNotes[child.id],
                            stickyNotes = stickyNotes[child.id] ?: emptyList(),
                            imageUrl = child.imageUrl,
                            reservations = reservations[child.id] ?: listOf()
                        )
                    }
                }
            }
            .also {
                Audit.ChildAttendanceChildrenRead.log(
                    targetId = unitId,
                    meta = mapOf("childCount" to it.size)
                )
            }
    }

    data class ChildAttendanceStatusResponse(
        val absences: List<ChildAbsence>,
        val attendances: List<AttendanceTimes>,
        val status: AttendanceStatus
    )

    @GetMapping("/units/{unitId}/attendances")
    fun getAttendanceStatuses(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
    ): Map<ChildId, ChildAttendanceStatusResponse> {
        val now = clock.now()
        val today = now.toLocalDate()

        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_CHILD_ATTENDANCES,
                        unitId
                    )

                    // Do not return anything for children that have no placement, attendances or
                    // absences; the implicit attendance status is COMING for children returned
                    // by the getChildren() endpoint
                    val childrenAttendances = tx.getUnitChildAttendances(unitId, now)
                    val childrenAbsences = tx.getUnitChildAbsences(unitId, today)
                    val childIds = childrenAttendances.keys + childrenAbsences.keys
                    val childPlacementTypes = tx.getChildPlacementTypes(childIds, today)
                    childIds
                        .asSequence()
                        .map { childId ->
                            val placementType = childPlacementTypes[childId]
                            if (placementType == null) {
                                null
                            } else {
                                val absences = childrenAbsences[childId] ?: emptyList()
                                val attendances = childrenAttendances[childId] ?: emptyList()
                                childId to
                                    ChildAttendanceStatusResponse(
                                        absences,
                                        attendances,
                                        getChildAttendanceStatus(
                                            placementType,
                                            attendances,
                                            absences
                                        )
                                    )
                            }
                        }
                        .filterNotNull()
                        .toMap()
                }
            }
            .also {
                Audit.ChildAttendanceStatusesRead.log(
                    targetId = unitId,
                    meta = mapOf("childCount" to it.size)
                )
            }
    }

    data class ArrivalRequest(
        @ForceCodeGenType(String::class) @DateTimeFormat(pattern = "HH:mm") val arrived: LocalTime
    )

    @PostMapping("/units/{unitId}/children/{childId}/arrival")
    fun postArrival(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: ChildId,
        @RequestBody body: ArrivalRequest
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.UPDATE_CHILD_ATTENDANCES,
                    unitId
                )
                tx.fetchChildPlacementBasics(childId, unitId, clock.today())
                tx.deleteAbsencesByDate(childId, clock.today())
                try {
                    tx.insertAttendance(
                        childId = childId,
                        unitId = unitId,
                        date = clock.today(),
                        startTime = body.arrived
                    )
                } catch (e: Exception) {
                    throw mapPSQLException(e)
                }
            }
        }
        Audit.ChildAttendancesArrivalCreate.log(targetId = childId, objectId = unitId)
    }

    @PostMapping("/units/{unitId}/children/{childId}/return-to-coming")
    fun returnToComing(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: ChildId
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.UPDATE_CHILD_ATTENDANCES,
                    unitId
                )
                tx.fetchChildPlacementBasics(childId, unitId, clock.today())
                tx.deleteAbsencesByDate(childId, clock.today())

                val attendance = tx.getChildOngoingAttendance(childId, unitId)
                if (attendance != null) tx.deleteAttendance(attendance.id)
            }
        }
        Audit.ChildAttendancesReturnToComing.log(targetId = childId, objectId = unitId)
    }

    @GetMapping("/units/{unitId}/children/{childId}/departure")
    fun getChildDeparture(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: ChildId
    ): List<AbsenceThreshold> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_CHILD_ATTENDANCES,
                        unitId
                    )
                    val attendance = tx.getChildOngoingAttendance(childId, unitId)
                    if (attendance == null) emptyList()
                    else getPartialAbsenceThresholds(tx, clock, childId, unitId, attendance)
                }
            }
            .also {
                Audit.ChildAttendancesDepartureRead.log(
                    targetId = childId,
                    objectId = unitId,
                    meta = mapOf("count" to it.size)
                )
            }
    }

    data class DepartureRequest(
        @ForceCodeGenType(String::class) @DateTimeFormat(pattern = "HH:mm") val departed: LocalTime,
        val absenceType: AbsenceType?
    )

    @PostMapping("/units/{unitId}/children/{childId}/departure")
    fun postDeparture(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: ChildId,
        @RequestBody body: DepartureRequest
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.UPDATE_CHILD_ATTENDANCES,
                    unitId
                )
                val now = clock.now()
                val today = clock.today()

                val attendance =
                    tx.getChildOngoingAttendance(childId, unitId)
                        ?: throw Conflict("Cannot depart, has not yet arrived")

                val absentFrom =
                    getPartialAbsenceThresholds(tx, clock, childId, unitId, attendance).filter {
                        body.departed <= it.time
                    }
                tx.deleteAbsencesByDate(childId, today)
                if (absentFrom.isNotEmpty() && body.absenceType != null) {
                    val absences =
                        absentFrom.map { (careType, _) ->
                            AbsenceUpsert(childId, today, careType, body.absenceType)
                        }
                    tx.insertAbsences(now, user.evakaUserId, absences)
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
        Audit.ChildAttendancesDepartureCreate.log(targetId = childId, objectId = unitId)
    }

    @PostMapping("/units/{unitId}/children/{childId}/return-to-present")
    fun returnToPresent(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: ChildId
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.UPDATE_CHILD_ATTENDANCES,
                    unitId
                )
                tx.fetchChildPlacementBasics(childId, unitId, clock.today())
                tx.deleteAbsencesByDate(childId, clock.today())

                tx.getChildAttendance(childId, unitId, clock.now())?.let { attendance ->
                    tx.unsetAttendanceEndTime(attendance.id)
                }
            }
        }
        Audit.ChildAttendancesReturnToPresent.log(targetId = childId, objectId = unitId)
    }

    data class FullDayAbsenceRequest(val absenceType: AbsenceType)

    @PostMapping("/units/{unitId}/children/{childId}/full-day-absence")
    fun postFullDayAbsence(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: ChildId,
        @RequestBody body: FullDayAbsenceRequest
    ) {
        val now = clock.now()
        val today = now.toLocalDate()

        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.UPDATE_CHILD_ATTENDANCES,
                    unitId
                )
                val placementBasics = tx.fetchChildPlacementBasics(childId, unitId, clock.today())

                val attendance = tx.getChildAttendance(childId, unitId, clock.now())
                if (attendance != null) {
                    throw Conflict("Cannot add full day absence, child already has attendance")
                }

                try {
                    tx.deleteAbsencesByDate(childId, today)
                    val absences =
                        placementBasics.placementType.absenceCategories().map { category ->
                            AbsenceUpsert(childId, today, category, body.absenceType)
                        }
                    tx.insertAbsences(now, user.evakaUserId, absences)
                } catch (e: Exception) {
                    throw mapPSQLException(e)
                }
            }
        }
        Audit.ChildAttendancesFullDayAbsenceCreate.log(targetId = childId, objectId = unitId)
    }

    data class AbsenceRangeRequest(
        val absenceType: AbsenceType,
        val range: FiniteDateRange,
    )

    @PostMapping("/units/{unitId}/children/{childId}/absence-range")
    fun postAbsenceRange(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: ChildId,
        @RequestBody body: AbsenceRangeRequest
    ) {
        val now = clock.now()
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.UPDATE_CHILD_ATTENDANCES,
                    unitId
                )
                val typeOnDates =
                    tx.fetchChildPlacementTypeDates(
                        childId,
                        unitId,
                        body.range.start,
                        body.range.end
                    )

                // Delete reservations from unconfirmed range
                val reservableRange =
                    getReservableRange(clock.now(), featureConfig.citizenReservationThresholdHours)
                body.range.intersection(reservableRange)?.let { unconfirmedRange ->
                    tx.clearOldReservations(unconfirmedRange.dates().map { childId to it }.toList())
                }
                try {
                    for ((date, placementType) in typeOnDates) {
                        tx.deleteAbsencesByDate(childId, date)
                        val absences =
                            placementType.absenceCategories().map { category ->
                                AbsenceUpsert(childId, date, category, body.absenceType)
                            }
                        tx.insertAbsences(now, user.evakaUserId, absences)
                    }
                } catch (e: Exception) {
                    throw mapPSQLException(e)
                }
            }
        }
        Audit.ChildAttendancesAbsenceRangeCreate.log(targetId = childId, objectId = unitId)
    }

    @DeleteMapping("/units/{unitId}/children/{childId}/absence-range")
    fun deleteAbsenceRange(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ) {
        val deleted =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.DELETE_ABSENCE_RANGE,
                        childId
                    )
                    tx.deleteAbsencesByFiniteDateRange(childId, FiniteDateRange(from, to))
                }
            }
        Audit.AbsenceDeleteRange.log(
            targetId = childId,
            objectId = deleted,
            meta = mapOf("from" to from, "to" to to)
        )
    }

    private fun getPartialAbsenceThresholds(
        tx: Database.Read,
        clock: EvakaClock,
        childId: ChildId,
        unitId: DaycareId,
        attendance: OngoingAttendance
    ): List<AbsenceThreshold> {
        if (!featureConfig.partialAbsenceThresholdsEnabled) {
            return emptyList()
        }
        val today = clock.today()
        val placementBasics = tx.fetchChildPlacementBasics(childId, unitId, today)
        val childHasPaidServiceNeedToday = tx.childHasPaidServiceNeedToday(childId, today)
        return getPartialAbsenceThresholds(
            placementBasics,
            attendance.startTime,
            childHasPaidServiceNeedToday
        )
    }
}

data class ChildPlacementBasics(val placementType: PlacementType, val dateOfBirth: LocalDate)

private fun Database.Read.fetchChildPlacementBasics(
    childId: ChildId,
    unitId: DaycareId,
    today: LocalDate
): ChildPlacementBasics {
    // language=sql
    val sql =
        """
        SELECT rp.placement_type, c.date_of_birth
        FROM person c 
        JOIN realized_placement_all(:today) rp
        ON c.id = rp.child_id
        WHERE c.id = :childId AND rp.unit_id = :unitId
        LIMIT 1
        """
            .trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("unitId", unitId)
        .bind("today", today)
        .toList<ChildPlacementBasics>()
        .firstOrNull()
        ?: throw BadRequest("Child $childId has no placement in unit $unitId on date $today")
}

data class PlacementTypeDate(val date: LocalDate, val placementType: PlacementType)

private fun Database.Read.fetchChildPlacementTypeDates(
    childId: ChildId,
    unitId: DaycareId,
    startDate: LocalDate,
    endDate: LocalDate
): List<PlacementTypeDate> {
    // language=sql
    val sql =
        """
        SELECT DISTINCT d::date AS date, placement_type
        FROM generate_series(:startDate, :endDate, '1 day') d
        JOIN realized_placement_all(d::date) rp ON true
        WHERE rp.child_id = :childId AND rp.unit_id = :unitId
    """
            .trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("unitId", unitId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .toList<PlacementTypeDate>()
}

private fun getChildAttendanceStatus(
    placementType: PlacementType,
    attendances: List<AttendanceTimes>,
    absences: List<ChildAbsence>
): AttendanceStatus {
    if (attendances.isNotEmpty()) {
        return if (attendances[0].departed == null) AttendanceStatus.PRESENT
        else AttendanceStatus.DEPARTED
    }

    if (isFullyAbsent(placementType, absences)) {
        return AttendanceStatus.ABSENT
    }

    return AttendanceStatus.COMING
}

private fun isFullyAbsent(placementType: PlacementType, absences: List<ChildAbsence>): Boolean {
    val absenceCategories = absences.asSequence().map { it.category }.toSet()
    return placementType.absenceCategories() == absenceCategories
}

data class AbsenceThreshold(
    val category: AbsenceCategory,
    @ForceCodeGenType(String::class) val time: LocalTime
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

private fun preschoolAbsenceThreshold(
    placementType: PlacementType,
    arrived: LocalTime
): AbsenceThreshold? {
    if (placementType in listOf(PRESCHOOL, PRESCHOOL_DAYCARE, PRESCHOOL_CLUB)) {
        val threshold =
            if (
                arrived > preschoolEnd ||
                    Duration.between(arrived, preschoolEnd) < preschoolMinimumDuration
            ) {
                LocalTime.of(23, 59)
            } else {
                minOf(maxOf(arrived, preschoolStart).plus(preschoolMinimumDuration), preschoolEnd)
            }
        return AbsenceThreshold(AbsenceCategory.NONBILLABLE, threshold)
    }

    if (placementType in listOf(PREPARATORY, PREPARATORY_DAYCARE)) {
        val threshold =
            if (
                arrived > preparatoryEnd ||
                    Duration.between(arrived, preparatoryEnd) < preparatoryMinimumDuration
            ) {
                LocalTime.of(23, 59)
            } else {
                minOf(
                    maxOf(arrived, preschoolStart).plus(preparatoryMinimumDuration),
                    preparatoryEnd
                )
            }
        return AbsenceThreshold(AbsenceCategory.NONBILLABLE, threshold)
    }

    return null
}

private fun preschoolDaycareAbsenceThreshold(
    placementType: PlacementType,
    arrived: LocalTime
): AbsenceThreshold? {
    if (placementType == PRESCHOOL_DAYCARE || placementType == PRESCHOOL_CLUB) {
        if (Duration.between(arrived, preschoolStart) > connectedDaycareBuffer) return null
        if (Duration.between(arrived, preschoolEnd) < connectedDaycareBuffer) return null

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

private fun daycareAbsenceThreshold(
    placementType: PlacementType,
    arrived: LocalTime,
    childHasPaidServiceNeedToday: Boolean
): AbsenceThreshold? {
    if (
        placementType in listOf(DAYCARE_FIVE_YEAR_OLDS, DAYCARE_PART_TIME_FIVE_YEAR_OLDS) &&
            childHasPaidServiceNeedToday
    ) {
        val threshold = arrived.plus(fiveYearOldFreeLimit)
        return AbsenceThreshold(AbsenceCategory.BILLABLE, threshold)
    }

    return null
}

private fun Database.Read.childHasPaidServiceNeedToday(
    childId: ChildId,
    today: LocalDate
): Boolean =
    createQuery(
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
    """
                .trimIndent()
        )
        .bind("childId", childId)
        .bind("today", today)
        .exactlyOne<Boolean>()
