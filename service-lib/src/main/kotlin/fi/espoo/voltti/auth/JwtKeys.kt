// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.auth

import com.auth0.jwt.interfaces.RSAKeyProvider
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.Base64Variants
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStream
import java.math.BigInteger
import java.net.URI
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

class JwtKeys(
    private val privateKeyId: String?,
    private val privateKey: RSAPrivateKey?,
    private val publicKeys: Map<String, RSAPublicKey>
) : RSAKeyProvider {
    override fun getPrivateKeyId(): String? = privateKeyId
    override fun getPrivateKey(): RSAPrivateKey? = privateKey
    override fun getPublicKeyById(keyId: String): RSAPublicKey? = publicKeys[keyId]
}

fun loadPrivateKey(inputStream: InputStream): RSAPrivateKey {
    val kf = KeyFactory.getInstance("RSA")
    val text = inputStream.bufferedReader().readLines().filterNot { it.startsWith('-') }.joinToString() // skip BEGIN/END lines
    val privateKeySpec = PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(text))
    return kf.generatePrivate(privateKeySpec) as RSAPrivateKey
}

fun loadPublicKeys(inputStream: InputStream): Map<String, RSAPublicKey> {
    val kf = KeyFactory.getInstance("RSA")
    return jacksonObjectMapper().apply {
        setBase64Variant(Base64Variants.MODIFIED_FOR_URL)
    }.readValue<JwkSet>(inputStream).keys
        .map { it.kid to kf.generatePublic(RSAPublicKeySpec(BigInteger(1, it.n), BigInteger(1, it.e))) as RSAPublicKey }
        .toMap()
}

@JsonIgnoreProperties(ignoreUnknown = true)
private class Jwk(val kid: String, val n: ByteArray, val e: ByteArray)
private class JwkSet(val keys: List<Jwk>)

@Deprecated("use the InputStream version of this function if possible")
fun loadPrivateKey(pem: URI): RSAPrivateKey = pem.toURL().openStream().use { loadPrivateKey(it) }

@Deprecated("use the InputStream version of this function if possible")
fun loadPublicKeys(jwks: URI): Map<String, RSAPublicKey> = jwks.toURL().openStream().use { loadPublicKeys(it) }
