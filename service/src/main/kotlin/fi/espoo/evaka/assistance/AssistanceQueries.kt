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
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import java.time.LocalDate

private fun getAssistanceFactors(predicate: Predicate) = QuerySql {
    sql(
        """
SELECT id, child_id, valid_during, capacity_factor, modified, (SELECT name FROM evaka_user WHERE id = modified_by) AS modified_by
FROM assistance_factor
WHERE ${predicate(predicate.forTable("assistance_factor"))}
"""
    )
}

fun Database.Read.getAssistanceFactors(child: ChildId): List<AssistanceFactor> =
    @Suppress("DEPRECATION")
    createQuery(getAssistanceFactors(Predicate { where("$it.child_id = ${bind(child)}") }))
        .toList<AssistanceFactor>()

fun Database.Read.getAssistanceFactorsByChildId(
    childId: ChildId,
    filter: AccessControlFilter<AssistanceFactorId>
): List<AssistanceFactor> =
    createQuery {
            sql(
                """
SELECT id, child_id, valid_during, capacity_factor, modified, (SELECT name FROM evaka_user WHERE id = modified_by) AS modified_by
FROM assistance_factor
WHERE child_id = ${bind(childId)} AND ${predicate(filter.forTable("assistance_factor"))}
"""
                    .trimIndent()
            )
        }
        .toList<AssistanceFactor>()

fun Database.Read.getAssistanceFactor(id: AssistanceFactorId): AssistanceFactor? =
    @Suppress("DEPRECATION")
    createQuery(getAssistanceFactors(Predicate { where("$it.id = ${bind(id)}") }))
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
    update: AssistanceFactorUpdate
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
DELETE FROM assistance_factor WHERE id = ${bind(id)}
RETURNING id, child_id, valid_during, capacity_factor, modified, (SELECT name FROM evaka_user WHERE id = modified_by) AS modified_by
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOneOrNull<AssistanceFactor>()

fun Database.Transaction.endAssistanceFactorsWhichBelongToPastPlacements(date: LocalDate) =
    createUpdate {
            sql(
                """
WITH assistance_factor_with_new_end_date AS (
    SELECT assistance_factor.id, max(placement.end_date) AS new_end_date
    FROM assistance_factor
    JOIN placement ON assistance_factor.child_id = placement.child_id
     AND assistance_factor.valid_during @> placement.end_date
    GROUP BY assistance_factor.id
    HAVING max(placement.end_date) < ${bind(date)}
)
UPDATE assistance_factor
SET valid_during = daterange(lower(valid_during), new_end_date, '[]')
FROM assistance_factor_with_new_end_date
WHERE assistance_factor_with_new_end_date.id = assistance_factor.id
"""
            )
        }
        .execute()

fun Database.Read.getDaycareAssistanceByChildId(
    child: ChildId,
    filter: AccessControlFilter<DaycareAssistanceId>
): List<DaycareAssistance> =
    createQuery {
            sql(
                """
SELECT id, child_id, valid_during, level, modified, (SELECT name FROM evaka_user WHERE id = modified_by) AS modified_by
FROM daycare_assistance
WHERE child_id = ${bind(child)} AND ${predicate(filter.forTable("daycare_assistance"))}
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
    update: DaycareAssistanceUpdate
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

fun Database.Read.getPreschoolAssistances(child: ChildId): List<PreschoolAssistance> =
    createQuery {
            sql(
                """
SELECT id, child_id, valid_during, level, modified, (SELECT name FROM evaka_user WHERE id = modified_by) AS modified_by
FROM preschool_assistance
WHERE child_id = ${bind(child)}
"""
            )
        }
        .toList<PreschoolAssistance>()

fun Database.Read.getPreschoolAssistanceByChildId(
    child: ChildId,
    filter: AccessControlFilter<PreschoolAssistanceId>
): List<PreschoolAssistance> =
    createQuery {
            sql(
                """
SELECT id, child_id, valid_during, level, modified, (SELECT name FROM evaka_user WHERE id = modified_by) AS modified_by
FROM preschool_assistance
WHERE child_id = ${bind(child)} AND ${predicate(filter.forTable("preschool_assistance"))}
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
    update: PreschoolAssistanceUpdate
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

fun Database.Read.getOtherAssistanceMeasures(child: ChildId): List<OtherAssistanceMeasure> =
    createQuery {
            sql(
                """
SELECT id, child_id, valid_during, type, modified, (SELECT name FROM evaka_user WHERE id = modified_by) AS modified_by
FROM other_assistance_measure
WHERE child_id = ${bind(child)}
"""
            )
        }
        .toList<OtherAssistanceMeasure>()

fun Database.Read.getOtherAssistanceMeasuresByChildId(
    child: ChildId,
    filter: AccessControlFilter<OtherAssistanceMeasureId>
): List<OtherAssistanceMeasure> =
    createQuery {
            sql(
                """
SELECT id, child_id, valid_during, type, modified, (SELECT name FROM evaka_user WHERE id = modified_by) AS modified_by
FROM other_assistance_measure
WHERE child_id = ${bind(child)} AND ${predicate(filter.forTable("other_assistance_measure"))}
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
    update: OtherAssistanceMeasureUpdate
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
    filter: AccessControlFilter<AssistanceActionId>
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
                    .trimIndent()
            )
        }
        .toList<AssistanceAction>()
