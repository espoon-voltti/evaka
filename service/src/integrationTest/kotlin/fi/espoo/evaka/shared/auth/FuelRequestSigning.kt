// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.github.kittinunf.fuel.core.Request
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Clock
import java.time.ZonedDateTime
import org.bouncycastle.util.encoders.Base64

fun Request.asUser(user: AuthenticatedUser): Request {
    this.header("Authorization", "Bearer $emptyJwt")
    this.header("X-User", jsonMapper().writeValueAsString(user))
    return this
}

private val privateKeyText =
    """
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
JDuPQL7iuc5ASUjOrGoAjswTGWjDDw==
    """
        .trimIndent()

private val algorithm: Algorithm by lazy {
    val kf = KeyFactory.getInstance("RSA")
    val pk = kf.generatePrivate(PKCS8EncodedKeySpec(Base64.decode(privateKeyText))) as RSAPrivateKey
    Algorithm.RSA256(null, pk)
}

private val emptyJwt: String by lazy {
    val now = ZonedDateTime.now(Clock.systemDefaultZone())
    JWT.create()
        .withKeyId("integration-test")
        .withIssuer("integration-test")
        .withIssuedAt(now.toInstant())
        .withExpiresAt(now.plusHours(12).toInstant())
        .sign(algorithm)
}
