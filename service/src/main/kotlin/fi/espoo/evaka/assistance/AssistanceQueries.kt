// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistance

import fi.espoo.evaka.assistanceaction.AssistanceAction
import fi.espoo.evaka.shared.*
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.db.QuerySql
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import java.time.LocalDate

private val assistanceSelectFields =
    """
    a.id,
    a.child_id,
    a.valid_during,
    a.capacity_factor,
    a.modified,
    e.id AS modified_by_id,
    e.name AS modified_by_name,
    e.type AS modified_by_type
"""

private fun getAssistanceFactors(predicate: Predicate) = QuerySql {
    sql(
        """
SELECT $assistanceSelectFields
FROM assistance_factor a
LEFT JOIN evaka_user e ON a.modified_by = e.id
WHERE ${predicate(predicate.forTable("a"))}
"""
    )
}

fun Database.Read.getAssistanceFactors(child: ChildId): List<AssistanceFactor> =
    createQuery { getAssistanceFactors(Predicate { where("$it.child_id = ${bind(child)}") }) }
        .toList<AssistanceFactor>()

fun Database.Read.getAssistanceFactorsForChildrenOverRange(
    childIds: Set<PersonId>,
    range: FiniteDateRange,
): List<AssistanceFactor> =
    if (childIds.isEmpty()) emptyList()
    else
        createQuery {
                getAssistanceFactors(
                    Predicate {
                        where(
                            "$it.child_id = ANY (${bind(childIds)}) AND $it.valid_during && ${bind(range)}"
                        )
                    }
                )
            }
            .toList<AssistanceFactor>()

fun Database.Read.getAssistanceFactorsByChildId(
    childId: ChildId,
    filter: AccessControlFilter<AssistanceFactorId>,
): List<AssistanceFactor> =
    createQuery {
            sql(
                """
SELECT $assistanceSelectFields
FROM assistance_factor a
LEFT JOIN evaka_user e ON a.modified_by = e.id
WHERE child_id = ${bind(childId)} AND ${predicate(filter.forTable("a"))}
"""
            )
        }
        .toList<AssistanceFactor>()

fun Database.Read.getAssistanceFactor(id: AssistanceFactorId): AssistanceFactor? =
    createQuery { getAssistanceFactors(Predicate { where("$it.id = ${bind(id)}") }) }
        .exactlyOneOrNull<AssistanceFactor>()

