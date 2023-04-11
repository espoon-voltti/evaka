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
import fi.espoo.evaka.shared.domain.DateRange
import java.time.LocalDate
import java.time.LocalTime

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
    return requests.flatMap { request ->
        (request.reservations ?: listOf()).mapNotNull { res ->
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
