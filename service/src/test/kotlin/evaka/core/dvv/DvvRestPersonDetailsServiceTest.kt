// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dvv

import evaka.core.identity.ExternalIdentifier
import evaka.core.shared.EvakaUserId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.domain.NotFound
import evaka.core.vtjclient.dto.VtjPerson
import evaka.core.vtjclient.service.persondetails.IPersonDetailsService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Contract of the adapter that lets `PersonService` import a person over REST. The mapping itself
 * is covered by [DvvPerustiedotMapperTest]; what matters here is the translation between the
 * single-person interface eVaka calls and the batch endpoint DVV offers.
 */
class DvvRestPersonDetailsServiceTest {
    private val ssn = "010192-921W"
    private val client: DvvPerustiedotPocClient = mock()
    private val service = DvvRestPersonDetailsService(client)

    private val query =
        IPersonDetailsService.DetailsQuery(
            requestingUser = EvakaUserId(AuthenticatedUser.SystemInternalUser.rawId()),
            targetIdentifier = ExternalIdentifier.SSN.getInstance(ssn),
        )

    private fun person(ssn: String) =
        VtjPerson(
            firstNames = "Hanna",
            lastName = "Kolehmainen Tes",
            socialSecurityNumber = ssn,
            restrictedDetails = null,
        )

    @Test
    fun `queries the single requested SSN and returns the mapped person`() {
        whenever(client.getPerustiedotAsVtjPersons(any(), any(), any()))
            .thenReturn(listOf(person(ssn)))

        val result = service.getBasicDetailsFor(query)

        assertEquals(ssn, result.socialSecurityNumber)
        verify(client).getPerustiedotAsVtjPersons(eq(listOf(ssn)), any(), eq(emptyList()))
    }

    /**
     * DVV answers 200 with the person silently absent for an unknown hetu, so an empty list is the
     * not-found signal rather than an error status.
     */
    @Test
    fun `an unknown SSN becomes NotFound rather than an empty result`() {
        whenever(client.getPerustiedotAsVtjPersons(any(), any(), any())).thenReturn(emptyList())

        assertFailsWith<NotFound> { service.getBasicDetailsFor(query) }
    }

    @Test
    fun `guardian and dependant hydration are refused, not silently empty`() {
        assertFailsWith<IllegalStateException> { service.getPersonWithDependants(query) }
        assertFailsWith<IllegalStateException> { service.getPersonWithGuardians(query) }
    }
}
