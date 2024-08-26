// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.invoicing.controller.PaymentDistinctiveParams
import fi.espoo.evaka.invoicing.controller.PaymentSortParam
import fi.espoo.evaka.invoicing.controller.SearchPaymentsRequest
import fi.espoo.evaka.invoicing.domain.Payment
import fi.espoo.evaka.invoicing.domain.PaymentDraft
import fi.espoo.evaka.invoicing.domain.PaymentStatus
import fi.espoo.evaka.shared.PaymentId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.mapToPaged

fun Database.Transaction.deletePaymentDraftsByDateRange(range: DateRange) {
    createUpdate { sql("DELETE FROM payment WHERE status = 'DRAFT' AND period && ${bind(range)}") }
        .execute()
}

fun Database.Transaction.insertPaymentDrafts(payments: List<PaymentDraft>) {
    executeBatch(payments) {
        sql(
            """
INSERT INTO payment (unit_id, unit_name, period, amount, status)
SELECT ${bind { it.unitId }}, d.name, ${bind { it.period }}, ${bind { it.amount }}, 'DRAFT'
FROM daycare d
WHERE d.id = ${bind { it.unitId }}
"""
        )
    }
}

fun Database.Read.readPaymentsByIdsWithFreshUnitData(ids: List<PaymentId>): List<Payment> {
    return createQuery {
            sql(
                """
SELECT 
    p.id, p.created, p.updated, p.unit_id, 
    d.name AS unit_name, d.business_id AS unit_business_id, d.iban AS unit_iban, d.provider_id AS unit_provider_id, d.type as unit_care_type,
    p.period, p.number, p.amount, p.status, p.payment_date, p.due_date, p.sent_at, p.sent_by
FROM payment p
JOIN daycare d ON d.id = p.unit_id
WHERE p.id = ANY(${bind(ids)})
"""
            )
        }
        .toList<Payment>()
}

fun Database.Read.readPayments(): List<Payment> {
    return createQuery {
            sql(
                """
SELECT
    p.id, p.created, p.updated,
    p.unit_id, p.unit_name, p.unit_business_id, p.unit_iban, p.unit_provider_id,
    p.period, p.number, p.amount, p.status, p.payment_date, p.due_date, p.sent_at, p.sent_by,
    d.type as unit_care_type
FROM payment p
JOIN daycare d ON d.id = p.unit_id
ORDER BY period DESC, unit_name
"""
            )
        }
        .toList<Payment>()
}

data class PagedPayments(val data: List<Payment>, val total: Int, val pages: Int)

fun Database.Read.searchPayments(params: SearchPaymentsRequest): PagedPayments {
    val orderBy =
        when (params.sortBy) {
            PaymentSortParam.UNIT -> "lower(p.unit_name)"
            PaymentSortParam.PERIOD -> "p.period"
            PaymentSortParam.CREATED -> "p.created"
            PaymentSortParam.NUMBER -> "p.number"
            PaymentSortParam.AMOUNT -> "p.amount"
        }
    val ascDesc = params.sortDirection.name
    val includeMissingDetails =
        params.distinctions.contains(PaymentDistinctiveParams.MISSING_PAYMENT_DETAILS)

    return createQuery {
            sql(
                """
SELECT
    p.id, p.created, p.updated,
    p.unit_id, p.unit_name,
    CASE WHEN p.status = 'SENT' THEN unit_business_id ELSE d.business_id END AS unit_business_id,
    CASE WHEN p.status = 'SENT' THEN unit_iban ELSE d.iban END AS unit_iban,
    CASE WHEN p.status = 'SENT' THEN unit_provider_id ELSE d.provider_id END AS unit_provider_id,
    d.type AS unit_care_type,
    p.period, p.number, p.amount, p.status, p.payment_date, p.due_date, p.sent_at, p.sent_by,
    count(*) OVER () AS count
FROM payment p
JOIN daycare d ON d.id = p.unit_id
JOIN care_area a ON a.id = d.care_area_id
WHERE
    (${bind(params.searchTerms)} = '' OR p.number::text LIKE ${bind(params.searchTerms)} || '%') AND
    (cardinality(${bind(params.area)}::text[]) = 0 OR a.short_name = ANY(${bind(params.area)}::text[])) AND
    (${bind(params.unit)}::uuid IS NULL OR p.unit_id = ${bind(params.unit)}::uuid) AND
    (NOT ${bind(includeMissingDetails)} OR (d.business_id = '' OR d.iban = '' OR d.provider_id = '')) AND
    p.status = ${bind(params.status)}::payment_status AND
    (
        p.payment_date IS NULL AND ${bind(params.paymentDateStart)} IS NULL AND ${bind(params.paymentDateEnd)} IS NULL OR
        between_start_and_end(daterange(${bind(params.paymentDateStart)}, ${bind(params.paymentDateEnd)}, '[]'), p.payment_date)
    )
ORDER BY $orderBy $ascDesc
LIMIT ${bind(params.pageSize)} OFFSET ${bind(params.pageSize)} * (${bind(params.page)} - 1)
"""
            )
        }
        .mapToPaged(::PagedPayments, params.pageSize)
}

fun Database.Read.getMaxPaymentNumber(): Long {
    return createQuery { sql("SELECT max(number) FROM payment") }.exactlyOneOrNull<Long>() ?: 0
}

fun Database.Transaction.deleteDraftPayments(draftIds: List<PaymentId>) {
    if (draftIds.isEmpty()) return

    createUpdate {
            sql(
                """
                DELETE FROM payment
                WHERE status = ${bind(PaymentStatus.DRAFT)}::payment_status
                AND id = ANY (${bind(draftIds)})
                """
            )
        }
        .execute()
}

fun Database.Transaction.updateConfirmedPaymentsAsSent(
    payments: List<Payment>,
    now: HelsinkiDateTime,
) {
    executeBatch(payments) {
        sql(
            """
UPDATE payment SET
    status = 'SENT',
    number = ${bind { it.number }},
    payment_date = ${bind { it.paymentDate }},
    due_date = ${bind { it.dueDate }},
    sent_at = ${bind(now)},
    sent_by = ${bind { it.sentBy }},
    unit_name = ${bind { it.unit.name }},
    unit_business_id = ${bind { it.unit.businessId }},
    unit_iban = ${bind { it.unit.iban }},
    unit_provider_id = ${bind { it.unit.providerId }}
WHERE id = ${bind { it.id }}
"""
        )
    }
}

fun Database.Transaction.confirmDraftPayments(draftIds: List<PaymentId>) {
    if (draftIds.isEmpty()) return

    execute {
        sql(
            """
                UPDATE payment set status = ${bind(PaymentStatus.CONFIRMED)} 
                WHERE status = ${bind(PaymentStatus.DRAFT)}
                AND id = ANY (${bind(draftIds)})
                """
        )
    }
}

fun Database.Transaction.revertPaymentsToDrafts(paymentIds: List<PaymentId>) {
    if (paymentIds.isEmpty()) return

    execute {
        sql(
            """
                UPDATE payment set status = ${bind(PaymentStatus.DRAFT)} 
                WHERE status = ${bind(PaymentStatus.CONFIRMED)}
                AND id = ANY (${bind(paymentIds)})
                """
        )
    }
}
