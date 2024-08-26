// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.ktlint

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class NoPrintTest {
    private val assertThatRule =
        com.pinterest.ktlint.test.KtLintAssertThat.assertThatRule { NoPrint() }

    private fun assertThatCode(@Language("kotlin") code: String) = assertThatRule(code)

    @Test
    fun `NoPrintln detects println call`() {
        assertThatCode(
                """
fun testing() {
    println("test")
}
"""
            )
            .hasLintViolationWithoutAutoCorrect(line = 3, col = 5, detail = NoPrint.ERROR_MESSAGE)
    }

    @Test
    fun `NoPrintln detects print call`() {
        assertThatCode(
                """
fun testing() {
    print("test")
}
"""
            )
            .hasLintViolationWithoutAutoCorrect(line = 3, col = 5, detail = NoPrint.ERROR_MESSAGE)
    }

    @Test
    fun `NoPrintln does not complain about a logger function`() {
        assertThatCode(
                """
fun testing() {
    logger.info("test")
}
"""
            )
            .hasNoLintViolations()
    }
}
