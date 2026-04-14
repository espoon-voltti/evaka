// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.webpush.VapidJwt
import fi.espoo.evaka.webpush.WebPush
import fi.espoo.evaka.webpush.WebPushCrypto
import fi.espoo.evaka.webpush.WebPushNotification
import fi.espoo.evaka.webpush.WebPushPayload
import java.net.URI
import java.security.SecureRandom
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus

class CitizenPushSenderTest {
    private val personId = PersonId(UUID.randomUUID())
    private val now =
        HelsinkiDateTime.from(ZonedDateTime.parse("2026-04-14T10:00:00+03:00[Europe/Helsinki]"))

    // Generate a real P-256 ECDH public key once so WebPushCrypto.decodePublicKey inside
    // the sender succeeds. Without this the sender lands in the decode-failure branch and
    // never calls WebPush.send.
    private val realEcdhKey: List<Byte> =
        WebPushCrypto.encode(WebPushCrypto.generateKeyPair(SecureRandom()).publicKey).toList()

    private lateinit var store: CitizenPushSubscriptionStore
    private lateinit var webPush: WebPush
    private lateinit var sender: CitizenPushSender
    private lateinit var fakeJwtProvider: VapidJwtProvider

    @BeforeEach
    fun setup() {
        store = mock()
        webPush = mock()
        sender = CitizenPushSender(store = store, webPush = webPush)
        fakeJwtProvider = VapidJwtProvider { _ -> mock<VapidJwt>() }
    }

    private fun entry(
        endpoint: String,
        categories: Set<CitizenPushCategory>,
    ) =
        CitizenPushSubscriptionEntry(
            endpoint = URI(endpoint),
            ecdhKey = realEcdhKey,
            authSecret = List(16) { (it + 1).toByte() },
            enabledCategories = categories,
            userAgent = null,
            createdAt = now,
        )

    @Test
    fun `notifyMessage skips subscriptions that do not include the category`() {
        whenever(store.load(personId)).thenReturn(
            CitizenPushStoreFile(
                personId,
                listOf(
                    entry("https://fcm.example/a", setOf(CitizenPushCategory.URGENT_MESSAGE)),
                    entry("https://fcm.example/b", setOf(CitizenPushCategory.MESSAGE)),
                ),
            )
        )

        sender.notifyMessage(
            personId = personId,
            threadId = "thread-123",
            category = CitizenPushCategory.URGENT_MESSAGE,
            senderName = "Alice",
            language = CitizenPushLanguage.FI,
            replyRecipientAccountIds = emptyList(),
            jwtProvider = fakeJwtProvider,
        )

        verify(webPush).send(
            any(),
            check { notification ->
                assertEquals(URI("https://fcm.example/a"), notification.endpoint.uri)
            },
        )
    }

    @Test
    fun `notifyMessage removes subscription when push service returns 410`() {
        whenever(store.load(personId)).thenReturn(
            CitizenPushStoreFile(
                personId,
                listOf(entry("https://fcm.example/gone", setOf(CitizenPushCategory.MESSAGE))),
            )
        )
        whenever(webPush.send(any(), any())).thenThrow(
            WebPush.SubscriptionExpired(HttpStatus.GONE, IllegalStateException("gone"))
        )

        sender.notifyMessage(
            personId = personId,
            threadId = "thread-123",
            category = CitizenPushCategory.MESSAGE,
            senderName = "Alice",
            language = CitizenPushLanguage.FI,
            replyRecipientAccountIds = emptyList(),
            jwtProvider = fakeJwtProvider,
        )

        verify(store).removeSubscription(personId, URI("https://fcm.example/gone"))
    }

    @Test
    fun `notifyMessage builds a payload with body, tag, and url`() {
        whenever(store.load(personId)).thenReturn(
            CitizenPushStoreFile(
                personId,
                listOf(entry("https://fcm.example/a", setOf(CitizenPushCategory.MESSAGE))),
            )
        )

        sender.notifyMessage(
            personId = personId,
            threadId = "thread-abc",
            category = CitizenPushCategory.MESSAGE,
            senderName = "Bob",
            language = CitizenPushLanguage.EN,
            replyRecipientAccountIds = emptyList(),
            jwtProvider = fakeJwtProvider,
        )

        verify(webPush).send(
            any(),
            check { notification ->
                val payload = notification.payloads.single() as WebPushPayload.NotificationV1
                assertEquals("New message", payload.title)
                assertEquals("Bob sent you a message.", payload.body)
                assertEquals("msg-thread-abc", payload.tag)
                assertEquals("/messages/thread-abc", payload.url)
            },
        )
    }

