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

fun Database.Transaction.clearFreeAbsencesWithinPeriod(period: FiniteDateRange, childIds: Set<ChildId>) {
    this.createUpdate(
        "DELETE FROM absence WHERE child_id = ANY(:childIds) AND :period @> date AND absence_type = :absenceType"
    )
        .bind("childIds", childIds.toTypedArray())
        .bind("period", period)
        .bind("absenceType", AbsenceType.FREE_ABSENCE)
        .execute()
}

fun Database.Transaction.clearOldAbsences(childDatePairs: List<Pair<ChildId, LocalDate>>) {
    val batch = prepareBatch(
        "DELETE FROM absence WHERE child_id = :childId AND date = :date"
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
