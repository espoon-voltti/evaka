// SPDX-FileCopyrightText: 2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka.archival

import evaka.core.shared.ChildDocumentId
import evaka.core.shared.DecisionId
import evaka.core.shared.FeeDecisionId
import evaka.core.shared.VoucherValueDecisionId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import evaka.core.shared.db.Predicate
import evaka.core.shared.domain.EvakaClock
import evaka.instance.tampere.ArchivalSchedule
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

fun planDocumentArchival(
    db: Database.Connection,
    clock: EvakaClock,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    schedule: ArchivalSchedule,
) {
    if (schedule.dailyDocumentLimit < 0)
        error("Invalid archival limit configuration of ${schedule.dailyDocumentLimit}")
    if (schedule.dailyDocumentLimit.toInt() == 0) {
        logger.info { "Archival daily document limit set to 0, skipping archival job creation" }
        return
    }

    db.transaction { tx ->
        val archivalPicks = selectLimitedEligibleDocumentIds(tx, clock, schedule)
        logger.info {
            "Scheduling archival for ${archivalPicks.planChildDocuments.size} plan child documents, ${archivalPicks.decisionChildDocuments.size} decision child documents, ${archivalPicks.decisions.size} decisions, ${archivalPicks.feeDecisions.size} fee decisions and ${archivalPicks.voucherValueDecisions.size} voucher value decisions, limit: ${schedule.dailyDocumentLimit})"
        }
        asyncJobRunner.plan(
            tx,
            archivalPicks.planChildDocuments.map { documentId ->
                AsyncJob.ArchiveChildDocument(user = null, documentId = documentId)
            } +
                archivalPicks.decisionChildDocuments.map { documentId ->
                    AsyncJob.ArchiveChildDocument(user = null, documentId = documentId)
                } +
                archivalPicks.decisions.map { documentId ->
                    AsyncJob.ArchiveDecision(user = null, decisionId = documentId)
                } +
                archivalPicks.feeDecisions.map { documentId ->
                    AsyncJob.ArchiveFeeDecision(user = null, feeDecisionId = documentId)
                } +
                archivalPicks.voucherValueDecisions.map { documentId ->
                    AsyncJob.ArchiveVoucherValueDecision(
                        user = null,
                        voucherValueDecisionId = documentId,
                    )
                },
            retryCount = 1, // Run once, no retries on failure
            runAt = clock.now(),
        )

        logger.info { "Successfully scheduled ${archivalPicks.totalCount()} archival jobs" }
    }
}

