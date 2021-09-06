// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.messaging

import fi.espoo.evaka.invoicing.service.FeeDecisionService
import fi.espoo.evaka.invoicing.service.VoucherValueDecisionService
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
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

    fun runCreateFeeDecisionPdf(db: Database, msg: AsyncJob.NotifyFeeDecisionApproved) = db.transaction { tx ->
        val decisionId = msg.decisionId
        feeService.createFeeDecisionPdf(tx, decisionId)
        logger.info { "Successfully created fee decision pdf (id: $decisionId)." }
        asyncJobRunner.plan(tx, listOf(AsyncJob.NotifyFeeDecisionPdfGenerated(decisionId)))
    }

    fun runSendFeeDecisionPdf(db: Database, msg: AsyncJob.NotifyFeeDecisionPdfGenerated) = db.transaction { tx ->
        val decisionId = msg.decisionId
        feeService.sendDecision(tx, decisionId)
        logger.info { "Successfully sent fee decision msg to suomi.fi (id: $decisionId)." }
    }

    fun runCreateVoucherValueDecisionPdf(db: Database, msg: AsyncJob.NotifyVoucherValueDecisionApproved) = db.transaction { tx ->
        val decisionId = msg.decisionId
        valueDecisionService.createDecisionPdf(tx, decisionId)
        logger.info { "Successfully created voucher value decision pdf (id: $decisionId)." }
        asyncJobRunner.plan(tx, listOf(AsyncJob.NotifyVoucherValueDecisionPdfGenerated(decisionId)))
    }

    fun runSendVoucherValueDecisionPdf(db: Database, msg: AsyncJob.NotifyVoucherValueDecisionPdfGenerated) = db.transaction { tx ->
        val decisionId = msg.decisionId
        valueDecisionService.sendDecision(tx, decisionId)
        logger.info { "Successfully sent fee decision msg to suomi.fi (id: $decisionId)." }
    }
}
