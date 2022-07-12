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
import fi.espoo.evaka.shared.db.bindNullable
import java.time.LocalDate

data class AbsenceInsert(
    val childId: ChildId,
    val date: LocalDate,
    val absenceType: AbsenceType,
    val questionnaireId: HolidayQuestionnaireId? = null
)

fun Database.Transaction.insertAbsences(userId: PersonId, absenceInserts: List<AbsenceInsert>) {
    val batch = prepareBatch(
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
        """.trimIndent()
    )

    absenceInserts.forEach { (childId, date, absenceType, questionnaireId) ->
        batch
            .bind("childId", childId)
            .bind("date", date)
            .bind("absenceType", absenceType)
            .bind("userId", userId)
            .bindNullable("questionnaireId", questionnaireId)
            .add()
    }

    batch.execute()
}

fun Database.Transaction.deleteAbsencesCreatedFromQuestionnaire(questionnaireId: HolidayQuestionnaireId, childIds: Set<ChildId>) {
    this.createUpdate(
        "DELETE FROM absence WHERE child_id = ANY(:childIds) AND questionnaire_id = :questionnaireId"
    )
        .bind("childIds", childIds.toTypedArray())
        .bind("questionnaireId", questionnaireId)
        .execute()
}

/**
 * A citizen is allowed to edit:
 * - absences added by a citizen
 * - other than FREE_ABSENCE
 */
fun Database.Transaction.clearOldCitizenEditableAbsences(childDatePairs: List<Pair<ChildId, LocalDate>>) {
    val batch = prepareBatch(
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
    val batch = prepareBatch("DELETE FROM attendance_reservation WHERE child_id = :childId AND date = :date")

    reservations.forEach { (childId, date) ->
        batch
            .bind("childId", childId)
            .bind("date", date)
            .add()
    }

    batch.execute()
}

fun Database.Transaction.insertValidReservations(userId: EvakaUserId, requests: List<DailyReservationRequest>) {
    val batch = prepareBatch(
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
