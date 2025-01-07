// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.absence.AbsenceUpsert
import fi.espoo.evaka.absence.getAbsencesOfChildByDate
import fi.espoo.evaka.absence.insertAbsences
import fi.espoo.evaka.absence.setChildDateAbsences
import fi.espoo.evaka.daycare.getClubTerms
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.note.child.daily.getChildDailyNotesForChildren
import fi.espoo.evaka.note.child.sticky.getChildStickyNotesForChildren
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.reservations.clearOldReservations
import fi.espoo.evaka.reservations.getExpectedAbsenceCategories
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
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeInterval
import fi.espoo.evaka.shared.domain.getOperationalDatesForChildren
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.utils.mapOfNotNullValues
import java.time.LocalDate
import java.time.LocalTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ChildAttendanceController(
    private val accessControl: AccessControl,
    private val featureConfig: FeatureConfig,
) {
    @GetMapping("/employee-mobile/attendances/units/{unitId}/children")
    fun getChildren(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
    ): List<AttendanceChild> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_CHILD_ATTENDANCES,
                        unitId,
                    )
                    val now = clock.now()
                    val today = now.toLocalDate()

                    val clubTerms = tx.getClubTerms()
                    val preschoolTerms = tx.getPreschoolTerms()

                    val childrenBasics = tx.fetchChildrenBasics(unitId, now)
                    val childIds = childrenBasics.asSequence().map { it.id }.toSet()
                    val operationalDatesByChild =
                        tx.getOperationalDatesForChildren(
                            range = FiniteDateRange(today, today.plusDays(7)),
                            children = childIds,
                        )

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
                            operationalDates = operationalDatesByChild[child.id] ?: emptySet(),
                            groupId = child.groupId,
                            backup = child.backup,
                            dailyServiceTimes = child.dailyServiceTimes?.times,
                            dailyNote = dailyNotes[child.id],
                            stickyNotes = stickyNotes[child.id] ?: emptyList(),
                            imageUrl = child.imageUrl,
                            reservations = reservations[child.id] ?: emptyList(),
                        )
                    }
                }
            }
            .also {
                Audit.ChildAttendanceChildrenRead.log(
                    targetId = AuditId(unitId),
                    meta = mapOf("childCount" to it.size),
                )
            }
    }

    data class ChildAttendanceStatusResponse(
        val absences: List<ChildAbsence>,
        val attendances: List<AttendanceTimes>,
        val status: AttendanceStatus,
    )

    @GetMapping("/employee-mobile/attendances/units/{unitId}/attendances")
    fun getAttendanceStatuses(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
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
                        unitId,
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
                                            clock.now(),
                                            placementType,
                                            attendances,
                                            absences,
                                        ),
                                    )
                            }
                        }
                        .filterNotNull()
                        .toMap()
                }
            }
            .also {
                Audit.ChildAttendanceStatusesRead.log(
                    targetId = AuditId(unitId),
                    meta = mapOf("childCount" to it.size),
                )
            }
    }

    data class ArrivalsRequest(
        val children: Set<ChildId>,
        @DateTimeFormat(pattern = "HH:mm") val arrived: LocalTime,
    )

    @PostMapping("/employee-mobile/attendances/units/{unitId}/arrivals")
    fun postArrivals(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @RequestBody body: ArrivalsRequest,
    ) {
        val today = clock.today()
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.UPDATE_CHILD_ATTENDANCES,
                        unitId,
                    )
                    body.children.map { childId ->
                        tx.fetchChildPlacementBasics(childId, unitId, today)
                        try {
                            tx.insertAttendance(
                                childId = childId,
                                unitId = unitId,
                                date = today,
                                range = TimeInterval(body.arrived, null),
                                now = clock.now(),
                                createdById = user.evakaUserId,
                            )
                        } catch (e: Exception) {
                            throw mapPSQLException(e)
                        }
                    }
                }
            }
            .also { attendanceIds ->
                Audit.ChildAttendancesArrivalCreate.log(
                    targetId = AuditId(body.children.toList()),
                    objectId = AuditId(attendanceIds),
                    meta = mapOf("unitId" to unitId),
                )
            }
    }

    @PostMapping("/employee-mobile/attendances/units/{unitId}/children/{childId}/return-to-coming")
    fun returnToComing(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: ChildId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.UPDATE_CHILD_ATTENDANCES,
                    unitId,
                )
                tx.fetchChildPlacementBasics(childId, unitId, clock.today())

                val attendance = tx.getOngoingAttendanceForChild(childId, unitId)
                if (attendance != null) tx.deleteAttendance(attendance.id)
            }
        }
        Audit.ChildAttendancesReturnToComing.log(
            targetId = AuditId(childId),
            objectId = AuditId(unitId),
        )
    }

    data class ExpectedAbsencesOnDeparturesRequest(
        val childIds: Set<ChildId>,
        @DateTimeFormat(pattern = "HH:mm") val departed: LocalTime,
    )

    data class ExpectedAbsencesOnDeparturesResponse(
        val categoriesByChild: Map<ChildId, Set<AbsenceCategory>?>
    )

    @PostMapping("/employee-mobile/attendances/units/{unitId}/departure/expected-absences")
    fun getExpectedAbsencesOnDepartures(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @RequestBody body: ExpectedAbsencesOnDeparturesRequest,
    ): ExpectedAbsencesOnDeparturesResponse {
        val today = clock.today()
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_CHILD_ATTENDANCES,
                        unitId,
                    )

                    val ongoingAttendances =
                        tx.getOngoingAttendanceForChildren(body.childIds, unitId)
                    val attendanceTimesToday =
                        tx.getCompletedAttendanceTimesForChildren(body.childIds, unitId, today)

                    val attendanceTimesByChild =
                        body.childIds.associateWith { childId ->
                            val ongoingAttendance =
                                ongoingAttendances[childId]
                                    ?: throw BadRequest("Cannot depart, has not yet arrived")

                            attendanceTimesToday.getOrDefault(childId, emptyList()) +
                                ongoingAttendance.toTimeRange(
                                    HelsinkiDateTime.of(today, body.departed)
                                )
                        }

                    ExpectedAbsencesOnDeparturesResponse(
                        categoriesByChild =
                            getExpectedAbsenceCategories(
                                tx = tx,
                                date = today,
                                attendanceTimesByChild = attendanceTimesByChild,
                            )
                    )
                }
            }
            .also {
                Audit.ChildAttendancesDepartureRead.log(
                    targetId = AuditId(body.childIds),
                    objectId = AuditId(unitId),
                )
            }
    }

    data class ChildDeparture(
        val childId: ChildId,
        val absenceTypeNonbillable: AbsenceType?,
        val absenceTypeBillable: AbsenceType?,
    )

    data class DeparturesRequest(
        val departures: List<ChildDeparture>,
        @DateTimeFormat(pattern = "HH:mm") val departed: LocalTime,
    )

    @PostMapping("/employee-mobile/attendances/units/{unitId}/departures")
    fun postDepartures(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @RequestBody body: DeparturesRequest,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.UPDATE_CHILD_ATTENDANCES,
                    unitId,
                )
                val now = clock.now()
                val today = clock.today()

                val childIds = body.departures.map { it.childId }.toSet()

                val ongoingAttendances = tx.getOngoingAttendanceForChildren(childIds, unitId)

                body.departures.forEach { departure ->
                    val childId = departure.childId
                    val ongoingAttendance =
                        ongoingAttendances[childId]
                            ?: throw BadRequest("Cannot depart, child $childId has not yet arrived")

                    validateAndSetAbsences(
                        tx = tx,
                        now = now,
                        user = user,
                        unitId = unitId,
                        childId = childId,
                        ongoingAttendance = ongoingAttendance,
                        departed = body.departed,
                        absences =
                            mapOfNotNullValues(
                                AbsenceCategory.NONBILLABLE to departure.absenceTypeNonbillable,
                                AbsenceCategory.BILLABLE to departure.absenceTypeBillable,
                            ),
                    )

                    try {
                        if (ongoingAttendance.date == today) {
                            tx.updateAttendanceEnd(
                                attendanceId = ongoingAttendance.id,
                                endTime = body.departed,
                            )
                        } else {
                            tx.updateAttendanceEnd(
                                attendanceId = ongoingAttendance.id,
                                endTime = LocalTime.of(23, 59),
                            )
                            generateSequence(ongoingAttendance.date.plusDays(1)) { it.plusDays(1) }
                                .takeWhile { it <= today }
                                .map { date ->
                                    Triple(
                                        date,
                                        LocalTime.of(0, 0),
                                        if (date < today) LocalTime.of(23, 59) else body.departed,
                                    )
                                }
                                .filter { (_, startTime, endTime) -> startTime != endTime }
                                .forEach { (date, startTime, endTime) ->
                                    tx.insertAttendance(
                                        childId,
                                        unitId,
                                        date,
                                        TimeInterval(startTime, endTime),
                                        clock.now(),
                                        user.evakaUserId,
                                    )
                                }
                        }
                    } catch (e: Exception) {
                        throw mapPSQLException(e)
                    }
                }
            }
        }

        Audit.ChildAttendancesDepartureCreate.log(
            targetId = AuditId(body.departures.map { it.childId }),
            objectId = AuditId(unitId),
        )
    }

    private fun validateAndSetAbsences(
        tx: Database.Transaction,
        now: HelsinkiDateTime,
        user: AuthenticatedUser,
        unitId: DaycareId,
        childId: ChildId,
        ongoingAttendance: OngoingAttendance,
        departed: LocalTime,
        absences: Map<AbsenceCategory, AbsenceType>,
    ) {
        val today = now.toLocalDate()
        val attendanceTimesToday =
            tx.getCompletedAttendanceTimesForChild(childId, unitId, today) +
                ongoingAttendance.toTimeRange(HelsinkiDateTime.of(today, departed))
        val expectedAbsences =
            getExpectedAbsenceCategories(
                tx = tx,
                date = today,
                childId = childId,
                attendanceTimes = attendanceTimesToday,
            )

        // TODO: once calculation works properly for Tampere special cases this could be
        //  changed to `expectedAbsences != null && absences.keys != expectedAbsences`
        if (expectedAbsences != null && absences.keys.any { !expectedAbsences.contains(it) }) {
            throw BadRequest("Absences in request do not match expected absences")
        }

        setChildDateAbsences(tx, now, user.evakaUserId, childId, now.toLocalDate(), absences)
    }

    @PostMapping("/employee-mobile/attendances/units/{unitId}/children/{childId}/return-to-present")
    fun returnToPresent(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: ChildId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.UPDATE_CHILD_ATTENDANCES,
                    unitId,
                )
                tx.fetchChildPlacementBasics(childId, unitId, clock.today())

                tx.getChildAttendanceId(childId, unitId, clock.now())?.also {
                    tx.unsetAttendanceEndTime(it)
                }
            }
        }
        Audit.ChildAttendancesReturnToPresent.log(
            targetId = AuditId(childId),
            objectId = AuditId(unitId),
        )
    }

    data class FullDayAbsenceRequest(val absenceType: AbsenceType)

    @PostMapping("/employee-mobile/attendances/units/{unitId}/children/{childId}/full-day-absence")
    fun postFullDayAbsence(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: ChildId,
        @RequestBody body: FullDayAbsenceRequest,
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
                    unitId,
                )
                val placementBasics = tx.fetchChildPlacementBasics(childId, unitId, clock.today())

                val ongoingAttendance = tx.getOngoingAttendanceForChild(childId, unitId)
                val completedAttendances =
                    tx.getCompletedAttendanceTimesForChild(childId, unitId, today)
                if (ongoingAttendance != null || completedAttendances.isNotEmpty()) {
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
        Audit.ChildAttendancesFullDayAbsenceCreate.log(
            targetId = AuditId(childId),
            objectId = AuditId(unitId),
        )
    }

    @DeleteMapping(
        "/employee-mobile/attendances/units/{unitId}/children/{childId}/full-day-absence"
    )
    fun cancelFullDayAbsence(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: ChildId,
    ) {
        val today = clock.today()

        val deletedAbsences =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.DELETE_ABSENCE,
                        childId,
                    )
                    val placementType =
                        tx.fetchChildPlacementBasics(childId, unitId, clock.today()).placementType
                    val absenceCategories =
                        tx.getAbsencesOfChildByDate(childId, today).map { it.category }.toSet()
                    val hasFullDayAbsence = placementType.absenceCategories() == absenceCategories
                    if (hasFullDayAbsence) {
                        tx.deleteAbsencesByDate(childId, today)
                    } else {
                        throw Conflict("Cannot cancel full day absence, child is not fully absent")
                    }
                }
            }

        Audit.ChildAttendancesFullDayAbsenceDelete.log(
            targetId = AuditId(childId),
            objectId = AuditId(unitId),
            meta = mapOf("deletedAbsences" to deletedAbsences, "date" to today),
        )
    }

    data class AbsenceRangeRequest(val absenceType: AbsenceType, val range: FiniteDateRange)

    @PostMapping("/employee-mobile/attendances/units/{unitId}/children/{childId}/absence-range")
    fun postAbsenceRange(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: ChildId,
        @RequestBody body: AbsenceRangeRequest,
    ) {
        val now = clock.now()
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.UPDATE_CHILD_ATTENDANCES,
                    unitId,
                )
                val typeOnDates =
                    tx.fetchChildPlacementTypeDates(
                        childId,
                        unitId,
                        body.range.start,
                        body.range.end,
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
        Audit.ChildAttendancesAbsenceRangeCreate.log(
            targetId = AuditId(childId),
            objectId = AuditId(unitId),
        )
    }

    @DeleteMapping("/employee-mobile/attendances/units/{unitId}/children/{childId}/absence-range")
    fun deleteAbsenceRange(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: ChildId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ) {
        val deleted =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.DELETE_ABSENCE_RANGE,
                        childId,
                    )
                    tx.deleteAbsencesByFiniteDateRange(childId, FiniteDateRange(from, to))
                }
            }
        Audit.AbsenceDeleteRange.log(
            targetId = AuditId(childId),
            objectId = AuditId(deleted),
            meta = mapOf("from" to from, "to" to to),
        )
    }
}

