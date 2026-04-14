// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import fi.espoo.evaka.shared.utils.writerFor
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class WebPushPayloadSerializationTest {
    private val mapper = defaultJsonMapperBuilder().build()
    private val writer = mapper.writerFor<List<WebPushPayload>>()

    @Test
    fun `NotificationV1 serializes replyAction`() {
        val threadId = MessageThreadId(UUID.randomUUID())
        val accountId = MessageAccountId(UUID.randomUUID())
        val payload: WebPushPayload =
            WebPushPayload.NotificationV1(
                title = "New message",
                body = "Alice sent you a message.",
                tag = "msg-$threadId",
                url = "/messages/$threadId",
                replyAction =
                    WebPushPayload.NotificationV1.ReplyAction(
                        threadId = threadId,
                        recipientAccountIds = setOf(accountId),
                        actionLabel = "Reply",
                        actionPlaceholder = "Type a reply…",
                        successTitle = "Reply sent",
                        successBody = "Your reply was delivered.",
                        errorTitle = "Reply not sent",
                        errorBody = "Open eVaka to retry.",
                    ),
            )

        val json = writer.writeValueAsString(listOf(payload))
        val tree = mapper.readTree(json)

        val expected =
            mapper.readTree(
                """
                {
                  "type": "NotificationV1",
                  "title": "New message",
                  "body": "Alice sent you a message.",
                  "tag": "msg-${threadId.raw}",
                  "url": "/messages/${threadId.raw}",
                  "replyAction": {
                    "threadId": "${threadId.raw}",
                    "recipientAccountIds": ["${accountId.raw}"],
                    "actionLabel": "Reply",
                    "actionPlaceholder": "Type a reply…",
                    "successTitle": "Reply sent",
                    "successBody": "Your reply was delivered.",
                    "errorTitle": "Reply not sent",
                    "errorBody": "Open eVaka to retry."
                  }
                }
                """
            )
        assertEquals(expected, tree[0])
    }

    @Test
    fun `NotificationV1 omits replyAction when null`() {
        val payload: WebPushPayload =
            WebPushPayload.NotificationV1(
                title = "Bulletin",
                body = "A new bulletin was posted.",
                tag = "msg-abc",
                url = "/messages/abc",
                replyAction = null,
            )

        val json = writer.writeValueAsString(listOf(payload))
        val tree = mapper.readTree(json)

        // `replyAction` should either be absent or a JSON null, not an empty object.
        assert(!tree[0].has("replyAction") || tree[0]["replyAction"].isNull) {
            "replyAction should be absent or null, was: ${tree[0]["replyAction"]}"
        }
    }
}
