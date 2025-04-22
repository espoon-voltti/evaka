// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sficlient

import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import io.github.oshai.kotlinlogging.KotlinLogging
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
        db.transaction {
            logger.info { "SfiAsyncJobs: starting to fetch events" }

            val continuationToken = it.getLatestSfiGetEventsContinuationToken()
            val eventsResponse = sfiClient.getEvents(continuationToken)
            logger.info { "SfiAsyncJobs: got ${eventsResponse.events.size} events" }

            logger.info { "SfiAsyncJobs: GetEvents response: $eventsResponse" }

            // TODO handle events
            it.storeSfiGetEventsContinuationToken(eventsResponse.continuationToken)
            logger.info { "SfiAsyncJobs: done fetching ${eventsResponse.events.size} events" }
        }
    }
}
