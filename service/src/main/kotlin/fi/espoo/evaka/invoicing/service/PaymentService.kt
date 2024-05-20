// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.invoicing.data.confirmDraftPayments
import fi.espoo.evaka.invoicing.data.getMaxPaymentNumber
import fi.espoo.evaka.invoicing.data.readPaymentsByIdsWithFreshUnitData
import fi.espoo.evaka.invoicing.data.updateConfirmedPaymentsAsSent
import fi.espoo.evaka.invoicing.domain.PaymentIntegrationClient
import fi.espoo.evaka.invoicing.domain.PaymentStatus
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.PaymentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class PaymentService(
    private val integrationClient: PaymentIntegrationClient,
    private val featureConfig: FeatureConfig
) {
    fun sendPayments(
        tx: Database.Transaction,
        now: HelsinkiDateTime,
        user: AuthenticatedUser.Employee,
        paymentIds: List<PaymentId>,
        paymentDate: LocalDate,
        dueDate: LocalDate
    ) {
        val seriesStart =
            featureConfig.paymentNumberSeriesStart
                ?: throw Error("paymentNumberSeriesStart not configured")
        val payments = tx.readPaymentsByIdsWithFreshUnitData(paymentIds)

        val notConfirmed = payments.filterNot { it.status == PaymentStatus.CONFIRMED }
        if (notConfirmed.isNotEmpty()) {
            throw BadRequest("Some payments are not confirmed")
        }

        var nextPaymentNumber =
            tx.getMaxPaymentNumber().let { if (it >= seriesStart) it + 1 else seriesStart }
        val updatedPayments =
            payments.flatMap { payment ->
                // Skip payments whose unit has missing payment details
                val missingDetails =
                    listOf(
                            payment.unit.name,
                            payment.unit.businessId,
                            payment.unit.iban,
                            payment.unit.providerId
                        )
                        .any { it.isNullOrBlank() }
                if (missingDetails) {
                    logger.warn {
                        "Skipping payment ${payment.id} because unit ${payment.unit.id} has missing payment details"
                    }
                    return@flatMap listOf()
                }

                val updatedPayment =
                    payment.copy(
                        number = nextPaymentNumber,
                        paymentDate = paymentDate,
                        dueDate = dueDate,
                        sentBy = user.evakaUserId
                    )
                nextPaymentNumber += 1
                listOf(updatedPayment)
            }

        val sendResult = integrationClient.send(updatedPayments, tx)
        logger.info {
            "Successfully sent ${sendResult.succeeded.size} payments: ${
            sendResult.succeeded.map { it.id }.joinToString(", ")
            }"
        }
        logger.info {
            "Failed to send ${sendResult.failed.size} payments: ${
            sendResult.failed.map { it.id }.joinToString(", ")
            }"
        }
        tx.updateConfirmedPaymentsAsSent(sendResult.succeeded, now)
    }

    fun confirmPayments(
        tx: Database.Transaction,
        paymentIds: List<PaymentId>,
    ) {
        val payments = tx.readPaymentsByIdsWithFreshUnitData(paymentIds)

        val notDrafts = payments.filterNot { it.status == PaymentStatus.DRAFT }
        if (notDrafts.isNotEmpty()) {
            throw BadRequest("Some payments are not drafts")
        }

        tx.confirmDraftPayments(paymentIds)
    }
}
