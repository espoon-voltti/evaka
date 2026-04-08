// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.domain

import evaka.core.daycare.CareType
import evaka.core.invoicing.data.deletePaymentDraftsByDateRange
import evaka.core.invoicing.data.insertPaymentDrafts
import evaka.core.invoicing.data.readPaymentUnits
import evaka.core.reports.REPORT_STATEMENT_TIMEOUT
import evaka.core.reports.getLastSnapshotMonth
import evaka.core.reports.getServiceVoucherReport
import evaka.core.shared.DaycareId
import evaka.core.shared.EvakaUserId
import evaka.core.shared.PaymentId
import evaka.core.shared.db.Database
import evaka.core.shared.db.DatabaseEnum
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.security.actionrule.AccessControlFilter
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import org.jdbi.v3.core.mapper.Nested
import tools.jackson.databind.json.JsonMapper

val logger = KotlinLogging.logger {}

interface PaymentIntegrationClient {
    data class SendResult(
        val succeeded: List<Payment> = listOf(),
        val failed: List<Payment> = listOf(),
    )

    fun send(payments: List<Payment>, tx: Database.Read): SendResult

    class MockClient(private val jsonMapper: JsonMapper) : PaymentIntegrationClient {
        override fun send(payments: List<Payment>, tx: Database.Read): SendResult {
            logger.info {
                "Mock payment integration client got payments ${jsonMapper.writeValueAsString(payments)}"
            }
            return SendResult(succeeded = payments)
        }
    }

    class FailingClient : PaymentIntegrationClient {
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
            FiniteDateRange(start, end)
        }

    val unitsAlreadyInProgress =
        tx.readPaymentUnits(
            period = period,
            status = listOf(PaymentStatus.CONFIRMED, PaymentStatus.SENT),
        )
    val payments =
        report.rows
            .filterNot { unitsAlreadyInProgress.contains(it.unit.id) }
            .map {
                PaymentDraft(
                    unitId = it.unit.id,
                    period = period.asDateRange(),
                    amount = it.monthlyPaymentSum,
                )
            }

    tx.deletePaymentDraftsByDateRange(period.asDateRange())
    tx.insertPaymentDrafts(payments)
}

data class PaymentUnit(
    val id: DaycareId,
    val name: String,
    val businessId: String?,
    val iban: String?,
    val providerId: String?,
    val partnerCode: String?,
    val careType: Set<CareType>,
    val costCenter: String?,
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
