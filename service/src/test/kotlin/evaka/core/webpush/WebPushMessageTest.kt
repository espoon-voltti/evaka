// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import evaka.core.shared.config.defaultJsonMapperBuilder
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class WebPushMessageTest {
    private val jsonMapper = defaultJsonMapperBuilder().build()

    @Test
    fun `declarative message encodes with the correct fields`() {
        val message =
            WebPushMessage.Declarative(
                DeclarativeNotification(
                    title = "Uusi viesti",
                    navigate = "https://evaka.example.com/messages/123",
                    body = "Esimies Essi",
                    tag = "message-123",
                )
            )
        assertEquals(
            """{"notification":{"title":"Uusi viesti","navigate":"https://evaka.example.com/messages/123","body":"Esimies Essi","tag":"message-123"},"web_push":8030}""",
            jsonMapper.writeValueAsString(message),
        )
    }

    @Test
    fun `versioned message encodes as an array of typed payloads`() {
        val message = WebPushMessage.Versioned(listOf(WebPushPayload.NotificationV1("Uusi viesti")))
        assertEquals(
            """[{"type":"NotificationV1","title":"Uusi viesti"}]""",
            jsonMapper.writeValueAsString(message),
        )
    }
}
