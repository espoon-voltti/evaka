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
        db.transaction { tx ->
            logger.info { "SfiAsyncJobs: starting to fetch events" }

            val continuationToken = tx.getLatestSfiGetEventsContinuationToken()
            val eventsResponse = sfiClient.getEvents(continuationToken)
            logger.info { "SfiAsyncJobs: got ${eventsResponse.events.size} events" }
            eventsResponse.events.forEach { event ->
                logger.info { "SfiAsyncJobs: processing event $event" }
                try {
                    val externalId =
                        UUID.fromString(event.metadata.externalId)
                            ?: throw IllegalStateException("SfiAsyncJobs: external ID is null")

                    val id =
                        tx.upsertSfiMessageEventIfSfiMessageExists(
                            SfiMessageEvent(
                                messageId = SfiMessageId(externalId),
                                eventType = event.type,
                            )
                        )
                    logger.info { "SfiAsyncJobs: successfully processed event $event with id $id" }
                } catch (e: Exception) {
                    logger.error(e) { "SfiAsyncJobs: failed to process event $event" }
                }
            }

            tx.storeSfiGetEventsContinuationToken(eventsResponse.continuationToken)
            logger.info { "SfiAsyncJobs: done fetching ${eventsResponse.events.size} events" }
        }
    }
}
