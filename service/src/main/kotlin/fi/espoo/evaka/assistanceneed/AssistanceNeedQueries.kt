// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed

import fi.espoo.evaka.shared.AssistanceNeedId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate
import java.util.UUID

fun Database.Transaction.insertAssistanceNeed(user: AuthenticatedUser, childId: UUID, data: AssistanceNeedRequest): AssistanceNeed {
    //language=sql
    val sql =
        """
        INSERT INTO assistance_need (
            child_id, 
            start_date, 
            end_date, 
            updated_by, 
            capacity_factor, 
            description, 
            other_basis                   
        )
        VALUES (
            :childId, 
            :startDate, 
            :endDate, 
            :updatedBy, 
            :capacityFactor, 
            :description,
            :otherBasis
        )
        RETURNING id
        """.trimIndent()

    val id = createQuery(sql)
        .bind("childId", childId)
        .bind("startDate", data.startDate)
        .bind("endDate", data.endDate)
        .bind("updatedBy", user.id)
        .bind("capacityFactor", data.capacityFactor)
        .bind("description", data.description)
        .bind("otherBasis", data.otherBasis)
        .mapTo<AssistanceNeedId>()
        .first()

    insertAssistanceBasisOptionRefs(id, data.bases)

    return getAssistanceNeedById(id)
}

fun Database.Transaction.insertAssistanceBasisOptionRefs(needId: AssistanceNeedId, options: Set<String>): IntArray {
    //language=sql
    val sql =
        """
        INSERT INTO assistance_basis_option_ref (need_id, option_id)
        VALUES (:need_id, (SELECT id FROM assistance_basis_option WHERE value = :option))
        ON CONFLICT DO NOTHING
        """.trimIndent()
    val batch = prepareBatch(sql)
    options.forEach { option -> batch.bind("need_id", needId).bind("option", option).add() }
    return batch.execute()
}

fun Database.Read.getAssistanceNeedById(id: AssistanceNeedId): AssistanceNeed {
    //language=sql
    val sql =
        """
        SELECT an.id, child_id, start_date, end_date, capacity_factor, description, array_remove(array_agg(value), null) AS bases, other_basis
        FROM assistance_need an
        LEFT JOIN assistance_basis_option_ref abor ON abor.need_id = an.id
        LEFT JOIN assistance_basis_option abo ON abo.id = abor.option_id
        WHERE an.id = :id
        GROUP BY an.id, child_id, start_date, end_date, capacity_factor, description, other_basis
        """.trimIndent()
    return createQuery(sql).bind("id", id).mapTo(AssistanceNeed::class.java).first()
}

fun Database.Read.getAssistanceNeedsByChild(childId: UUID): List<AssistanceNeed> {
    //language=sql
    val sql =
        """
        SELECT an.id, child_id, start_date, end_date, capacity_factor, description, array_remove(array_agg(value), null) AS bases, other_basis
        FROM assistance_need an
        LEFT JOIN assistance_basis_option_ref abor ON abor.need_id = an.id
        LEFT JOIN assistance_basis_option abo ON abo.id = abor.option_id
        WHERE child_id = :childId
        GROUP BY an.id, child_id, start_date, end_date, capacity_factor, description, other_basis
        ORDER BY start_date DESC
        """.trimIndent()
    return createQuery(sql)
        .bind("childId", childId)
        .mapTo(AssistanceNeed::class.java)
        .list()
}

fun Database.Transaction.updateAssistanceNeed(user: AuthenticatedUser, id: AssistanceNeedId, data: AssistanceNeedRequest): AssistanceNeed {
    //language=sql
    val sql =
        """
        UPDATE assistance_need SET 
            start_date = :startDate,
            end_date = :endDate,
            updated_by = :updatedBy,
            capacity_factor = :capacityFactor,
            description = :description,
            other_basis = :otherBasis
        WHERE id = :id
        RETURNING id
        """.trimIndent()

    createQuery(sql)
        .bind("id", id)
        .bind("startDate", data.startDate)
        .bind("endDate", data.endDate)
        .bind("updatedBy", user.id)
        .bind("capacityFactor", data.capacityFactor)
        .bind("description", data.description)
        .bind("otherBasis", data.otherBasis)
        .mapTo<AssistanceNeedId>()
        .firstOrNull() ?: throw NotFound("Assistance need $id not found")

    deleteAssistanceBasisOptionRefsByNeedId(id, data.bases)
    insertAssistanceBasisOptionRefs(id, data.bases)

    return getAssistanceNeedById(id)
}

fun Database.Transaction.shortenOverlappingAssistanceNeed(user: AuthenticatedUser, childId: UUID, startDate: LocalDate, endDate: LocalDate) {
    //language=sql
    val sql =
        """
        UPDATE assistance_need 
        SET end_date = :startDate - interval '1 day', updated_by = :updatedBy
        WHERE child_id = :childId AND daterange(start_date, end_date, '[]') @> :startDate AND start_date <> :startDate
        RETURNING *
        """.trimIndent()

    createUpdate(sql)
        .bind("childId", childId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .bind("updatedBy", user.id)
        .execute()
}

fun Database.Transaction.deleteAssistanceNeed(id: AssistanceNeedId) {
    //language=sql
    val sql = "DELETE FROM assistance_need WHERE id = :id"
    val deleted = createUpdate(sql).bind("id", id).execute()
    if (deleted == 0) throw NotFound("Assistance need $id not found")
}

fun Database.Transaction.deleteAssistanceBasisOptionRefsByNeedId(needId: AssistanceNeedId, excluded: Set<String>): Int {
    //language=sql
    val sql =
        """
        DELETE FROM assistance_basis_option_ref
        WHERE need_id = :need_id
        AND option_id NOT IN (SELECT id FROM assistance_basis_option WHERE value = ANY(:excluded))
        """.trimIndent()
    return createUpdate(sql)
        .bind("need_id", needId)
        .bind("excluded", excluded.toTypedArray())
        .execute()
}

fun Database.Transaction.getAssistanceBasisOptions(): List<AssistanceBasisOption> {
    //language=sql
    val sql = "SELECT value, name_fi, description_fi FROM assistance_basis_option ORDER BY display_order"
    return createQuery(sql).mapTo(AssistanceBasisOption::class.java).list()
}
