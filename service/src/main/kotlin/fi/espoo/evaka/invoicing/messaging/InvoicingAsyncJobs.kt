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
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
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

    fun runCreateFeeDecisionPdf(h: Handle, msg: NotifyFeeDecisionApproved) {
        val decisionId = msg.decisionId
        feeService.createFeeDecisionPdf(h, decisionId)
        logger.info { "Successfully created fee decision pdf (id: $decisionId)." }
        asyncJobRunner.plan(h, listOf(NotifyFeeDecisionPdfGenerated(decisionId)))
    }

    fun runSendFeeDecisionPdf(h: Handle, msg: NotifyFeeDecisionPdfGenerated) {
        val decisionId = msg.decisionId
        feeService.sendDecision(h, decisionId)
        logger.info { "Successfully sent fee decision msg to suomi.fi (id: $decisionId)." }
    }

    fun runCreateVoucherValueDecisionPdf(h: Handle, msg: NotifyVoucherValueDecisionApproved) {
        val decisionId = msg.decisionId
        valueDecisionService.createDecisionPdf(h, decisionId)
        logger.info { "Successfully created voucher value decision pdf (id: $decisionId)." }
        asyncJobRunner.plan(h, listOf(NotifyVoucherValueDecisionPdfGenerated(decisionId)))
    }

    fun runSendVoucherValueDecisionPdf(h: Handle, msg: NotifyVoucherValueDecisionPdfGenerated) {
        val decisionId = msg.decisionId
        valueDecisionService.sendDecision(h, decisionId)
        logger.info { "Successfully sent fee decision msg to suomi.fi (id: $decisionId)." }
    }
}
