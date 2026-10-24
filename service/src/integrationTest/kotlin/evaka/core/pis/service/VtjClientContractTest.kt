// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.identity.Ssn
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.dev.DevPerson
import evaka.core.vtjclient.dto.VtjRelative
import evaka.core.vtjclient.mapper.VtjHenkiloMapper
import evaka.core.vtjclient.service.persondetails.MockVtjClient
import evaka.core.vtjclient.service.persondetails.SoapVtjClient
import evaka.core.vtjclient.service.persondetails.VtjClient
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.RequestType.HUOLLETTAVA_HUOLTAJAT
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.RequestType.HUOLTAJA_HUOLLETTAVA
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

/**
 * A relations query returns the person in full and each relative as a stub: SSN, date of birth and
 * names. Subclass this for every new [VtjClient].
 */
abstract class VtjClientContract {
    protected val guardian =
        DevPerson(
            dateOfBirth = LocalDate.of(1981, 2, 22),
            firstName = "Mikael",
            lastName = "Högfors",
            ssn = "220281-9456",
            language = "fi",
            streetAddress = "Kamreerintie 4",
            postalCode = "02100",
            postOffice = "Espoo",
        )

    protected val child =
        DevPerson(
            dateOfBirth = LocalDate.of(2013, 10, 7),
            firstName = "Antero",
            lastName = "Högfors",
            ssn = "071013A960W",
            language = "fi",
            streetAddress = "Kamreerintie 4",
            postalCode = "02100",
            postOffice = "Espoo",
        )

    /** Seed the implementation's backing store with [guardian] as the parent of [child]. */
    protected abstract fun createClientReturningTheFamily(): VtjClient

    protected fun query(person: DevPerson) =
        VtjClient.DetailsQuery(
            AuthenticatedUser.SystemInternalUser.evakaUserId,
            Ssn.parse(person.ssn!!)!!,
        )

    @Test
    fun `dependants are stubs`() {
        val result = createClientReturningTheFamily().fetchWithDependants(query(guardian))

        assertEquals(guardian.ssn, result.person.socialSecurityNumber)
        assertEquals(guardian.streetAddress, result.person.address?.streetAddress)
        assertEquals(listOf(child.toStub()), result.dependants)
    }

    @Test
    fun `guardians are stubs`() {
        val result = createClientReturningTheFamily().fetchWithGuardians(query(child))

        assertEquals(child.ssn, result.person.socialSecurityNumber)
        assertEquals(child.streetAddress, result.person.address?.streetAddress)
        assertEquals(listOf(guardian.toStub()), result.guardians)
    }

    private fun DevPerson.toStub() =
        VtjRelative(
            ssn = Ssn.parse(ssn!!)!!,
            dateOfBirth = dateOfBirth,
            firstNames = firstName,
            lastName = lastName,
        )
}

class SoapVtjClientContractTest : VtjClientContract() {
    override fun createClientReturningTheFamily(): VtjClient {
        MockVtjSoapTransport.resetQueryCounts()
        MockVtjSoapTransport.addPERUSSANOMA3RequestExpectation(guardian)
        MockVtjSoapTransport.addPERUSSANOMA3RequestExpectation(child)
        MockVtjSoapTransport.addHUOLTAJAHUOLLETTAVARequestExpectation(guardian, listOf(child))
        MockVtjSoapTransport.addHUOLLETTAVAHUOLTAJATRequestExpectation(child, listOf(guardian))
        return SoapVtjClient(MockVtjSoapTransport(), VtjHenkiloMapper())
    }

    @Test
    fun `a relations query is one request`() {
        val client = createClientReturningTheFamily()

        client.fetchWithDependants(query(guardian))
        client.fetchWithGuardians(query(child))

        MockVtjSoapTransport.assertRequests(
            HUOLTAJA_HUOLLETTAVA to guardian,
            HUOLLETTAVA_HUOLTAJAT to child,
        )
    }
}

class MockVtjClientContractTest : VtjClientContract() {
    override fun createClientReturningTheFamily(): VtjClient {
        MockVtjClient.reset()
        MockVtjClient.addPersons(guardian, child)
        MockVtjClient.addDependants(guardian, child)
        return MockVtjClient()
    }
}
