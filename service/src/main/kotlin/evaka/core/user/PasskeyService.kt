// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.user

import com.yubico.webauthn.AssertionRequest
import com.yubico.webauthn.CredentialRepository
import com.yubico.webauthn.FinishAssertionOptions
import com.yubico.webauthn.FinishRegistrationOptions
import com.yubico.webauthn.RegisteredCredential
import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.StartAssertionOptions
import com.yubico.webauthn.StartRegistrationOptions
import com.yubico.webauthn.data.AttestationConveyancePreference
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria
import com.yubico.webauthn.data.ByteArray as WebAuthnByteArray
import com.yubico.webauthn.data.PublicKeyCredential
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor
import com.yubico.webauthn.data.RelyingPartyIdentity
import com.yubico.webauthn.data.ResidentKeyRequirement
import com.yubico.webauthn.data.UserIdentity
import com.yubico.webauthn.data.UserVerificationRequirement
import evaka.core.EvakaEnv
import evaka.core.shared.PersonId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.Forbidden
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.ByteBuffer
import java.util.Optional
import java.util.UUID
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

data class StartedPasskeyRegistration(
    /** Serialized options for server-side storage, needed to finish the ceremony */
    val options: String,
    /** Argument for the browser's navigator.credentials.create() */
    val credentialsCreate: String,
)

data class StartedPasskeyAssertion(
    /** Serialized assertion request, held by the caller and passed back to the finish leg */
    val assertionRequest: String,
    /** Argument for the browser's navigator.credentials.get() */
    val credentialsGet: String,
)

data class FinishedPasskeyRegistration(
    val credentialId: ByteArray,
    val publicKey: ByteArray,
    val signatureCounter: Long,
    val aaguid: UUID,
    val transports: List<String>,
)

data class FinishedPasskeyAssertion(
    val person: PersonId,
    val passkey: PasskeyCredential,
    val signatureCounter: Long,
)

@Service
class PasskeyService(private val env: EvakaEnv) {
    private fun relyingParty(credentialRepository: CredentialRepository): RelyingParty {
        val webAuthnRpId =
            env.webAuthnRpId ?: throw IllegalStateException("WebAuthn RP ID not configured")
        val webAuthOrigin =
            env.webAuthnOrigin ?: throw IllegalStateException("WebAuthn Origin not configured")

        val identity = RelyingPartyIdentity.builder().id(webAuthnRpId).name("eVaka").build()
        val origins = setOf(webAuthOrigin)
        return RelyingParty.builder()
            .identity(identity)
            .credentialRepository(credentialRepository)
            .origins(origins)
            .attestationConveyancePreference(AttestationConveyancePreference.NONE)
            .allowUntrustedAttestation(true)
            .validateSignatureCounter(false)
            .build()
    }

    fun startRegistration(
        person: PersonId,
        accountName: String,
        existingCredentialIds: List<ByteArray>,
    ): StartedPasskeyRegistration {
        val rp = relyingParty(fixedDataRepository(person, existingCredentialIds, emptyList()))
        val options =
            rp.startRegistration(
                StartRegistrationOptions.builder()
                    .user(
                        UserIdentity.builder()
                            .name(accountName)
                            .displayName(accountName)
                            .id(person.toUserHandle())
                            .build()
                    )
                    .authenticatorSelection(
                        AuthenticatorSelectionCriteria.builder()
                            .residentKey(ResidentKeyRequirement.REQUIRED)
                            .userVerification(UserVerificationRequirement.REQUIRED)
                            .build()
                    )
                    .build()
            )
        return StartedPasskeyRegistration(
            options = options.toJson(),
            credentialsCreate = options.toCredentialsCreateJson(),
        )
    }

    fun finishRegistration(
        tx: Database.Read,
        person: PersonId,
        optionsJson: String,
        credentialJson: String,
    ): FinishedPasskeyRegistration {
        val options = PublicKeyCredentialCreationOptions.fromJson(optionsJson)
        val response =
            try {
                PublicKeyCredential.parseRegistrationResponseJson(credentialJson)
            } catch (e: Exception) {
                throw BadRequest("Invalid passkey registration response", cause = e)
            }
        val existing = tx.getPasskeyByCredentialId(response.id.bytes)
        val rp = relyingParty(fixedDataRepository(person, emptyList(), listOfNotNull(existing)))
        val result =
            try {
                rp.finishRegistration(
                    FinishRegistrationOptions.builder().request(options).response(response).build()
                )
            } catch (e: Exception) {
                throw BadRequest("Passkey registration failed", cause = e)
            }
        if (!result.isUserVerified) throw BadRequest("Passkey registration failed")
        return FinishedPasskeyRegistration(
            credentialId = result.keyId.id.bytes,
            publicKey = result.publicKeyCose.bytes,
            signatureCounter = result.signatureCount,
            aaguid = result.aaguid.toUuid(),
            transports =
                result.keyId.transports.map { set -> set.map { it.id } }.orElse(emptyList()),
        )
    }

