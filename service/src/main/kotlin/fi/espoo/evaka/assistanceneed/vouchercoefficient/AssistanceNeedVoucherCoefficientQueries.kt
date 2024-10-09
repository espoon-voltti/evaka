// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.vouchercoefficient

import fi.espoo.evaka.shared.AssistanceNeedVoucherCoefficientId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound

private val anvcSelectFields =
    """
    a.id,
    a.child_id,
    a.coefficient,
    a.validity_period,
    a.modified_at,
    e.id AS modified_by_id,
    e.name AS modified_by_name,
    e.type AS modified_by_type
"""

fun Database.Transaction.insertAssistanceNeedVoucherCoefficient(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    childId: ChildId,
    data: AssistanceNeedVoucherCoefficientRequest,
): AssistanceNeedVoucherCoefficient =
    createQuery {
            sql(
                """
WITH a AS (
    INSERT INTO assistance_need_voucher_coefficient (child_id, coefficient, validity_period, modified_at, modified_by)
    VALUES (${bind(childId)}, ${bind(data.coefficient)}, ${bind(data.validityPeriod)}, ${bind(now)}, ${bind(user.evakaUserId)})
    RETURNING id, child_id, coefficient, validity_period, modified_at, modified_by
) SELECT $anvcSelectFields FROM a LEFT JOIN evaka_user e ON a.modified_by = e.id
"""
            )
        }
        .exactlyOne()

fun Database.Read.getAssistanceNeedVoucherCoefficientById(
    id: AssistanceNeedVoucherCoefficientId
): AssistanceNeedVoucherCoefficient =
    createQuery {
            sql(
                """
SELECT $anvcSelectFields
FROM assistance_need_voucher_coefficient a
LEFT JOIN evaka_user e ON a.modified_by = e.id
WHERE a.id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull() ?: throw NotFound("Assistance need voucher coefficient $id not found")

fun Database.Read.getAssistanceNeedVoucherCoefficientsForChild(
    childId: ChildId
): List<AssistanceNeedVoucherCoefficient> =
    createQuery {
            sql(
                """
SELECT $anvcSelectFields
FROM assistance_need_voucher_coefficient a
LEFT JOIN evaka_user e ON a.modified_by = e.id
WHERE a.child_id = ${bind(childId)}
"""
            )
        }
        .toList()

fun Database.Transaction.updateAssistanceNeedVoucherCoefficient(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    id: AssistanceNeedVoucherCoefficientId,
    data: AssistanceNeedVoucherCoefficientRequest,
): AssistanceNeedVoucherCoefficient =
    createQuery {
            sql(
                """
WITH a AS (
    UPDATE assistance_need_voucher_coefficient
    SET coefficient = ${bind(data.coefficient)},
        validity_period = ${bind(data.validityPeriod)},
        modified_at = ${bind(now)},
        modified_by = ${bind(user.evakaUserId)}
    WHERE id = ${bind(id)}
    RETURNING id, child_id, coefficient, validity_period, modified_at, modified_by
) SELECT $anvcSelectFields FROM a LEFT JOIN evaka_user e ON a.modified_by = e.id
"""
            )
        }
        .exactlyOneOrNull() ?: throw NotFound("Assistance need voucher coefficient $id not found")

fun Database.Transaction.deleteAssistanceNeedVoucherCoefficient(
    id: AssistanceNeedVoucherCoefficientId
): AssistanceNeedVoucherCoefficient? =
    createQuery {
            sql(
                """
WITH a AS (
    DELETE FROM assistance_need_voucher_coefficient
    WHERE id = ${bind(id)}
    RETURNING id, child_id, coefficient, validity_period, modified_at, modified_by
) SELECT $anvcSelectFields FROM a LEFT JOIN evaka_user e ON a.modified_by = e.id
"""
            )
        }
        .exactlyOneOrNull()

fun Database.Read.getOverlappingAssistanceNeedVoucherCoefficientsForChild(
    childId: ChildId,
    range: FiniteDateRange,
): List<AssistanceNeedVoucherCoefficient> =
    createQuery {
            sql(
                """
SELECT $anvcSelectFields
FROM assistance_need_voucher_coefficient a
LEFT JOIN evaka_user e ON a.modified_by = e.id
WHERE a.child_id = ${bind(childId)}
  AND ${bind(range)} && a.validity_period
"""
            )
        }
        .toList()

/**
 * End a voucher coefficient if 1) placement has ended yesterday, or 2) there is a new placement,
 * but it's to a different unit.
 */
fun Database.Transaction.endOutdatedAssistanceNeedVoucherCoefficients(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
) {
    val today = now.toLocalDate()
    val yesterday = today.minusDays(1)
    execute {
        sql(
            """
UPDATE assistance_need_voucher_coefficient a
SET validity_period = daterange(lower(validity_period), ${bind(yesterday)}, '[]'),
    modified_at = ${bind(now)},
    modified_by = ${bind(user.evakaUserId)}
FROM placement pl
WHERE
    a.validity_period && daterange(${bind(yesterday)}, ${bind(today)}, '[]') AND
    daterange(pl.start_date, pl.end_date, '[]') @> ${bind(yesterday)} AND
    pl.child_id = a.child_id AND
    NOT EXISTS (
        SELECT FROM placement pl_new
        WHERE
            daterange(pl_new.start_date, pl_new.end_date, '[]') @> ${bind(today)} AND
            pl_new.child_id = a.child_id AND
            pl_new.unit_id = pl.unit_id
    )
"""
        )
    }
}
