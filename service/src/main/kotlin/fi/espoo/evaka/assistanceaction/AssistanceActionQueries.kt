// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceaction

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import java.time.LocalDate
import java.util.UUID

fun Database.Transaction.insertAssistanceAction(user: AuthenticatedUser, childId: UUID, data: AssistanceActionRequest): AssistanceAction {
    //language=sql
    val sql =
        """
        INSERT INTO assistance_action (
            child_id, 
            start_date, 
            end_date, 
            updated_by, 
            other_action,
            measures
        )
        VALUES (
            :childId, 
            :startDate, 
            :endDate, 
            :updatedBy,
            :otherAction,
            :measures::assistance_measure[]
        )
        RETURNING id
        """.trimIndent()

    val id = createQuery(sql)
        .bind("childId", childId)
        .bind("startDate", data.startDate)
        .bind("endDate", data.endDate)
        .bind("updatedBy", user.id)
        .bind("otherAction", data.otherAction)
        .bind("measures", data.measures.map { it.toString() }.toTypedArray())
        .mapTo(UUID::class.java)
        .first()

    insertAssistanceActionOptionRefs(id, data.actions)

    return getAssistanceActionById(id)
}

fun Database.Transaction.insertAssistanceActionOptionRefs(actionId: UUID, options: Set<String>): IntArray {
    //language=sql
    val sql =
        """
        INSERT INTO assistance_action_option_ref (action_id, option_id)
        VALUES (:action_id, (SELECT id FROM assistance_action_option WHERE value = :option))
        ON CONFLICT DO NOTHING
        """.trimIndent()
    val batch = prepareBatch(sql)
    options.forEach { option -> batch.bind("action_id", actionId).bind("option", option).add() }
    return batch.execute()
}

fun Database.Read.getAssistanceActionById(id: UUID): AssistanceAction {
    //language=sql
    val sql =
        """
        SELECT aa.id, child_id, start_date, end_date, array_remove(array_agg(value), null) AS actions, other_action, measures
        FROM assistance_action aa
        LEFT JOIN assistance_action_option_ref aaor ON aaor.action_id = aa.id
        LEFT JOIN assistance_action_option aao ON aao.id = aaor.option_id
        WHERE aa.id = :id
        GROUP BY aa.id, child_id, start_date, end_date, other_action, measures
        """.trimIndent()
    return createQuery(sql).bind("id", id).mapTo(AssistanceAction::class.java).first()
}

fun Database.Read.getAssistanceActionsByChild(childId: UUID): List<AssistanceAction> {
    //language=sql
    val sql =
        """
        SELECT aa.id, child_id, start_date, end_date, array_remove(array_agg(value), null) AS actions, other_action, measures
        FROM assistance_action aa
        LEFT JOIN assistance_action_option_ref aaor ON aaor.action_id = aa.id
        LEFT JOIN assistance_action_option aao ON aao.id = aaor.option_id
        WHERE child_id = :childId
        GROUP BY aa.id, child_id, start_date, end_date, other_action, measures
        ORDER BY start_date DESC
        """.trimIndent()
    return createQuery(sql)
        .bind("childId", childId)
        .mapTo(AssistanceAction::class.java)
        .list()
}

fun Database.Transaction.updateAssistanceAction(user: AuthenticatedUser, id: UUID, data: AssistanceActionRequest): AssistanceAction {
    //language=sql
    val sql =
        """
        UPDATE assistance_action SET 
            start_date = :startDate,
            end_date = :endDate,
            updated_by = :updatedBy,
            other_action = :otherAction,
            measures = :measures::assistance_measure[]
        WHERE id = :id
        RETURNING id
        """.trimIndent()

    createQuery(sql)
        .bind("id", id)
        .bind("startDate", data.startDate)
        .bind("endDate", data.endDate)
        .bind("updatedBy", user.id)
        .bind("otherAction", data.otherAction)
        .bind("measures", data.measures.map { it.toString() }.toTypedArray())
        .mapTo(UUID::class.java)
        .firstOrNull() ?: throw NotFound("Assistance action $id not found")

    deleteAssistanceActionOptionRefsByActionId(id, data.actions)
    insertAssistanceActionOptionRefs(id, data.actions)

    return getAssistanceActionById(id)
}

fun Database.Transaction.shortenOverlappingAssistanceAction(user: AuthenticatedUser, childId: UUID, startDate: LocalDate, endDate: LocalDate) {
    //language=sql
    val sql =
        """
        UPDATE assistance_action 
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

fun Database.Transaction.deleteAssistanceAction(id: UUID) {
    //language=sql
    val sql = "DELETE FROM assistance_action WHERE id = :id"
    val deleted = createUpdate(sql).bind("id", id).execute()
    if (deleted == 0) throw NotFound("Assistance action $id not found")
}

fun Database.Transaction.deleteAssistanceActionOptionRefsByActionId(actionId: UUID, excluded: Set<String>): Int {
    //language=sql
    val sql =
        """
        DELETE FROM assistance_action_option_ref
        WHERE action_id = :action_id
        AND option_id NOT IN (SELECT id FROM assistance_action_option WHERE value = ANY(:excluded))
        """.trimIndent()
    return createUpdate(sql)
        .bind("action_id", actionId)
        .bind("excluded", excluded.toTypedArray())
        .execute()
}

fun Database.Transaction.getAssistanceActionOptions(): List<AssistanceActionOption> {
    //language=sql
    val sql = "SELECT value, name_fi FROM assistance_action_option ORDER BY priority"
    return createQuery(sql).mapTo(AssistanceActionOption::class.java).list()
}
