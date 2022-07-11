// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.vouchercoefficient

import fi.espoo.evaka.shared.AssistanceNeedVoucherCoefficientId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo

fun Database.Transaction.insertAssistanceNeedVoucherCoefficient(
    childId: ChildId,
    data: AssistanceNeedVoucherCoefficientRequest
): AssistanceNeedVoucherCoefficient {
    //language=sql
    val sql =
        """
        INSERT INTO assistance_need_voucher_coefficient (child_id, coefficient, validity_period)
        VALUES (:childId, :coefficient, :validityPeriod)
        RETURNING id, child_id, coefficient, validity_period
        """.trimIndent()

    return createQuery(sql)
        .bindKotlin(data)
        .bind("childId", childId)
        .mapTo<AssistanceNeedVoucherCoefficient>()
        .first()
}

fun Database.Read.getAssistanceNeedVoucherCoefficientById(id: AssistanceNeedVoucherCoefficientId): AssistanceNeedVoucherCoefficient {
    //language=sql
    val sql =
        """
        SELECT id, child_id, coefficient, validity_period
        FROM assistance_need_voucher_coefficient
        WHERE id = :id
        """.trimIndent()
    return createQuery(sql)
        .bind("id", id)
        .mapTo<AssistanceNeedVoucherCoefficient>()
        .firstOrNull() ?: throw NotFound("Assistance need voucher coefficient $id not found")
}

fun Database.Read.getAssistanceNeedVoucherCoefficientsForChild(childId: ChildId): List<AssistanceNeedVoucherCoefficient> {
    //language=sql
    val sql =
        """
        SELECT id, child_id, coefficient, validity_period
        FROM assistance_need_voucher_coefficient
        WHERE child_id = :childId
        """.trimIndent()
    return createQuery(sql)
        .bind("childId", childId)
        .mapTo<AssistanceNeedVoucherCoefficient>()
        .list()
}

fun Database.Transaction.updateAssistanceNeedVoucherCoefficient(
    id: AssistanceNeedVoucherCoefficientId,
    data: AssistanceNeedVoucherCoefficientRequest
): AssistanceNeedVoucherCoefficient {
    //language=sql
    val sql =
        """
        UPDATE assistance_need_voucher_coefficient
        SET coefficient = :coefficient,
            validity_period = :validityPeriod
        WHERE id = :id
        RETURNING id, child_id, coefficient, validity_period
        """.trimIndent()

    return createQuery(sql)
        .bindKotlin(data)
        .bind("id", id)
        .mapTo<AssistanceNeedVoucherCoefficient>()
        .firstOrNull() ?: throw NotFound("Assistance need voucher coefficient $id not found")
}

fun Database.Transaction.deleteAssistanceNeedVoucherCoefficient(id: AssistanceNeedVoucherCoefficientId): Boolean {
    val sql =
        """
        DELETE FROM assistance_need_voucher_coefficient
        WHERE id = :id
        RETURNING id
        """.trimIndent()
    return createQuery(sql)
        .bind("id", id)
        .mapTo<AssistanceNeedVoucherCoefficientId>()
        .firstOrNull() != null
}

fun Database.Read.getOverlappingAssistanceNeedVoucherCoefficientsForChild(
    childId: ChildId,
    range: FiniteDateRange
): List<AssistanceNeedVoucherCoefficient> {
    //language=sql
    val sql =
        """
        SELECT id, child_id, coefficient, validity_period
        FROM assistance_need_voucher_coefficient
        WHERE child_id = :childId
          AND :range && validity_period
        """.trimIndent()
    return createQuery(sql)
        .bind("childId", childId)
        .bind("range", range)
        .mapTo<AssistanceNeedVoucherCoefficient>()
        .list()
}