private fun selectLimitedEligibleDocumentIds(
    tx: Database.Transaction,
    clock: EvakaClock,
    schedule: ArchivalSchedule,
): ArchivalPicks {
    val decisionPredicate = Predicate {
        where(
            """
            $it.status in ('ACCEPTED','REJECTED')
                  AND $it.type <> 'CLUB'
                  AND $it.archived_at IS NULL
                  AND $it.resolved <= ${bind(clock.today().minusDays(schedule.decisionDelayDays)!!)}
                  AND $it.document_key IS NOT NULL
        """
        )
    }

    val feeDecisionPredicate = Predicate {
        where(
            """
            $it.approved_at <= ${bind(clock.today().minusDays(schedule.feeDecisionDelayDays)!!)}
                  AND $it.status in ('ANNULLED','SENT')
                  AND $it.archived_at IS NULL
                  AND $it.document_key IS NOT NULL
        """
        )
    }

    val voucherDecisionPredicate = Predicate {
        where(
            """
            $it.approved_at <= ${bind(clock.today().minusDays(schedule.voucherDecisionDelayDays)!!)}
                  AND $it.status in ('ANNULLED','SENT')
                  AND $it.archived_at IS NULL
                  AND $it.document_key IS NOT NULL
        """
        )
    }

    val childDocumentPredicate = Predicate {
        where(
            """
                  $it.archived_at IS NULL
                  AND $it.status = 'COMPLETED'
                  AND EXISTS (SELECT FROM child_document_published_version v WHERE v.child_document_id = $it.id AND v.document_key IS NOT NULL)
        """
        )
    }

    val childDocumentPlanTemplatePredicate = Predicate {
        where(
            """
            -- upper is exclusive
            upper($it.validity) - interval '1 day' <= ${bind(clock.today().minusDays(schedule.documentPlanDelayDays)!!)}
            AND $it.archive_externally = true
            AND $it.type <> 'OTHER_DECISION'
        """
        )
    }

    val childDocumentDecisionTemplatePredicate = Predicate {
        where(
            """
            -- must have no placements at all since delay days ago
            NOT EXISTS (select from placement pla where cd.child_id = pla.child_id AND pla.end_date > ${bind(clock.today().minusDays(schedule.documentDecisionDelayDays)!!)})
            AND $it.archive_externally = true
            AND $it.type = 'OTHER_DECISION'
        """
        )
    }

    logger.info { "Planning document archival jobs, max limit: ${schedule.dailyDocumentLimit}" }

    // check eligible document counts
    val archivablePlanDocumentCount =
        tx.getEligibleChildDocumentCount(childDocumentPredicate, childDocumentPlanTemplatePredicate)
    val archivableDecisionDocumentCount =
        tx.getEligibleChildDocumentCount(
            childDocumentPredicate,
            childDocumentDecisionTemplatePredicate,
        )
    val archivableDecisionCount = tx.getEligibleDecisionCount(decisionPredicate)
    val archivableFeeDecisionCount = tx.getEligibleFeeDecisionCount(feeDecisionPredicate)
    val archivableVoucherValueDecisionCount =
        tx.getEligibleVoucherDecisionCount(voucherDecisionPredicate)

    // distribute the daily archival quota proportionally based on eligible document occurrence
    val proportionalPickCounts =
        distributeProportionally(
            listOf(
                archivablePlanDocumentCount,
                archivableDecisionDocumentCount,
                archivableDecisionCount,
                archivableFeeDecisionCount,
                archivableVoucherValueDecisionCount,
            ),
            schedule.dailyDocumentLimit.toInt(),
        )

    val planChildDocumentIds =
        tx.getChildDocumentsEligibleForArchival(
            childDocumentPredicate,
            childDocumentPlanTemplatePredicate,
            proportionalPickCounts[0],
        )
    val decisionChildDocumentIds =
        tx.getChildDocumentsEligibleForArchival(
            childDocumentPredicate,
            childDocumentDecisionTemplatePredicate,
            proportionalPickCounts[1],
        )
    val decisionIds =
        tx.getDecisionsEligibleForArchival(decisionPredicate, proportionalPickCounts[2])
    val feeDecisionIds =
        tx.getFeeDecisionsEligibleForArchival(feeDecisionPredicate, proportionalPickCounts[3])
    val voucherValueDecisionIds =
        tx.getVoucherValueDecisionsEligibleForArchival(
            voucherDecisionPredicate,
            proportionalPickCounts[4],
        )

    return ArchivalPicks(
        planChildDocumentIds,
        decisionChildDocumentIds,
        decisionIds,
        feeDecisionIds,
        voucherValueDecisionIds,
    )
}

fun distributeProportionally(sizes: List<Int>, totalLimit: Int): List<Int> {
    val totalAvailable = sizes.sum()
    val actualTargetCount = minOf(totalAvailable, totalLimit)

    return sizes.map { size ->
        if (totalAvailable == 0) {
            0
        } else {
            ((size.toDouble() / totalAvailable) * actualTargetCount).toInt()
        }
    }
}

data class ArchivalPicks(
    val planChildDocuments: List<ChildDocumentId>,
    val decisionChildDocuments: List<ChildDocumentId>,
    val decisions: List<DecisionId>,
    val feeDecisions: List<FeeDecisionId>,
    val voucherValueDecisions: List<VoucherValueDecisionId>,
) {
    fun totalCount(): Int =
        this.planChildDocuments.size +
            this.decisionChildDocuments.size +
            this.decisions.size +
            this.feeDecisions.size +
            this.voucherValueDecisions.size
}
