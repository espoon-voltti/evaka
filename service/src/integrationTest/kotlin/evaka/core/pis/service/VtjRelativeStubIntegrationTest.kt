// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.PureJdbiTest
import evaka.core.pis.getPersonById
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.vtjclient.dto.NativeLanguage
import evaka.core.vtjclient.dto.PersonAddress
import evaka.core.vtjclient.dto.RestrictedDetails
import evaka.core.vtjclient.dto.VtjPerson
import evaka.core.vtjclient.service.persondetails.IPersonDetailsService
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * What an [IPersonDetailsService] returning name+SSN stubs does to the database. No shipped
 * implementation behaves this way, but nothing in the write path prevents it and DVV's REST
 * `/perustiedot` returns relatives in exactly this shape.
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

    private val personService = PersonService(StubRelativeDetailsService(guardian, child))

    @BeforeEach
    fun insertFamily() {
        db.transaction {
            it.insert(guardian, DevPersonType.ADULT)
            it.insert(child, DevPersonType.CHILD)
        }
    }

    @Test
    fun `refreshing a guardian blanks every VTJ-owned field of their dependant`() {
        refreshGuardian()

        val refreshed = db.read { it.getPersonById(child.id) }!!
        assertEquals("", refreshed.streetAddress)
        assertEquals("", refreshed.postalCode)
        assertEquals("", refreshed.postOffice)
        assertEquals("", refreshed.residenceCode)
        assertEquals("", refreshed.municipalityOfResidence)
        assertEquals("", refreshed.language)
        assertEquals(emptyList(), refreshed.nationalities)
    }

    @Test
    fun `refreshing a guardian clears their dependant's restricted details`() {
        refreshGuardian()

        val refreshed = db.read { it.getPersonById(child.id) }!!
        assertFalse(refreshed.restrictedDetailsEnabled)
        assertNull(refreshed.restrictedDetailsEndDate)
    }

    /** `personsLiveInTheSameAddress` gates `createParentship`, so no fridge family forms. */
    @Test
    fun `a blanked dependant stops counting as living with their guardian`() {
        refreshGuardian()

        val refreshedGuardian = db.read { it.getPersonById(guardian.id) }!!
        val refreshedChild = db.read { it.getPersonById(child.id) }!!
        assertFalse(personService.personsLiveInTheSameAddress(refreshedChild, refreshedGuardian))
    }

    private fun refreshGuardian() {
        db.transaction {
            personService.getPersonWithChildren(it, user, now, guardian.id, forceRefresh = true)
        }
    }
}

/**
 * Returns the queried person in full but their relatives as name+SSN only — the shape of VTJ's
 * HUOLTAJA_HUOLLETTAVA response and of DVV's REST `/perustiedot` response before hydration.
 */
private class StubRelativeDetailsService(private vararg val persons: DevPerson) :
    IPersonDetailsService {
    override fun getBasicDetailsFor(query: IPersonDetailsService.DetailsQuery): VtjPerson =
        persons.single { it.ssn == query.targetIdentifier.ssn }.toFullVtjPerson()

    override fun getPersonWithDependants(query: IPersonDetailsService.DetailsQuery): VtjPerson =
        getBasicDetailsFor(query).copy(dependants = relativesOf(query))

    override fun getPersonWithGuardians(query: IPersonDetailsService.DetailsQuery): VtjPerson =
        getBasicDetailsFor(query).copy(guardians = relativesOf(query))

    private fun relativesOf(query: IPersonDetailsService.DetailsQuery) =
        persons.filter { it.ssn != query.targetIdentifier.ssn }.map { it.toRelativeStub() }
}

private fun DevPerson.toFullVtjPerson() =
    VtjPerson(
        firstNames = firstName,
        lastName = lastName,
        socialSecurityNumber = ssn!!,
        address = PersonAddress(streetAddress, postalCode, postOffice),
        nativeLanguage = language?.let { NativeLanguage(code = it) },
        residenceCode = residenceCode,
        municipalityOfResidence = municipalityOfResidence,
        restrictedDetails = RestrictedDetails(restrictedDetailsEnabled, restrictedDetailsEndDate),
    )

private fun DevPerson.toRelativeStub() =
    VtjPerson(
        firstNames = firstName,
        lastName = lastName,
        socialSecurityNumber = ssn!!,
        restrictedDetails = null,
    )
