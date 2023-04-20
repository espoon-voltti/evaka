// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.AttendanceReservationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.HolidayQuestionnaireId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.time.LocalTime
import org.jdbi.v3.json.Json

data class AbsenceInsert(
    val childId: ChildId,
    val date: LocalDate,
    val absenceType: AbsenceType,
    val questionnaireId: HolidayQuestionnaireId? = null
)

fun Database.Transaction.insertAbsences(
    userId: EvakaUserId,
    absenceInserts: List<AbsenceInsert>
): List<AbsenceId> {
    val batch =
        prepareBatch(
            """
        INSERT INTO absence (child_id, date, category, absence_type, modified_by, questionnaire_id)
        SELECT
            :childId,
            :date,
            category,
            :absenceType,
            :userId,
            :questionnaireId
        FROM (
            SELECT unnest(absence_categories(type)) AS category
            FROM placement
            WHERE child_id = :childId AND :date BETWEEN start_date AND end_date
        ) care_type
        ON CONFLICT DO NOTHING
        RETURNING id
        """
                .trimIndent()
        )

    absenceInserts.forEach { (childId, date, absenceType, questionnaireId) ->
        batch
            .bind("childId", childId)
            .bind("date", date)
            .bind("absenceType", absenceType)
            .bind("userId", userId)
            .bind("questionnaireId", questionnaireId)
            .add()
    }

    return batch.executeAndReturn().mapTo<AbsenceId>().toList()
}

fun Database.Transaction.deleteAbsencesCreatedFromQuestionnaire(
    questionnaireId: HolidayQuestionnaireId,
    childIds: Set<ChildId>
) {
    this.createUpdate(
            "DELETE FROM absence WHERE child_id = ANY(:childIds) AND questionnaire_id = :questionnaireId"
        )
        .bind("childIds", childIds)
        .bind("questionnaireId", questionnaireId)
        .execute()
}

fun Database.Transaction.clearOldReservations(
    reservations: List<Pair<ChildId, LocalDate>>
): List<AttendanceReservationId> {
    val batch =
        prepareBatch(
            "DELETE FROM attendance_reservation WHERE child_id = :childId AND date = :date RETURNING id"
        )

    reservations.forEach { (childId, date) ->
        batch.bind("childId", childId).bind("date", date).add()
    }

    return batch.executeAndReturn().mapTo<AttendanceReservationId>().toList()
}

fun Database.Transaction.clearReservationsForRangeExceptInHolidayPeriod(
    childId: ChildId,
    range: DateRange
): Int {
    return this.createUpdate(
            """
            DELETE FROM attendance_reservation
            WHERE child_id = :childId
            AND between_start_and_end(:range, date)
            AND NOT EXISTS (SELECT 1 FROM holiday_period hp WHERE period @> date)
            """
        )
        .bind("childId", childId)
        .bind("range", range)
        .execute()
}

fun Database.Transaction.deleteAllCitizenReservationsInRange(range: FiniteDateRange) {
    this.createUpdate(
            "DELETE FROM attendance_reservation WHERE created_by IN (SELECT id FROM evaka_user WHERE type = 'CITIZEN') AND between_start_and_end(:range, date)"
        )
        .bind("range", range)
        .execute()
}

fun Database.Transaction.insertValidReservations(
    userId: EvakaUserId,
    requests: List<DailyReservationRequest.Reservations>
): List<AttendanceReservationId> {
    return requests.flatMap { request ->
        listOfNotNull(request.reservation, request.secondReservation).mapNotNull { res ->
            createQuery(
                    """
        INSERT INTO attendance_reservation (child_id, date, start_time, end_time, created_by)
        SELECT :childId, :date, :start, :end, :userId
        FROM realized_placement_all(:date) rp
        JOIN daycare d ON d.id = rp.unit_id AND 'RESERVATIONS' = ANY(d.enabled_pilot_features)
        WHERE 
            rp.child_id = :childId AND
            extract(isodow FROM :date) = ANY(d.operation_days) AND
            (d.round_the_clock OR NOT EXISTS(SELECT 1 FROM holiday h WHERE h.date = :date)) AND
            NOT EXISTS(SELECT 1 FROM absence ab WHERE ab.child_id = :childId AND ab.date = :date)
        ON CONFLICT DO NOTHING
        RETURNING id
        """
                )
                .bind("userId", userId)
                .bind("childId", request.childId)
                .bind("date", request.date)
                .let {
                    when (res) {
                        is Reservation.Times ->
                            it.bind("start", res.startTime).bind("end", res.endTime)
                        is Reservation.NoTimes ->
                            it.bind<LocalTime?>("start", null).bind<LocalTime?>("end", null)
                    }
                }
                .mapTo<AttendanceReservationId>()
                .singleOrNull()
        }
    }
}

private data class ReservationRow(
    val childId: ChildId,
    val startTime: LocalTime?,
    val endTime: LocalTime?
)

