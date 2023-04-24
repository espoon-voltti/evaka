// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.AbsenceType.OTHER_ABSENCE
import fi.espoo.evaka.daycare.service.AbsenceType.PLANNED_ABSENCE
import fi.espoo.evaka.daycare.service.AbsenceType.SICKLEAVE
import fi.espoo.evaka.daycare.service.clearOldCitizenEditableAbsences
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
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
                    val placements =
                        tx.getReservationPlacements(user.id, clock.today(), requestedRange)
                    val children = tx.getReservationChildren(placements.keys, clock.today())
                    val includesWeekends =
                        placements.any { (_, pls) ->
                            (pls.regularPlacements + pls.backupPlacements).any {
                                it.operationDays.contains(6) || it.operationDays.contains(7)
                            }
                        }

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
                            .mapNotNull { date ->
                                if (!includesWeekends && date.dayOfWeek.value > 5)
                                    return@mapNotNull null
                                val isHoliday = holidays.contains(date)
                                ReservationResponseDay(
                                    date = date,
                                    holiday = isHoliday,
                                    children =
                                        // Includes all children that are eligible for daycare
                                        // on this date: have a placement, is an operation day in
                                        // their unit
                                        children
                                            .mapNotNull { child ->
                                                placementDay(date, isHoliday, placements[child.id])
                                                    ?.let { placementDay ->
                                                        val key = Pair(child.id, date)
                                                        ReservationResponseDayChild(
                                                            childId = child.id,
                                                            shiftCare = placementDay.shiftCare,
                                                            contractDays =
                                                                placementDay.contractDays,
                                                            absence = absences[key],
                                                            reservations = reservations[key]
                                                                    ?: listOf(),
                                                            attendances = attendances[key]
                                                                    ?: listOf(),
                                                        )
                                                    }
                                            }
                                            .sortedBy { it.childId }
                                )
                            }
                            .toList()

                    ReservationsResponse(
                        children = children,
                        days = days,
                        reservableRange = reservableRange,
                        includesWeekends = includesWeekends,
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
                                    AbsenceInsert(childId, date, body.absenceType)
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

data class PlacementDay(val shiftCare: Boolean, val contractDays: Boolean)

private fun placementDay(
    date: LocalDate,
    isHoliday: Boolean,
    placements: ReservationPlacements?
): PlacementDay? {
    if (placements == null) return null

    // Backup placements take precedence
    val placement =
        placements.backupPlacements.find { it.startDate <= date && date <= it.endDate }
            ?: placements.regularPlacements.find { it.startDate <= date && date <= it.endDate }

    return if (placement == null) {
        null
    } else if (placement.operationDays == setOf(1, 2, 3, 4, 5, 6, 7)) {
        PlacementDay(shiftCare = true, contractDays = placement.hasContractDays)
    } else if (placement.operationDays.contains(date.dayOfWeek.value) && !isHoliday) {
        PlacementDay(shiftCare = false, contractDays = placement.hasContractDays)
    } else {
        null
    }
}

data class ReservationsResponse(
    val children: List<ReservationChild>,
    val days: List<ReservationResponseDay>,
    val reservableRange: FiniteDateRange,
    val includesWeekends: Boolean
)

data class ReservationResponseDay(
    val date: LocalDate,
    val holiday: Boolean,
    val children: List<ReservationResponseDayChild>
)

data class ReservationResponseDayChild(
    val childId: ChildId,
    val shiftCare: Boolean, // Can make more one reservation for this day
    val contractDays: Boolean,
    val absence: AbsenceInfo?,
    val reservations: List<Reservation>,
    val attendances: List<OpenTimeRange>,
)

data class AbsenceInfo(val type: AbsenceType, val editable: Boolean)

data class AbsenceRequest(
    val childIds: Set<ChildId>,
    val dateRange: FiniteDateRange,
    val absenceType: AbsenceType
)
