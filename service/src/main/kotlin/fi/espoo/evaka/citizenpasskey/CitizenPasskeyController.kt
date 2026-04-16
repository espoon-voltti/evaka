// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenpasskey

import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class CitizenPasskeyController(private val store: CitizenPasskeyCredentialStore) {

    data class CitizenPasskeyCredentialSummary(
        val credentialId: String,
        val label: String,
        val deviceHint: String?,
        val createdAt: HelsinkiDateTime,
        val lastUsedAt: HelsinkiDateTime?,
    )

    data class RenamePasskeyRequest(val label: String)

    data class InternalUpsertPasskeyRequest(
        val personId: PersonId,
        val credentialId: String,
        val publicKey: String,
        val signCounter: Long,
        val transports: List<String>,
        val label: String,
        val deviceHint: String?,
    )

    data class InternalTouchPasskeyRequest(val signCounter: Long)

    // -------------------------------------------------------------------------
    // Citizen-facing endpoints
    // -------------------------------------------------------------------------

    @GetMapping("/citizen/passkey/credentials")
    fun listCredentials(user: AuthenticatedUser.Citizen): List<CitizenPasskeyCredentialSummary> =
        store.listCredentials(user.id).map { it.toSummary() }

    @PatchMapping("/citizen/passkey/credentials/{credentialId}")
    fun renameCredential(
        user: AuthenticatedUser.Citizen,
        @PathVariable credentialId: String,
        @RequestBody body: RenamePasskeyRequest,
    ) {
        val label = body.label.trim()
        require(label.isNotEmpty() && label.length <= 80)
        if (!store.renameCredential(user.id, credentialId, label)) {
            throw NotFound(errorCode = "credential-not-found")
        }
    }

    @DeleteMapping("/citizen/passkey/credentials/{credentialId}")
    fun revokeCredential(user: AuthenticatedUser.Citizen, @PathVariable credentialId: String) {
        if (!store.revokeCredential(user.id, credentialId)) {
            throw NotFound(errorCode = "credential-not-found")
        }
    }

    // -------------------------------------------------------------------------
    // Internal (apigw → service) endpoints
    // -------------------------------------------------------------------------

    @GetMapping("/system/citizen-passkey/credentials/{personId}")
    fun listCredentialsInternal(
        user: AuthenticatedUser.SystemInternalUser,
        @PathVariable personId: PersonId,
    ): List<CitizenPasskeyCredential> = store.listCredentials(personId)

    @PostMapping("/system/citizen-passkey/credentials")
    fun upsertCredentialInternal(
        user: AuthenticatedUser.SystemInternalUser,
        clock: EvakaClock,
        @RequestBody body: InternalUpsertPasskeyRequest,
    ) {
        val credential =
            CitizenPasskeyCredential(
                credentialId = body.credentialId,
                publicKey = body.publicKey,
                signCounter = body.signCounter,
                transports = body.transports,
                label = body.label,
                deviceHint = body.deviceHint,
                createdAt = clock.now(),
                lastUsedAt = null,
            )
        store.upsertCredential(body.personId, credential)
    }

    @PostMapping("/system/citizen-passkey/credentials/{personId}/{credentialId}/touch")
    fun touchCredentialInternal(
        user: AuthenticatedUser.SystemInternalUser,
        clock: EvakaClock,
        @PathVariable personId: PersonId,
        @PathVariable credentialId: String,
        @RequestBody body: InternalTouchPasskeyRequest,
    ) {
        if (!store.touchCredential(personId, credentialId, body.signCounter, clock.now())) {
            throw NotFound(errorCode = "credential-not-found")
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun CitizenPasskeyCredential.toSummary() =
        CitizenPasskeyCredentialSummary(
            credentialId = credentialId,
            label = label,
            deviceHint = deviceHint,
            createdAt = createdAt,
            lastUsedAt = lastUsedAt,
        )
}