fun Database.Read.getUnitReservations(
    unitId: DaycareId,
    date: LocalDate
): Map<ChildId, List<Reservation>> =
    createQuery(
            """
    WITH placed_children AS (
        SELECT child_id FROM placement WHERE unit_id = :unitId AND :date BETWEEN start_date AND end_date
        UNION
        SELECT child_id FROM backup_care WHERE unit_id = :unitId AND :date BETWEEN start_date AND end_date
    )
    SELECT
        pc.child_id,
        ar.start_time,
        ar.end_time
    FROM placed_children pc
    JOIN attendance_reservation ar ON ar.child_id = pc.child_id 
    WHERE ar.date = :date
    """
                .trimIndent()
        )
        .bind("unitId", unitId)
        .bind("date", date)
        .mapTo<ReservationRow>()
        .map { it.childId to Reservation.fromLocalTimes(it.startTime, it.endTime) }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, value) -> value.sorted() }

fun Database.Read.getChildAttendanceReservationStartDatesByRange(
    childId: ChildId,
    period: DateRange
): List<LocalDate> {
    return createQuery(
            """
        SELECT date
        FROM attendance_reservation
        WHERE between_start_and_end(:period, date)
        AND child_id = :childId
        AND (start_time IS NULL OR start_time != '00:00'::time)  -- filter out overnight reservations
        """
        )
        .bind("period", period)
        .bind("childId", childId)
        .mapTo<LocalDate>()
        .list()
}

data class DailyReservationData(val date: LocalDate, @Json val children: List<ChildDailyData>)

@Json
data class ChildDailyData(
    val childId: ChildId,
    val markedByEmployee: Boolean,
    val absence: AbsenceType?,
    val reservations: List<Reservation>,
    val attendances: List<OpenTimeRange>
)

fun Database.Read.getReservationsCitizen(
    today: LocalDate,
    userId: PersonId,
    range: FiniteDateRange,
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
GROUP BY date
        """
                .trimIndent()
        )
        .bind("today", today)
        .bind("userId", userId)
        .bind("start", range.start)
        .bind("end", range.end)
        .mapTo<DailyReservationData>()
        .toList()
}

data class ReservationChild(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val preferredName: String,
    val duplicateOf: PersonId?,
    val imageId: ChildImageId?,
    val upcomingPlacementType: PlacementType?,
)

fun Database.Read.getReservationChildren(
    childIds: Set<ChildId>,
    today: LocalDate
): List<ReservationChild> {
    return createQuery(
            """
SELECT
    p.id,
    p.first_name,
    p.last_name,
    p.preferred_name,
    p.duplicate_of,
    ci.id AS image_id,
    (
        SELECT type FROM placement
        WHERE child_id = p.id AND :today <= end_date
        ORDER BY start_date
        LIMIT 1
    ) AS upcoming_placement_type
FROM person p
LEFT JOIN child_images ci ON ci.child_id = p.id
WHERE p.id = ANY (:childIds)
ORDER BY p.date_of_birth, p.duplicate_of
        """
        )
        .bind("childIds", childIds)
        .bind("today", today)
        .mapTo<ReservationChild>()
        .list()
}

data class ReservationPlacement(
    val isBackup: Boolean,
    val childId: ChildId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val operationDays: Set<Int>,
    val hasContractDays: Boolean
)

data class ReservationPlacements(
    val backupPlacements: List<ReservationPlacement>,
    val regularPlacements: List<ReservationPlacement>
)

fun Database.Read.getReservationPlacements(
    guardianId: PersonId,
    today: LocalDate,
    range: FiniteDateRange
): Map<ChildId, ReservationPlacements> {
    return createQuery(
            """
WITH children AS (
    SELECT child_id FROM guardian WHERE guardian_id = :guardianId
    UNION
    SELECT child_id FROM foster_parent WHERE parent_id = :guardianId AND valid_during @> :today
)
SELECT
    FALSE AS is_backup,
    pl.child_id,
    pl.start_date,
    pl.end_date,
    u.operation_days,
    coalesce(sno.contract_days_per_month, sno_default.contract_days_per_month) IS NOT NULL AS has_contract_days
FROM placement pl
JOIN daycare u ON pl.unit_id = u.id
LEFT JOIN service_need sn ON sn.placement_id = pl.id AND daterange(sn.start_date, sn.end_date, '[]') && :range
LEFT JOIN service_need_option sno ON sno.id = sn.option_id
LEFT JOIN service_need_option sno_default ON sno_default.valid_placement_type = pl.type AND sno_default.default_option
WHERE
    pl.child_id = ANY (SELECT child_id FROM children) AND
    daterange(pl.start_date, pl.end_date, '[]') && :range AND
    'RESERVATIONS' = ANY(u.enabled_pilot_features)

UNION ALL

SELECT
    TRUE AS is_backup,
    bc.child_id,
    bc.start_date,
    bc.end_date,
    u.operation_days,
    FALSE AS has_contract_days
FROM backup_care bc
JOIN daycare u ON bc.unit_id = u.id
WHERE
    bc.child_id = ANY (SELECT child_id FROM children) AND
    daterange(bc.start_date, bc.end_date, '[]') && :range AND
    'RESERVATIONS' = ANY(u.enabled_pilot_features)
"""
        )
        .bind("guardianId", guardianId)
        .bind("today", today)
        .bind("range", range)
        .mapTo<ReservationPlacement>()
        .groupBy { it.childId }
        .mapValues { (_, placements) ->
            val (backupPlacements, regularPlacements) = placements.partition { it.isBackup }
            ReservationPlacements(
                backupPlacements = backupPlacements,
                regularPlacements = regularPlacements
            )
        }
}
