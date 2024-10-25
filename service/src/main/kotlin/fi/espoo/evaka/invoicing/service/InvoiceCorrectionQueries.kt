// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.InvoiceCorrectionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.YearMonth
import kotlin.math.abs

private fun Database.Read.getInvoiceCorrections(where: Predicate): List<InvoiceCorrection> =
    createQuery {
            sql(
                """
SELECT
    invoice_correction.id,
    target_month,
    head_of_family_id,
    child_id,
    unit_id,
    product,
    period,
    amount,
    unit_price,
    description,
    note,
    invoice.id AS invoice_id,
    invoice.status AS invoice_status
FROM invoice_correction
LEFT JOIN LATERAL (
    SELECT i.id, i.status
    FROM invoice i
    JOIN invoice_row ir ON ir.invoice_id = i.id
    WHERE ir.correction_id = invoice_correction.id
    LIMIT 1
) invoice ON true
WHERE ${predicate(where.forTable("invoice_correction"))}
"""
            )
        }
        .toList()

fun Database.Read.getUnappliedInvoiceCorrections(): List<InvoiceCorrection> =
    getInvoiceCorrections(Predicate { where("$it.target_month IS NULL") })

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

data class InvoiceCorrectionInsert(
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
    return prepareBatch(corrections) {
            sql(
                """
INSERT INTO invoice_correction (head_of_family_id, child_id, unit_id, product, period, amount, unit_price, description, note)
VALUES (
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
    val targetMonth: YearMonth,
    val amount: Int,
    val unitPrice: Int,
)

fun Database.Transaction.updateInvoiceCorrections(items: Iterable<InvoiceCorrectionUpdate>) =
    executeBatch(items) {
        sql(
            """
UPDATE invoice_correction
SET target_month = ${bind { it.targetMonth }}, amount = ${bind { it.amount }}, unit_price = ${bind { it.unitPrice }}
WHERE id = ${bind { it.id }}
"""
        )
    }
