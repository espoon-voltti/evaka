// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.messaging

import fi.espoo.evaka.invoicing.service.FeeDecisionService
import fi.espoo.evaka.invoicing.service.VoucherValueDecisionService
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class InvoicingAsyncJobs(
    private val feeService: FeeDecisionService,
    private val valueDecisionService: VoucherValueDecisionService,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>
) {
    init {
        asyncJobRunner.registerHandler(::runCreateFeeDecisionPdf)
        asyncJobRunner.registerHandler(::runSendFeeDecisionPdf)
        asyncJobRunner.registerHandler(::runCreateVoucherValueDecisionPdf)
        asyncJobRunner.registerHandler(::runSendVoucherValueDecisionPdf)
    }

    fun runCreateFeeDecisionPdf(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.NotifyFeeDecisionApproved
    ) = db.transaction { tx ->
        val decisionId = msg.decisionId
        feeService.createFeeDecisionPdf(tx, decisionId)
        logger.info { "Successfully created fee decision pdf (id: $decisionId)." }
        asyncJobRunner.plan(
            tx,
            listOf(AsyncJob.NotifyFeeDecisionPdfGenerated(decisionId)),
            runAt = clock.now()
        )
    }

    fun runSendFeeDecisionPdf(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.NotifyFeeDecisionPdfGenerated
    ) = db.transaction { tx ->
        val decisionId = msg.decisionId
        feeService.sendDecision(tx, clock, decisionId).let { sent ->
            logger.info {
                if (sent) {
                    "Successfully sent fee decision msg to suomi.fi (id: $decisionId)."
                } else {
                    "Marked fee decision as requiring manual sending (id: $decisionId)."
                }
            }
        }
    }

    fun runCreateVoucherValueDecisionPdf(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.NotifyVoucherValueDecisionApproved
    ) = db.transaction { tx ->
        val decisionId = msg.decisionId
        valueDecisionService.createDecisionPdf(tx, decisionId)
        logger.info { "Successfully created voucher value decision pdf (id: $decisionId)." }
        asyncJobRunner.plan(
            tx,
            listOf(AsyncJob.NotifyVoucherValueDecisionPdfGenerated(decisionId)),
            runAt = clock.now()
        )
    }

    fun runSendVoucherValueDecisionPdf(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.NotifyVoucherValueDecisionPdfGenerated
    ) = db.transaction { tx ->
        val decisionId = msg.decisionId
        valueDecisionService.sendDecision(tx, clock, decisionId).let { sent ->
            logger.info {
                if (sent) {
                    "Successfully sent voucher value decision msg to suomi.fi (id: $decisionId)."
                } else {
                    "Marked voucher value decision as requiring manual sending (id: $decisionId)."
                }
            }
        }
    }
}
