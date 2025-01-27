// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.Sensitive
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordConstraintsTest {
    private val unconstrained = PasswordConstraints.UNCONSTRAINED

    @Test
    fun `isPasswordStructureValid checks minLength correctly`() {
        val constraints = unconstrained.copy(minLength = 4)
        assertFalse(constraints.isPasswordStructureValid(Sensitive("123")))
        assertTrue(constraints.isPasswordStructureValid(Sensitive("1234")))
        assertTrue(constraints.isPasswordStructureValid(Sensitive("12345")))
    }

    @Test
    fun `isPasswordStructureValid checks maxLength correctly`() {
        val constraints = unconstrained.copy(maxLength = 4)
        assertFalse(constraints.isPasswordStructureValid(Sensitive("12345")))
        assertTrue(constraints.isPasswordStructureValid(Sensitive("1234")))
        assertTrue(constraints.isPasswordStructureValid(Sensitive("123")))
    }

    @Test
    fun `isPasswordStructureValid checks minLowers correctly`() {
        val constraints = unconstrained.copy(minLowers = 1)
        assertFalse(constraints.isPasswordStructureValid(Sensitive("1_2")))
        assertFalse(constraints.isPasswordStructureValid(Sensitive("1A2")))
        assertTrue(constraints.isPasswordStructureValid(Sensitive("1a2")))
        assertTrue(constraints.isPasswordStructureValid(Sensitive("1ab")))
        assertTrue(constraints.isPasswordStructureValid(Sensitive("1ä2")))
    }

    @Test
    fun `isPasswordStructureValid checks minUppers correctly`() {
        val constraints = unconstrained.copy(minUppers = 1)
        assertFalse(constraints.isPasswordStructureValid(Sensitive("1_2")))
        assertFalse(constraints.isPasswordStructureValid(Sensitive("1a2")))
        assertTrue(constraints.isPasswordStructureValid(Sensitive("1A2")))
        assertTrue(constraints.isPasswordStructureValid(Sensitive("1AB")))
        assertTrue(constraints.isPasswordStructureValid(Sensitive("1Ä2")))
    }

    @Test
    fun `isPasswordStructureValid checks minDigits correctly`() {
        val constraints = unconstrained.copy(minDigits = 1)
        assertFalse(constraints.isPasswordStructureValid(Sensitive("abc")))
        assertTrue(constraints.isPasswordStructureValid(Sensitive("a1c")))
        assertTrue(constraints.isPasswordStructureValid(Sensitive("a12")))
    }

    @Test
    fun `isPasswordStructureValid checks minSymbols correctly`() {
        val constraints = unconstrained.copy(minSymbols = 1)
        assertFalse(constraints.isPasswordStructureValid(Sensitive("123")))
        assertFalse(constraints.isPasswordStructureValid(Sensitive("abc")))
        assertTrue(constraints.isPasswordStructureValid(Sensitive("a#c")))
        assertTrue(constraints.isPasswordStructureValid(Sensitive("a#2")))
        assertTrue(constraints.isPasswordStructureValid(Sensitive("💩")))
    }
}
