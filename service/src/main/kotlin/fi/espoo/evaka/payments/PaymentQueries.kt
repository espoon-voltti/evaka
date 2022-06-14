// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.payments

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
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

fun Database.Transaction.insertPaymentsDrafts(payments: List<PaymentDraft>) {
    val batch = prepareBatch(
        """
        INSERT INTO payment (unit_id, period, amount, status)
        VALUES (:unitId, :period, :amount, 'DRAFT')
    """
    )
    payments.forEach { batch.bindKotlin(it).add() }
    batch.execute()
}

fun Database.Read.readPayments(): List<Payment> {
    return createQuery(
        """
            SELECT p.id, p.created, p.updated, unit_id, period, number, amount, status, payment_date, due_date, sent_at, sent_by
            FROM payment p
            JOIN daycare d on p.unit_id = d.id
            ORDER BY lower(period) DESC, d.name
        """
    )
        .mapTo<Payment>()
        .list()
}
