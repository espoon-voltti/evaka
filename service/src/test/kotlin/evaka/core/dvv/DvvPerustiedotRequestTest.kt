// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dvv

import evaka.core.shared.config.defaultJsonMapperBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import tools.jackson.databind.json.JsonMapper

class DvvPerustiedotRequestTest {
    private val jsonMapper: JsonMapper = defaultJsonMapperBuilder().build()

    @Test
    fun `omits tietoryhmat entirely when not specified`() {
        // DVV's PerustietoRequest schema declares additionalProperties=false and types tietoryhmat
        // as an array, so an explicit null is not a safe way to say "give me everything"
        assertEquals(
            """{"hetulista":["010101-123N"]}""",
            jsonMapper.writeValueAsString(
                DvvPerustiedotRequest(hetulista = listOf("010101-123N"), tietoryhmat = null)
            ),
        )
    }

    @Test
    fun `includes tietoryhmat when specified`() {
        assertEquals(
            """{"hetulista":["010101-123N"],"tietoryhmat":["HENKILON_NIMI","HUOLTAJA"]}""",
            jsonMapper.writeValueAsString(
                DvvPerustiedotRequest(
                    hetulista = listOf("010101-123N"),
                    tietoryhmat = listOf("HENKILON_NIMI", "HUOLTAJA"),
                )
            ),
        )
    }
}
