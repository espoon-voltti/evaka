// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.identity

import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SSNUtilsTest {
    @Test
    fun `Parse 20th century ssn`() {
        assertEquals(LocalDate.of(1999, 7, 15), getDobFromSsn("150799-0101"))
        assertEquals(LocalDate.of(1999, 7, 15), getDobFromSsn("150799X0101"))
    }

    @Test
    fun `Parse 21th century ssn`() {
        assertEquals(LocalDate.of(2015, 7, 15), getDobFromSsn("150715A0101"))
        assertEquals(LocalDate.of(2015, 7, 15), getDobFromSsn("150715B0101"))
    }

    @Test
    fun `Invalid ssn century marker throws exception`() {
        assertThrows<IllegalArgumentException> { getDobFromSsn("150715J0101") }
    }

    @Test
    fun `SSN validation works for people born in 2020`() {
        assertTrue(isValidSSN("090920A2786"))
        assertTrue(isValidSSN("090920F2786"))
    }

    @Test
    fun `SSN validation works for people born in 1939`() {
        assertTrue(isValidSSN("080839-616J"))
        assertTrue(isValidSSN("080839U616J"))
    }

    @Test
    fun `SSN validation does not work with invalid check digit`() {
        assertFalse(isValidSSN("191086-838I"))
    }

    @Test
    fun `SSN validation does not work with invalid date`() {
        assertFalse(isValidSSN("321086-838X"))
    }
}
