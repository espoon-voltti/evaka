// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.dao.PersonDAO
import fi.espoo.evaka.pis.testdata.personUUID
import fi.espoo.evaka.pis.testdata.ssn
import fi.espoo.evaka.pis.testdata.validPerson
import fi.espoo.evaka.pis.testdata.vtjPersonDTO
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.PersonAddress
import fi.espoo.evaka.vtjclient.dto.RestrictedDetails
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import fi.espoo.evaka.vtjclient.service.persondetails.IPersonDetailsService
import fi.espoo.evaka.vtjclient.service.persondetails.PersonDetails
import fi.espoo.evaka.vtjclient.service.persondetails.PersonStorageService
import fi.espoo.evaka.vtjclient.usecases.dto.PersonResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.transaction.PlatformTransactionManager
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class PersonServiceTest {
    @Mock
    lateinit var personDAO: PersonDAO

    @Mock
    lateinit var personDetailsService: IPersonDetailsService

    @Mock
    lateinit var personStorageService: PersonStorageService

    @Mock
    lateinit var txManager: PlatformTransactionManager

    @InjectMocks
    lateinit var service: PersonService

    @Test
    fun `getting up-to-date person who has been updated recently returns data from db`() {
        val user = AuthenticatedUser(personUUID, setOf())
        val personDTO = validPerson.copy(
            updatedFromVtj = Instant.now().minusSeconds(15)
        )
        whenever(personDAO.getPersonByVolttiId(personUUID)).thenReturn(personDTO)

        val person = service.getUpToDatePerson(user, personUUID)
        assertEquals(personDTO, person)
    }

    @Test
    fun `getting up-to-date person who has not been updated recently returns data from db without address if vtj refresh fails`() {
        val user = AuthenticatedUser(personUUID, setOf())
        val personDTO = validPerson.copy(
            updatedFromVtj = Instant.now().minusSeconds(9 * 24 * 60 * 60) // 9 days
        )
        whenever(personDAO.getPersonByVolttiId(personUUID)).thenReturn(personDTO)

        val person = service.getUpToDatePerson(user, personUUID)
        val expected = personDTO.copy(
            streetAddress = "",
            postalCode = "",
            postOffice = ""
        )
        assertEquals(expected, person)
    }

    @Test
    fun `getting up-to-date person who has not been updated recently first updates data from vtj`() {
        val user = AuthenticatedUser(personUUID, setOf())
        val updatedFromVtj = Instant.now().minusSeconds(9 * 24 * 60 * 60) // 9 days
        val personDTO = validPerson.copy(
            updatedFromVtj = updatedFromVtj
        )
        val vtjResult = PersonDetails.Result(
            VtjPerson(
                firstNames = personDTO.firstName!!,
                lastName = personDTO.lastName!!,
                socialSecurityNumber = personDTO.identity.toString(),
                address = PersonAddress(
                    streetAddress = "new address",
                    postalCode = personDTO.postalCode,
                    postOffice = personDTO.postOffice,
                    streetAddressSe = "swedish address",
                    postOfficeSe = "swedish post office"
                ),
                nativeLanguage = NativeLanguage(
                    code = "fi"
                ),
                restrictedDetails = RestrictedDetails(
                    enabled = false,
                    endDate = null
                )
            )
        )

        whenever(personDAO.getPersonByVolttiId(personUUID)).thenReturn(personDTO)
        val query = IPersonDetailsService.DetailsQuery(user, validPerson.identity as ExternalIdentifier.SSN)
        whenever(personDetailsService.getBasicDetailsFor(query)).thenReturn(vtjResult)

        service.getUpToDatePerson(user, personUUID)!!
        verify(personStorageService, times(1)).upsertVtjPerson(
            PersonResult.Result(
                vtjResult.vtjPerson.mapToDto()
            )
        )
    }

    @Test
    fun `getting or creating person who is not in database updates data from vtj`() {
        val user = AuthenticatedUser(personUUID, setOf())
        val personNotInDatabase = null
        val vtjResult = PersonDetails.Result(
            VtjPerson(
                firstNames = validPerson.firstName!!,
                lastName = validPerson.lastName!!,
                socialSecurityNumber = validPerson.identity.toString(),
                address = PersonAddress(
                    streetAddress = validPerson.streetAddress,
                    postalCode = validPerson.postalCode,
                    postOffice = validPerson.postOffice,
                    streetAddressSe = "Vägen 1",
                    postOfficeSe = "Esbo"
                ),
                nativeLanguage = NativeLanguage(
                    code = "fi"
                ),
                restrictedDetails = RestrictedDetails(
                    enabled = false,
                    endDate = null
                )
            )
        )

        whenever(personDAO.getPersonByExternalId(ssn)).thenReturn(personNotInDatabase)

        val query = IPersonDetailsService.DetailsQuery(user, ssn)
        whenever(personDetailsService.getBasicDetailsFor(query)).thenReturn(vtjResult)

        service.getOrCreatePerson(user, ssn)
        verify(personStorageService, times(1)).upsertVtjPerson(
            PersonResult.Result(
                vtjResult.vtjPerson.mapToDto()
            )
        )
    }

    @Test
    fun `getting or creating person who has been updated recently returns data from db`() {
        val user = AuthenticatedUser(personUUID, setOf())
        val personDTO = validPerson.copy(updatedFromVtj = Instant.now().minusSeconds(15))

        whenever(personDAO.getPersonByExternalId(ssn)).thenReturn(personDTO)

        val person = service.getOrCreatePerson(user, ssn)
        assertEquals(personDTO, person)
    }

    @Test
    fun `getting or creating person who has not been updated recently updates and returns data from vtj`() {
        val user = AuthenticatedUser(personUUID, setOf())
        val updatedFromVtj = Instant.now().minusSeconds(9 * 24 * 60 * 60) // 9 days
        val personDTO = validPerson.copy(updatedFromVtj = updatedFromVtj)
        val vtjResult = PersonDetails.Result(
            VtjPerson(
                firstNames = personDTO.firstName!!,
                lastName = personDTO.lastName!!,
                socialSecurityNumber = personDTO.identity.toString(),
                address = PersonAddress(
                    streetAddress = "new address",
                    postalCode = personDTO.postalCode,
                    postOffice = personDTO.postOffice,
                    streetAddressSe = "Vägen 1",
                    postOfficeSe = "Esbo"
                ),
                nativeLanguage = NativeLanguage(
                    code = "fi"
                ),
                restrictedDetails = RestrictedDetails(
                    enabled = false,
                    endDate = null
                )
            )
        )

        whenever(personDAO.getPersonByExternalId(ssn)).thenReturn(personDTO)

        val query = IPersonDetailsService.DetailsQuery(user, validPerson.identity as ExternalIdentifier.SSN)
        whenever(personDetailsService.getBasicDetailsFor(query)).thenReturn(vtjResult)

        service.getOrCreatePerson(user, ssn)!!
        verify(personStorageService, times(1)).upsertVtjPerson(
            PersonResult.Result(
                vtjResult.vtjPerson.mapToDto()
            )
        )
    }

    @Test
    fun `getting or creating person who has not been updated recently returns data from db without address if vtj refresh fails`() {
        val user = AuthenticatedUser(personUUID, setOf())
        val personDTO = validPerson.copy(updatedFromVtj = Instant.now().minusSeconds(9 * 24 * 60 * 60)) // 9 days
        whenever(personDAO.getPersonByExternalId(ssn)).thenReturn(personDTO)

        val person = service.getOrCreatePerson(user, ssn)
        val expected = personDTO.copy(
            streetAddress = "",
            postalCode = "",
            postOffice = ""
        )
        assertEquals(expected, person)
    }

    @Test
    fun `getting person with children touches database`() {
        val user = AuthenticatedUser(personUUID, setOf())
        val personDTO = validPerson.copy(updatedFromVtj = Instant.now())
        val vtjResult = PersonDetails.Result(
            VtjPerson(
                firstNames = validPerson.firstName!!,
                lastName = validPerson.lastName!!,
                socialSecurityNumber = validPerson.identity.toString(),
                address = PersonAddress(
                    streetAddress = validPerson.streetAddress,
                    postalCode = validPerson.postalCode,
                    postOffice = validPerson.postOffice,
                    streetAddressSe = "",
                    postOfficeSe = ""
                ),
                nativeLanguage = NativeLanguage(
                    code = "fi"
                ),
                restrictedDetails = RestrictedDetails(
                    enabled = false,
                    endDate = null
                )
            )
        )
        whenever(personDAO.getPersonByVolttiId(personUUID)).thenReturn(personDTO)
        val query = IPersonDetailsService.DetailsQuery(user, validPerson.identity as ExternalIdentifier.SSN)
        whenever(personDetailsService.getPersonWithDependants(query)).thenReturn(vtjResult)
        whenever(personStorageService.upsertVtjGuardianAndChildren(PersonResult.Result(vtjResult.vtjPerson.mapToDto()))).thenReturn(
            PersonResult.Result(vtjPersonDTO)
        )

        service.getUpToDatePersonWithChildren(user, personUUID)!!
        verify(personStorageService, times(1)).upsertVtjGuardianAndChildren(
            PersonResult.Result(
                vtjResult.vtjPerson.mapToDto()
            )
        )
    }
}
