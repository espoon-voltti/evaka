// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.payments

import fi.espoo.evaka.reports.REPORT_STATEMENT_TIMEOUT
import fi.espoo.evaka.reports.getLastSnapshotMonth
import fi.espoo.evaka.reports.getServiceVoucherReport
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PaymentId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class PaymentStatus : DatabaseEnum {
    DRAFT,
    SENT;

    override val sqlType = "payment_status"
}

data class PaymentDraft(
    val unitId: DaycareId,
    val period: DateRange,
    val amount: Int,
)

fun createPaymentDrafts(tx: Database.Transaction) {
    val lastSnapshot = tx.getLastSnapshotMonth() ?: throw BadRequest("No voucher value report snapshot found")

    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
    tx.createUpdate("LOCK TABLE payment").execute()

    val report = getServiceVoucherReport(tx, lastSnapshot.year, lastSnapshot.month, null, null)
    if (report.locked == null) throw BadRequest("Voucher value report is not locked")

    val period = lastSnapshot.let {
        val start = LocalDate.of(it.year, it.month, 1)
        val end = start.with(TemporalAdjusters.lastDayOfMonth())
        DateRange(start, end)
    }

    val payments = report.rows.map {
        PaymentDraft(unitId = it.unit.id, period = period, amount = it.monthlyPaymentSum)
    }

    tx.deletePaymentDraftsByDateRange(period)
    tx.insertPaymentsDrafts(payments)
}

data class Payment(
    val id: PaymentId,
    val unitId: DaycareId,
    val number: Long?,
    val period: DateRange,
    val amount: Int,
    val status: PaymentStatus,
    val paymentDate: LocalDate?,
    val dueDate: LocalDate?,
    val sentAt: HelsinkiDateTime?,
    val sentBy: EmployeeId?,
)
