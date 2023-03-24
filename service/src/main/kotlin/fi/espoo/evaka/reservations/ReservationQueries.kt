// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.AttendanceReservationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.HolidayQuestionnaireId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.lang.Exception
import java.time.LocalDate
import java.time.LocalTime

data class AbsenceInsert(
    val childId: ChildId,
    val date: LocalDate,
    val absenceType: AbsenceType,
    val questionnaireId: HolidayQuestionnaireId? = null
)

// language=sql
val notDayOffCheck =
    """
    (
        SELECT (
            CASE date_part('isodow', :date)
                WHEN 1 THEN monday_times IS NULL
                WHEN 2 THEN tuesday_times IS NULL
                WHEN 3 THEN wednesday_times IS NULL
                WHEN 4 THEN thursday_times IS NULL
                WHEN 5 THEN friday_times IS NULL
                WHEN 6 THEN saturday_times IS NULL
                WHEN 7 THEN sunday_times IS NULL
            END
        )
        FROM daily_service_time dst
        WHERE dst.child_id = :childId AND dst.validity_period @> :date AND dst.type = 'IRREGULAR'
        LIMIT 1
    ) IS NOT TRUE
"""
        .trimIndent()

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
        WHERE $notDayOffCheck
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

/**
 * A citizen is allowed to edit:
 * - absences added by a citizen
 * - other than FREE_ABSENCE
 */
fun Database.Transaction.clearOldCitizenEditableAbsences(
    childDatePairs: List<Pair<ChildId, LocalDate>>
): List<AbsenceId> {
    val batch =
        prepareBatch(
            """
DELETE FROM absence 
WHERE child_id = :childId
AND date = :date
AND absence_type <> 'FREE_ABSENCE'::absence_type
AND (SELECT evaka_user.type = 'CITIZEN' FROM evaka_user WHERE evaka_user.id = absence.modified_by)
RETURNING id
"""
        )

    childDatePairs.forEach { (childId, date) ->
        batch.bind("childId", childId).bind("date", date).add()
    }

    return batch.executeAndReturn().mapTo<AbsenceId>().toList()
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

fun Database.Transaction.clearReservationsForRange(childId: ChildId, range: DateRange): Int {
    return this.createUpdate(
            "DELETE FROM attendance_reservation WHERE child_id = :childId AND between_start_and_end(:range, date)"
        )
        .bind("childId", childId)
        .bind("range", range)
        .execute()
}

fun Database.Transaction.insertValidReservations(
    userId: EvakaUserId,
    requests: List<DailyReservationRequest>
): List<AttendanceReservationId> {
    val batch =
        prepareBatch(
            """
        INSERT INTO attendance_reservation (child_id, date, start_time, end_time, created_by)
        SELECT :childId, :date, :start, :end, :userId
        FROM realized_placement_all(:date) rp
        JOIN daycare d ON d.id = rp.unit_id AND 'RESERVATIONS' = ANY(d.enabled_pilot_features)
        WHERE 
            rp.child_id = :childId AND
            extract(isodow FROM :date) = ANY(d.operation_days) AND
            (d.round_the_clock OR NOT EXISTS(SELECT 1 FROM holiday h WHERE h.date = :date)) AND
            NOT EXISTS(SELECT 1 FROM absence ab WHERE ab.child_id = :childId AND ab.date = :date) AND
            $notDayOffCheck
        ON CONFLICT DO NOTHING
        RETURNING id
        """
                .trimIndent()
        )

    requests.forEach { request ->
        request.reservations?.forEach { res ->
            if (res !is Reservation.Times) {
                throw IllegalArgumentException("Only reservations with times are supported")
            }
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

    return batch.executeAndReturn().mapTo<AttendanceReservationId>().toList()
}

fun Database.Read.getReservationSpans(
    unitId: DaycareId,
    date: LocalDate
): Map<ChildId, List<ReservationSpan>> =
    createQuery(
            """
    WITH placed_children AS (
        SELECT child_id FROM placement WHERE unit_id = :unitId AND :date BETWEEN start_date AND end_date
        UNION
        SELECT child_id FROM backup_care WHERE unit_id = :unitId AND :date BETWEEN start_date AND end_date
    )
    SELECT
        child.id AS child_id,
        coalesce(real_start.date, res.date) AS start_date,
        coalesce(real_start.start_time, res.start_time) AS start_time,
        coalesce(real_end.date, res.date) AS end_date,
        coalesce(real_end.end_time, res.end_time) AS end_time
    FROM placed_children pc
    JOIN attendance_reservation res ON res.child_id = pc.child_id AND res.date = :date
    JOIN person child ON res.child_id = child.id
    LEFT JOIN attendance_reservation real_start ON res.start_time = '00:00'::time AND res.child_id = real_start.child_id AND real_start.end_time = '23:59'::time AND res.date = real_start.date + 1
    LEFT JOIN attendance_reservation real_end ON res.end_time = '23:59'::time AND res.child_id = real_end.child_id AND real_end.start_time = '00:00'::time AND res.date = real_end.date - 1
    """
                .trimIndent()
        )
        .bind("unitId", unitId)
        .bind("date", date)
        .map { ctx ->
            val childId = ctx.mapColumn<ChildId>("child_id")
            val startDate = ctx.mapColumn<LocalDate>("start_date")
            val startTime = ctx.mapColumn<LocalTime?>("start_time")
            val endDate = ctx.mapColumn<LocalDate>("end_date")
            val endTime = ctx.mapColumn<LocalTime?>("end_time")

            childId to
                if (startTime != null && endTime != null) {
                    ReservationSpan.Times(
                        HelsinkiDateTime.of(startDate, startTime),
                        HelsinkiDateTime.of(endDate, endTime)
                    )
                } else {
                    if (startDate != endDate) {
                        throw Exception(
                            "BUG: Found adjacent reservations for a reservation with no times"
                        )
                    }
                    ReservationSpan.NoTimes(startDate)
                }
        }
        .groupBy { (childId, _) -> childId }
        .mapValues { it.value.map { (_, reservation) -> reservation } }

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
