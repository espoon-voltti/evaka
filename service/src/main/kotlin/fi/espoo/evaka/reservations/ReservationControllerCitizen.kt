// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import fi.espoo.evaka.Audit
import fi.espoo.evaka.attendance.childrenHaveAttendanceInRange
import fi.espoo.evaka.daycare.ClubTerm
import fi.espoo.evaka.daycare.PreschoolTerm
import fi.espoo.evaka.daycare.getClubTerms
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.AbsenceType.OTHER_ABSENCE
import fi.espoo.evaka.daycare.service.AbsenceType.PLANNED_ABSENCE
import fi.espoo.evaka.daycare.service.AbsenceType.SICKLEAVE
import fi.espoo.evaka.daycare.service.clearOldCitizenEditableAbsences
import fi.espoo.evaka.placement.ScheduleType
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.domain.getHolidays
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ReservationControllerCitizen(
    private val accessControl: AccessControl,
    private val featureConfig: FeatureConfig
) {
    @GetMapping("/citizen/reservations")
    fun getReservations(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ): ReservationsResponse {
        val requestedRange =
            try {
                FiniteDateRange(from, to)
            } catch (e: IllegalArgumentException) {
                throw BadRequest("Invalid date range $from - $to")
            }

        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_RESERVATIONS,
                        user.id
                    )
                    val holidays = tx.getHolidays(requestedRange)
                    val preschoolTerms = tx.getPreschoolTerms()
                    val clubTerms = tx.getClubTerms()
                    val children = tx.getReservationChildren(user.id, clock.today())
                    val childIds = children.map { it.id }.toSet()
                    val placements = tx.getReservationPlacements(childIds, requestedRange)
                    val backupPlacements =
                        tx.getReservationBackupPlacements(childIds, requestedRange)

                    val reservationData =
                        tx.getReservationsCitizen(clock.today(), user.id, requestedRange)
                    val absences: Map<Pair<ChildId, LocalDate>, AbsenceInfo> =
                        reservationData
                            .flatMap { d ->
                                d.children.mapNotNull { c ->
                                    if (c.absence == null) null
                                    else
                                        Pair(
                                            Pair(c.childId, d.date),
                                            AbsenceInfo(c.absence, c.absenceEditable)
                                        )
                                }
                            }
                            .toMap()
                    val reservations: Map<Pair<ChildId, LocalDate>, List<Reservation>> =
                        reservationData
                            .flatMap { d ->
                                d.children.map { c ->
                                    Pair(Pair(c.childId, d.date), c.reservations)
                                }
                            }
                            .toMap()
                    val attendances: Map<Pair<ChildId, LocalDate>, List<OpenTimeRange>> =
                        reservationData
                            .flatMap { d ->
                                d.children.map { c -> Pair(Pair(c.childId, d.date), c.attendances) }
                            }
                            .toMap()

                    val reservableRange =
                        getReservableRange(
                            clock.now(),
                            featureConfig.citizenReservationThresholdHours,
                        )
                    val days =
                        requestedRange
                            .dates()
                            .map { date ->
                                val isHoliday = holidays.contains(date)
                                ReservationResponseDay(
                                    date = date,
                                    holiday = isHoliday,
                                    children =
                                        // Includes all children that are eligible for daycare
                                        // on this date: have a placement and is an operation day in
                                        // their unit, or child has intermittent shift care.
                                        //
                                        // Preschool/club only children are eligible when there's
                                        // activity on the date. This excludes e.g. Christmas or
                                        // winter holidays.
                                        children
                                            .mapNotNull { child ->
                                                placements[child.id]
                                                    ?.find { it.range.includes(date) }
                                                    ?.let { placement ->
                                                        placementDay(
                                                            date,
                                                            isHoliday,
                                                            placement,
                                                            backupPlacements[child.id],
                                                            clubTerms,
                                                            preschoolTerms
                                                        )
                                                    }
                                                    ?.let { placementDay ->
                                                        val key = Pair(child.id, date)
                                                        ReservationResponseDayChild(
                                                            childId = child.id,
                                                            scheduleType =
                                                                placementDay.scheduleType,
                                                            shiftCare = placementDay.shiftCare,
                                                            absence = absences[key],
                                                            reservations = reservations[key]
                                                                    ?: listOf(),
                                                            attendances = attendances[key]
                                                                    ?: listOf(),
                                                            reservableTimeRange =
                                                                placementDay.reservableTimeRange
                                                        )
                                                    }
                                            }
                                            .sortedBy { it.childId }
                                )
                            }
                            .toList()

                    ReservationsResponse(
                        children = children.filter { placements.containsKey(it.id) },
                        days = days,
                        reservableRange = reservableRange
                    )
                }
            }
            .also {
                Audit.AttendanceReservationCitizenRead.log(
                    targetId = user.id,
                    meta = mapOf("from" to from, "to" to to)
                )
            }
    }

    @PostMapping("/citizen/reservations")
    fun postReservations(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: List<DailyReservationRequest>
    ) {
        val children = body.map { it.childId }.toSet()

        val result =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.CREATE_RESERVATION,
                        children
                    )

                    val reservableRange =
                        getReservableRange(
                            clock.now(),
                            featureConfig.citizenReservationThresholdHours,
                        )
                    if (!body.all { request -> reservableRange.includes(request.date) }) {
                        throw BadRequest("Some days are not reservable", "NON_RESERVABLE_DAYS")
                    }
                    createReservationsAndAbsences(tx, clock.today(), user, body)
                }
            }
        Audit.AttendanceReservationCitizenCreate.log(
            targetId = children,
            meta =
                mapOf(
                    "deletedAbsences" to result.deletedAbsences,
                    "deletedReservations" to result.deletedReservations,
                    "upsertedAbsences" to result.upsertedAbsences,
                    "upsertedReservations" to result.upsertedReservations
                )
        )
    }

    @PostMapping("/citizen/absences")
    fun postAbsences(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: AbsenceRequest
    ) {
        if (body.dateRange.start.isBefore(clock.today())) {
            throw BadRequest("Cannot mark absences for past days")
        }

        if (!listOf(OTHER_ABSENCE, PLANNED_ABSENCE, SICKLEAVE).contains(body.absenceType)) {
            throw BadRequest("Invalid absence type")
        }

        val (deleted, inserted) =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.CREATE_ABSENCE,
                        body.childIds
                    )
                    if (tx.childrenHaveAttendanceInRange(body.childIds, body.dateRange)) {
                        throw BadRequest(
                            "Attendance already exists for given dates",
                            "ATTENDANCE_ALREADY_EXISTS"
                        )
                    }

                    val reservableRange =
                        getReservableRange(
                            clock.now(),
                            featureConfig.citizenReservationThresholdHours
                        )
                    val childContractDays =
                        body.dateRange.intersection(reservableRange)?.let { range ->
                            tx.getReservationContractDayRanges(body.childIds, range)
                        }
                            ?: emptyMap()

                    val deleted =
                        tx.clearOldCitizenEditableAbsences(
                            body.childIds.flatMap { childId ->
                                body.dateRange.dates().map { childId to it }
                            }
                        )
                    val inserted =
                        tx.insertAbsences(
                            user.evakaUserId,
                            body.childIds.flatMap { childId ->
                                body.dateRange.dates().map { date ->
                                    AbsenceInsert(
                                        childId,
                                        date,
                                        if (
                                            reservableRange.includes(date) &&
                                                body.absenceType == OTHER_ABSENCE
                                        ) {
                                            val hasContractDays =
                                                childContractDays[childId]?.includes(date) ?: false
                                            if (hasContractDays) {
                                                PLANNED_ABSENCE
                                            } else {
                                                body.absenceType
                                            }
                                        } else {
                                            body.absenceType
                                        }
                                    )
                                }
                            }
                        )
                    Pair(deleted, inserted)
                }
            }
        Audit.AbsenceCitizenCreate.log(
            targetId = body.childIds,
            objectId = inserted,
            meta = mapOf("deleted" to deleted)
        )
    }
}

