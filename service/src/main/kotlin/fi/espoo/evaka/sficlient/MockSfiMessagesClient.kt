// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sficlient

import fi.espoo.evaka.sficlient.rest.GetEventsResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

private typealias MessageId = String

class MockSfiMessagesClient : SfiMessagesClient {
    private val logger = KotlinLogging.logger {}

    override fun send(msg: SfiMessage) {
        logger.info { "Mock message client got $msg" }
        lock.write { data[msg.messageId] = msg }
    }

    override fun rotatePassword() {}

    override fun getEvents(continuationToken: String?): GetEventsResponse {
        logger.info {
            "Mock message client got request to fetch events with continuationToken $continuationToken"
        }
        return events.removeFirst()
    }

    companion object {
        private val data = mutableMapOf<MessageId, SfiMessage>()
        private val lock = ReentrantReadWriteLock()
        private val events = mutableListOf<GetEventsResponse>()

        fun getMessages() = lock.read { data.values.toList() }

        fun clearMessages() = lock.write { data.clear() }

        fun getEvents() = lock.read { events.toList() }

        fun clearEvents() = lock.write { events.clear() }
    }
}
