// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.identity

import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.shared.config.defaultJsonMapper
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class ExternalIdTest {
    private val jsonMapper = defaultJsonMapper()

    @Test
    fun `external ids can serialized to and from json`() {
        val id = ExternalId.of("espoo-ad", "123456")
        val json = jsonMapper.writeValueAsString(id)
        assertEquals("\"espoo-ad:123456\"", json)
        assertEquals(id, jsonMapper.readValue<ExternalId>(json))
    }

    @Test
    fun `external ids have a string representation and can be parsed from it`() {
        val id = ExternalId.of("espoo-ad", "123456")
        val text = id.toString()
        assertEquals("espoo-ad:123456", text)
        assertEquals(id, ExternalId.parse(text))
    }
}
