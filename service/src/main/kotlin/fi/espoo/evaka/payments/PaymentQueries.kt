// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.payments

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PaymentId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
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
