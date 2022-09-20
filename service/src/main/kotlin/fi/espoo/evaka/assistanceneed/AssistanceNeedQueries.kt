// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed

import fi.espoo.evaka.shared.AssistanceNeedId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import java.time.LocalDate

fun Database.Transaction.insertAssistanceNeed(
    user: AuthenticatedUser,
    childId: ChildId,
    data: AssistanceNeedRequest
): AssistanceNeed {
    // language=sql
    val sql =
        """
        INSERT INTO assistance_need (
            child_id, 
            start_date, 
            end_date, 
            updated_by, 
            capacity_factor 
        )
        VALUES (
            :childId, 
            :startDate, 
            :endDate, 
            :updatedBy, 
            :capacityFactor 
        )
        RETURNING id
        """.trimIndent(
        )

    val id =
        createQuery(sql)
            .bind("childId", childId)
            .bind("startDate", data.startDate)
            .bind("endDate", data.endDate)
            .bind("updatedBy", user.evakaUserId)
            .bind("capacityFactor", data.capacityFactor)
            .mapTo<AssistanceNeedId>()
            .first()

    insertAssistanceBasisOptionRefs(id, data.bases)

    return getAssistanceNeedById(id)
}

fun Database.Transaction.insertAssistanceBasisOptionRefs(
    needId: AssistanceNeedId,
    options: Set<String>
): IntArray {
    // language=sql
    val sql =
        """
        INSERT INTO assistance_basis_option_ref (need_id, option_id)
        VALUES (:need_id, (SELECT id FROM assistance_basis_option WHERE value = :option))
        ON CONFLICT DO NOTHING
        """.trimIndent(
        )
    val batch = prepareBatch(sql)
    options.forEach { option -> batch.bind("need_id", needId).bind("option", option).add() }
    return batch.execute()
}

fun Database.Read.getAssistanceNeedById(id: AssistanceNeedId): AssistanceNeed {
    // language=sql
    val sql =
        """
        SELECT an.id, child_id, start_date, end_date, capacity_factor, array_remove(array_agg(value), null) AS bases
        FROM assistance_need an
        LEFT JOIN assistance_basis_option_ref abor ON abor.need_id = an.id
        LEFT JOIN assistance_basis_option abo ON abo.id = abor.option_id
        WHERE an.id = :id
        GROUP BY an.id, child_id, start_date, end_date, capacity_factor
        """.trimIndent(
        )
    return createQuery(sql).bind("id", id).mapTo<AssistanceNeed>().first()
}

fun Database.Read.getAssistanceNeedsByChild(childId: ChildId): List<AssistanceNeed> {
    // language=sql
    val sql =
        """
        SELECT an.id, child_id, start_date, end_date, capacity_factor, array_remove(array_agg(value), null) AS bases
        FROM assistance_need an
        LEFT JOIN assistance_basis_option_ref abor ON abor.need_id = an.id
        LEFT JOIN assistance_basis_option abo ON abo.id = abor.option_id
        WHERE child_id = :childId
        GROUP BY an.id, child_id, start_date, end_date, capacity_factor
        ORDER BY start_date DESC
        """.trimIndent(
        )
    return createQuery(sql).bind("childId", childId).mapTo<AssistanceNeed>().list()
}

fun Database.Read.getCapacityFactorsByChild(childId: ChildId): List<AssistanceNeedCapacityFactor> {
    return createQuery(
            """
         SELECT 
           daterange(start_date, end_date, '[]') as date_range,
           capacity_factor
         FROM assistance_need
         WHERE child_id = :childId
     """
        )
        .bind("childId", childId)
        .mapTo<AssistanceNeedCapacityFactor>()
        .list()
}

fun Database.Transaction.updateAssistanceNeed(
    user: AuthenticatedUser,
    id: AssistanceNeedId,
    data: AssistanceNeedRequest
): AssistanceNeed {
    // language=sql
    val sql =
        """
        UPDATE assistance_need SET 
            start_date = :startDate,
            end_date = :endDate,
            updated_by = :updatedBy,
            capacity_factor = :capacityFactor
        WHERE id = :id
        RETURNING id
        """.trimIndent(
        )

    createQuery(sql)
        .bind("id", id)
        .bind("startDate", data.startDate)
        .bind("endDate", data.endDate)
        .bind("updatedBy", user.evakaUserId)
        .bind("capacityFactor", data.capacityFactor)
        .mapTo<AssistanceNeedId>()
        .firstOrNull()
        ?: throw NotFound("Assistance need $id not found")

    deleteAssistanceBasisOptionRefsByNeedId(id, data.bases)
    insertAssistanceBasisOptionRefs(id, data.bases)

    return getAssistanceNeedById(id)
}

fun Database.Transaction.shortenOverlappingAssistanceNeed(
    user: AuthenticatedUser,
    childId: ChildId,
    startDate: LocalDate,
    endDate: LocalDate
) {
    // language=sql
    val sql =
        """
        UPDATE assistance_need 
        SET end_date = :startDate - interval '1 day', updated_by = :updatedBy
        WHERE child_id = :childId AND daterange(start_date, end_date, '[]') @> :startDate AND start_date <> :startDate
        RETURNING *
        """.trimIndent(
        )

    createUpdate(sql)
        .bind("childId", childId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .bind("updatedBy", user.evakaUserId)
        .execute()
}

fun Database.Transaction.deleteAssistanceNeed(id: AssistanceNeedId): AssistanceNeedChildRange {
    return createQuery(
            "DELETE FROM assistance_need WHERE id = :id RETURNING child_id, daterange(start_date, end_date, '[]') as date_range"
        )
        .bind("id", id)
        .mapTo<AssistanceNeedChildRange>()
        .firstOrNull()
        ?: throw NotFound("Assistance need $id not found")
}

fun Database.Transaction.deleteAssistanceBasisOptionRefsByNeedId(
    needId: AssistanceNeedId,
    excluded: Set<String>
): Int {
    // language=sql
    val sql =
        """
        DELETE FROM assistance_basis_option_ref
        WHERE need_id = :need_id
        AND option_id NOT IN (SELECT id FROM assistance_basis_option WHERE value = ANY(:excluded))
        """.trimIndent(
        )
    return createUpdate(sql).bind("need_id", needId).bind("excluded", excluded).execute()
}

fun Database.Read.getAssistanceBasisOptions(): List<AssistanceBasisOption> {
    // language=sql
    val sql =
        "SELECT value, name_fi, description_fi FROM assistance_basis_option ORDER BY display_order"
    return createQuery(sql).mapTo<AssistanceBasisOption>().list()
}