    @Test
    fun `notifyMessage no-ops when store is empty`() {
        whenever(store.load(personId)).thenReturn(null)
        sender.notifyMessage(
            personId = personId,
            threadId = "t",
            category = CitizenPushCategory.MESSAGE,
            senderName = "Alice",
            language = CitizenPushLanguage.FI,
            replyRecipientAccountIds = emptyList(),
            jwtProvider = fakeJwtProvider,
        )
        verify(webPush, never()).send(any(), any())
    }

    @Test
    fun `notifyMessage includes replyAction for MESSAGE category when recipients present`() {
        val threadUuid = UUID.randomUUID()
        val replyAcc = MessageAccountId(UUID.randomUUID())
        whenever(store.load(personId)).thenReturn(
            CitizenPushStoreFile(
                personId,
                listOf(entry("https://fcm.example/a", setOf(CitizenPushCategory.MESSAGE))),
            )
        )

        sender.notifyMessage(
            personId = personId,
            threadId = threadUuid.toString(),
            category = CitizenPushCategory.MESSAGE,
            senderName = "Alice",
            language = CitizenPushLanguage.EN,
            replyRecipientAccountIds = listOf(replyAcc),
            jwtProvider = fakeJwtProvider,
        )

        val captor = argumentCaptor<WebPushNotification>()
        verify(webPush).send(any(), captor.capture())
        val payload = captor.firstValue.payloads.single() as WebPushPayload.NotificationV1
        val replyAction = payload.replyAction
        assertNotNull(replyAction)
        assertEquals(MessageThreadId(threadUuid), replyAction.threadId)
        assertEquals(setOf(replyAcc), replyAction.recipientAccountIds)
        assertEquals("Reply", replyAction.actionLabel)
        assertEquals("Reply sent", replyAction.successTitle)
    }

    @Test
    fun `notifyMessage omits replyAction for BULLETIN category`() {
        val replyAcc = MessageAccountId(UUID.randomUUID())
        whenever(store.load(personId)).thenReturn(
            CitizenPushStoreFile(
                personId,
                listOf(entry("https://fcm.example/a", setOf(CitizenPushCategory.BULLETIN))),
            )
        )

        sender.notifyMessage(
            personId = personId,
            threadId = UUID.randomUUID().toString(),
            category = CitizenPushCategory.BULLETIN,
            senderName = "Alice",
            language = CitizenPushLanguage.EN,
            replyRecipientAccountIds = listOf(replyAcc),
            jwtProvider = fakeJwtProvider,
        )

        val captor = argumentCaptor<WebPushNotification>()
        verify(webPush).send(any(), captor.capture())
        val payload = captor.firstValue.payloads.single() as WebPushPayload.NotificationV1
        assertNull(payload.replyAction)
    }

    @Test
    fun `notifyMessage omits replyAction when reply recipients are empty`() {
        whenever(store.load(personId)).thenReturn(
            CitizenPushStoreFile(
                personId,
                listOf(entry("https://fcm.example/a", setOf(CitizenPushCategory.MESSAGE))),
            )
        )

        sender.notifyMessage(
            personId = personId,
            threadId = UUID.randomUUID().toString(),
            category = CitizenPushCategory.MESSAGE,
            senderName = "Alice",
            language = CitizenPushLanguage.EN,
            replyRecipientAccountIds = emptyList(),
            jwtProvider = fakeJwtProvider,
        )

        val captor = argumentCaptor<WebPushNotification>()
        verify(webPush).send(any(), captor.capture())
        val payload = captor.firstValue.payloads.single() as WebPushPayload.NotificationV1
        assertNull(payload.replyAction)
    }
}
