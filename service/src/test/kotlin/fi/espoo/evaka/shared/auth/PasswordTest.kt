// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.Sensitive
import kotlin.test.*
import org.bouncycastle.crypto.params.Argon2Parameters

class PasswordTest {
    private val algorithm = PasswordHashAlgorithm.DEFAULT

    @Test
    fun `encoding the same raw password twice gives passwords that match the original but have different salt and hash values`() {
        val raw = Sensitive("password")

        val first = algorithm.encode(raw)
        assertTrue(first.isMatch(raw))

        val second = algorithm.encode(raw)
        assertTrue(second.isMatch(raw))

        assertNotEquals(first, second)
        assertEquals(first.algorithm, second.algorithm)
        assertNotEquals(first.salt, second.salt)
        assertNotEquals(first.hash, second.hash)
    }

    @Test
    fun `an encoded password does not match a different raw password`() {
        val password = algorithm.encode(Sensitive("password"))
        assertFalse(password.isMatch(Sensitive("wontmatch")))
    }

    @Test
    fun `an encoded password can be formatted to a string and parsed back to its original form`() {
        val password = algorithm.encode(Sensitive("password"))
        val parsed = EncodedPassword.parse(password.toString())
        assertEquals(password, parsed)
    }

    @Test
    fun `matching an Argon2 password originating from Keycloak is possible`() {
        // Raw example keycloak credential data from the `credential` database table:
        // secret_data:
        // {"value":"O7vS90g14jWr4ESbUpJ3KX5y1NMZcMgjuqPHrZ4Eq8U=","salt":"LgSKeCa5qZH6Dh0PE17AwQ==","additionalParameters":{}}
        // credential_data:
        // {"hashIterations":5,"algorithm":"argon2","additionalParameters":{"hashLength":["32"],"memory":["7168"],"type":["id"],"version":["1.3"],"parallelism":["1"]}}
        val password =
            EncodedPassword(
                PasswordHashAlgorithm.Argon2id(
                    hashLength = 32,
                    version = Argon2Parameters.ARGON2_VERSION_13,
                    m = 7168,
                    t = 5,
                    p = 1,
                ),
                EncodedPassword.Salt.parse("LgSKeCa5qZH6Dh0PE17AwQ=="),
                EncodedPassword.Hash.parse("O7vS90g14jWr4ESbUpJ3KX5y1NMZcMgjuqPHrZ4Eq8U="),
            )
        assertTrue(password.isMatch(Sensitive("testpassword")))
        // make sure it also survives a format/parse round-trip
        assertEquals(password, EncodedPassword.parse(password.toString()))
    }
}
