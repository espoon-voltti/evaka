// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service.persondetails

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import fi.espoo.evaka.identity.ExternalIdentifier.SSN
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.vtjclient.dto.Nationality
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.PersonAddress
import fi.espoo.evaka.vtjclient.dto.RestrictedDetails
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import fi.espoo.evaka.vtjclient.mapper.IVtjHenkiloMapper
import fi.espoo.evaka.vtjclient.mapper.VtjPersonNotFoundException
import fi.espoo.evaka.vtjclient.service.cache.VtjCache
import fi.espoo.evaka.vtjclient.service.persondetails.IPersonDetailsService.DetailsQuery
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.RequestType.HUOLTAJA_HUOLLETTAVA
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.RequestType.PERUSSANOMA3
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.VTJQuery
import fi.espoo.evaka.vtjclient.soap.VTJHenkiloVastaussanoma.Henkilo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.util.UUID.randomUUID

@ExtendWith(MockitoExtension::class)
class VTJPersonDetailsServiceTest {

    @InjectMocks
    lateinit var service: VTJPersonDetailsService

    @Mock
    lateinit var vtjService: IVtjClientService

    @Mock
    lateinit var vtjCache: VtjCache

    @Mock
    lateinit var henkiloMapper: IVtjHenkiloMapper

    @Test
    fun `basic query should return results without dependants details with a successful query`() {
        val expectedResult = validPerson
        val query = queryAboutSelf(vtjPerson = expectedResult)
        val expectedVtjQuery = query.mapToVtjQuery(PERUSSANOMA3)
        val expectedVtjResponse = query.asMinimalVtjResponse()

        whenever(vtjService.query(expectedVtjQuery)).thenReturn(expectedVtjResponse)
        whenever(henkiloMapper.mapToVtjPerson(expectedVtjResponse)).thenReturn(expectedResult)

        val result = service.getBasicDetailsFor(query) as PersonDetails.Result
        assertThat(result.vtjPerson.socialSecurityNumber).isEqualTo(expectedResult.socialSecurityNumber)
        assertThat(result.vtjPerson.address).isEqualTo(expectedResult.address)
        assertThat(result.vtjPerson.dependants).isEmpty()
    }

