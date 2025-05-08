// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.InvoiceCorrectionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
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
    created_at,
    creator.id AS created_by_id,
    creator.name AS created_by_name,
    creator.type AS created_by_type,
    modified_at,
    modifier.id AS modified_by_id,
    modifier.name AS modified_by_name,
    modifier.type AS modified_by_type,
    invoice.id AS invoice_id,
    invoice.status AS invoice_status
FROM invoice_correction
JOIN evaka_user creator ON creator.id = invoice_correction.created_by
JOIN evaka_user modifier ON modifier.id = invoice_correction.modified_by
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
    correction: InvoiceCorrectionInsert,
    userId: EvakaUserId,
    now: HelsinkiDateTime,
): InvoiceCorrectionId = insertInvoiceCorrections(listOf(correction), userId, now).first()

fun Database.Transaction.insertInvoiceCorrections(
    corrections: Iterable<InvoiceCorrectionInsert>,
    userId: EvakaUserId,
    now: HelsinkiDateTime,
): List<InvoiceCorrectionId> {
    return prepareBatch(corrections) {
            sql(
                """
INSERT INTO invoice_correction (head_of_family_id, child_id, unit_id, product, period, amount, unit_price, description, note, created_by, modified_by, modified_at)
VALUES (
    ${bind { it.headOfFamilyId }},
    ${bind { it.childId }},
    ${bind { it.unitId }},
    ${bind { it.product }},
    ${bind { it.period }},
    ${bind { it.amount }},
    ${bind { it.unitPrice }},
    ${bind { it.description }},
    ${bind { it.note }},
    ${bind(userId)},
    ${bind(userId)},
    ${bind(now)}
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

data class InvoiceCorrectionModificationMetadata(
    val userId: EvakaUserId,
    val now: HelsinkiDateTime,
)

fun Database.Transaction.updateInvoiceCorrections(
    items: Iterable<InvoiceCorrectionUpdate>,
    modified: InvoiceCorrectionModificationMetadata?,
) =
    executeBatch(items) {
        sql(
            """
UPDATE invoice_correction
SET target_month = ${bind { it.targetMonth }}, amount = ${bind { it.amount }}, unit_price = ${bind { it.unitPrice }}
${if (modified != null) ", modified_by = ${bind(modified.userId)}, modified_at = ${bind(modified.now)}" else ""}
WHERE id = ${bind { it.id }}
"""
        )
    }
