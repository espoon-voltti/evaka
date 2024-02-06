// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LocalHmTest {
    private val jsonMapper = defaultJsonMapperBuilder().build()

    @Test
    fun `JSON serialization works`() {
        assertEquals("\"00:00\"", jsonMapper.writeValueAsString(LocalHm(0, 0)))
        assertEquals("\"23:59\"", jsonMapper.writeValueAsString(LocalHm(23, 59)))
    }

    @Test
    fun `JSON deserialization works`() {
        assertEquals(LocalHm(0, 0), jsonMapper.readValue("\"00:00\"", LocalHm::class.java))
        assertEquals(LocalHm(23, 59), jsonMapper.readValue("\"23:59\"", LocalHm::class.java))
        listOf("foo", "foo:bar", "24:00", "06:00:01").forEach {
            assertThrows<IllegalArgumentException> {
                jsonMapper.readValue("\"$it\"", LocalHm::class.java)
            }
        }
    }

    @Test
    fun `invalid timestamps cannot be constructed`() {
        assertThrows<IllegalArgumentException> { LocalHm(24, 0) }
        assertThrows<IllegalArgumentException> { LocalHm(0, 60) }
        assertThrows<IllegalArgumentException> { LocalHm(-1, 0) }
    }

    @Test
    fun `plusMinutes works`() {
        assertEquals(LocalHm(0, 0), LocalHm(0, 0).plusMinutes(0))
        assertEquals(LocalHm(2, 10), LocalHm(1, 50).plusMinutes(20))
        assertEquals(LocalHm(3, 50), LocalHm(1, 50).plusMinutes(120))
        assertNull(LocalHm(23, 59).plusMinutes(1))
    }

    @Test
    fun `minusMinutes works`() {
        assertEquals(LocalHm(0, 0), LocalHm(0, 0).minusMinutes(0))
        assertEquals(LocalHm(1, 50), LocalHm(2, 10).minusMinutes(20))
        assertEquals(LocalHm(1, 50), LocalHm(3, 50).minusMinutes(120))
        assertNull(LocalHm(0, 0).minusMinutes(1))
    }
}
