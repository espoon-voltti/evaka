// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import java.net.URI
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest

data class SaveResult(val wasFirstWrite: Boolean, val wasNewEndpoint: Boolean = false)

class CitizenPushSubscriptionStore(private val s3Client: S3Client, private val bucket: String) {
    private val jsonMapper = defaultJsonMapperBuilder().build()

    private fun key(personId: PersonId): String = "citizen-push-subscriptions/$personId.json"

    fun load(personId: PersonId): CitizenPushStoreFile? {
        val request = GetObjectRequest.builder().bucket(bucket).key(key(personId)).build()
        return try {
            s3Client.getObject(request).use { stream ->
                jsonMapper.readValue(stream.readAllBytes(), CitizenPushStoreFile::class.java)
            }
        } catch (_: NoSuchKeyException) {
            null
        }
    }

    fun save(personId: PersonId, file: CitizenPushStoreFile): SaveResult {
        val wasFirstWrite = load(personId) == null
        val bytes = jsonMapper.writeValueAsBytes(file)
        val request =
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key(personId))
                .contentType("application/json")
                .build()
        s3Client.putObject(request, RequestBody.fromBytes(bytes))
        return SaveResult(wasFirstWrite)
    }

    fun delete(personId: PersonId) {
        // The decisions bucket (where these PoC files live — see
        // CitizenWebPushConfig for the reason) denies s3:DeleteObject for the
        // service role (allow_delete = false in evaka-ecs/s3.tf). Logical-
        // delete by writing an empty-subscriptions file instead. All readers
        // treat an empty subscription list identically to a missing file.
        save(personId, CitizenPushStoreFile(personId, emptyList()))
    }

    fun removeSubscription(personId: PersonId, endpoint: URI) {
        val current = load(personId) ?: return
        val remaining = current.subscriptions.filterNot { it.endpoint == endpoint }
        if (remaining.isEmpty()) {
            delete(personId)
        } else {
            save(personId, current.copy(subscriptions = remaining))
        }
    }

    fun upsertSubscription(personId: PersonId, entry: CitizenPushSubscriptionEntry): SaveResult {
        val current = load(personId) ?: CitizenPushStoreFile(personId, emptyList())
        val wasNewEndpoint = current.subscriptions.none { it.endpoint == entry.endpoint }
        val filtered = current.subscriptions.filterNot { it.endpoint == entry.endpoint }
        val next = current.copy(subscriptions = filtered + entry)
        return save(personId, next).copy(wasNewEndpoint = wasNewEndpoint)
    }
}
