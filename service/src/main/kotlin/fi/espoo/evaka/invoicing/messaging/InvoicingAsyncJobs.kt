// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.messaging

import fi.espoo.evaka.invoicing.service.FeeDecisionService
import fi.espoo.evaka.invoicing.service.VoucherValueDecisionService
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyFeeDecisionApproved
import fi.espoo.evaka.shared.async.NotifyFeeDecisionPdfGenerated
import fi.espoo.evaka.shared.async.NotifyVoucherValueDecisionApproved
import fi.espoo.evaka.shared.async.NotifyVoucherValueDecisionPdfGenerated
import fi.espoo.evaka.shared.db.Database
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class InvoicingAsyncJobs(
    private val feeService: FeeDecisionService,
    private val valueDecisionService: VoucherValueDecisionService,
    private val asyncJobRunner: AsyncJobRunner
) {
    init {
        asyncJobRunner.notifyFeeDecisionApproved = ::runCreateFeeDecisionPdf
        asyncJobRunner.notifyFeeDecisionPdfGenerated = ::runSendFeeDecisionPdf
        asyncJobRunner.notifyVoucherValueDecisionApproved = ::runCreateVoucherValueDecisionPdf
        asyncJobRunner.notifyVoucherValueDecisionPdfGenerated = ::runSendVoucherValueDecisionPdf
    }

    fun runCreateFeeDecisionPdf(db: Database, msg: NotifyFeeDecisionApproved) = db.transaction { tx ->
        val decisionId = msg.decisionId
        feeService.createFeeDecisionPdf(tx, decisionId)
        logger.info { "Successfully created fee decision pdf (id: $decisionId)." }
        asyncJobRunner.plan(tx, listOf(NotifyFeeDecisionPdfGenerated(decisionId)))
    }

    fun runSendFeeDecisionPdf(db: Database, msg: NotifyFeeDecisionPdfGenerated) = db.transaction { tx ->
        val decisionId = msg.decisionId
        feeService.sendDecision(tx, decisionId)
        logger.info { "Successfully sent fee decision msg to suomi.fi (id: $decisionId)." }
    }

    fun runCreateVoucherValueDecisionPdf(db: Database, msg: NotifyVoucherValueDecisionApproved) = db.transaction { tx ->
        val decisionId = msg.decisionId
        valueDecisionService.createDecisionPdf(tx, decisionId)
        logger.info { "Successfully created voucher value decision pdf (id: $decisionId)." }
        asyncJobRunner.plan(tx, listOf(NotifyVoucherValueDecisionPdfGenerated(decisionId)))
    }

    fun runSendVoucherValueDecisionPdf(db: Database, msg: NotifyVoucherValueDecisionPdfGenerated) = db.transaction { tx ->
        val decisionId = msg.decisionId
        valueDecisionService.sendDecision(tx, decisionId)
        logger.info { "Successfully sent fee decision msg to suomi.fi (id: $decisionId)." }
    }
}
