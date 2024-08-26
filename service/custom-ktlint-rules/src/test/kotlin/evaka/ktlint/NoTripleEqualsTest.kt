// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.ktlint

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class NoTripleEqualsTest {
    private val assertThatRule =
        com.pinterest.ktlint.test.KtLintAssertThat.assertThatRule { NoTripleEquals() }

    private fun assertThatCode(@Language("kotlin") code: String) = assertThatRule(code)

    @Test
    fun `using the triple equals operator triggers a lint violation`() {
        assertThatCode(
                """
fun testing(input: Int): Boolean =
    input === 1
"""
            )
            .hasLintViolation(line = 3, col = 11, detail = NoTripleEquals.ERROR_MESSAGE)
    }

    @Test
    fun `using the triple not equals operator triggers a lint violation`() {
        assertThatCode(
                """
fun testing(input: Int): Boolean =
    input !== 1
"""
            )
            .hasLintViolation(line = 3, col = 11, detail = NoTripleEquals.ERROR_MESSAGE)
    }

    @Test
    fun `normal equals and not equals operators don't trigger lint violations`() {
        assertThatCode(
                """
fun testing(input: Int): Boolean =
    input == 1 || input != 9
"""
            )
            .hasNoLintViolations()
    }
}
