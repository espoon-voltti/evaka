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
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SfiAsyncJobsIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var sfiAsyncJobs: SfiAsyncJobs

    private val clock = MockEvakaClock(HelsinkiDateTime.now())

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
}
