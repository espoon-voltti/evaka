// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceaction

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.Handle
import java.time.LocalDate
import java.util.UUID

fun insertAssistanceAction(h: Handle, user: AuthenticatedUser, childId: UUID, data: AssistanceActionRequest): AssistanceAction {
    //language=sql
    val sql =
        """
        INSERT INTO assistance_action (
            child_id, 
            start_date, 
            end_date, 
            updated_by, 
            actions,
            other_action,
            measures
        )
        VALUES (
            :childId, 
            :startDate, 
            :endDate, 
            :updatedBy,
            :actions::assistance_action_type[],
            :otherAction,
            :measures::assistance_measure[]
        )
        RETURNING *
        """.trimIndent()

    return h
        .createQuery(sql)
        .bind("childId", childId)
        .bind("startDate", data.startDate)
        .bind("endDate", data.endDate)
        .bind("updatedBy", user.id)
        .bind("actions", data.actions.map { it.toString() }.toTypedArray())
        .bind("otherAction", data.otherAction)
        .bind("measures", data.measures.map { it.toString() }.toTypedArray())
        .mapTo(AssistanceAction::class.java)
        .first()
}

fun getAssistanceActionsByChild(h: Handle, childId: UUID): List<AssistanceAction> {
    //language=sql
    val sql = "SELECT * FROM assistance_action WHERE child_id = :childId ORDER BY start_date DESC "
    return h.createQuery(sql)
        .bind("childId", childId)
        .mapTo(AssistanceAction::class.java)
        .list()
}

fun updateAssistanceAction(h: Handle, user: AuthenticatedUser, id: UUID, data: AssistanceActionRequest): AssistanceAction {
    //language=sql
    val sql =
        """
        UPDATE assistance_action SET 
            start_date = :startDate,
            end_date = :endDate,
            updated_by = :updatedBy,
            actions = :actions::assistance_action_type[],
            other_action = :otherAction,
            measures = :measures::assistance_measure[]
        WHERE id = :id
        RETURNING *
        """.trimIndent()

    return h.createQuery(sql)
        .bind("id", id)
        .bind("startDate", data.startDate)
        .bind("endDate", data.endDate)
        .bind("updatedBy", user.id)
        .bind("actions", data.actions.map { it.toString() }.toTypedArray())
        .bind("otherAction", data.otherAction)
        .bind("measures", data.measures.map { it.toString() }.toTypedArray())
        .mapTo(AssistanceAction::class.java)
        .firstOrNull() ?: throw NotFound("Assistance action $id not found")
}

fun shortenOverlappingAssistanceAction(h: Handle, user: AuthenticatedUser, childId: UUID, startDate: LocalDate, endDate: LocalDate) {
    //language=sql
    val sql =
        """
        UPDATE assistance_action 
        SET end_date = :startDate - interval '1 day', updated_by = :updatedBy
        WHERE child_id = :childId AND daterange(start_date, end_date, '[]') @> :startDate AND start_date <> :startDate
        RETURNING *
        """.trimIndent()

    h.createUpdate(sql)
        .bind("childId", childId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .bind("updatedBy", user.id)
        .execute()
}

fun deleteAssistanceAction(h: Handle, id: UUID) {
    //language=sql
    val sql = "DELETE FROM assistance_action WHERE id = :id"
    val deleted = h.createUpdate(sql).bind("id", id).execute()
    if (deleted == 0) throw NotFound("Assistance action $id not found")
}
