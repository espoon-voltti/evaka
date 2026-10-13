// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.identity

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class SsnTest {
    @Test
    fun `parse accepts a valid SSN`() {
        assertEquals("010199-8137", Ssn.parse("010199-8137")?.value)
    }

    @Test
    fun `parse rejects an invalid SSN`() {
        assertNull(Ssn.parse(""))
        assertNull(Ssn.parse("010199-8138"))
        assertNull(Ssn.parse("NoID"))
    }

    @Test
    fun `toString does not show the whole SSN`() {
        val ssn = Ssn.parse("010199-8137")!!

        assertEquals("010199*****", ssn.toString())
        assertEquals("SSN 010199*****", "SSN $ssn")
    }
}
