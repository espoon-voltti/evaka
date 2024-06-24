// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver
import kotlin.test.assertEquals
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class SealedSubclassSimpleNameTest {
    private val jsonMapper = defaultJsonMapperBuilder().build()

    @JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
    @JsonTypeIdResolver(SealedSubclassSimpleName::class)
    sealed interface Super {
        data class Sub1(
            val a: String
        ) : Super

        @JsonTypeName("OTHER")
        data class Sub2(
            val b: String
        ) : Super

        data object Sub3 : Super {
            val c: String = "value-c"
        }
    }

    private fun serialize(value: Any): JsonNode = jsonMapper.readTree(jsonMapper.writeValueAsString(value))

    private inline fun <reified T : Any> deserialize(tree: JsonNode): T = jsonMapper.treeToValue(tree, T::class.java)

    private fun parse(
        @Language("json") json: String
    ): JsonNode = jsonMapper.readTree(json)

    @Test
    fun `data class with no extra configuration is serialized and deserialized correctly`() {
        val value = Super.Sub1("value-a")

        val serialized = serialize(value)
        assertEquals(parse("""{"type": "Sub1", "a": "value-a"}"""), serialized)

        assertEquals(value, deserialize<Super>(serialized))
        assertEquals(value, deserialize<Super.Sub1>(serialized))
    }

    @Test
    fun `data class with a name override is serialized and deserialized correctly`() {
        val value = Super.Sub2("value-b")

        val serialized = serialize(value)
        assertEquals(parse("""{"type": "OTHER", "b": "value-b"}"""), serialized)

        assertEquals(value, deserialize<Super>(serialized))
        assertEquals(value, deserialize<Super.Sub2>(serialized))
    }

    @Test
    fun `data object with no extra configuration is serialized and deserialized correctly`() {
        val value = Super.Sub3

        val serialized = serialize(value)
        assertEquals(parse("""{"type": "Sub3", "c": "value-c"}"""), serialized)

        // When deserializing, Jackson expects to find only the type field, so we can't actually
        // deserialize what we just serialized
        val json = parse("""{"type": "Sub3"}""")
        assertEquals(value, deserialize<Super>(json))
        assertEquals(value, deserialize<Super.Sub3>(json))
    }
}
