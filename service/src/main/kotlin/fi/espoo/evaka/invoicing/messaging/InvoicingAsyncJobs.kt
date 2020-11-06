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
import fi.espoo.evaka.shared.db.transaction
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class InvoicingAsyncJobs(
    private val feeService: FeeDecisionService,
    private val valueDecisionService: VoucherValueDecisionService,
    private val jdbi: Jdbi,
    private val asyncJobRunner: AsyncJobRunner
) {
    init {
        asyncJobRunner.notifyFeeDecisionApproved = ::runCreateFeeDecisionPdf
        asyncJobRunner.notifyFeeDecisionPdfGenerated = ::runSendFeeDecisionPdf
        asyncJobRunner.notifyVoucherValueDecisionApproved = ::runCreateVoucherValueDecisionPdf
        asyncJobRunner.notifyVoucherValueDecisionPdfGenerated = ::runSendVoucherValueDecisionPdf
    }

    fun runCreateFeeDecisionPdf(msg: NotifyFeeDecisionApproved) = jdbi.transaction { h ->
        val decisionId = msg.decisionId
        feeService.createFeeDecisionPdf(h, decisionId)
        logger.info { "Successfully created fee decision pdf (id: $decisionId)." }
        asyncJobRunner.plan(h, listOf(NotifyFeeDecisionPdfGenerated(decisionId)))
    }

    fun runSendFeeDecisionPdf(msg: NotifyFeeDecisionPdfGenerated) = jdbi.transaction { h ->
        val decisionId = msg.decisionId
        feeService.sendDecision(h, decisionId)
        logger.info { "Successfully sent fee decision msg to suomi.fi (id: $decisionId)." }
    }

    fun runCreateVoucherValueDecisionPdf(msg: NotifyVoucherValueDecisionApproved) = jdbi.transaction { h ->
        val decisionId = msg.decisionId
        valueDecisionService.createDecisionPdf(h, decisionId)
        logger.info { "Successfully created voucher value decision pdf (id: $decisionId)." }
        asyncJobRunner.plan(h, listOf(NotifyVoucherValueDecisionPdfGenerated(decisionId)))
    }

    fun runSendVoucherValueDecisionPdf(msg: NotifyVoucherValueDecisionPdfGenerated) = jdbi.transaction { h ->
        val decisionId = msg.decisionId
        valueDecisionService.sendDecision(h, decisionId)
        logger.info { "Successfully sent fee decision msg to suomi.fi (id: $decisionId)." }
    }
}
