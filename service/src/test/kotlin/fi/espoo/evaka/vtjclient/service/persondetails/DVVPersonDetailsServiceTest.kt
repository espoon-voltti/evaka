// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service.persondetails

import fi.espoo.evaka.identity.ExternalIdentifier.SSN
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.vtjclient.dto.Nationality
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.PersonAddress
import fi.espoo.evaka.vtjclient.dto.RestrictedDetails
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import fi.espoo.evaka.vtjclient.mapper.VtjHenkiloMapper
import fi.espoo.evaka.vtjclient.service.persondetails.IPersonDetailsService.DetailsQuery
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.RequestType.HUOLTAJA_HUOLLETTAVA
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.RequestType.PERUSSANOMA3
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.VTJQuery
import fi.espoo.evaka.vtjclient.soap.VTJHenkiloVastaussanoma.Henkilo
import java.time.LocalDate
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class DVVPersonDetailsServiceTest {

    @InjectMocks lateinit var service: VTJPersonDetailsService

    @Mock lateinit var vtjService: IVtjClientService

    @Mock lateinit var henkiloMapper: VtjHenkiloMapper

    @Test
    fun `basic query should return results without dependants details with a successful query`() {
        val expectedResult = validPerson
        val query = queryAboutSelf(vtjPerson = expectedResult)
        val expectedVtjQuery = query.mapToVtjQuery(PERUSSANOMA3)
        val expectedVtjResponse = query.asMinimalVtjResponse()

        whenever(vtjService.query(expectedVtjQuery)).thenReturn(expectedVtjResponse)
        whenever(henkiloMapper.mapToVtjPerson(expectedVtjResponse)).thenReturn(expectedResult)

        val result = service.getBasicDetailsFor(query)
        assertThat(result.socialSecurityNumber).isEqualTo(expectedResult.socialSecurityNumber)
        assertThat(result.address).isEqualTo(expectedResult.address)
        assertThat(result.dependants).isEmpty()
    }

    @Test
    fun `details query service should return results with dependant details with a successful query`() {
        val expectedChildResult =
            validPerson.copy(firstNames = "Lapsi Lapsonen", socialSecurityNumber = "291013A950S")
        val expectedResult = validPerson.copy(dependants = listOf(expectedChildResult))

        val query = queryAboutSelf(vtjPerson = expectedResult)

        val expectedVtjQuery = query.mapToVtjQuery(HUOLTAJA_HUOLLETTAVA)
        val expectedVTJResponse = query.asMinimalVtjResponse()

        val expectedChildQuery =
            DetailsQuery(
                requestingUser = query.requestingUser,
                targetIdentifier = SSN.getInstance(expectedChildResult.socialSecurityNumber),
            )
        val expectedChildVtjQuery =
            expectedChildQuery.mapToVtjQuery(IVtjClientService.RequestType.PERUSSANOMA3)
        val expectedChildVtjResponse = expectedChildQuery.asMinimalVtjResponse()

        whenever(vtjService.query(any()))
            .thenReturn(expectedVTJResponse)
            .thenReturn(expectedChildVtjResponse)
        whenever(henkiloMapper.mapToVtjPerson(any()))
            .thenReturn(expectedResult)
            .thenReturn(expectedChildResult)

        val result = service.getPersonWithDependants(query)
        assertThat(result.socialSecurityNumber).isEqualTo(expectedResult.socialSecurityNumber)
        assertThat(result.firstNames).isEqualTo(expectedResult.firstNames)

        assertThat(result.dependants).size().isEqualTo(1)
        val childResult = result.dependants[0]

        assertThat(childResult.socialSecurityNumber)
            .isEqualTo(expectedChildResult.socialSecurityNumber)
        assertThat(childResult.firstNames).isEqualTo(expectedChildResult.firstNames)
        assertThat(childResult.address).isEqualTo(expectedChildResult.address)

        argumentCaptor<VTJQuery>().apply {
            verify(vtjService, times(2)).query(capture())
            assertThat(firstValue).isEqualTo(expectedVtjQuery)
            assertThat(secondValue).isEqualTo(expectedChildVtjQuery)
        }

        argumentCaptor<Henkilo>().apply {
            verify(henkiloMapper, times(2)).mapToVtjPerson(capture())
            assertThat(firstValue).isEqualTo(expectedVTJResponse)
            assertThat(secondValue).isEqualTo(expectedChildVtjResponse)
        }
    }

    private fun queryAboutSelf(vtjPerson: VtjPerson = validPerson) =
        vtjPerson.toPersonDTO().let {
            DetailsQuery(
                requestingUser = EvakaUserId(it.id.raw),
                targetIdentifier = it.identity as SSN,
            )
        }

    private fun DetailsQuery.asMinimalVtjResponse(): Henkilo = minimalVtjResponse("$requestingUser")

    private fun minimalVtjResponse(ssn: String) =
        Henkilo().also { it.henkilotunnus = Henkilo.Henkilotunnus().also { ht -> ht.value = ssn } }

    private fun VtjPerson.toPersonDTO() =
        PersonDTO(
            id = PersonId(randomUUID()),
            duplicateOf = null,
            identity = SSN.getInstance(socialSecurityNumber),
            ssnAddingDisabled = false,
            firstName = firstNames,
            lastName = lastName,
            preferredName = "",
            language = nativeLanguage?.code ?: "fi",
            email = "example@example.org",
            phone = "+573601234567",
            backupPhone = "+573601234569",
            dateOfBirth = LocalDate.now().minusYears(15),
            restrictedDetailsEnabled = false,
            restrictedDetailsEndDate = null,
            streetAddress = "",
            postalCode = "",
            postOffice = "",
            residenceCode = "",
        )

    private val validPerson =
        VtjPerson(
            firstNames = "Jonne Johannes Jooseppi",
            lastName = "Testaaja",
            socialSecurityNumber = "010882-983Y",
            nativeLanguage = NativeLanguage(languageName = "suomi", code = "fi"),
            address =
                PersonAddress(
                    streetAddress = "Kaivomestarinkatu 15",
                    postOffice = "Espoo",
                    postalCode = "12311",
                    streetAddressSe = "Brunnsmästargatan 15",
                    postOfficeSe = "Esbo",
                ),
            restrictedDetails = RestrictedDetails(enabled = false),
            nationalities = listOf(Nationality(countryCode = "246", countryName = "Suomi")),
            dependants = emptyList(),
        )
}
