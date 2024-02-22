// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceaction

import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import java.time.LocalDate

fun Database.Transaction.insertAssistanceAction(
    user: AuthenticatedUser,
    childId: ChildId,
    data: AssistanceActionRequest
): AssistanceAction {
    // language=sql
    val sql =
        """
        INSERT INTO assistance_action (
            child_id, 
            start_date, 
            end_date, 
            updated_by, 
            other_action
        )
        VALUES (
            :childId, 
            :startDate, 
            :endDate, 
            :updatedBy,
            :otherAction
        )
        RETURNING id
        """
            .trimIndent()

    val id =
        @Suppress("DEPRECATION")
        createQuery(sql)
            .bind("childId", childId)
            .bind("startDate", data.startDate)
            .bind("endDate", data.endDate)
            .bind("updatedBy", user.evakaUserId)
            .bind("otherAction", data.otherAction)
            .exactlyOne<AssistanceActionId>()

    insertAssistanceActionOptionRefs(id, data.actions)

    return getAssistanceActionById(id)
}

fun Database.Transaction.insertAssistanceActionOptionRefs(
    actionId: AssistanceActionId,
    options: Set<String>
): IntArray {
    // language=sql
    val sql =
        """
        INSERT INTO assistance_action_option_ref (action_id, option_id)
        VALUES (:action_id, (SELECT id FROM assistance_action_option WHERE value = :option))
        ON CONFLICT DO NOTHING
        """
            .trimIndent()
    val batch = prepareBatch(sql)
    options.forEach { option -> batch.bind("action_id", actionId).bind("option", option).add() }
    return batch.execute()
}

fun Database.Read.getAssistanceActionById(id: AssistanceActionId): AssistanceAction {
    // language=sql
    val sql =
        """
        SELECT aa.id, child_id, start_date, end_date, array_remove(array_agg(value), null) AS actions, other_action
        FROM assistance_action aa
        LEFT JOIN assistance_action_option_ref aaor ON aaor.action_id = aa.id
        LEFT JOIN assistance_action_option aao ON aao.id = aaor.option_id
        WHERE aa.id = :id
        GROUP BY aa.id, child_id, start_date, end_date, other_action
        """
            .trimIndent()
    @Suppress("DEPRECATION") return createQuery(sql).bind("id", id).exactlyOne<AssistanceAction>()
}

fun Database.Read.getAssistanceActionsByChild(childId: ChildId): List<AssistanceAction> {
    // language=sql
    val sql =
        """
        SELECT aa.id, child_id, start_date, end_date, array_remove(array_agg(value), null) AS actions, other_action
        FROM assistance_action aa
        LEFT JOIN assistance_action_option_ref aaor ON aaor.action_id = aa.id
        LEFT JOIN assistance_action_option aao ON aao.id = aaor.option_id
        WHERE child_id = :childId
        GROUP BY aa.id, child_id, start_date, end_date, other_action
        ORDER BY start_date DESC
        """
            .trimIndent()
    @Suppress("DEPRECATION")
    return createQuery(sql).bind("childId", childId).toList<AssistanceAction>()
}

fun Database.Transaction.updateAssistanceAction(
    user: AuthenticatedUser,
    id: AssistanceActionId,
    data: AssistanceActionRequest
): AssistanceAction {
    // language=sql
    val sql =
        """
        UPDATE assistance_action SET 
            start_date = :startDate,
            end_date = :endDate,
            updated_by = :updatedBy,
            other_action = :otherAction
        WHERE id = :id
        RETURNING id
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    createQuery(sql)
        .bind("id", id)
        .bind("startDate", data.startDate)
        .bind("endDate", data.endDate)
        .bind("updatedBy", user.evakaUserId)
        .bind("otherAction", data.otherAction)
        .exactlyOneOrNull<AssistanceActionId>() ?: throw NotFound("Assistance action $id not found")

    deleteAssistanceActionOptionRefsByActionId(id, data.actions)
    insertAssistanceActionOptionRefs(id, data.actions)

    return getAssistanceActionById(id)
}

fun Database.Transaction.shortenOverlappingAssistanceAction(
    user: AuthenticatedUser,
    childId: ChildId,
    startDate: LocalDate,
    endDate: LocalDate
) {
    // language=sql
    val sql =
        """
        UPDATE assistance_action 
        SET end_date = :startDate - interval '1 day', updated_by = :updatedBy
        WHERE child_id = :childId AND daterange(start_date, end_date, '[]') @> :startDate AND start_date <> :startDate
        RETURNING *
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    createUpdate(sql)
        .bind("childId", childId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .bind("updatedBy", user.evakaUserId)
        .execute()
}

fun Database.Transaction.deleteAssistanceAction(id: AssistanceActionId) {
    // language=sql
    val sql = "DELETE FROM assistance_action WHERE id = :id"
    @Suppress("DEPRECATION") val deleted = createUpdate(sql).bind("id", id).execute()
    if (deleted == 0) throw NotFound("Assistance action $id not found")
}

fun Database.Transaction.deleteAssistanceActionOptionRefsByActionId(
    actionId: AssistanceActionId,
    excluded: Set<String>
): Int {
    // language=sql
    val sql =
        """
        DELETE FROM assistance_action_option_ref
        WHERE action_id = :action_id
        AND option_id NOT IN (SELECT id FROM assistance_action_option WHERE value = ANY(:excluded))
        """
            .trimIndent()
    @Suppress("DEPRECATION")
    return createUpdate(sql).bind("action_id", actionId).bind("excluded", excluded).execute()
}

fun Database.Read.getAssistanceActionOptions(): List<AssistanceActionOption> {
    // language=sql
    val sql =
        "SELECT value, name_fi, description_fi FROM assistance_action_option ORDER BY display_order"
    @Suppress("DEPRECATION") return createQuery(sql).toList<AssistanceActionOption>()
}
