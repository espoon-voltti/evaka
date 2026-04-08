// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.springframework.core.env.SystemEnvironmentPropertySource
import org.springframework.mock.env.MockEnvironment

class EvakaEnvTest {
    private fun env(vararg pairs: Pair<String, String>) =
        MockEnvironment().apply { pairs.forEach { (k, v) -> setProperty(k, v) } }

    private fun envVars(vararg pairs: Pair<String, String>) =
        MockEnvironment().apply {
            propertySources.addFirst(
                SystemEnvironmentPropertySource("systemEnvironment", pairs.toMap())
            )
        }

    @Test
    fun `looks up a string property`() {
        val e = env("some.key" to "hello")
        assertEquals("hello", e.lookup<String>("some.key"))
    }

    @Test
    fun `looks up a property with underscores in key`() {
        val e = env("some.snake_case_key" to "hello")
        assertEquals("hello", e.lookup<String>("some.snake_case_key"))
    }

    @Test
    fun `looks up a property with underscores in key from environment variable`() {
        val e = envVars("SOME_SNAKE_CASE_KEY" to "hello")
        assertEquals("hello", e.lookup<String>("some.snake_case_key"))
    }

    @Test
    fun `looks up an int property`() {
        val e = env("some.key" to "42")
        assertEquals(42, e.lookup<Int>("some.key"))
    }

    @Test
    fun `looks up a boolean property`() {
        val e = env("some.key" to "true")
        assertEquals(true, e.lookup<Boolean>("some.key"))
    }

    @Test
    fun `returns null for missing optional property`() {
        val e = env()
        assertNull(e.lookup<String?>("missing.key"))
    }

    @Test
    fun `throws for missing required property`() {
        val e = env()
        val ex = assertFailsWith<IllegalStateException> { e.lookup<String>("missing.key") }
        assertEquals(
            "Missing required configuration: missing.key (environment variable MISSING_KEY)",
            ex.message,
        )
    }

    @Test
    fun `falls back to deprecated key`() {
        val e = env("old.key" to "value")
        assertEquals("value", e.lookup<String>("new.key", "old.key"))
    }

    @Test
    fun `prefers deprecated key over current key`() {
        val e = env("new.key" to "new", "old.key" to "old")
        assertEquals("old", e.lookup<String>("new.key", "old.key"))
    }

    @Test
    fun `looks up a list of strings property`() {
        val e = env("some.key[0]" to "a", "some.key[1]" to "b")
        assertEquals(listOf("a", "b"), e.lookup<List<String>>("some.key"))
    }

    @Test
    fun `looks up a list of ints property`() {
        val e = env("some.key[0]" to "6", "some.key[1]" to "7")
        assertEquals(listOf(6, 7), e.lookup<List<Int>>("some.key"))
    }

    @Test
    fun `looks up a set of strings property`() {
        val e = env("some.key[0]" to "a", "some.key[1]" to "b")
        assertEquals(setOf("a", "b"), e.lookup<Set<String>>("some.key"))
    }

    @Test
    fun `looks up a set of ints property`() {
        val e = env("some.key[0]" to "6", "some.key[1]" to "7")
        assertEquals(setOf(6, 7), e.lookup<Set<Int>>("some.key"))
    }

    @Test
    fun `throws for missing list property`() {
        val e = env()
        assertFailsWith<IllegalStateException> { e.lookup<List<String>>("missing.key") }
    }
}
