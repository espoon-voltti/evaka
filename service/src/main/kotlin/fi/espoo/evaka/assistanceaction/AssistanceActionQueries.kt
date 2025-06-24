// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceaction

import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import java.time.LocalDate

fun Database.Transaction.insertAssistanceAction(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    childId: ChildId,
    data: AssistanceActionRequest,
): AssistanceAction {
    val id =
        createQuery {
                sql(
                    """
INSERT INTO assistance_action (
    child_id, 
    start_date, 
    end_date,
    created_at,
    modified_at,
    modified_by, 
    other_action
)
VALUES (
    ${bind(childId)}, 
    ${bind(data.startDate)}, 
    ${bind(data.endDate)}, 
    ${bind(now)},
    ${bind(now)},
    ${bind(user.evakaUserId)},
    ${bind(data.otherAction)}
)
RETURNING id
"""
                )
            }
            .exactlyOne<AssistanceActionId>()

    insertAssistanceActionOptionRefs(id, data.actions)

    return getAssistanceActionById(id)
}

fun Database.Transaction.insertAssistanceActionOptionRefs(
    actionId: AssistanceActionId,
    options: Set<String>,
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
SELECT aa.id, child_id, start_date, end_date, array_remove(array_agg(value), null) AS actions, other_action,
    aa.modified_at,
    e.id AS modified_by_id,
    e.name AS modified_by_name,
    e.type AS modified_by_type
FROM assistance_action aa
LEFT JOIN assistance_action_option_ref aaor ON aaor.action_id = aa.id
LEFT JOIN assistance_action_option aao ON aao.id = aaor.option_id
LEFT JOIN evaka_user e ON aa.modified_by = e.id
WHERE aa.id = ${bind(id)}
GROUP BY aa.id, child_id, start_date, end_date, other_action, aa.modified_at, e.id, e.name, e.type
"""
            )
        }
        .exactlyOne()

fun Database.Read.getAssistanceActionsByChild(
    childId: ChildId,
    filter: AccessControlFilter<AssistanceActionId> = AccessControlFilter.PermitAll,
): List<AssistanceAction> =
    createQuery {
            sql(
                """
SELECT aa.id, child_id, start_date, end_date, array_remove(array_agg(value), null) AS actions, other_action,
    aa.modified_at,
    e.id AS modified_by_id,
    e.name AS modified_by_name,
    e.type AS modified_by_type
FROM assistance_action aa
LEFT JOIN assistance_action_option_ref aaor ON aaor.action_id = aa.id
LEFT JOIN assistance_action_option aao ON aao.id = aaor.option_id
LEFT JOIN evaka_user e ON aa.modified_by = e.id
WHERE child_id = ${bind(childId)} AND ${predicate(filter.forTable("aa"))}
GROUP BY aa.id, child_id, start_date, end_date, other_action, aa.modified_at, e.id, e.name, e.type
ORDER BY start_date DESC
"""
            )
        }
        .toList()

fun Database.Transaction.updateAssistanceAction(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    id: AssistanceActionId,
    data: AssistanceActionRequest,
): AssistanceAction {
    createQuery {
            sql(
                """
UPDATE assistance_action SET 
    start_date = ${bind(data.startDate)},
    end_date = ${bind(data.endDate)},
    modified_at = ${bind(now)},
    modified_by = ${bind(user.evakaUserId)},
    other_action = ${bind(data.otherAction)}
WHERE id = ${bind(id)}
RETURNING id
"""
            )
        }
        .exactlyOneOrNull<AssistanceActionId>() ?: throw NotFound("Assistance action $id not found")

    deleteAssistanceActionOptionRefsByActionId(id, data.actions)
    insertAssistanceActionOptionRefs(id, data.actions)

    return getAssistanceActionById(id)
}

fun Database.Transaction.shortenOverlappingAssistanceAction(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    childId: ChildId,
    startDate: LocalDate,
) {
    execute {
        sql(
            """
UPDATE assistance_action 
SET end_date = ${bind(startDate)} - interval '1 day', modified_at = ${bind(now)}, modified_by = ${bind(user.evakaUserId)}
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
    excluded: Set<String>,
): Int = execute {
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
                """
                    SELECT value, name_fi, description_fi, category, display_order, valid_from, valid_to 
                    FROM assistance_action_option 
                """
            )
        }
        .toList()
