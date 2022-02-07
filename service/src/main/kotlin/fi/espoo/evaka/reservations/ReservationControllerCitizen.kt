// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.AbsenceType.OTHER_ABSENCE
import fi.espoo.evaka.daycare.service.AbsenceType.PLANNED_ABSENCE
import fi.espoo.evaka.daycare.service.AbsenceType.SICKLEAVE
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.utils.dateNow
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.json.Json
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class ReservationControllerCitizen(
    private val accessControl: AccessControl,
    private val featureConfig: FeatureConfig
) {
    @GetMapping("/citizen/reservations")
    fun getReservations(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): ReservationsResponse {
        Audit.AttendanceReservationCitizenRead.log(targetId = user.id)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.CITIZEN_WEAK, UserRole.END_USER)

        val range = try {
            FiniteDateRange(from, to)
        } catch (e: IllegalArgumentException) {
            throw BadRequest("Invalid date range $from - $to")
        }

        return db.connect { dbc ->
            dbc.read { tx ->
                val children = tx.getReservationChildren(PersonId(user.id), range)
                val includeWeekends = children.any {
                    it.maxOperationalDays.contains(6) || it.maxOperationalDays.contains(7)
                }
                val reservations = tx.getReservationsCitizen(PersonId(user.id), range, includeWeekends)
                val reservableDays = getReservableDays(HelsinkiDateTime.now(), featureConfig.citizenReservationThresholdHours).let { reservableDays ->
                    val maxPlacementEnd = children.maxOfOrNull { it.placementMaxEnd } ?: LocalDate.MAX
                    if (reservableDays.start > maxPlacementEnd) null
                    else FiniteDateRange(
                        maxOf(reservableDays.start, children.minOfOrNull { it.placementMinStart } ?: LocalDate.MIN),
                        minOf(reservableDays.end, maxPlacementEnd)
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
    }

    @PostMapping("/citizen/reservations")
    fun postReservations(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: List<DailyReservationRequest>
    ) {
        Audit.AttendanceReservationCitizenCreate.log(targetId = body.map { it.childId }.toSet().joinToString())
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.CITIZEN_WEAK, UserRole.END_USER)
        accessControl.requireGuardian(user, body.map { it.childId }.toSet())

        db.connect { dbc ->
            dbc.transaction { tx ->
                val reservableDays = getReservableDays(HelsinkiDateTime.now(), featureConfig.citizenReservationThresholdHours)
                createReservationsAsCitizen(tx, PersonId(user.id), body.validate(reservableDays))
            }
        }
    }

    @PostMapping("/citizen/absences")
    fun postAbsences(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: AbsenceRequest
    ) {
        Audit.AbsenceCitizenCreate.log(targetId = body.childIds.toSet().joinToString())
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.CITIZEN_WEAK, UserRole.END_USER)
        accessControl.requireGuardian(user, body.childIds)

        if (body.dateRange.start.isBefore(dateNow()))
            throw BadRequest("Cannot mark absences for past days")

        if (!listOf(OTHER_ABSENCE, PLANNED_ABSENCE, SICKLEAVE).contains(body.absenceType))
            throw BadRequest("Invalid absence type")

        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.clearOldAbsences(
                    body.childIds.flatMap { childId ->
                        body.dateRange.dates().map { childId to it }
                    }
                )
                tx.insertAbsences(PersonId(user.id), body)
            }
        }
    }
}

data class ReservationsResponse(
    val dailyData: List<DailyReservationData>,
    val children: List<ReservationChild>,
    val reservableDays: FiniteDateRange?,
    val includesWeekends: Boolean
)

data class DailyReservationData(
    val date: LocalDate,
    val isHoliday: Boolean,
    @Json
    val children: List<ChildDailyData>
)

@Json
data class ChildDailyData(
    val childId: ChildId,
    val absence: AbsenceType?,
    val reservations: List<Reservation>
)

@Json
data class Reservation(
    val startTime: String,
    val endTime: String
)

data class ReservationChild(
    val id: ChildId,
    val firstName: String,
    val preferredName: String?,
    val placementMinStart: LocalDate,
    val placementMaxEnd: LocalDate,
    val maxOperationalDays: Set<Int>,
    val inShiftCareUnit: Boolean
)

data class AbsenceRequest(
    val childIds: Set<ChildId>,
    val dateRange: FiniteDateRange,
    val absenceType: AbsenceType
)

