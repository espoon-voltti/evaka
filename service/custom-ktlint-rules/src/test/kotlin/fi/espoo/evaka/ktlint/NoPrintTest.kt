// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.ktlint

import org.junit.jupiter.api.Test
import com.pinterest.ktlint.test.lint
import org.junit.jupiter.api.Assertions.assertTrue

class NoPrintTest {
    @Test
    fun `NoPrintln detects println call`() {
        val lintErrors = NoPrint().lint("""fun testing() { println("test") }""")
        assertTrue(lintErrors.isNotEmpty())
    }

    @Test
    fun `NoPrintln detects print call`() {
        val lintErrors = NoPrint().lint("""fun testing() { print("test") }""")
        assertTrue(lintErrors.isNotEmpty())
    }

    @Test
    fun `NoPrintln does not complain about a logger function`() {
        val lintErrors = NoPrint().lint("""fun testing() { logger.info("test") }""")
        assertTrue(lintErrors.isEmpty())
    }
}