data class ChildPlacementBasics(val placementType: PlacementType, val dateOfBirth: LocalDate)

private fun Database.Read.fetchChildPlacementBasics(
    childId: ChildId,
    unitId: DaycareId,
    today: LocalDate,
): ChildPlacementBasics =
    createQuery {
            sql(
                """
SELECT rp.placement_type, c.date_of_birth
FROM person c 
JOIN realized_placement_all(${bind(today)}) rp
ON c.id = rp.child_id
WHERE c.id = ${bind(childId)} AND rp.unit_id = ${bind(unitId)}
LIMIT 1
"""
            )
        }
        .exactlyOneOrNull<ChildPlacementBasics>()
        ?: throw BadRequest("Child $childId has no placement in unit $unitId on date $today")

data class PlacementTypeDate(val date: LocalDate, val placementType: PlacementType)

private fun Database.Read.fetchChildPlacementTypeDates(
    childId: ChildId,
    unitId: DaycareId,
    startDate: LocalDate,
    endDate: LocalDate,
): List<PlacementTypeDate> =
    createQuery {
            sql(
                """
SELECT DISTINCT d::date AS date, placement_type
FROM generate_series(${bind(startDate)}, ${bind(endDate)}, '1 day') d
JOIN realized_placement_all(d::date) rp ON true
WHERE rp.child_id = ${bind(childId)} AND rp.unit_id = ${bind(unitId)}
"""
            )
        }
        .toList()

private fun getChildAttendanceStatus(
    now: HelsinkiDateTime,
    placementType: PlacementType,
    attendances: List<AttendanceTimes>,
    absences: List<ChildAbsence>,
): AttendanceStatus {
    if (attendances.any { it.departed == null }) {
        return AttendanceStatus.PRESENT
    }

    val hasArrivedToday = attendances.any { it.arrived.toLocalDate() == now.toLocalDate() }
    val hasDepartedRecently =
        attendances.any { it.departed != null && it.departed > now.minusMinutes(30) }
    if (hasArrivedToday || hasDepartedRecently) {
        return AttendanceStatus.DEPARTED
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
