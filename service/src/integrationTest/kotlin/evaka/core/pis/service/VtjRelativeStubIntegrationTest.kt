// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.PureJdbiTest
import evaka.core.identity.Ssn
import evaka.core.pis.getPersonById
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.vtjclient.dto.Nationality
import evaka.core.vtjclient.dto.NativeLanguage
import evaka.core.vtjclient.dto.PersonAddress
import evaka.core.vtjclient.dto.RestrictedDetails
import evaka.core.vtjclient.dto.VtjPerson
import evaka.core.vtjclient.dto.VtjPersonWithRelatives
import evaka.core.vtjclient.dto.VtjRelative
import evaka.core.vtjclient.service.persondetails.VtjClient
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

/**
 * A [VtjClient] returns relatives as stubs (SSN, date of birth and names), so a refresh must not
 * write a stub over the VTJ-owned fields of a relative's person row.
 */
class VtjRelativeStubIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val user = AuthenticatedUser.SystemInternalUser
    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 1, 1), LocalTime.of(12, 0))

    private val guardian =
        DevPerson(
            dateOfBirth = LocalDate.of(1981, 2, 22),
            firstName = "Mikael",
            lastName = "Högfors",
            ssn = "220281-9456",
            language = "fi",
            streetAddress = "Kamreerintie 4",
            postalCode = "02100",
            postOffice = "Espoo",
            residenceCode = "abc123",
            municipalityOfResidence = "Espoo",
            nationalities = listOf("246"),
        )

    private val child =
        DevPerson(
            dateOfBirth = LocalDate.of(2013, 10, 7),
            firstName = "Antero",
            lastName = "Högfors",
            ssn = "071013A960W",
            language = "fi",
            streetAddress = "Kamreerintie 4",
            postalCode = "02100",
            postOffice = "Espoo",
            residenceCode = "abc123",
            municipalityOfResidence = "Espoo",
            nationalities = listOf("246"),
            restrictedDetailsEnabled = true,
            restrictedDetailsEndDate = LocalDate.of(2030, 1, 1),
        )

    private val vtjClient = StubRelativeVtjClient(guardian, child)
    private val personService = PersonService(vtjClient, VtjSyncService(vtjClient, mock()))

    @BeforeEach
    fun insertFamily() {
        db.transaction {
            it.insert(guardian, DevPersonType.ADULT)
            it.insert(child, DevPersonType.CHILD)
        }
    }

    @Test
    fun `refreshing a guardian keeps every VTJ-owned field of their dependant`() {
        refreshGuardian()

        val refreshed = db.read { it.getPersonById(child.id) }!!
        assertEquals(child.streetAddress, refreshed.streetAddress)
        assertEquals(child.postalCode, refreshed.postalCode)
        assertEquals(child.postOffice, refreshed.postOffice)
        assertEquals(child.residenceCode, refreshed.residenceCode)
        assertEquals(child.municipalityOfResidence, refreshed.municipalityOfResidence)
        assertEquals(child.language, refreshed.language)
        assertEquals(child.nationalities, refreshed.nationalities)
    }

    @Test
    fun `refreshing a guardian keeps their dependant's restricted details`() {
        refreshGuardian()

        val refreshed = db.read { it.getPersonById(child.id) }!!
        assertTrue(refreshed.restrictedDetailsEnabled)
        assertEquals(child.restrictedDetailsEndDate, refreshed.restrictedDetailsEndDate)
    }

    @Test
    fun `refreshing a child keeps their guardian's address`() {
        db.transaction { personService.getGuardians(it, user, now, child.id, forceRefresh = true) }

        val refreshed = db.read { it.getPersonById(guardian.id) }!!
        assertEquals(guardian.streetAddress, refreshed.streetAddress)
        assertEquals(guardian.postalCode, refreshed.postalCode)
        assertEquals(guardian.postOffice, refreshed.postOffice)
    }

    private fun refreshGuardian() {
        db.transaction {
            personService.getPersonWithChildren(it, user, now, guardian.id, forceRefresh = true)
        }
    }
}

private class StubRelativeVtjClient(private vararg val persons: DevPerson) : VtjClient {
    override fun fetchPerson(query: VtjClient.DetailsQuery): VtjPerson =
        persons.single { it.ssn == query.ssn.value }.toFullVtjPerson()

    override fun fetchWithDependants(query: VtjClient.DetailsQuery) =
        VtjPersonWithRelatives(fetchPerson(query), relativesOf(query))

    override fun fetchWithGuardians(query: VtjClient.DetailsQuery) =
        VtjPersonWithRelatives(fetchPerson(query), relativesOf(query))

    private fun relativesOf(query: VtjClient.DetailsQuery) =
        persons.filter { it.ssn != query.ssn.value }.map { it.toRelativeStub() }
}

private fun DevPerson.toFullVtjPerson() =
    VtjPerson(
        firstNames = firstName,
        lastName = lastName,
        socialSecurityNumber = ssn!!,
        address = PersonAddress(streetAddress, postalCode, postOffice),
        nativeLanguage = language?.let { NativeLanguage(code = it) },
        nationalities = nationalities.map { Nationality(countryName = it, countryCode = it) },
        residenceCode = residenceCode,
        municipalityOfResidence = municipalityOfResidence,
        restrictedDetails = RestrictedDetails(restrictedDetailsEnabled, restrictedDetailsEndDate),
    )

private fun DevPerson.toRelativeStub() =
    VtjRelative(
        ssn = Ssn.parse(ssn!!)!!,
        dateOfBirth = dateOfBirth,
        firstNames = firstName,
        lastName = lastName,
    )
