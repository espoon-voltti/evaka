// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.vouchercoefficient

import fi.espoo.evaka.shared.AssistanceNeedVoucherCoefficientId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import java.time.LocalDate

fun Database.Transaction.insertAssistanceNeedVoucherCoefficient(
    childId: ChildId,
    data: AssistanceNeedVoucherCoefficientRequest,
): AssistanceNeedVoucherCoefficient =
    createQuery {
            sql(
                """
INSERT INTO assistance_need_voucher_coefficient (child_id, coefficient, validity_period)
VALUES (${bind(childId)}, ${bind(data.coefficient)}, ${bind(data.validityPeriod)})
RETURNING id, child_id, coefficient, validity_period
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
SELECT id, child_id, coefficient, validity_period
FROM assistance_need_voucher_coefficient
WHERE id = ${bind(id)}
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
SELECT id, child_id, coefficient, validity_period
FROM assistance_need_voucher_coefficient
WHERE child_id = ${bind(childId)}
"""
            )
        }
        .toList()

fun Database.Transaction.updateAssistanceNeedVoucherCoefficient(
    id: AssistanceNeedVoucherCoefficientId,
    data: AssistanceNeedVoucherCoefficientRequest,
): AssistanceNeedVoucherCoefficient =
    createQuery {
            sql(
                """
UPDATE assistance_need_voucher_coefficient
SET coefficient = ${bind(data.coefficient)},
    validity_period = ${bind(data.validityPeriod)}
WHERE id = ${bind(id)}
RETURNING id, child_id, coefficient, validity_period
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
DELETE FROM assistance_need_voucher_coefficient
WHERE id = ${bind(id)}
RETURNING id, child_id, coefficient, validity_period
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
SELECT id, child_id, coefficient, validity_period
FROM assistance_need_voucher_coefficient
WHERE child_id = ${bind(childId)}
  AND ${bind(range)} && validity_period
"""
            )
        }
        .toList()

/**
 * End a voucher coefficient if 1) placement has ended yesterday, or 2) there is a new placement,
 * but it's to a different unit.
 */
fun Database.Transaction.endOutdatedAssistanceNeedVoucherCoefficients(today: LocalDate) {
    val yesterday = today.minusDays(1)
    execute {
        sql(
            """
UPDATE assistance_need_voucher_coefficient a
SET validity_period = daterange(lower(validity_period), ${bind(yesterday)}, '[]')
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
