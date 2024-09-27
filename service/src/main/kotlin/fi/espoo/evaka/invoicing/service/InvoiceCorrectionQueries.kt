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
import fi.espoo.evaka.shared.db.PredicateSql
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.time.YearMonth

private fun Database.Read.getInvoiceCorrections(where: Predicate) =
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
WHERE
    target_month IS NOT NULL AND
    ${predicate(where.forTable("invoice_correction"))}
"""
            )
        }
        .toList {
            InvoiceCorrection(
                id = column("id"),
                targetMonth = YearMonth.from(column<LocalDate>("target_month")),
                headOfFamilyId = column("head_of_family_id"),
                childId = column("child_id"),
                unitId = column("unit_id"),
                product = column("product"),
                period = column("period"),
                amount = column("amount"),
                unitPrice = column("unit_price"),
                description = column("description"),
                note = column("note"),
            )
        }

// TODO: indeksi
// CREATE INDEX idx$invoice_correction_target ON invoice_correction(target)
fun Database.Read.getInvoiceCorrectionsForMonth(
    month: YearMonth
): List<InvoiceGenerator.InvoiceCorrection> =
    getInvoiceCorrections(Predicate { where("$it.target_month = ${bind(month.atDay(1))}") }).map {
        InvoiceGenerator.InvoiceCorrection(
            id = it.id,
            headOfFamilyId = it.headOfFamilyId,
            childId = it.childId,
            unitId = it.unitId,
            product = it.product,
            period = it.period,
            amount = it.amount,
            unitPrice = it.unitPrice,
            description = it.description,
        )
    }

fun Database.Read.getInvoiceCorrectionsByIds(
    ids: Set<InvoiceCorrectionId>
): List<InvoiceCorrection> =
    getInvoiceCorrections(Predicate { where("$it.id = ANY(${bind(ids)})") })

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

    // TODO: This query has to scan all past invoice corrections
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
    val targetMonth: YearMonth,
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

fun Database.Transaction.insertInvoiceCorrections(corrections: Iterable<InvoiceCorrectionInsert>) =
    executeBatch(corrections) {
        sql(
            """
INSERT INTO invoice_correction (target_month, head_of_family_id, child_id, unit_id, product, period, amount, unit_price, description, note)
VALUES (
    ${bind { it.targetMonth.atDay(1) }},
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
"""
        )
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
