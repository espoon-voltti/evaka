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
import evaka.core.vtjclient.dto.PersonAddress
import evaka.core.vtjclient.dto.VtjPerson
import evaka.core.vtjclient.service.persondetails.IPersonDetailsService
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach

/**
 * What a product-scope shortfall writes to `person`. DVV answers `200 OK` and simply omits a
 * tietoryhmä the customer's product does not grant, so the response cannot be told apart from one
 * describing a person who genuinely has no such data.
 */
class VtjSparseResponseIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val user = AuthenticatedUser.SystemInternalUser
    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 1, 1), LocalTime.of(12, 0))

    private val person =
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
            restrictedDetailsEnabled = true,
            restrictedDetailsEndDate = LocalDate.of(2030, 1, 1),
        )

    private val personService = PersonService(SparseDetailsService(person))

    @BeforeEach
    fun insertPerson() {
        db.transaction { it.insert(person, DevPersonType.ADULT) }
    }

    @Test
    fun `a group the product does not grant is written as empty over the stored value`() {
        refresh()

        val refreshed = db.read { it.getPersonById(person.id) }!!
        assertEquals("", refreshed.language)
        assertEquals("", refreshed.residenceCode)
        assertEquals("", refreshed.municipalityOfResidence)
        assertEquals(emptyList(), refreshed.nationalities)
    }

    /** The half that arrived still looks right, which is why a shortfall goes unnoticed. */
    @Test
    fun `the groups the product does grant survive the same refresh`() {
        refresh()

        val refreshed = db.read { it.getPersonById(person.id) }!!
        assertEquals(person.streetAddress, refreshed.streetAddress)
        assertEquals(person.postalCode, refreshed.postalCode)
        assertEquals(person.postOffice, refreshed.postOffice)
    }

    @Test
    fun `an absent turvakielto group clears an active turvakielto`() {
        refresh()

        val refreshed = db.read { it.getPersonById(person.id) }!!
        assertFalse(refreshed.restrictedDetailsEnabled)
        assertNull(refreshed.restrictedDetailsEndDate)
    }

    private fun refresh() {
        db.transaction {
            personService.getPersonWithChildren(it, user, now, person.id, forceRefresh = true)
        }
    }
}

/**
 * Name and address only: no `AIDINKIELI`, residence code, kotikunta, nationalities or turvakielto —
 * the groups a narrower product would leave out.
 */
private class SparseDetailsService(private val person: DevPerson) : IPersonDetailsService {
    override fun getBasicDetailsFor(query: IPersonDetailsService.DetailsQuery): VtjPerson =
        VtjPerson(
            firstNames = person.firstName,
            lastName = person.lastName,
            socialSecurityNumber = person.ssn!!,
            address = PersonAddress(person.streetAddress, person.postalCode, person.postOffice),
            restrictedDetails = null,
        )

    override fun getPersonWithDependants(query: IPersonDetailsService.DetailsQuery): VtjPerson =
        getBasicDetailsFor(query)

    override fun getPersonWithGuardians(query: IPersonDetailsService.DetailsQuery): VtjPerson =
        getBasicDetailsFor(query)
}
