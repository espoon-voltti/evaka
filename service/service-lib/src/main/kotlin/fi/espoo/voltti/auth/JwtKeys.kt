// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.auth

import com.auth0.jwt.interfaces.RSAKeyProvider
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.Base64Variants
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStream
import java.math.BigInteger
import java.net.URI
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec

class JwtKeys(
    private val publicKeys: Map<String, RSAPublicKey>
) : RSAKeyProvider {
    override fun getPrivateKeyId(): String? = null

    override fun getPrivateKey(): RSAPrivateKey? = null

    override fun getPublicKeyById(keyId: String): RSAPublicKey? = publicKeys[keyId]
}

fun loadPublicKeys(inputStream: InputStream): Map<String, RSAPublicKey> {
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Jwk(
        val kid: String,
        val n: ByteArray,
        val e: ByteArray
    )

    class JwkSet(
        val keys: List<Jwk>
    )

    val kf = KeyFactory.getInstance("RSA")
    return jacksonMapperBuilder()
        .defaultBase64Variant(Base64Variants.MODIFIED_FOR_URL)
        .build()
        .readValue<JwkSet>(inputStream)
        .keys
        .associate {
            it.kid to
                kf.generatePublic(
                    RSAPublicKeySpec(
                        BigInteger(1, it.n),
                        BigInteger(1, it.e)
                    )
                ) as RSAPublicKey
        }
}

@Deprecated("use the InputStream version of this function if possible")
fun loadPublicKeys(jwks: URI): Map<String, RSAPublicKey> = jwks.toURL().openStream().use { loadPublicKeys(it) }