fun Database.Read.getReservationsCitizen(
    guardianId: PersonId,
    range: FiniteDateRange,
    includeWeekends: Boolean
): List<DailyReservationData> {
    if (range.durationInDays() > 450) throw BadRequest("Range too long")

    return createQuery(
        """
SELECT
    t::date AS date,
    EXISTS(SELECT 1 FROM holiday h WHERE h.date = t::date) AS is_holiday,
    coalesce(
        jsonb_agg(
            jsonb_build_object(
                'childId', g.child_id,
                'absence', a.absence_type,
                'reservations', coalesce(ar.reservations, '[]')
            )
        ) FILTER (WHERE a.absence_type IS NOT NULL OR ar.reservations IS NOT NULL),
        '[]'
    ) AS children
FROM generate_series(:start, :end, '1 day') t
JOIN guardian g ON g.guardian_id = :guardianId
JOIN placement p ON g.child_id = p.child_id
JOIN daycare d ON p.unit_id = d.id AND 'RESERVATIONS' = ANY(d.enabled_pilot_features)
LEFT JOIN LATERAL (
    SELECT
        jsonb_agg(
            jsonb_build_object(
                'startTime', to_char(ar.start_time, 'HH24:MI'),
                'endTime', to_char(ar.end_time, 'HH24:MI')
            ) ORDER BY ar.start_time ASC
        ) AS reservations
    FROM attendance_reservation ar WHERE ar.child_id = g.child_id AND ar.date = t::date
) ar ON true
LEFT JOIN LATERAL (
    SELECT a.absence_type FROM absence a WHERE a.child_id = g.child_id AND a.date = t::date LIMIT 1
) a ON true
WHERE (:includeWeekends OR date_part('isodow', t) = ANY('{1, 2, 3, 4, 5}'))
GROUP BY date, is_holiday
        """.trimIndent()
    )
        .bind("guardianId", guardianId)
        .bind("start", range.start)
        .bind("end", range.end)
        .bind("includeWeekends", includeWeekends)
        .mapTo<DailyReservationData>()
        .toList()
}

private fun Database.Read.getReservationChildren(guardianId: PersonId, range: FiniteDateRange): List<ReservationChild> {
    return createQuery(
        """
SELECT ch.id, ch.first_name, ch.preferred_name, p.placement_min_start, p.placement_max_end, p.max_operational_days, p.in_shift_care_unit
FROM person ch
JOIN guardian g ON ch.id = g.child_id AND g.guardian_id = :guardianId
LEFT JOIN LATERAL (
    SELECT
        min(p.start_date) AS placement_min_start,
        max(p.end_date) AS placement_max_end,
        array_agg(DISTINCT p.operation_days) AS max_operational_days,
        bool_or(p.round_the_clock) AS in_shift_care_unit
    FROM (
        SELECT pl.start_date, pl.end_date, unnest(u.operation_days) AS operation_days, u.round_the_clock
        FROM placement pl
        JOIN daycare u ON pl.unit_id = u.id
        WHERE pl.child_id = g.child_id AND daterange(pl.start_date, pl.end_date, '[]') && :range AND 'RESERVATIONS' = ANY(u.enabled_pilot_features)

        UNION ALL

        SELECT bc.start_date, bc.end_date, unnest(u.operation_days) AS operation_days, u.round_the_clock AS shift_care
        FROM backup_care bc
        JOIN daycare u ON bc.unit_id = u.id
        WHERE bc.child_id = g.child_id AND daterange(bc.start_date, bc.end_date, '[]') && :range AND 'RESERVATIONS' = ANY(u.enabled_pilot_features)
    ) p
) p ON true
WHERE p.placement_min_start IS NOT NULL
ORDER BY first_name
        """.trimIndent()
    )
        .bind("guardianId", guardianId)
        .bind("range", range)
        .mapTo<ReservationChild>()
        .list()
}

fun getNextMonday(now: LocalDate): LocalDate = now.plusDays(7 - now.dayOfWeek.value + 1L)

fun getNextReservableMonday(now: HelsinkiDateTime, thresholdHours: Long, nextMonday: LocalDate): LocalDate =
    if (nextMonday.isAfter(now.plusHours(thresholdHours).toLocalDate())) {
        nextMonday
    } else {
        getNextReservableMonday(now, thresholdHours, nextMonday.plusWeeks(1))
    }

