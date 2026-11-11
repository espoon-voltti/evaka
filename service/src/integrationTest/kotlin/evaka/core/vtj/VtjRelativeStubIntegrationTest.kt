// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.PureJdbiTest
import evaka.core.identity.Ssn
import evaka.core.person.getPersonById
import evaka.core.shared.EvakaUserId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

/**
 * A [VtjClient] returns relatives as SSNs only, so a refresh must not change the VTJ-owned fields
 * of a relative who is already in eVaka.
 */
class VtjRelativeStubIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val user = AuthenticatedUser.SystemInternalUser
    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2026, 1, 1), LocalTime.of(12, 0)))

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
            restrictedDetailsEnabled = true,
            restrictedDetailsEndDate = LocalDate.of(2030, 1, 1),
        )

    private val vtjClient = StubRelativeVtjClient(guardian, child)
    private val vtjSyncService = VtjSyncService(vtjClient, mock())

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
        vtjSyncService.refreshPersons(db, user.evakaUserId, clock, listOf(child.id))

        val refreshed = db.read { it.getPersonById(guardian.id) }!!
        assertEquals(guardian.streetAddress, refreshed.streetAddress)
        assertEquals(guardian.postalCode, refreshed.postalCode)
        assertEquals(guardian.postOffice, refreshed.postOffice)
    }

    private fun refreshGuardian() {
        vtjSyncService.refreshPersonsGreedy(db, user.evakaUserId, clock, listOf(guardian.id))
    }
}

private class StubRelativeVtjClient(private vararg val persons: DevPerson) : VtjClient {
    override fun fetchPersons(requestingUser: EvakaUserId, ssns: Collection<Ssn>) =
        ssns.associateWith { ssn ->
            persons.single { it.ssn == ssn.value }.toFullVtjPerson()
        }

    override fun fetchWithDependants(requestingUser: EvakaUserId, ssns: Collection<Ssn>) =
        fetchPersons(requestingUser, ssns).mapValues { (ssn, person) ->
            VtjPersonAndDependants(person, relativesOf(ssn))
        }

    override fun fetchWithGuardians(requestingUser: EvakaUserId, ssns: Collection<Ssn>) =
        fetchPersons(requestingUser, ssns).mapValues { (ssn, person) ->
            VtjPersonAndGuardians(person, relativesOf(ssn))
        }

    private fun relativesOf(ssn: Ssn) =
        persons.filter { it.ssn != ssn.value }.map { Ssn.from(it.ssn!!) }
}

private fun DevPerson.toFullVtjPerson() =
    VtjPerson(
        firstNames = firstName,
        lastName = lastName,
        ssn = Ssn.from(ssn!!),
        residentialAddress = PersonAddress(streetAddress, postalCode, postOffice),
        nativeLanguage = language?.let { NativeLanguage(code = it) },
        residenceCode = residenceCode,
        municipalityOfResidence = municipalityOfResidence,
        restrictedDetails = RestrictedDetails(restrictedDetailsEnabled, restrictedDetailsEndDate),
    )
