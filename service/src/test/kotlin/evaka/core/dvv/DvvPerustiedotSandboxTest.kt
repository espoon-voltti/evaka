// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dvv

import evaka.core.DvvPerustiedotPocEnv
import evaka.core.Sensitive
import evaka.core.shared.config.defaultJsonMapperBuilder
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.Tag
import tools.jackson.databind.json.JsonMapper

/**
 * Live call against DVV's public sandbox — excluded from the default `test` task because it needs
 * network access and DVV's uptime. Run with `./gradlew dvvSandboxTest`.
 *
 * Asserts only what a captured fixture cannot: that DVV **accepts** our serialised request.
 * `PerustietoRequest` declares `additionalProperties: false`, so [DvvPerustiedotRequestTest] can
 * check what JSON we emit, but only the real service says whether it is acceptable.
 *
 * Assertions are deliberately thin — the sandbox's synthetic persons are DVV's to change without
 * notice. Mapping detail belongs to [DvvPerustiedotMapperTest] and cross-interface claims to
 * [DvvSoapRestParityTest]. Asserting the requested hetu comes back is a real check, not a
 * formality: DVV answers `200` with the person silently absent for an unknown hetu or an
 * out-of-scope tietoryhmä.
 *
 * Credentials and SSNs are DVV's published demo values; the sandbox holds only synthetic persons.
 */
@Tag("dvvSandbox")
class DvvPerustiedotSandboxTest {
    private val jsonMapper: JsonMapper = defaultJsonMapperBuilder().build()

    private val env =
        DvvPerustiedotPocEnv(
            url = "https://api.hiekkalaatikko.muutostietopalvelu.dvv.fi/api/v1",
            userId = "mutpT1x",
            password = Sensitive("pwd"),
        )

    private val ssn = "010188-916P"
    private val today = LocalDate.of(2026, 7, 29)

    @Test
    fun `the sandbox accepts our request and returns the person we asked for`() {
        val client = DvvPerustiedotPocClient(jsonMapper, env)

        val persons = client.getPerustiedotAsVtjPersons(listOf(ssn), today)

        assertEquals(listOf(ssn), persons.map { it.socialSecurityNumber })
    }

    @Test
    fun `an explicit tietoryhmat filter is accepted and honoured`() {
        val client = DvvPerustiedotPocClient(jsonMapper, env)

        val persons =
            client.getPerustiedotAsVtjPersons(
                listOf(ssn),
                today,
                tietoryhmat = listOf("HENKILON_NIMI"),
            )

        assertEquals(listOf(ssn), persons.map { it.socialSecurityNumber })
        assertEquals(true, persons.single().firstNames.isNotBlank())
    }
}
