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
    val id =
        createQuery {
            sql(
                """
INSERT INTO assistance_action (
    child_id, 
    start_date, 
    end_date, 
    updated_by, 
    other_action
)
VALUES (
    ${bind(childId)}, 
    ${bind(data.startDate)}, 
    ${bind(data.endDate)}, 
    ${bind(user.evakaUserId)},
    ${bind(data.otherAction)}
)
RETURNING id
"""
            )
        }.exactlyOne<AssistanceActionId>()

    insertAssistanceActionOptionRefs(id, data.actions)

    return getAssistanceActionById(id)
}

fun Database.Transaction.insertAssistanceActionOptionRefs(
    actionId: AssistanceActionId,
    options: Set<String>
): IntArray =
    executeBatch(options) {
        sql(
            """
INSERT INTO assistance_action_option_ref (action_id, option_id)
VALUES (${bind(actionId)}, (SELECT id FROM assistance_action_option WHERE value = ${bind { it }}))
ON CONFLICT DO NOTHING
"""
        )
    }

fun Database.Read.getAssistanceActionById(id: AssistanceActionId): AssistanceAction =
    createQuery {
        sql(
            """
SELECT aa.id, child_id, start_date, end_date, array_remove(array_agg(value), null) AS actions, other_action
FROM assistance_action aa
LEFT JOIN assistance_action_option_ref aaor ON aaor.action_id = aa.id
LEFT JOIN assistance_action_option aao ON aao.id = aaor.option_id
WHERE aa.id = ${bind(id)}
GROUP BY aa.id, child_id, start_date, end_date, other_action
"""
        )
    }.exactlyOne()

fun Database.Read.getAssistanceActionsByChild(childId: ChildId): List<AssistanceAction> =
    createQuery {
        sql(
            """
SELECT aa.id, child_id, start_date, end_date, array_remove(array_agg(value), null) AS actions, other_action
FROM assistance_action aa
LEFT JOIN assistance_action_option_ref aaor ON aaor.action_id = aa.id
LEFT JOIN assistance_action_option aao ON aao.id = aaor.option_id
WHERE child_id = ${bind(childId)}
GROUP BY aa.id, child_id, start_date, end_date, other_action
ORDER BY start_date DESC
"""
        )
    }.toList()

fun Database.Transaction.updateAssistanceAction(
    user: AuthenticatedUser,
    id: AssistanceActionId,
    data: AssistanceActionRequest
): AssistanceAction {
    createQuery {
        sql(
            """
UPDATE assistance_action SET 
    start_date = ${bind(data.startDate)},
    end_date = ${bind(data.endDate)},
    updated_by = ${bind(user.evakaUserId)},
    other_action = ${bind(data.otherAction)}
WHERE id = ${bind(id)}
RETURNING id
"""
        )
    }.exactlyOneOrNull<AssistanceActionId>() ?: throw NotFound("Assistance action $id not found")

    deleteAssistanceActionOptionRefsByActionId(id, data.actions)
    insertAssistanceActionOptionRefs(id, data.actions)

    return getAssistanceActionById(id)
}

fun Database.Transaction.shortenOverlappingAssistanceAction(
    user: AuthenticatedUser,
    childId: ChildId,
    startDate: LocalDate
) {
    execute {
        sql(
            """
UPDATE assistance_action 
SET end_date = ${bind(startDate)} - interval '1 day', updated_by = ${bind(user.evakaUserId)}
WHERE child_id = ${bind(childId)} AND daterange(start_date, end_date, '[]') @> ${bind(startDate)} AND start_date <> ${bind(startDate)}
RETURNING *
"""
        )
    }
}

fun Database.Transaction.deleteAssistanceAction(id: AssistanceActionId) {
    val deleted = execute { sql("DELETE FROM assistance_action WHERE id = ${bind(id)}") }
    if (deleted == 0) throw NotFound("Assistance action $id not found")
}

fun Database.Transaction.deleteAssistanceActionOptionRefsByActionId(
    actionId: AssistanceActionId,
    excluded: Set<String>
): Int =
    execute {
        sql(
            """
DELETE FROM assistance_action_option_ref
WHERE action_id = ${bind(actionId)}
AND option_id NOT IN (SELECT id FROM assistance_action_option WHERE value = ANY(${bind(excluded)}))
"""
        )
    }

fun Database.Read.getAssistanceActionOptions(): List<AssistanceActionOption> =
    createQuery {
        sql(
            "SELECT value, name_fi, description_fi FROM assistance_action_option ORDER BY display_order"
        )
    }.toList()