    fun startAssertion(): StartedPasskeyAssertion {
        val rp = relyingParty(fixedDataRepository(null, emptyList(), emptyList()))
        val request =
            rp.startAssertion(
                StartAssertionOptions.builder()
                    .userVerification(UserVerificationRequirement.REQUIRED)
                    .build()
            )
        return StartedPasskeyAssertion(
            assertionRequest = request.toJson(),
            credentialsGet = request.toCredentialsGetJson(),
        )
    }

    fun finishAssertion(
        tx: Database.Read,
        assertionRequestJson: String,
        credentialJson: String,
    ): FinishedPasskeyAssertion {
        val request = AssertionRequest.fromJson(assertionRequestJson)
        val response =
            try {
                PublicKeyCredential.parseAssertionResponseJson(credentialJson)
            } catch (e: Exception) {
                throw Forbidden(cause = e)
            }
        val passkey = tx.getPasskeyByCredentialId(response.id.bytes) ?: throw Forbidden()
        val rp =
            relyingParty(fixedDataRepository(passkey.citizenUserId, emptyList(), listOf(passkey)))
        val result =
            try {
                rp.finishAssertion(
                    FinishAssertionOptions.builder().request(request).response(response).build()
                )
            } catch (e: Exception) {
                throw Forbidden(cause = e)
            }
        if (!result.isSuccess || !result.isUserVerified) throw Forbidden()
        if (!result.isSignatureCounterValid) {
            logger.warn {
                "Passkey ${passkey.id} reported a non-incrementing signature counter " +
                    "(stored ${passkey.signatureCounter}, got ${result.signatureCount})"
            }
        }
        return FinishedPasskeyAssertion(
            person = passkey.citizenUserId,
            passkey = passkey,
            signatureCounter = result.signatureCount,
        )
    }

    /**
     * The library resolves credentials through this interface during ceremonies. All needed rows
     * are fetched before the ceremony, so the repository serves fixed data and never touches the
     * database.
     */
    private fun fixedDataRepository(
        person: PersonId?,
        excludeCredentialIds: List<ByteArray>,
        credentials: List<PasskeyCredential>,
    ): CredentialRepository =
        object : CredentialRepository {
            override fun getCredentialIdsForUsername(
                username: String
            ): Set<PublicKeyCredentialDescriptor> =
                excludeCredentialIds
                    .map {
                        PublicKeyCredentialDescriptor.builder().id(WebAuthnByteArray(it)).build()
                    }
                    .toSet()

            override fun getUserHandleForUsername(username: String): Optional<WebAuthnByteArray> =
                Optional.ofNullable(person?.toUserHandle())

            override fun getUsernameForUserHandle(userHandle: WebAuthnByteArray): Optional<String> =
                Optional.ofNullable(person?.takeIf { it.toUserHandle() == userHandle }?.toString())

            override fun lookup(
                credentialId: WebAuthnByteArray,
                userHandle: WebAuthnByteArray,
            ): Optional<RegisteredCredential> =
                Optional.ofNullable(
                    credentials
                        .find { WebAuthnByteArray(it.credentialId) == credentialId }
                        ?.toRegistered()
                )

            override fun lookupAll(credentialId: WebAuthnByteArray): Set<RegisteredCredential> =
                credentials
                    .filter { WebAuthnByteArray(it.credentialId) == credentialId }
                    .map { it.toRegistered() }
                    .toSet()
        }

    private fun PasskeyCredential.toRegistered(): RegisteredCredential =
        RegisteredCredential.builder()
            .credentialId(WebAuthnByteArray(credentialId))
            .userHandle(citizenUserId.toUserHandle())
            .publicKeyCose(WebAuthnByteArray(publicKey))
            .signatureCount(signatureCounter)
            .build()
}

private fun PersonId.toUserHandle(): WebAuthnByteArray {
    val buffer = ByteBuffer.allocate(16)
    buffer.putLong(raw.mostSignificantBits)
    buffer.putLong(raw.leastSignificantBits)
    return WebAuthnByteArray(buffer.array())
}

private fun WebAuthnByteArray.toUuid(): UUID {
    val buffer = ByteBuffer.wrap(bytes)
    return UUID(buffer.long, buffer.long)
}