    @Test
    fun `details query service should return results with dependant details with a successful query`() {

        val expectedChildResult = validPerson.copy(
            firstNames = "Lapsi Lapsonen",
            socialSecurityNumber = "291013A950S"
        )
        val expectedResult = validPerson.copy(
            dependants = listOf(expectedChildResult)
        )

        val query = queryAboutSelf(vtjPerson = expectedResult)

        val expectedVtjQuery = query.mapToVtjQuery(HUOLTAJA_HUOLLETTAVA)
        val expectedVTJResponse = query.asMinimalVtjResponse()

        val expectedChildQuery = DetailsQuery(
            requestingUser = query.requestingUser,
            targetIdentifier = SSN.getInstance(expectedChildResult.socialSecurityNumber)
        )
        val expectedChildVtjQuery = expectedChildQuery.mapToVtjQuery(IVtjClientService.RequestType.PERUSSANOMA3)
        val expectedChildVtjResponse = expectedChildQuery.asMinimalVtjResponse()

        whenever(vtjService.query(any()))
            .thenReturn(expectedVTJResponse)
            .thenReturn(expectedChildVtjResponse)
        whenever(henkiloMapper.mapToVtjPerson(any()))
            .thenReturn(expectedResult)
            .thenReturn(expectedChildResult)

        val result = service.getPersonWithDependants(query) as PersonDetails.Result
        assertThat(result.vtjPerson.socialSecurityNumber).isEqualTo(expectedResult.socialSecurityNumber)
        assertThat(result.vtjPerson.firstNames).isEqualTo(expectedResult.firstNames)

        assertThat(result.vtjPerson.dependants).size().isEqualTo(1)
        val childResult = result.vtjPerson.dependants[0]

        assertThat(childResult.socialSecurityNumber).isEqualTo(expectedChildResult.socialSecurityNumber)
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

    @Test
    fun `service should return not found when person is not found from VTJ`() {
        val expectedResult = validPerson
        val query = queryAboutSelf(vtjPerson = expectedResult)
        val expectedVtjQuery = query.mapToVtjQuery(PERUSSANOMA3)

        whenever(vtjService.query(expectedVtjQuery)).thenThrow(VtjPersonNotFoundException::class.java)
        verifyZeroInteractions(henkiloMapper)

        val result = service.getBasicDetailsFor(query) as PersonDetails.PersonNotFound
        assertThat(result.message).isEqualTo("Person not found")
    }

    @Test
    fun `service should return query error when VTJ query fails`() {
        val expectedResult = validPerson
        val query = queryAboutSelf(vtjPerson = expectedResult)
        val expectedVtjQuery = query.mapToVtjQuery(PERUSSANOMA3)

        val expectedErrorMessage = "some error 123 11"

        whenever(vtjService.query(expectedVtjQuery)).thenThrow(IllegalStateException(expectedErrorMessage))
        verifyZeroInteractions(henkiloMapper)

        val result = service.getBasicDetailsFor(query) as PersonDetails.QueryError
        assertThat(result.message).isEqualTo(expectedErrorMessage)
    }

    @Test
    fun `service should only return results for dependants that are successfully mapped`() {

        val expectedResult = validPerson.copy(
            dependants = listOf(
                validPerson.copy(socialSecurityNumber = "070115A9583"),
                validPerson.copy(socialSecurityNumber = "020915A9878"),
                validPerson.copy(socialSecurityNumber = "151014A968K")
            )
        )

        val query = queryAboutSelf(vtjPerson = expectedResult)

        val expectedVTJResponse = query.asMinimalVtjResponse()

        val expectedChild = expectedResult.dependants[0]

        whenever(vtjService.query(any()))
            .thenReturn(expectedVTJResponse)
            .thenReturn(minimalVtjResponse(expectedChild.socialSecurityNumber))
            .thenReturn(null)
            .thenThrow(IllegalStateException("some error 43"))
        whenever(henkiloMapper.mapToVtjPerson(any()))
            .thenReturn(expectedResult)
            .thenReturn(expectedChild)

        val result = service.getPersonWithDependants(query) as PersonDetails.Result
        assertThat(result.vtjPerson.socialSecurityNumber).isEqualTo(expectedResult.socialSecurityNumber)

        assertThat(result.vtjPerson.dependants).size().isEqualTo(1)
        val childResult = result.vtjPerson.dependants[0]

        assertThat(childResult.socialSecurityNumber).isEqualTo(expectedChild.socialSecurityNumber)

        verify(vtjService, times(4)).query(any())

        verify(henkiloMapper, times(2)).mapToVtjPerson(any())
    }

    private fun queryAboutSelf(vtjPerson: VtjPerson = validPerson) =
        vtjPerson
            .toPersonDTO()
            .let { DetailsQuery(requestingUser = AuthenticatedUser.Employee(it.id, setOf()), targetIdentifier = it.identity as SSN) }

    private fun DetailsQuery.asMinimalVtjResponse(): Henkilo = minimalVtjResponse("${requestingUser.id}")

    private fun minimalVtjResponse(ssn: String) = Henkilo()
        .also {
            it.henkilotunnus = Henkilo.Henkilotunnus()
                .also { ht ->
                    ht.value = ssn
                }
        }

    private fun VtjPerson.toPersonDTO() = PersonDTO(
        id = randomUUID(),
        identity = SSN.getInstance(socialSecurityNumber),
        firstName = firstNames,
        lastName = lastName,
        language = nativeLanguage?.code ?: "fi",
        email = "example@example.org",
        phone = "+573601234567",
        backupPhone = "+573601234569",
        dateOfBirth = LocalDate.now().minusYears(15),
        restrictedDetailsEnabled = false,
        restrictedDetailsEndDate = null,
        streetAddress = "",
        postalCode = "",
        postOffice = ""
    )

    private val validPerson = VtjPerson(
        firstNames = "Jonne Johannes Jooseppi",
        lastName = "Testaaja",
        socialSecurityNumber = "010882-983Y",
        nativeLanguage = NativeLanguage(languageName = "suomi", code = "fi"),
        address = PersonAddress(
            streetAddress = "Kaivomestarinkatu 15",
            postOffice = "Espoo",
            postalCode = "12311",
            streetAddressSe = "Brunnsmästargatan 15",
            postOfficeSe = "Esbo"
        ),
        restrictedDetails = RestrictedDetails(enabled = false),
        nationalities = listOf(Nationality(countryCode = "246", countryName = "Suomi")),
        dependants = emptyList()
    )
}