fun Database.Transaction.insertAssistanceFactor(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    child: ChildId,
    update: AssistanceFactorUpdate,
): AssistanceFactorId =
    createUpdate {
            sql(
                """
INSERT INTO assistance_factor (child_id, modified, modified_by, valid_during, capacity_factor)
VALUES (${bind(child)}, ${bind(now)}, ${bind(user.evakaUserId)}, ${bind(update.validDuring)}, ${bind(update.capacityFactor)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<AssistanceFactorId>()

fun Database.Transaction.updateAssistanceFactor(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    id: AssistanceFactorId,
    update: AssistanceFactorUpdate,
) =
    createUpdate {
            sql(
                """
UPDATE assistance_factor
SET
    capacity_factor = ${bind(update.capacityFactor)},
    valid_during = ${bind(update.validDuring)},
    modified = ${bind(now)},
    modified_by = ${bind(user.evakaUserId)}
WHERE id = ${bind(id)}
"""
            )
        }
        .updateExactlyOne()

fun Database.Transaction.deleteAssistanceFactor(id: AssistanceFactorId): AssistanceFactor? =
    createUpdate {
            sql(
                """
WITH a AS (
    DELETE FROM assistance_factor WHERE id = ${bind(id)}
    RETURNING id, child_id, valid_during, capacity_factor, modified, modified_by
) SELECT $assistanceSelectFields FROM a LEFT JOIN evaka_user e ON a.modified_by = e.id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOneOrNull<AssistanceFactor>()

fun Database.Transaction.endAssistanceFactorsWhichBelongToPastPlacements(date: LocalDate) =
    createUpdate {
            sql(
                """
WITH
adjacent_placement AS (
  SELECT
    child_id,
    unnest(range_agg(daterange(start_date, end_date, '[]'))) AS valid_during
  FROM placement
  GROUP BY child_id, unit_id
)
UPDATE assistance_factor
SET valid_during = daterange(lower(assistance_factor.valid_during), upper(adjacent_placement.valid_during), '[)')
FROM adjacent_placement
WHERE adjacent_placement.child_id = assistance_factor.child_id
  AND adjacent_placement.valid_during @> lower(assistance_factor.valid_during)
  AND NOT adjacent_placement.valid_during @> (upper(assistance_factor.valid_during) - interval '1 day')::date
  AND upper(adjacent_placement.valid_during) = ${bind(date)}
"""
            )
        }
        .execute()

fun Database.Read.getDaycareAssistanceByChildId(
    child: ChildId,
    filter: AccessControlFilter<DaycareAssistanceId>,
): List<DaycareAssistance> =
    createQuery {
            sql(
                """
SELECT d.id, d.child_id, d.valid_during, d.level, d.modified, e.id AS modified_by_id, e.name AS modified_by_name, e.type AS modified_by_type
FROM daycare_assistance d
LEFT JOIN evaka_user e ON d.modified_by = e.id
WHERE child_id = ${bind(child)} AND ${predicate(filter.forTable("d"))}
"""
            )
        }
        .toList<DaycareAssistance>()

fun Database.Transaction.insertDaycareAssistance(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    child: ChildId,
    update: DaycareAssistanceUpdate,
): DaycareAssistanceId =
    createUpdate {
            sql(
                """
INSERT INTO daycare_assistance (child_id, modified, modified_by, valid_during, level)
VALUES (${bind(child)}, ${bind(now)}, ${bind(user.evakaUserId)}, ${bind(update.validDuring)}, ${bind(update.level)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<DaycareAssistanceId>()

fun Database.Transaction.updateDaycareAssistance(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    id: DaycareAssistanceId,
    update: DaycareAssistanceUpdate,
) =
    createUpdate {
            sql(
                """
UPDATE daycare_assistance
SET
    level = ${bind(update.level)},
    valid_during = ${bind(update.validDuring)},
    modified = ${bind(now)},
    modified_by = ${bind(user.evakaUserId)}
WHERE id = ${bind(id)}
"""
            )
        }
        .updateExactlyOne()

fun Database.Transaction.deleteDaycareAssistance(id: DaycareAssistanceId) =
    createUpdate { sql("DELETE FROM daycare_assistance WHERE id = ${bind(id)}") }.execute()

private val preschoolAssistanceSelectFields =
    """
    p.id,
    p.child_id,
    p.valid_during,
    p.level,
    p.modified,
    e.id AS modified_by_id,
    e.name AS modified_by_name,
    e.type AS modified_by_type
"""

fun Database.Read.getPreschoolAssistances(child: ChildId): List<PreschoolAssistance> =
    createQuery {
            sql(
                """
SELECT $preschoolAssistanceSelectFields
FROM preschool_assistance p
LEFT JOIN evaka_user e ON p.modified_by = e.id
WHERE child_id = ${bind(child)}
"""
            )
        }
        .toList<PreschoolAssistance>()

fun Database.Read.getPreschoolAssistanceByChildId(
    child: ChildId,
    filter: AccessControlFilter<PreschoolAssistanceId>,
): List<PreschoolAssistance> =
    createQuery {
            sql(
                """
SELECT $preschoolAssistanceSelectFields
FROM preschool_assistance p
LEFT JOIN evaka_user e ON p.modified_by = e.id
WHERE child_id = ${bind(child)} AND ${predicate(filter.forTable("p"))}
"""
            )
        }
        .toList<PreschoolAssistance>()

fun Database.Transaction.insertPreschoolAssistance(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    child: ChildId,
    update: PreschoolAssistanceUpdate,
): PreschoolAssistanceId =
    createUpdate {
            sql(
                """
INSERT INTO preschool_assistance (child_id, modified, modified_by, valid_during, level)
VALUES (${bind(child)}, ${bind(now)}, ${bind(user.evakaUserId)}, ${bind(update.validDuring)}, ${bind(update.level)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<PreschoolAssistanceId>()

fun Database.Transaction.updatePreschoolAssistance(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    id: PreschoolAssistanceId,
    update: PreschoolAssistanceUpdate,
) =
    createUpdate {
            sql(
                """
UPDATE preschool_assistance
SET
    level = ${bind(update.level)},
    valid_during = ${bind(update.validDuring)},
    modified = ${bind(now)},
    modified_by = ${bind(user.evakaUserId)}
WHERE id = ${bind(id)}
"""
            )
        }
        .updateExactlyOne()

fun Database.Transaction.deletePreschoolAssistance(id: PreschoolAssistanceId) =
    createUpdate { sql("DELETE FROM preschool_assistance WHERE id = ${bind(id)}") }.execute()

private val otherAssistanceSelectFields =
    """
    o.id,
    o.child_id,
    o.valid_during,
    o.type,
    o.modified,
    e.id AS modified_by_id,
    e.name AS modified_by_name,
    e.type AS modified_by_type
"""

fun Database.Read.getOtherAssistanceMeasures(child: ChildId): List<OtherAssistanceMeasure> =
    createQuery {
            sql(
                """
SELECT $otherAssistanceSelectFields
FROM other_assistance_measure o
LEFT JOIN evaka_user e ON o.modified_by = e.id
WHERE child_id = ${bind(child)}
"""
            )
        }
        .toList<OtherAssistanceMeasure>()

fun Database.Read.getOtherAssistanceMeasuresByChildId(
    child: ChildId,
    filter: AccessControlFilter<OtherAssistanceMeasureId>,
): List<OtherAssistanceMeasure> =
    createQuery {
            sql(
                """
SELECT $otherAssistanceSelectFields
FROM other_assistance_measure o
LEFT JOIN evaka_user e ON o.modified_by = e.id
WHERE child_id = ${bind(child)} AND ${predicate(filter.forTable("o"))}
"""
            )
        }
        .toList<OtherAssistanceMeasure>()

fun Database.Transaction.insertOtherAssistanceMeasure(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    child: ChildId,
    update: OtherAssistanceMeasureUpdate,
): OtherAssistanceMeasureId =
    createUpdate {
            sql(
                """
INSERT INTO other_assistance_measure (child_id, modified, modified_by, valid_during, type)
VALUES (${bind(child)}, ${bind(now)}, ${bind(user.evakaUserId)}, ${bind(update.validDuring)}, ${bind(update.type)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<OtherAssistanceMeasureId>()

fun Database.Transaction.updateOtherAssistanceMeasure(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    id: OtherAssistanceMeasureId,
    update: OtherAssistanceMeasureUpdate,
) =
    createUpdate {
            sql(
                """
UPDATE other_assistance_measure
SET
    type = ${bind(update.type)},
    valid_during = ${bind(update.validDuring)},
    modified = ${bind(now)},
    modified_by = ${bind(user.evakaUserId)}
WHERE id = ${bind(id)}
"""
            )
        }
        .updateExactlyOne()

fun Database.Transaction.deleteOtherAssistanceMeasure(id: OtherAssistanceMeasureId) =
    createUpdate { sql("DELETE FROM other_assistance_measure WHERE id = ${bind(id)}") }.execute()

fun Database.Read.getAssistanceActionsByChildId(
    childId: ChildId,
    filter: AccessControlFilter<AssistanceActionId>,
): List<AssistanceAction> =
    createQuery {
            sql(
                """
SELECT aa.id, child_id, start_date, end_date, array_remove(array_agg(value), null) AS actions, other_action
FROM assistance_action aa
LEFT JOIN assistance_action_option_ref aaor ON aaor.action_id = aa.id
LEFT JOIN assistance_action_option aao ON aao.id = aaor.option_id
WHERE aa.child_id = ${bind(childId)} AND ${predicate(filter.forTable("aa"))}
GROUP BY aa.id, child_id, start_date, end_date, other_action
ORDER BY start_date DESC     
"""
            )
        }
        .toList<AssistanceAction>()
