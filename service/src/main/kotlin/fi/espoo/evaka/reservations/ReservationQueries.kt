// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.ExcludeCodeGen
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.util.UUID

@ExcludeCodeGen
data class AbsenceInsert(
    val childId: ChildId,
    val dateRange: FiniteDateRange,
    val absenceType: AbsenceType
)

fun Database.Transaction.insertAbsences(userId: PersonId, absenceInserts: List<AbsenceInsert>) {
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
            SELECT unnest(absence_categories(type)) AS category
            FROM placement
            WHERE child_id = :childId AND :date BETWEEN start_date AND end_date
        ) care_type
        ON CONFLICT DO NOTHING
        """.trimIndent()
    )

    absenceInserts.forEach { (childId, dateRange, absenceType) ->
        dateRange.dates().forEach { date ->
            batch
                .bind("childId", childId)
                .bind("date", date)
                .bind("absenceType", absenceType)
                .bind("userId", userId)
                .add()
        }
    }

    batch.execute()
}

fun Database.Transaction.clearAbsencesWithinPeriod(period: FiniteDateRange, absenceType: AbsenceType, childIds: Set<ChildId>) {
    this.createUpdate(
        "DELETE FROM absence WHERE child_id = ANY(:childIds) AND :period @> date AND absence_type = :absenceType"
    )
        .bind("childIds", childIds.toTypedArray())
        .bind("period", period)
        .bind("absenceType", absenceType)
        .execute()
}

fun Database.Transaction.clearOldAbsencesExcludingFreeAbsences(childDatePairs: List<Pair<ChildId, LocalDate>>) {
    val batch = prepareBatch(
        "DELETE FROM absence WHERE child_id = :childId AND date = :date AND absence_type <> 'FREE_ABSENCE'::absence_type"
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

fun Database.Transaction.insertValidReservations(userId: UUID, requests: List<DailyReservationRequest>) {
    val batch = prepareBatch(
        """
        INSERT INTO attendance_reservation (child_id, date, start_time, end_time, created_by)
        SELECT :childId, :date, :start, :end, :userId
        FROM placement pl
        LEFT JOIN backup_care bc ON daterange(bc.start_date, bc.end_date, '[]') @> :date AND bc.child_id = :childId
        JOIN daycare d ON d.id = coalesce(bc.unit_id, pl.unit_id) AND 'RESERVATIONS' = ANY(d.enabled_pilot_features)
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
