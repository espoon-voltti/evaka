// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.archival

import fi.espoo.evaka.document.childdocument.getChildDocumentsEligibleForArchival
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.voltti.logging.loggers.info
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

fun planChildDocumentArchival(
    db: Database.Connection,
    clock: EvakaClock,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    delayDays: Int,
    limit: Int = 0,
) {
    logger.info(mapOf("eventCode" to "CHILD_DOCUMENT_ARCHIVAL_PLANNING_STARTED")) {
        "Planning child document archival jobs (delay: $delayDays days, limit: ${if (limit > 0) limit else "none"})"
    }

    val eligibleDate = clock.today().minusDays(delayDays.toLong())
    val allDocumentIds = db.read { it.getChildDocumentsEligibleForArchival(eligibleDate) }

    if (allDocumentIds.isEmpty()) {
        logger.info(mapOf("eventCode" to "CHILD_DOCUMENT_ARCHIVAL_NO_DOCUMENTS")) {
            "No child documents found for archival"
        }
        return
    }

    val documentIds = if (limit > 0) allDocumentIds.take(limit) else allDocumentIds

    logger.info(mapOf("eventCode" to "CHILD_DOCUMENT_ARCHIVAL_SCHEDULING")) {
        "Scheduling archival for ${documentIds.size} child documents (total eligible: ${allDocumentIds.size}, limit: ${if (limit > 0) limit else "none"})"
    }

    db.transaction { tx ->
        asyncJobRunner.plan(
            tx,
            documentIds.asSequence().map { documentId ->
                AsyncJob.ArchiveChildDocument(user = null, documentId = documentId)
            },
            retryCount = 1, // Run once, no retries on failure
            runAt = clock.now(),
        )
    }

    logger.info(mapOf("eventCode" to "CHILD_DOCUMENT_ARCHIVAL_JOBS_SCHEDULED")) {
        "Successfully scheduled ${documentIds.size} child document archival jobs"
    }
}
