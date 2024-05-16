// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.vouchercoefficient

import fi.espoo.evaka.shared.AssistanceNeedVoucherCoefficientId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound

fun Database.Transaction.insertAssistanceNeedVoucherCoefficient(
    childId: ChildId,
    data: AssistanceNeedVoucherCoefficientRequest
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
    data: AssistanceNeedVoucherCoefficientRequest
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
    range: FiniteDateRange
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
