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
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.jdbi.v3.json.Json
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
        val range =
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
                    val children = tx.getReservationChildren(clock.today(), user.id, range)
                    val includeWeekends =
                        children.any {
                            it.maxOperationalDays.contains(6) || it.maxOperationalDays.contains(7)
                        }
                    val reservations =
                        tx.getReservationsCitizen(clock.today(), user.id, range, includeWeekends)
                    val reservableRange =
                        getReservableRange(
                            clock.now(),
                            featureConfig.citizenReservationThresholdHours,
                        )
                    val reservableDays =
                        children.associate { child ->
                            Pair(
                                child.id,
                                child.placements.mapNotNull { placement ->
                                    reservableRange.intersection(placement)
                                }
                            )
                        }
                    ReservationsResponse(
                        dailyData = reservations,
                        children = children,
                        reservableDays = reservableDays,
                        includesWeekends = includeWeekends
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

data class ReservationsResponse(
    val dailyData: List<DailyReservationData>,
    val children: List<ReservationChild>,
    val reservableDays: Map<ChildId, List<FiniteDateRange>>,
    val includesWeekends: Boolean
)

data class DailyReservationData(
    val date: LocalDate,
    val isHoliday: Boolean,
    @Json val children: List<ChildDailyData>
)

@Json
data class ChildDailyData(
    val childId: ChildId,
    val markedByEmployee: Boolean,
    val absence: AbsenceType?,
    val reservations: List<Reservation>,
    val attendances: List<OpenTimeRange>
)

data class ReservationChild(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val preferredName: String,
    val duplicateOf: PersonId?,
    val imageId: ChildImageId?,
    val placements: List<FiniteDateRange>,
    val upcomingPlacementType: PlacementType?,
    val maxOperationalDays: Set<Int>,
    val inShiftCareUnit: Boolean,
    val hasContractDays: Boolean
)

data class AbsenceRequest(
    val childIds: Set<ChildId>,
    val dateRange: FiniteDateRange,
    val absenceType: AbsenceType
)

fun Database.Read.getReservationsCitizen(
    today: LocalDate,
    userId: PersonId,
    range: FiniteDateRange,
    includeWeekends: Boolean
): List<DailyReservationData> {
    if (range.durationInDays() > 450) throw BadRequest("Range too long")

    return createQuery(
            """
WITH children AS (
    SELECT child_id FROM guardian WHERE guardian_id = :userId
    UNION
    SELECT child_id FROM foster_parent WHERE parent_id = :userId AND valid_during @> :today
)
SELECT
    t::date AS date,
    EXISTS(SELECT 1 FROM holiday h WHERE h.date = t::date) AS is_holiday,
    coalesce(
        jsonb_agg(
            jsonb_build_object(
                'childId', c.child_id,
                'markedByEmployee', a.modified_by_type <> 'CITIZEN',
                'absence', a.absence_type,
                'reservations', coalesce(ar.reservations, '[]'),
                'attendances', coalesce(ca.attendances, '[]')
            )
        ) FILTER (
            WHERE (a.absence_type IS NOT NULL OR ar.reservations IS NOT NULL OR ca.attendances IS NOT NULL)
              AND EXISTS(
                SELECT 1 FROM placement p
                JOIN daycare d ON p.unit_id = d.id AND 'RESERVATIONS' = ANY(d.enabled_pilot_features)
                WHERE c.child_id = p.child_id AND p.start_date <= t::date AND p.end_date >= t::date
              )
        ),
        '[]'
    ) AS children
FROM generate_series(:start, :end, '1 day') t, children c
LEFT JOIN LATERAL (
    SELECT
        jsonb_agg(
            CASE WHEN ar.start_time IS NULL OR ar.end_time IS NULL THEN
                jsonb_build_object('type', 'NO_TIMES')
            ELSE
                jsonb_build_object('type', 'TIMES', 'startTime', ar.start_time, 'endTime', ar.end_time)
            END
            ORDER BY ar.start_time
        ) AS reservations
    FROM attendance_reservation ar WHERE ar.child_id = c.child_id AND ar.date = t::date
) ar ON true
LEFT JOIN LATERAL (
    SELECT
        jsonb_agg(
            jsonb_build_object(
                'startTime', to_char(ca.start_time, 'HH24:MI'),
                'endTime', to_char(ca.end_time, 'HH24:MI')
            ) ORDER BY ca.start_time ASC
        ) AS attendances
    FROM child_attendance ca WHERE ca.child_id = c.child_id AND ca.date = t::date
) ca ON true
LEFT JOIN LATERAL (
    SELECT a.absence_type, eu.type AS modified_by_type
    FROM absence a JOIN evaka_user eu ON eu.id = a.modified_by
    WHERE a.child_id = c.child_id AND a.date = t::date
    LIMIT 1
) a ON true
WHERE (:includeWeekends OR date_part('isodow', t) = ANY('{1, 2, 3, 4, 5}'))
GROUP BY date, is_holiday
        """
                .trimIndent()
        )
        .bind("today", today)
        .bind("userId", userId)
        .bind("start", range.start)
        .bind("end", range.end)
        .bind("includeWeekends", includeWeekends)
        .mapTo<DailyReservationData>()
        .toList()
}

private fun Database.Read.getReservationChildren(
    today: LocalDate,
    userId: PersonId,
    range: FiniteDateRange
): List<ReservationChild> {
    return createQuery(
            """
WITH children AS (
    SELECT child_id FROM guardian WHERE guardian_id = :userId
    UNION
    SELECT child_id FROM foster_parent WHERE parent_id = :userId AND valid_during @> :today
)
SELECT
    ch.id,
    ch.first_name,
    ch.last_name,
    ch.preferred_name,
    ch.duplicate_of,
    ci.id AS image_id,
    p.placements,
    upcoming_pl.type AS upcoming_placement_type,
    p.max_operational_days,
    p.in_shift_care_unit,
    p.has_contract_days
FROM person ch
JOIN children c ON ch.id = c.child_id
LEFT JOIN child_images ci ON ci.child_id = ch.id
LEFT JOIN (
    SELECT
        p.child_id,
        array_agg(DISTINCT daterange(p.start_date, p.end_date, '[]')) as placements,
        array_agg(DISTINCT p.operation_days) AS max_operational_days,
        bool_or(p.round_the_clock) AS in_shift_care_unit,
        bool_or(p.contract_days_per_month IS NOT NULL) AS has_contract_days
    FROM (
             SELECT pl.start_date, pl.end_date, unnest(u.operation_days) AS operation_days, u.round_the_clock, pl.child_id, coalesce(sno.contract_days_per_month, sno_default.contract_days_per_month) AS contract_days_per_month
             FROM placement pl
             JOIN daycare u ON pl.unit_id = u.id
             LEFT JOIN service_need sn ON sn.placement_id = pl.id AND daterange(sn.start_date, sn.end_date, '[]') && :range
             LEFT JOIN service_need_option sno ON sno.id = sn.option_id
             LEFT JOIN service_need_option sno_default ON sno_default.valid_placement_type = pl.type AND sno_default.default_option
             WHERE daterange(pl.start_date, pl.end_date, '[]') && :range AND 'RESERVATIONS' = ANY(u.enabled_pilot_features)

             UNION ALL

             SELECT bc.start_date, bc.end_date, unnest(u.operation_days) AS operation_days, u.round_the_clock, bc.child_id, NULL AS contract_days_per_month
             FROM backup_care bc
             JOIN daycare u ON bc.unit_id = u.id
             WHERE daterange(bc.start_date, bc.end_date, '[]') && :range AND 'RESERVATIONS' = ANY(u.enabled_pilot_features)
    ) p
    GROUP BY p.child_id
) p ON p.child_id = c.child_id
LEFT JOIN LATERAL (
  SELECT child_id, type
  FROM placement
  WHERE child_id = ch.id AND :today <= end_date
  ORDER BY start_date
  LIMIT 1
) upcoming_pl ON true
WHERE p.placements IS NOT NULL
ORDER BY ch.date_of_birth, ch.duplicate_of
        """
                .trimIndent()
        )
        .bind("today", today)
        .bind("userId", userId)
        .bind("range", range)
        .mapTo<ReservationChild>()
        .list()
}
