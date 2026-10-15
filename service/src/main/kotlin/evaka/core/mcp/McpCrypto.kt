// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

private val secureRandom = SecureRandom()
private val base64Url = Base64.getUrlEncoder().withoutPadding()

/** Generates a random, URL-safe secret (256 bits of entropy) */
fun generateMcpSecret(): String {
    val bytes = ByteArray(32)
    secureRandom.nextBytes(bytes)
    return base64Url.encodeToString(bytes)
}

/** Returns the SHA-256 hash of the given string as base64url. Used to store tokens at rest. */
fun sha256Base64Url(value: String): String =
    base64Url.encodeToString(
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.US_ASCII))
    )

/** PKCE S256: BASE64URL(SHA256(ASCII(code_verifier))) == code_challenge */
fun verifyPkceS256(codeVerifier: String, codeChallenge: String): Boolean {
    if (codeVerifier.length !in 43..128) return false
    return MessageDigest.isEqual(
        sha256Base64Url(codeVerifier).toByteArray(),
        codeChallenge.toByteArray(),
    )
}
