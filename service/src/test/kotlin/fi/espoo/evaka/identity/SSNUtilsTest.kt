// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.identity

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SSNUtilsTest {
    @Test
    fun `Parse 20th century ssn`() {
        val dob = getDobFromSsn("150799-0101")
        Assertions.assertThat(dob).isEqualTo("1999-07-15")
    }

    @Test
    fun `Parse 21th century ssn`() {
        val dob = getDobFromSsn("150715A0101")
        Assertions.assertThat(dob).isEqualTo("2015-07-15")
    }

    @Test
    fun `Invalid ssn century marker throws exception`() {
        assertThrows<IllegalArgumentException> { getDobFromSsn("150715B0101") }
    }

    @Test
    fun `SSN validation works for people born in 2020`() {
        val isValid = isValidSSN("090920A2786")
        assertTrue(isValid)
    }

    @Test
    fun `SSN validation works for people born in 1939`() {
        val isValid = isValidSSN("080839-616J")
        assertTrue(isValid)
    }

    @Test
    fun `SSN validation does not work with invalid check digit`() {
        val isValid = isValidSSN("191086-838I")
        assertFalse(isValid)
    }

    @Test
    fun `SSN validation does not work with invalid date`() {
        val isValid = isValidSSN("321086-838X")
        assertFalse(isValid)
    }
}
