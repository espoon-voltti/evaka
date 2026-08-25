// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.sficlient

import evaka.core.shared.SfiMessageId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class SfiAsyncJobs(
    private val sfiClient: SfiMessagesClient,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
    init {
        asyncJobRunner.registerHandler { _, _, payload: AsyncJob.SendMessage ->
            sendMessagePDF(payload.message)
        }
    }

    fun sendMessagePDF(msg: SfiMessage) {
        sfiClient.send(msg)
    }

    fun getEvents(db: Database.Connection, clock: EvakaClock) {
        logger.info { "SfiAsyncJobs: starting to fetch events" }
        // The API returns a limited number of events per request and signals that everything
        // has been consumed by returning an empty batch. Events expire after 60 days, so a
        // single request per run would let them be lost if we ever fall behind.
        repeat(MAX_EVENT_BATCHES) {
            val batchSize = db.transaction { tx -> fetchAndStoreEventBatch(tx) }
            if (batchSize == 0) {
                logger.info { "SfiAsyncJobs: done fetching events" }
                return
            }
        }
        logger.warn {
            "SfiAsyncJobs: stopped after $MAX_EVENT_BATCHES batches, more events may be pending"
        }
    }

    private fun fetchAndStoreEventBatch(tx: Database.Transaction): Int {
        val continuationToken = tx.getLatestSfiGetEventsContinuationToken()
        val eventsResponse = sfiClient.getEvents(continuationToken)
        logger.info { "SfiAsyncJobs: got ${eventsResponse.events.size} events" }
        eventsResponse.events.forEach { event ->
            logger.info { "SfiAsyncJobs: processing event $event" }
            try {
                val externalId =
                    event.metadata.externalId?.let {
                        runCatching { UUID.fromString(it) }.getOrNull()
                    }
                if (externalId == null) {
                    // Messages sent before the external ID became the sfi_message ID used formats
                    // such as "<decisionId>|<guardianId>", and Suomi.fi still reports events for
                    // them
                    logger.info { "SfiAsyncJobs: skipped event $event (external ID is not a UUID)" }
                    return@forEach
                }

                val id =
                    tx.upsertSfiMessageEventIfSfiMessageExists(
                        SfiMessageEvent(
                            messageId = SfiMessageId(externalId),
                            eventType = event.type,
                            eventTime = event.eventTime,
                        )
                    )
                if (id != null) {
                    logger.info { "SfiAsyncJobs: successfully processed event $event with id $id" }
                } else {
                    logger.info { "SfiAsyncJobs: skipped event $event (no matching sfi_message)" }
                }
            } catch (e: Exception) {
                logger.error(e) { "SfiAsyncJobs: failed to process event $event" }
            }
        }

        tx.storeSfiGetEventsContinuationToken(eventsResponse.continuationToken)
        return eventsResponse.events.size
    }
}

private const val MAX_EVENT_BATCHES = 100
