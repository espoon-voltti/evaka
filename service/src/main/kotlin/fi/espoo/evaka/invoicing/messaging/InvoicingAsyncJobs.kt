// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.messaging

import fi.espoo.evaka.invoicing.service.FeeDecisionService
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyFeeDecisionApproved
import fi.espoo.evaka.shared.async.NotifyFeeDecisionPdfGenerated
import fi.espoo.evaka.shared.db.transaction
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class InvoicingAsyncJobs(
    private val jdbi: Jdbi,
    private val feeService: FeeDecisionService,
    private val asyncJobRunner: AsyncJobRunner
) {
    init {
        asyncJobRunner.notifyFeeDecisionApproved = ::runCreatePdf
        asyncJobRunner.notifyFeeDecisionPdfGenerated = ::runSendPdf
    }

    fun runCreatePdf(msg: NotifyFeeDecisionApproved) {
        val decisionId = msg.decisionId
        jdbi.transaction { h ->
            feeService.createFeeDecisionPdf(h, decisionId)
            logger.info { "Successfully created fee decision pdf (id: $decisionId)." }
            asyncJobRunner.plan(h, listOf(NotifyFeeDecisionPdfGenerated(decisionId)))
        }
        asyncJobRunner.scheduleImmediateRun()
    }

    fun runSendPdf(msg: NotifyFeeDecisionPdfGenerated) {
        val decisionId = msg.decisionId
        jdbi.transaction { h -> feeService.sendDecision(h, decisionId) }
        logger.info { "Successfully sent fee decision msg to suomi.fi (id: $decisionId)." }
    }
}
