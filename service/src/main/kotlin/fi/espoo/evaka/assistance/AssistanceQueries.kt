// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistance

import fi.espoo.evaka.shared.AssistanceFactorId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.DaycareAssistanceId
import fi.espoo.evaka.shared.OtherAssistanceMeasureId
import fi.espoo.evaka.shared.PreschoolAssistanceId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.db.QuerySql
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import kotlin.jvm.optionals.getOrNull

private fun getAssistanceFactors(predicate: Predicate<DatabaseTable.AssistanceFactor>) =
    QuerySql.of<DatabaseTable.AssistanceFactor> {
        sql(
            """
SELECT id, child_id, valid_during, capacity_factor, modified, (SELECT name FROM evaka_user WHERE id = modified_by) AS modified_by
FROM assistance_factor
WHERE ${predicate(predicate.forTable("assistance_factor"))}
"""
        )
    }

fun Database.Read.getAssistanceFactors(child: ChildId): List<AssistanceFactor> =
    createQuery(getAssistanceFactors(Predicate { where("$it.child_id = ${bind(child)}") }))
        .mapTo<AssistanceFactor>()
        .list()

fun Database.Read.getAssistanceFactor(id: AssistanceFactorId): AssistanceFactor? =
    createQuery(getAssistanceFactors(Predicate { where("$it.id = ${bind(id)}") }))
        .mapTo<AssistanceFactor>()
        .findOne()
        .getOrNull()

fun Database.Transaction.insertAssistanceFactor(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    child: ChildId,
    update: AssistanceFactorUpdate,
): AssistanceFactorId =
    createUpdate<DatabaseTable.AssistanceFactor> {
            sql(
                """
INSERT INTO assistance_factor (child_id, modified, modified_by, valid_during, capacity_factor)
VALUES (${bind(child)}, ${bind(now)}, ${bind(user.evakaUserId)}, ${bind(update.validDuring)}, ${bind(update.capacityFactor)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .mapTo<AssistanceFactorId>()
        .single()

fun Database.Transaction.updateAssistanceFactor(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    id: AssistanceFactorId,
    update: AssistanceFactorUpdate
) =
    createUpdate<DatabaseTable.AssistanceFactor> {
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
    createUpdate<DatabaseTable.AssistanceFactor> {
            sql(
                """
DELETE FROM assistance_factor WHERE id = ${bind(id)}
RETURNING id, child_id, valid_during, capacity_factor, modified, (SELECT name FROM evaka_user WHERE id = modified_by) AS modified_by
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .mapTo<AssistanceFactor>()
        .findOne()
        .getOrNull()

fun Database.Read.getDaycareAssistances(child: ChildId): List<DaycareAssistance> =
    createQuery<DatabaseTable.DaycareAssistance> {
            sql(
                """
SELECT id, child_id, valid_during, level, modified, (SELECT name FROM evaka_user WHERE id = modified_by) AS modified_by
FROM daycare_assistance
WHERE child_id = ${bind(child)}
"""
            )
        }
        .mapTo<DaycareAssistance>()
        .list()

fun Database.Transaction.insertDaycareAssistance(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    child: ChildId,
    update: DaycareAssistanceUpdate,
): DaycareAssistanceId =
    createUpdate<DatabaseTable.DaycareAssistance> {
            sql(
                """
INSERT INTO daycare_assistance (child_id, modified, modified_by, valid_during, level)
VALUES (${bind(child)}, ${bind(now)}, ${bind(user.evakaUserId)}, ${bind(update.validDuring)}, ${bind(update.level)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .mapTo<DaycareAssistanceId>()
        .single()

fun Database.Transaction.updateDaycareAssistance(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    id: DaycareAssistanceId,
    update: DaycareAssistanceUpdate
) =
    createUpdate<DatabaseTable.DaycareAssistance> {
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
    createUpdate<DatabaseTable.DaycareAssistance> {
            sql("DELETE FROM daycare_assistance WHERE id = ${bind(id)}")
        }
        .execute()

fun Database.Read.getPreschoolAssistances(child: ChildId): List<PreschoolAssistance> =
    createQuery<DatabaseTable.PreschoolAssistance> {
            sql(
                """
SELECT id, child_id, valid_during, level, modified, (SELECT name FROM evaka_user WHERE id = modified_by) AS modified_by
FROM preschool_assistance
WHERE child_id = ${bind(child)}
"""
            )
        }
        .mapTo<PreschoolAssistance>()
        .list()

fun Database.Transaction.insertPreschoolAssistance(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    child: ChildId,
    update: PreschoolAssistanceUpdate,
): PreschoolAssistanceId =
    createUpdate<DatabaseTable.PreschoolAssistance> {
            sql(
                """
INSERT INTO preschool_assistance (child_id, modified, modified_by, valid_during, level)
VALUES (${bind(child)}, ${bind(now)}, ${bind(user.evakaUserId)}, ${bind(update.validDuring)}, ${bind(update.level)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .mapTo<PreschoolAssistanceId>()
        .single()

fun Database.Transaction.updatePreschoolAssistance(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    id: PreschoolAssistanceId,
    update: PreschoolAssistanceUpdate
) =
    createUpdate<DatabaseTable.PreschoolAssistance> {
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
    createUpdate<DatabaseTable.PreschoolAssistance> {
            sql("DELETE FROM preschool_assistance WHERE id = ${bind(id)}")
        }
        .execute()

fun Database.Read.getOtherAssistanceMeasures(child: ChildId): List<OtherAssistanceMeasure> =
    createQuery<DatabaseTable.OtherAssistanceMeasure> {
            sql(
                """
SELECT id, child_id, valid_during, type, modified, (SELECT name FROM evaka_user WHERE id = modified_by) AS modified_by
FROM other_assistance_measure
WHERE child_id = ${bind(child)}
"""
            )
        }
        .mapTo<OtherAssistanceMeasure>()
        .list()

fun Database.Transaction.insertOtherAssistanceMeasure(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    child: ChildId,
    update: OtherAssistanceMeasureUpdate,
): OtherAssistanceMeasureId =
    createUpdate<DatabaseTable.OtherAssistanceMeasure> {
            sql(
                """
INSERT INTO other_assistance_measure (child_id, modified, modified_by, valid_during, type)
VALUES (${bind(child)}, ${bind(now)}, ${bind(user.evakaUserId)}, ${bind(update.validDuring)}, ${bind(update.type)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .mapTo<OtherAssistanceMeasureId>()
        .single()

fun Database.Transaction.updateOtherAssistanceMeasure(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    id: OtherAssistanceMeasureId,
    update: OtherAssistanceMeasureUpdate
) =
    createUpdate<DatabaseTable.OtherAssistanceMeasure> {
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
    createUpdate<DatabaseTable.OtherAssistanceMeasure> {
            sql("DELETE FROM other_assistance_measure WHERE id = ${bind(id)}")
        }
        .execute()
