// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtjclient.service.persondetails

import evaka.core.identity.Ssn
import evaka.core.shared.EvakaUserId
import evaka.core.vtjclient.dto.Nationality
import evaka.core.vtjclient.dto.NativeLanguage
import evaka.core.vtjclient.dto.PersonAddress
import evaka.core.vtjclient.dto.RestrictedDetails
import evaka.core.vtjclient.dto.VtjPerson
import evaka.core.vtjclient.dto.VtjRelative
import evaka.core.vtjclient.mapper.VtjHenkiloMapper
import evaka.core.vtjclient.service.persondetails.VtjClient.DetailsQuery
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.RequestType.HUOLTAJA_HUOLLETTAVA
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.RequestType.PERUSSANOMA3
import evaka.core.vtjclient.soap.VTJHenkiloVastaussanoma.Henkilo
import java.time.LocalDate
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class SoapVtjClientTest {

    @InjectMocks lateinit var service: SoapVtjClient

    @Mock lateinit var vtjService: VtjSoapTransport

    @Mock lateinit var henkiloMapper: VtjHenkiloMapper

    @Test
    fun `basic query should return the person with a successful query`() {
        val expectedVtjQuery = query.mapToVtjQuery(PERUSSANOMA3)
        val expectedVtjResponse = query.asMinimalVtjResponse()

        whenever(vtjService.query(expectedVtjQuery)).thenReturn(expectedVtjResponse)
        whenever(henkiloMapper.mapToVtjPerson(expectedVtjResponse)).thenReturn(validPerson)

        val result = service.fetchPerson(query)
        assertThat(result.socialSecurityNumber).isEqualTo(validPerson.socialSecurityNumber)
        assertThat(result.residentialAddress).isEqualTo(validPerson.residentialAddress)
    }

    @Test
    fun `dependants query should return the dependants as stubs with one request`() {
        val expectedChild =
            VtjRelative(
                ssn = Ssn.parse("291013A950S")!!,
                dateOfBirth = LocalDate.of(2013, 10, 29),
                firstNames = "Lapsi Lapsonen",
                lastName = "Testaaja",
            )
        val expectedVtjQuery = query.mapToVtjQuery(HUOLTAJA_HUOLLETTAVA)
        val expectedVtjResponse = query.asMinimalVtjResponse()

        whenever(vtjService.query(expectedVtjQuery)).thenReturn(expectedVtjResponse)
        whenever(henkiloMapper.mapToVtjPerson(expectedVtjResponse)).thenReturn(validPerson)
        whenever(henkiloMapper.mapDependants(expectedVtjResponse)).thenReturn(listOf(expectedChild))

        val result = service.fetchWithDependants(query)
        assertThat(result.person.socialSecurityNumber).isEqualTo(validPerson.socialSecurityNumber)
        assertThat(result.person.firstNames).isEqualTo(validPerson.firstNames)
        assertThat(result.dependants).containsExactly(expectedChild)

        val _ = verify(vtjService, times(1)).query(any())
    }

    private fun DetailsQuery.asMinimalVtjResponse(): Henkilo = minimalVtjResponse("$requestingUser")

    private fun minimalVtjResponse(ssn: String) =
        Henkilo().also { it.henkilotunnus = Henkilo.Henkilotunnus().also { ht -> ht.value = ssn } }

    private val validPerson =
        VtjPerson(
            firstNames = "Jonne Johannes Jooseppi",
            lastName = "Testaaja",
            socialSecurityNumber = "010882-983Y",
            nativeLanguage = NativeLanguage(languageName = "suomi", code = "fi"),
            residentialAddress =
                PersonAddress(
                    streetAddress = "Kaivomestarinkatu 15",
                    postOffice = "Espoo",
                    postalCode = "12311",
                    streetAddressSe = "Brunnsmästargatan 15",
                    postOfficeSe = "Esbo",
                ),
            restrictedDetails = RestrictedDetails(enabled = false),
            nationalities = listOf(Nationality(countryCode = "246", countryName = "Suomi")),
        )

    private val query =
        DetailsQuery(
            requestingUser = EvakaUserId(randomUUID()),
            ssn = Ssn.parse(validPerson.socialSecurityNumber)!!,
        )
}
