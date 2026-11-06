// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.dvv.DvvModificationsServiceIntegrationTestBase
import evaka.core.dvv.MockDvvModificationsService
import evaka.core.identity.Ssn
import evaka.core.shared.auth.AuthenticatedUser
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DvvVtjClientIntegrationTest :
    DvvModificationsServiceIntegrationTestBase(resetDbBeforeEach = false) {
    private val user = AuthenticatedUser.SystemInternalUser.evakaUserId
    private val activeSsn = Ssn.from("010192-921W")
    private val correctedSsn = Ssn.from("010192-912L")
    private val unknownSsn = Ssn.from("070644-937X")

    private fun client() = DvvVtjClient(dvvModificationsServiceClient)

    @Test
    fun `a person that DVV does not find has no key`() {
        val persons = client().fetchPersons(user, listOf(activeSsn, unknownSsn))

        assertEquals(setOf(activeSsn), persons.keys)
        val person = persons.getValue(activeSsn)
        assertEquals("Hanna", person.firstNames)
        assertEquals("Mikkolantie 30 F 28", person.residentialAddress?.streetAddress)
        assertEquals("Helsinki", person.municipalityOfResidence)
    }

    @Test
    fun `a corrected SSN gives the person of the active SSN`() {
        val persons = client().fetchPersons(user, listOf(correctedSsn))

        assertEquals(setOf(correctedSsn), persons.keys)
        assertEquals(activeSsn, persons.getValue(correctedSsn).ssn)
        assertEquals("Hanna", persons.getValue(correctedSsn).firstNames)
    }

    @Test
    fun `dependants come only from the query with the dependants`() {
        assertEquals(
            listOf(Ssn.from("120514A9501")),
            client().fetchWithDependants(user, listOf(activeSsn)).getValue(activeSsn).dependants,
        )
        assertEquals(
            emptyList(),
            client().fetchWithGuardians(user, listOf(activeSsn)).getValue(activeSsn).guardians,
        )
    }

    @Test
    fun `the rate limit of DVV fails the call`() {
        assertThrows<IllegalStateException> {
            client()
                .fetchPersons(user, listOf(Ssn.from(MockDvvModificationsService.RATE_LIMITED_SSN)))
        }
    }
}
