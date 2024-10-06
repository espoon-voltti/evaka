// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.invoicing.data.getFirstUninvoicedMonth
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.InvoiceCorrectionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.db.PredicateSql
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.YearMonth
import kotlin.math.abs

private fun Database.Read.getInvoiceCorrections(where: Predicate): List<InvoiceCorrection> =
    createQuery {
            sql(
                """
SELECT
    id,
    target_month,
    head_of_family_id,
    child_id,
    unit_id,
    product,
    period,
    amount,
    unit_price,
    description,
    note
FROM invoice_correction
WHERE ${predicate(where.forTable("invoice_correction"))}
"""
            )
        }
        .toList()

fun Database.Read.getInvoiceCorrectionsForMonth(month: YearMonth): List<InvoiceCorrection> =
    getInvoiceCorrections(Predicate { where("$it.target_month = ${bind(month)}") })

fun Database.Read.getInvoiceCorrectionsByIds(
    ids: Set<InvoiceCorrectionId>
): List<InvoiceCorrection> =
    getInvoiceCorrections(Predicate { where("$it.id = ANY(${bind(ids)})") })

fun Database.Read.getInvoiceCorrectionsForHeadOfFamily(
    personId: PersonId
): List<InvoiceCorrection> =
    getInvoiceCorrections(where = Predicate { where("$it.head_of_family_id = ${bind(personId)}") })
        .sortedWith(
            compareByDescending<InvoiceCorrection> { it.targetMonth }
                .thenByDescending { abs(it.amount * it.unitPrice) }
        )

fun Database.Transaction.movePastUnappliedInvoiceCorrections(
    headOfFamilyIds: Set<PersonId>?,
    targetMonth: YearMonth,
) {
    val firstOfMonth = targetMonth.atDay(1)
    val headOfFamilyPredicate =
        if (headOfFamilyIds != null) {
            PredicateSql { where("head_of_family_id = ANY (${bind(headOfFamilyIds)})") }
        } else {
            PredicateSql.alwaysTrue()
        }

    execute {
        sql(
            """
UPDATE invoice_correction
SET target_month = ${bind(firstOfMonth)}
WHERE
    target_month < ${bind(firstOfMonth)} AND
    ${predicate(headOfFamilyPredicate)} AND
    NOT EXISTS (SELECT FROM invoice_row WHERE correction_id = invoice_correction.id)
"""
        )
    }
}

data class InvoiceCorrectionInsert(
    val targetMonth: YearMonth?,
    val headOfFamilyId: PersonId,
    val childId: ChildId,
    val unitId: DaycareId,
    val product: ProductKey,
    val period: FiniteDateRange,
    val amount: Int,
    val unitPrice: Int,
    val description: String,
    val note: String,
)

fun Database.Transaction.insertInvoiceCorrection(
    correction: InvoiceCorrectionInsert
): InvoiceCorrectionId = insertInvoiceCorrections(listOf(correction)).first()

fun Database.Transaction.insertInvoiceCorrections(
    corrections: Iterable<InvoiceCorrectionInsert>
): List<InvoiceCorrectionId> {
    val defaultTargetMonth =
        if (corrections.any { it.targetMonth == null }) {
            getFirstUninvoicedMonth()
        } else null // not needed

    return prepareBatch(corrections) {
            sql(
                """
INSERT INTO invoice_correction (target_month, head_of_family_id, child_id, unit_id, product, period, amount, unit_price, description, note)
VALUES (
    ${bind { it.targetMonth ?: defaultTargetMonth }},
    ${bind { it.headOfFamilyId }},
    ${bind { it.childId }},
    ${bind { it.unitId }},
    ${bind { it.product }},
    ${bind { it.period }},
    ${bind { it.amount }},
    ${bind { it.unitPrice }},
    ${bind { it.description }},
    ${bind { it.note }}
)
RETURNING id
"""
            )
        }
        .executeAndReturn()
        .toList()
}

data class InvoiceCorrectionUpdate(
    val id: InvoiceCorrectionId,
    val amount: Int,
    val unitPrice: Int,
)

fun Database.Transaction.updateInvoiceCorrections(items: Iterable<InvoiceCorrectionUpdate>) =
    executeBatch(items) {
        sql(
            """
UPDATE invoice_correction
SET amount = ${bind { it.amount }}, unit_price = ${bind { it.unitPrice }}
WHERE id = ${bind { it.id }}
"""
        )
    }
