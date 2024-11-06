// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.Sensitive
import kotlin.test.*

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
}
