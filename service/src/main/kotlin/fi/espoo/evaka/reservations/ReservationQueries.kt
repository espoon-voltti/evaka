// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.HolidayQuestionnaireId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import java.time.LocalDate

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
""".trimIndent(
    )

fun Database.Transaction.insertAbsences(userId: PersonId, absenceInserts: List<AbsenceInsert>) {
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
        """.trimIndent(
            )
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

    batch.execute()
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
) {
    val batch =
        prepareBatch(
            """
DELETE FROM absence 
WHERE child_id = :childId
AND date = :date
AND absence_type <> 'FREE_ABSENCE'::absence_type
AND (SELECT evaka_user.type = 'CITIZEN' FROM evaka_user WHERE evaka_user.id = absence.modified_by)
"""
        )

    childDatePairs.forEach { (childId, date) ->
        batch.bind("childId", childId).bind("date", date).add()
    }

    batch.execute()
}

fun Database.Transaction.clearOldReservations(reservations: List<Pair<ChildId, LocalDate>>) {
    val batch =
        prepareBatch(
            "DELETE FROM attendance_reservation WHERE child_id = :childId AND date = :date"
        )

    reservations.forEach { (childId, date) ->
        batch.bind("childId", childId).bind("date", date).add()
    }

    batch.execute()
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
) {
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
        """.trimIndent(
            )
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
