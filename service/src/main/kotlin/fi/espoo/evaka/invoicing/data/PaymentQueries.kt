// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.invoicing.controller.PaymentDistinctiveParams
import fi.espoo.evaka.invoicing.controller.PaymentSortParam
import fi.espoo.evaka.invoicing.controller.SearchPaymentsRequest
import fi.espoo.evaka.invoicing.domain.Payment
import fi.espoo.evaka.invoicing.domain.PaymentDraft
import fi.espoo.evaka.invoicing.domain.PaymentUnit
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.PaymentId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.mapToPaged
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo

fun Database.Transaction.deletePaymentDraftsByDateRange(range: DateRange) {
    createUpdate("DELETE FROM payment WHERE status = 'DRAFT' AND period && :range")
        .bind("range", range)
        .execute()
}

fun Database.Read.getLastPaymentNumber(): Int? {
    return createQuery("""SELECT max(number) FROM payment""").mapTo<Int>().firstOrNull()
}

fun Database.Transaction.insertPaymentDrafts(payments: List<PaymentDraft>) {
    val batch = prepareBatch(
        """
        INSERT INTO payment (unit_id, unit_name, period, amount, status)
        SELECT :unitId, d.name, :period, :amount, 'DRAFT'
        FROM daycare d
        WHERE d.id = :unitId
    """
    )
    payments.forEach { batch.bindKotlin(it).add() }
    batch.execute()
}

fun Database.Read.readPaymentsByIds(ids: List<PaymentId>): List<Payment> {
    return createQuery(
        """
        SELECT 
            id, created, updated,
            unit_id, unit_name, unit_business_id, unit_iban, unit_provider_id,
            period, number, amount, status, payment_date, due_date, sent_at, sent_by
        FROM payment p
        WHERE id = ANY(:ids)
        """
    )
        .bind("ids", ids.toTypedArray())
        .mapTo<Payment>()
        .list()
}

fun Database.Read.readPayments(): List<Payment> {
    return createQuery(
        """
            SELECT
                id, created, updated,
                unit_id, unit_name, unit_business_id, unit_iban, unit_provider_id,
                period, number, amount, status, payment_date, due_date, sent_at, sent_by
            FROM payment
            ORDER BY lower(period) DESC, unit_name
        """
    )
        .mapTo<Payment>()
        .list()
}

fun Database.Read.searchPayments(params: SearchPaymentsRequest): Paged<Payment> {
    val orderBy = when (params.sortBy) {
        PaymentSortParam.UNIT -> "lower(p.unit_name)"
        PaymentSortParam.PERIOD -> "lower(p.period)"
        PaymentSortParam.CREATED -> "p.created"
        PaymentSortParam.NUMBER -> "p.number"
        PaymentSortParam.AMOUNT -> "p.amount"
    }
    val ascDesc = params.sortDirection.name
    val includeMissingDetails = params.distinctions.contains(PaymentDistinctiveParams.MISSING_PAYMENT_DETAILS)

    return createQuery(
        """
            SELECT
                p.id, p.created, p.updated,
                p.unit_id, p.unit_name,
                CASE WHEN p.status = 'SENT' THEN unit_business_id ELSE d.business_id END AS unit_business_id,
                CASE WHEN p.status = 'SENT' THEN unit_iban ELSE d.iban END AS unit_iban,
                CASE WHEN p.status = 'SENT' THEN unit_provider_id ELSE d.provider_id END AS unit_provider_id,
                p.period, p.number, p.amount, p.status, p.payment_date, p.due_date, p.sent_at, p.sent_by,
                count(*) OVER () AS count
            FROM payment p
            JOIN daycare d ON d.id = p.unit_id
            JOIN care_area a ON a.id = d.care_area_id
            WHERE
                (:searchTerms = '' OR p.number::text LIKE '%' || :searchTerms) AND
                (cardinality(:area::text[]) = 0 OR a.short_name = ANY(:area::text[])) AND
                (:unit::uuid IS NULL OR p.unit_id = :unit::uuid) AND
                (NOT :includeMissingDetails OR (d.business_id = '' OR d.iban = '' OR d.provider_id = '')) AND
                p.status = :status::payment_status AND
                (
                    p.payment_date IS NULL AND :paymentDateStart IS NULL AND :paymentDateEnd IS NULL OR
                    between_start_and_end(daterange(:paymentDateStart, :paymentDateEnd, '[]'), p.payment_date)
                )
            ORDER BY $orderBy $ascDesc
            LIMIT :pageSize OFFSET :pageSize * (:page - 1)
        """
    )
        .bind("searchTerms", params.searchTerms)
        .bind("area", params.area.toTypedArray())
        .bindNullable("unit", params.unit)
        .bind("includeMissingDetails", includeMissingDetails)
        .bind("status", params.status)
        .bindNullable("paymentDateStart", params.paymentDateStart)
        .bindNullable("paymentDateEnd", params.paymentDateEnd)
        .bind("page", params.page)
        .bind("pageSize", params.pageSize)
        .mapToPaged(params.pageSize)
}

fun Database.Read.getMaxPaymentNumber(): Long {
    return createQuery("SELECT max(number) FROM payment").mapTo<Long>().firstOrNull() ?: 0
}

fun Database.Transaction.updatePaymentDraftsAsSent(payments: List<Payment>, now: HelsinkiDateTime) {
    val batch = prepareBatch(
        """
        UPDATE payment SET
            status = 'SENT',
            number = :number,
            payment_date = :paymentDate,
            due_date = :dueDate,
            sent_at = :now,
            sent_by = :sentBy,
            unit_name = :unit.name,
            unit_business_id = :unit.businessId,
            unit_iban = :unit.iban,
            unit_provider_id = :unit.providerId
        WHERE id = :id
        """
    )
    payments.forEach {
        batch.bind("now", now).bindKotlin(it).add()
    }
    batch.execute()
}

fun Database.Read.getPaymentUnitsByIds(ids: List<DaycareId>): List<PaymentUnit> {
    return createQuery(
        """
        SELECT id, name, business_id, iban, provider_id
        FROM daycare
        WHERE id = ANY(:ids)
        """
    )
        .bind("ids", ids.toTypedArray())
        .mapTo<PaymentUnit>()
        .list()
}
