// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.controller

import com.upokecenter.cbor.CBORObject
import evaka.core.shared.config.defaultJsonMapperBuilder
import java.nio.ByteBuffer
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.readValue

/**
 * A WebAuthn authenticator in software: generates a P-256 key pair, produces registration and
 * assertion responses in the JSON format browsers return from navigator.credentials.create/get.
 */
class SoftwareAuthenticator(
    private val rpId: String = "localhost",
    private val origin: String = "http://localhost:9099",
    var userVerified: Boolean = true,
    /** 0 simulates a synced passkey whose signature counter never increments */
    private val signCountStep: Long = 1,
) {
    private val json =
        defaultJsonMapperBuilder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()
    private val base64Url = Base64.getUrlEncoder().withoutPadding()
    private val keyPair =
        KeyPairGenerator.getInstance("EC")
            .apply { initialize(ECGenParameterSpec("secp256r1")) }
            .generateKeyPair()

    val credentialId: ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) }

    /** Identifies the authenticator model, so all zeroes for a software one */
    private val aaguid = ByteArray(16)
    private var signCount: Long = 0
    private var userHandle: ByteArray? = null

    /** Takes the JSON argument of navigator.credentials.create() and returns its response */
    fun create(credentialsCreateJson: String): String {
        val publicKey = json.readValue<CredentialCreationOptions>(credentialsCreateJson).publicKey
        userHandle = Base64.getUrlDecoder().decode(publicKey.user.id)

        val clientData = clientDataJson("webauthn.create", publicKey.challenge)
        val authData =
            authenticatorData(flags(attestedCredentialIncluded = true)) + attestedCredentialData()
        val attestationObject =
            CBORObject.NewMap()
                .Add("fmt", "none")
                .Add("attStmt", CBORObject.NewMap())
                .Add("authData", authData)
                .EncodeToBytes()

        return json.writeValueAsString(
            RegistrationResponseJson(
                id = base64Url.encodeToString(credentialId),
                rawId = base64Url.encodeToString(credentialId),
                response =
                    RegistrationResponseJson.Response(
                        clientDataJSON = base64Url.encodeToString(clientData),
                        attestationObject = base64Url.encodeToString(attestationObject),
                    ),
            )
        )
    }

    /** Takes the JSON argument of navigator.credentials.get() and returns its response */
    fun get(credentialsGetJson: String): String {
        val publicKey = json.readValue<CredentialRequestOptions>(credentialsGetJson).publicKey

        signCount += signCountStep
        val clientData = clientDataJson("webauthn.get", publicKey.challenge)
        val authData = authenticatorData(flags(attestedCredentialIncluded = false))
        val signature =
            Signature.getInstance("SHA256withECDSA")
                .apply {
                    initSign(keyPair.private)
                    update(authData + sha256(clientData))
                }
                .sign()

        return json.writeValueAsString(
            AuthenticationResponseJson(
                id = base64Url.encodeToString(credentialId),
                rawId = base64Url.encodeToString(credentialId),
                response =
                    AuthenticationResponseJson.Response(
                        clientDataJSON = base64Url.encodeToString(clientData),
                        authenticatorData = base64Url.encodeToString(authData),
                        signature = base64Url.encodeToString(signature),
                        userHandle = userHandle?.let { base64Url.encodeToString(it) },
                    ),
            )
        )
    }

    private fun clientDataJson(type: String, challenge: String): ByteArray =
        json.writeValueAsString(ClientData(type, challenge, origin)).toByteArray()

    /** https://www.w3.org/TR/webauthn-3/#table-authData */
    private fun flags(attestedCredentialIncluded: Boolean): Int {
        var flags = 0x01 // UP: user present
        if (userVerified) flags = flags or 0x04 // UV: user verified
        if (attestedCredentialIncluded) flags = flags or 0x40 // AT: attested credential data
        return flags
    }

    /** https://www.w3.org/TR/webauthn-3/#sctn-authenticator-data */
    private fun authenticatorData(flags: Int): ByteArray =
        ByteBuffer.allocate(32 + 1 + 4)
            .put(sha256(rpId.toByteArray()))
            .put(flags.toByte())
            .putInt(signCount.toInt())
            .array()

    /**
     * https://www.w3.org/TR/webauthn-3/#sctn-attested-credential-data
     *
     * The public key is a COSE_Key, whose labels and values come from
     * https://www.iana.org/assignments/cose/cose.xhtml
     */
    private fun attestedCredentialData(): ByteArray {
        val publicKey = keyPair.public as ECPublicKey
        val coseKey =
            CBORObject.NewMap()
                .Add(1, 2) // kty: EC2
                .Add(3, -7) // alg: ES256
                .Add(-1, 1) // crv: P-256
                .Add(-2, publicKey.w.affineX.toFixedLength())
                .Add(-3, publicKey.w.affineY.toFixedLength())
                .EncodeToBytes()
        return ByteBuffer.allocate(16 + 2 + credentialId.size + coseKey.size)
            .put(aaguid)
            .putShort(credentialId.size.toShort())
            .put(credentialId)
            .put(coseKey)
            .array()
    }

    /**
     * A COSE_Key coordinate must preserve leading zeroes, so it's always as long as the curve field
     * (32 bytes for P-256), unlike the minimal encoding of BigInteger
     */
    private fun java.math.BigInteger.toFixedLength(): ByteArray {
        val bytes = toByteArray()
        val result = ByteArray(32)
        bytes.copyInto(
            result,
            destinationOffset = maxOf(0, 32 - bytes.size),
            startIndex = maxOf(0, bytes.size - 32),
        )
        return result
    }

    private fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)
}

private data class CredentialCreationOptions(val publicKey: PublicKey) {
    data class PublicKey(val challenge: String, val user: User)

    data class User(val id: String)
}

private data class CredentialRequestOptions(val publicKey: PublicKey) {
    data class PublicKey(val challenge: String)
}

private data class RegistrationResponseJson(
    val id: String,
    val rawId: String,
    val response: Response,
    val type: String = "public-key",
    val clientExtensionResults: Map<String, Any> = emptyMap(),
) {
    data class Response(
        val clientDataJSON: String,
        val attestationObject: String,
        val transports: List<String> = listOf("internal"),
    )
}

private data class AuthenticationResponseJson(
    val id: String,
    val rawId: String,
    val response: Response,
    val type: String = "public-key",
    val clientExtensionResults: Map<String, Any> = emptyMap(),
) {
    data class Response(
        val clientDataJSON: String,
        val authenticatorData: String,
        val signature: String,
        val userHandle: String?,
    )
}

private data class ClientData(val type: String, val challenge: String, val origin: String)
