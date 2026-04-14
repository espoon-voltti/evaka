// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenpasskey

import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest

class CitizenPasskeyCredentialStore(private val s3Client: S3Client, private val bucket: String) {
    private val jsonMapper = defaultJsonMapperBuilder().build()

    private fun key(personId: PersonId): String = "citizen-passkey-credentials/$personId.json"

    fun load(personId: PersonId): CitizenPasskeyStoreFile? {
        val request = GetObjectRequest.builder().bucket(bucket).key(key(personId)).build()
        return try {
            s3Client.getObject(request).use { stream ->
                jsonMapper.readValue(stream.readAllBytes(), CitizenPasskeyStoreFile::class.java)
            }
        } catch (_: NoSuchKeyException) {
            null
        }
    }

    fun save(personId: PersonId, file: CitizenPasskeyStoreFile) {
        val bytes = jsonMapper.writeValueAsBytes(file)
        val request =
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key(personId))
                .contentType("application/json")
                .build()
        s3Client.putObject(request, RequestBody.fromBytes(bytes))
    }

    fun delete(personId: PersonId) {
        val request = DeleteObjectRequest.builder().bucket(bucket).key(key(personId)).build()
        s3Client.deleteObject(request)
    }

    fun listCredentials(personId: PersonId): List<CitizenPasskeyCredential> =
        load(personId)?.credentials ?: emptyList()

    fun upsertCredential(personId: PersonId, credential: CitizenPasskeyCredential) {
        val current = load(personId) ?: CitizenPasskeyStoreFile(personId, emptyList())
        val filtered = current.credentials.filterNot { it.credentialId == credential.credentialId }
        save(personId, current.copy(credentials = filtered + credential))
    }

    fun renameCredential(personId: PersonId, credentialId: String, label: String): Boolean {
        val current = load(personId) ?: return false
        val idx = current.credentials.indexOfFirst { it.credentialId == credentialId }
        if (idx < 0) return false
        val updated = current.credentials.toMutableList()
        updated[idx] = updated[idx].copy(label = label)
        save(personId, current.copy(credentials = updated))
        return true
    }

    fun revokeCredential(personId: PersonId, credentialId: String): Boolean {
        val current = load(personId) ?: return false
        val remaining = current.credentials.filterNot { it.credentialId == credentialId }
        if (remaining.size == current.credentials.size) return false
        if (remaining.isEmpty()) {
            delete(personId)
        } else {
            save(personId, current.copy(credentials = remaining))
        }
        return true
    }

    fun touchCredential(
        personId: PersonId,
        credentialId: String,
        newSignCounter: Long,
        now: HelsinkiDateTime,
    ): Boolean {
        val current = load(personId) ?: return false
        val idx = current.credentials.indexOfFirst { it.credentialId == credentialId }
        if (idx < 0) return false
        val updated = current.credentials.toMutableList()
        updated[idx] = updated[idx].copy(signCounter = newSignCounter, lastUsedAt = now)
        save(personId, current.copy(credentials = updated))
        return true
    }
}
