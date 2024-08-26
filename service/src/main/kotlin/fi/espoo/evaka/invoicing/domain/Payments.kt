// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.invoicing.data.deletePaymentDraftsByDateRange
import fi.espoo.evaka.invoicing.data.insertPaymentDrafts
import fi.espoo.evaka.reports.REPORT_STATEMENT_TIMEOUT
import fi.espoo.evaka.reports.getLastSnapshotMonth
import fi.espoo.evaka.reports.getServiceVoucherReport
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.PaymentId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import mu.KotlinLogging
import org.jdbi.v3.core.mapper.Nested

val logger = KotlinLogging.logger {}

interface PaymentIntegrationClient {
    data class SendResult(
        val succeeded: List<Payment> = listOf(),
        val failed: List<Payment> = listOf(),
    )

    fun send(payments: List<Payment>, tx: Database.Read): SendResult

    class MockClient(private val jsonMapper: JsonMapper) : PaymentIntegrationClient {
        override fun send(payments: List<Payment>, tx: Database.Read): SendResult {
            logger.info(
                "Mock payment integration client got payments ${jsonMapper.writeValueAsString(payments)}"
            )
            return SendResult(succeeded = payments)
        }
    }

    class FailingClient() : PaymentIntegrationClient {
        override fun send(payments: List<Payment>, tx: Database.Read): SendResult {
            throw RuntimeException("Payments are not in use")
        }
    }
}

enum class PaymentStatus : DatabaseEnum {
    DRAFT,
    CONFIRMED,
    SENT;

    override val sqlType = "payment_status"
}

data class PaymentDraft(val unitId: DaycareId, val period: DateRange, val amount: Int)

fun createPaymentDrafts(tx: Database.Transaction) {
    val lastSnapshot =
        tx.getLastSnapshotMonth() ?: throw BadRequest("No voucher value report snapshot found")

    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
    tx.createUpdate { sql("LOCK TABLE payment") }.execute()

    val report =
        getServiceVoucherReport(
            tx,
            lastSnapshot.year,
            lastSnapshot.month,
            null,
            AccessControlFilter.PermitAll,
        )
    if (report.locked == null) throw BadRequest("Voucher value report is not locked")

    val period =
        lastSnapshot.let {
            val start = LocalDate.of(it.year, it.month, 1)
            val end = start.with(TemporalAdjusters.lastDayOfMonth())
            DateRange(start, end)
        }

    val payments =
        report.rows.map {
            PaymentDraft(unitId = it.unit.id, period = period, amount = it.monthlyPaymentSum)
        }

    tx.deletePaymentDraftsByDateRange(period)
    tx.insertPaymentDrafts(payments)
}

data class PaymentUnit(
    val id: DaycareId,
    val name: String,
    val businessId: String?,
    val iban: String?,
    val providerId: String?,
    val careType: Set<CareType>,
)

data class Payment(
    val id: PaymentId,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    @Nested("unit_") val unit: PaymentUnit,
    val number: Long?,
    val period: DateRange,
    val amount: Int,
    val status: PaymentStatus,
    val paymentDate: LocalDate?,
    val dueDate: LocalDate?,
    val sentAt: HelsinkiDateTime?,
    val sentBy: EvakaUserId?,
)