data class PlacementDay(
    val scheduleType: ScheduleType,
    val shiftCare: Boolean,
    val reservableTimeRange: ReservableTimeRange
)

private fun placementDay(
    date: LocalDate,
    isHoliday: Boolean,
    placement: ReservationPlacement,
    backupPlacements: List<ReservationBackupPlacement>?,
    clubTerms: List<ClubTerm>,
    preschoolTerms: List<PreschoolTerm>
): PlacementDay? {
    val serviceNeed = placement.serviceNeeds.find { it.range.includes(date) }
    val backupPlacementForDate = backupPlacements?.find { it.range.includes(date) }

    // Backup placements take precedence
    val operationTimes = backupPlacementForDate?.operationTimes ?: placement.operationTimes

    val shiftCareType = serviceNeed?.shiftCareType ?: ShiftCareType.NONE
    val alwaysOpen = operationTimes.all { it != null }
    val shiftCare = alwaysOpen || shiftCareType == ShiftCareType.INTERMITTENT

    // null means that the unit is not open today
    val operationTime =
        if (isHoliday && !alwaysOpen) {
            null
        } else {
            operationTimes[date.dayOfWeek.value - 1]
        }

    return if (operationTime != null || shiftCare) {
        PlacementDay(
            scheduleType = placement.type.scheduleType(date, clubTerms, preschoolTerms),
            shiftCare = shiftCare,
            reservableTimeRange =
                if (shiftCareType == ShiftCareType.INTERMITTENT) {
                    ReservableTimeRange.IntermittentShiftCare(operationTime)
                } else {
                    ReservableTimeRange.Normal(operationTime!!)
                }
        )
    } else {
        null
    }
}

data class ReservationsResponse(
    val children: List<ReservationChild>,
    val days: List<ReservationResponseDay>,
    val reservableRange: FiniteDateRange
)

data class ReservationResponseDay(
    val date: LocalDate,
    val holiday: Boolean,
    val children: List<ReservationResponseDayChild>
)

data class ReservationResponseDayChild(
    val childId: ChildId,
    val scheduleType: ScheduleType,
    val shiftCare: Boolean, // Whether child in 7-day-a-week or intermittent shift care
    val absence: AbsenceInfo?,
    val reservations: List<Reservation>,
    val attendances: List<OpenTimeRange>,
    val reservableTimeRange: ReservableTimeRange
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class ReservableTimeRange {
    // Child is in normal daycare: The child can make reservations on days according to the
    // placement unit's operation times
    @JsonTypeName("NORMAL") data class Normal(val range: TimeRange) : ReservableTimeRange()

    // Child is in intermittent shift care: The child can make any reservations on any days, and we
    // return the placement unit's operational times just for showing it as information in the UI.
    // `null` means that the placement unit is closed on this day.
    @JsonTypeName("INTERMITTENT_SHIFT_CARE")
    data class IntermittentShiftCare(val placementUnitOperationTime: TimeRange?) :
        ReservableTimeRange()
}

data class AbsenceInfo(val type: AbsenceType, val editable: Boolean)

data class AbsenceRequest(
    val childIds: Set<ChildId>,
    val dateRange: FiniteDateRange,
    val absenceType: AbsenceType
)