fun getReservableDays(now: HelsinkiDateTime, thresholdHours: Long): FiniteDateRange {
    val nextReservableMonday = getNextReservableMonday(now, thresholdHours, getNextMonday(now.toLocalDate()))

    val firstOfJuly = nextReservableMonday.withMonth(7).withDayOfMonth(1)
    val lastReservableDay = if (nextReservableMonday.isBefore(firstOfJuly)) {
        firstOfJuly.withDayOfMonth(31)
    } else {
        firstOfJuly.withDayOfMonth(31).plusYears(1)
    }

    return FiniteDateRange(nextReservableMonday, lastReservableDay)
}

fun createReservationsAsCitizen(tx: Database.Transaction, userId: PersonId, reservations: List<DailyReservationRequest>) {
    tx.clearOldAbsences(reservations.filter { it.reservations != null }.map { it.childId to it.date })
    tx.clearOldReservations(reservations.map { it.childId to it.date })
    tx.insertValidReservations(userId, reservations)
}

private fun Database.Transaction.insertValidReservations(userId: PersonId, requests: List<DailyReservationRequest>) {
    val batch = prepareBatch(
        """
        INSERT INTO attendance_reservation (child_id, date, start_time, end_time, created_by)
        SELECT :childId, :date, :start, :end, :userId
        FROM placement pl
        LEFT JOIN backup_care bc ON daterange(bc.start_date, bc.end_date, '[]') @> :date AND bc.child_id = :childId
        JOIN daycare d ON d.id = coalesce(bc.unit_id, pl.unit_id) AND 'RESERVATIONS' = ANY(d.enabled_pilot_features)
        JOIN guardian g ON g.child_id = pl.child_id AND g.guardian_id = :userId
        WHERE 
            pl.child_id = :childId AND 
            daterange(pl.start_date, pl.end_date, '[]') @> :date AND 
            extract(isodow FROM :date) = ANY(d.operation_days) AND
            (d.round_the_clock OR NOT EXISTS(SELECT 1 FROM holiday h WHERE h.date = :date)) AND
            NOT EXISTS(SELECT 1 FROM absence ab WHERE ab.child_id = :childId AND ab.date = :date)
        ON CONFLICT DO NOTHING;
        """.trimIndent()
    )

    requests.forEach { request ->
        request.reservations?.forEach { res ->
            batch
                .bind("userId", userId)
                .bind("childId", request.childId)
                .bind("date", request.date)
                .bind("start", res.startTime)
                .bind("end", res.endTime)
                .bind("date", request.date)
                .add()
        }
    }

    batch.execute()
}

private fun Database.Transaction.insertAbsences(userId: PersonId, request: AbsenceRequest) {
    val batch = prepareBatch(
        """
        INSERT INTO absence (child_id, date, category, absence_type, modified_by)
        SELECT
            :childId,
            :date,
            category,
            :absenceType,
            :userId
        FROM (
            SELECT unnest((CASE type
                WHEN 'CLUB'::placement_type THEN '{NONBILLABLE}'
                WHEN 'SCHOOL_SHIFT_CARE'::placement_type THEN '{NONBILLABLE}'
                WHEN 'PRESCHOOL'::placement_type THEN '{NONBILLABLE}'
                WHEN 'PREPARATORY'::placement_type THEN '{NONBILLABLE}'
                WHEN 'PRESCHOOL_DAYCARE'::placement_type THEN '{NONBILLABLE, BILLABLE}'
                WHEN 'PREPARATORY_DAYCARE'::placement_type THEN '{NONBILLABLE, BILLABLE}'
                WHEN 'DAYCARE'::placement_type THEN '{BILLABLE}'
                WHEN 'DAYCARE_PART_TIME'::placement_type THEN '{BILLABLE}'
                WHEN 'DAYCARE_FIVE_YEAR_OLDS'::placement_type THEN '{BILLABLE, NONBILLABLE}'
                WHEN 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS'::placement_type THEN '{BILLABLE, NONBILLABLE}'
                WHEN 'TEMPORARY_DAYCARE'::placement_type THEN '{}'
                WHEN 'TEMPORARY_DAYCARE_PART_DAY'::placement_type THEN '{}'
            END)::absence_category[]) AS category
            FROM placement
            WHERE child_id = :childId AND :date BETWEEN start_date AND end_date
        ) care_type
        ON CONFLICT DO NOTHING
        """.trimIndent()
    )

    request.childIds.forEach { childId ->
        request.dateRange.dates().forEach { date ->
            batch
                .bind("childId", childId)
                .bind("date", date)
                .bind("absenceType", request.absenceType)
                .bind("userId", userId)
                .add()
        }
    }

    batch.execute()
}
