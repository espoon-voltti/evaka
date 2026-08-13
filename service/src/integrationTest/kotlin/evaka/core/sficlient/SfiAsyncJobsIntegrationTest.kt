// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.sficlient

import evaka.core.FullApplicationTest
import evaka.core.sficlient.rest.EventType
import evaka.core.sficlient.rest.GetEvent
import evaka.core.sficlient.rest.GetEventsResponse
import evaka.core.sficlient.rest.MessageEventMetadata
import evaka.core.shared.SfiMessageId
import evaka.core.shared.dev.DevFeeDecision
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SfiAsyncJobsIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var sfiAsyncJobs: SfiAsyncJobs

    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2026, 3, 14), LocalTime.of(0, 10)))

    @BeforeEach
    fun beforeEach() {
        MockSfiMessagesClient.reset()
    }

    @Test
    fun `event for unknown sfi_message is skipped without error and continuation token advances`() {
        val unknownMessageId = SfiMessageId(UUID.randomUUID())
        MockSfiMessagesClient.addEventsResponse(
            GetEventsResponse(
                continuationToken = "token-after-unknown",
                events =
                    listOf(
                        GetEvent(
                            eventTime = clock.now(),
                            type = EventType.ELECTRONIC_MESSAGE_CREATED,
                            metadata =
                                MessageEventMetadata(
                                    messageId = 1L,
                                    externalId = unknownMessageId.raw.toString(),
                                    serviceId = "espoo_ws_vaka",
                                ),
                        )
                    ),
            )
        )

        sfiAsyncJobs.getEvents(db, clock)

        db.read {
            assertTrue(it.getSfiMessageEventsByMessageId(unknownMessageId).isEmpty())
            assertEquals(listOf("token-after-unknown"), it.getSfiGetEventsContinuationTokens())
        }
    }

    @Test
    fun `events are stored with the time reported by Suomi fi, not the ingest time`() {
        val messageId = createSfiMessage()
        val sentAt = HelsinkiDateTime.of(LocalDate.of(2026, 3, 12), LocalTime.of(10, 0))
        val readAt = HelsinkiDateTime.of(LocalDate.of(2026, 3, 13), LocalTime.of(9, 12))
        MockSfiMessagesClient.addEventsResponse(
            eventsResponse(
                "token-1",
                event(messageId, EventType.ELECTRONIC_MESSAGE_CREATED, sentAt),
                event(messageId, EventType.ELECTRONIC_MESSAGE_READ, readAt),
            )
        )

        sfiAsyncJobs.getEvents(db, clock)

        db.read {
            assertEquals(
                mapOf(
                    EventType.ELECTRONIC_MESSAGE_CREATED to sentAt,
                    EventType.ELECTRONIC_MESSAGE_READ to readAt,
                ),
                it.getSfiMessageEventsByMessageId(messageId).associate { e ->
                    e.eventType to e.eventTime
                },
            )
        }
    }

    @Test
    fun `repeated read events keep the earliest event time`() {
        val messageId = createSfiMessage()
        val firstRead = HelsinkiDateTime.of(LocalDate.of(2026, 3, 13), LocalTime.of(9, 12))
        val secondRead = HelsinkiDateTime.of(LocalDate.of(2026, 3, 13), LocalTime.of(17, 45))
        // Later read arrives first to prove the result does not depend on batch ordering
        MockSfiMessagesClient.addEventsResponse(
            eventsResponse(
                "token-1",
                event(messageId, EventType.ELECTRONIC_MESSAGE_READ, secondRead),
                event(messageId, EventType.ELECTRONIC_MESSAGE_READ, firstRead),
            )
        )

        sfiAsyncJobs.getEvents(db, clock)

        db.read {
            val events = it.getSfiMessageEventsByMessageId(messageId)
            assertEquals(1, events.size)
            assertEquals(EventType.ELECTRONIC_MESSAGE_READ, events[0].eventType)
            assertEquals(firstRead, events[0].eventTime)
        }
    }

    private fun createSfiMessage(): SfiMessageId {
        val guardian = DevPerson()
        return db.transaction { tx ->
            tx.insert(guardian, DevPersonType.ADULT)
            val feeDecisionId =
                tx.insert(
                    DevFeeDecision(
                        validDuring =
                            FiniteDateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)),
                        headOfFamilyId = guardian.id,
                    )
                )
            tx.storeSentSfiMessage(
                SentSfiMessage(guardianId = guardian.id, feeDecisionId = feeDecisionId)
            )
        }
    }

    private fun eventsResponse(continuationToken: String, vararg events: GetEvent) =
        GetEventsResponse(continuationToken = continuationToken, events = events.toList())

    private fun event(messageId: SfiMessageId, type: EventType, eventTime: HelsinkiDateTime) =
        GetEvent(
            eventTime = eventTime,
            type = type,
            metadata =
                MessageEventMetadata(
                    messageId = 1L,
                    externalId = messageId.raw.toString(),
                    serviceId = "espoo_ws_vaka",
                ),
        )
}
