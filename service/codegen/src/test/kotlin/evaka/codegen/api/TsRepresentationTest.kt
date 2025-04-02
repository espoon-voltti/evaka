// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.api

import com.fasterxml.jackson.annotation.JsonValue
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TsRepresentationTest {
    enum class BasicEnum {
        A,
        B,
    }

    @Test
    fun `TsStringEnum works for basicEnum`() {
        val tsEnum = TsStringEnum(BasicEnum::class)
        assertEquals("BasicEnum", tsEnum.name)
        assertEquals(listOf("A", "B"), tsEnum.values)
    }

    enum class ComplexEnum(@JsonValue val value: String) {
        A("a"),
        B("b"),
    }

    @Test
    fun `TsStringEnum prohibits enums with JsonValue field`() {
        assertThrows<IllegalStateException> { TsStringEnum(ComplexEnum::class) }
    }
}
