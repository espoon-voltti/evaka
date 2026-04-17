// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.sficlient

import evaka.core.sficlient.rest.EventType
import evaka.core.sficlient.rest.GetEvent
import evaka.core.sficlient.rest.GetEventsResponse
import evaka.core.sficlient.rest.MessageEventMetadata
import evaka.core.shared.SfiMessageId
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

private typealias MessageId = String

class MockSfiMessagesClient : SfiMessagesClient {
    private val logger = KotlinLogging.logger {}

    override fun send(msg: SfiMessage) {
        logger.info { "Mock message client got $msg" }
        lock.write {
            data[msg.messageId] = msg
            addEventsResponseFromMessage(msg)
        }
    }

    override fun rotatePassword() {}

    override fun getEvents(continuationToken: String?): GetEventsResponse {
        logger.info {
            "Mock message client got request to fetch events with continuationToken $continuationToken"
        }
        return events.removeFirst()
    }

    private fun addEventsResponseFromMessage(message: SfiMessage) {
        logger.info { "Mock message client adding event from $message" }
        addEventsResponse(
            GetEventsResponse(
                continuationToken = "test_continuation_token",
                events =
                    listOf(
                        GetEvent(
                            eventTime = MockEvakaClock(HelsinkiDateTime.now()).now(),
                            type = EventType.ELECTRONIC_MESSAGE_CREATED,
                            metadata =
                                MessageEventMetadata(
                                    messageId = getNextMessageId(),
                                    externalId = message.messageId.raw.toString(),
                                    serviceId = "espoo_ws_vaka",
                                ),
                        )
                    ),
            )
        )
    }

    companion object {
        private var messageId = 0L
        private val data = mutableMapOf<SfiMessageId, SfiMessage>()
        private val lock = ReentrantReadWriteLock()
        private val events = mutableListOf<GetEventsResponse>()

        fun getMessages() = lock.read { data.values.toList() }

        fun reset() =
            lock.write {
                data.clear()
                events.clear()
                messageId = 0
            }

        fun getEvents() = lock.read { events.toList() }

        fun addEventsResponse(getEventsResponse: GetEventsResponse) =
            lock.write { events.add(getEventsResponse) }

        fun getNextMessageId(): Long {
            return lock.write { messageId++ }
        }
    }
}
