// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.actionenum

import kotlin.test.Test
import kotlin.test.assertEquals

class GeneratorsTest {
    private enum class Empty

    @Suppress("unused")
    private enum class Single {
        VARIANT
    }

    @Suppress("unused")
    private enum class Multi {
        A,
        B,
        C
    }

    @Test
    fun `an enum with no variants should generate a type aliased to never`() {
        assertEquals("export type Empty = never\n", generateEnum<Empty>()())
    }

    @Test
    fun `an enum with a single variant should generate a one-liner type declaration`() {
        assertEquals("export type Single = 'VARIANT'\n", generateEnum<Single>()())
    }

    @Test
    fun `an enum with multiple variants should generate a multi-line union type declaration`() {
        assertEquals(
            """
export type Multi =
  | 'A'
  | 'B'
  | 'C'
""".trimStart(),
            generateEnum<Multi>()()
        )
    }
}
