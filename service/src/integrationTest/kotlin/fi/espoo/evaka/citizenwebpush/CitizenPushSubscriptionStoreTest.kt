// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.s3.S3Client

class CitizenPushSubscriptionStoreTest : FullApplicationTest(resetDbBeforeEach = false) {
    @Autowired lateinit var s3Client: S3Client
    @Autowired lateinit var bucketEnv: BucketEnv

    private val store by lazy { CitizenPushSubscriptionStore(s3Client, bucketEnv.data) }
    private val personId = PersonId(UUID.randomUUID())
    private val now =
        HelsinkiDateTime.from(ZonedDateTime.parse("2026-04-14T10:00:00+03:00[Europe/Helsinki]"))

    private fun entry(
        endpoint: String,
        categories: Set<CitizenPushCategory> = setOf(CitizenPushCategory.MESSAGE),
    ) =
        CitizenPushSubscriptionEntry(
            endpoint = URI(endpoint),
            ecdhKey = List(65) { it.toByte() },
            authSecret = List(16) { it.toByte() },
            enabledCategories = categories,
            userAgent = "Mozilla/5.0 (Test)",
            createdAt = now,
        )

    @Test
    fun `load returns null when no file exists`() {
        store.delete(personId)
        assertNull(store.load(personId))
    }

    @Test
    fun `save creates file on first write and reports wasFirstWrite=true`() {
        store.delete(personId)
        val result =
            store.save(personId, CitizenPushStoreFile(personId, listOf(entry("https://fcm.example/a"))))
        assertTrue(result.wasFirstWrite)
        assertNotNull(store.load(personId))
    }

    @Test
    fun `save on existing file reports wasFirstWrite=false`() {
        store.delete(personId)
        store.save(personId, CitizenPushStoreFile(personId, listOf(entry("https://fcm.example/a"))))
        val result =
            store.save(
                personId,
                CitizenPushStoreFile(
                    personId,
                    listOf(entry("https://fcm.example/a"), entry("https://fcm.example/b")),
                ),
            )
        assertFalse(result.wasFirstWrite)
        assertEquals(2, store.load(personId)?.subscriptions?.size)
    }

    @Test
    fun `removeSubscription drops the matching endpoint and keeps the rest`() {
        store.delete(personId)
        store.save(
            personId,
            CitizenPushStoreFile(
                personId,
                listOf(entry("https://fcm.example/a"), entry("https://fcm.example/b")),
            ),
        )
        store.removeSubscription(personId, URI("https://fcm.example/a"))
        val remaining = store.load(personId)?.subscriptions
        assertNotNull(remaining)
        assertEquals(1, remaining.size)
        assertEquals(URI("https://fcm.example/b"), remaining[0].endpoint)
    }

    @Test
    fun `removeSubscription deletes file when last entry is removed`() {
        store.delete(personId)
        store.save(
            personId,
            CitizenPushStoreFile(personId, listOf(entry("https://fcm.example/a"))),
        )
        store.removeSubscription(personId, URI("https://fcm.example/a"))
        assertNull(store.load(personId))
    }

    @Test
    fun `roundtrip preserves categories and byte arrays`() {
        store.delete(personId)
        val original =
            entry(
                "https://fcm.example/a",
                categories = setOf(CitizenPushCategory.URGENT_MESSAGE, CitizenPushCategory.BULLETIN),
            )
        store.save(personId, CitizenPushStoreFile(personId, listOf(original)))
        val loaded = store.load(personId)?.subscriptions?.single()
        assertNotNull(loaded)
        assertEquals(original.endpoint, loaded.endpoint)
        assertEquals(original.enabledCategories, loaded.enabledCategories)
        assertEquals(original.ecdhKey, loaded.ecdhKey)
        assertEquals(original.authSecret, loaded.authSecret)
    }
}
