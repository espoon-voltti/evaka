// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.getGuardianChildIds
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.PersonDataSource
import fi.espoo.evaka.vtjclient.dto.VtjPersonDTO
import fi.espoo.evaka.vtjclient.service.persondetails.PersonStorageService
import fi.espoo.evaka.vtjclient.usecases.dto.PersonResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class PersonStorageServiceIntegrationTest : PureJdbiTest() {
    lateinit var service: PersonStorageService

    @BeforeEach
    private fun beforeEach() {
        service = PersonStorageService()
        jdbi.handle { h ->
            resetDatabase(h)
            insertGeneralTestFixtures(h)
        }
    }

    @Test
    fun `upsert new person with no children creates new person`() {
        val testGuardian = generatePerson(ssn = "130535-531K")
        val personResult = db.transaction { service.upsertVtjPerson(it, PersonResult.Result(testGuardian)) }
        assertTrue(personResult is PersonResult.Result)
        val createdPerson = (personResult as PersonResult.Result).vtjPersonDTO
        assertEquals(testGuardian.socialSecurityNumber, createdPerson.socialSecurityNumber)
        assertEquals(0, createdPerson.children.size)
    }

    @Test
    fun `upsert existing person with no children updates person`() {
        val testGuardian = generatePerson(ssn = "130535-531K")
        val modifiedPersonResult = db.transaction {
            service.upsertVtjPerson(it, PersonResult.Result(testGuardian))
            service.upsertVtjPerson(it, PersonResult.Result(testGuardian.copy(streetAddress = "Modified")))
        }

        assertTrue(modifiedPersonResult is PersonResult.Result)
        val modifiedPerson = (modifiedPersonResult as PersonResult.Result).vtjPersonDTO
        assertEquals("Modified", modifiedPerson.streetAddress)
    }

    @Test
    fun `create new person with children creates guardian relationships`() {
        val testChild = generatePerson("020319A123K")
        val testGuardian = generatePerson(ssn = "130535-531K", children = mutableListOf(testChild))

        val personResult =
            db.transaction { service.upsertVtjGuardianAndChildren(it, PersonResult.Result(testGuardian)) }
        assertTrue(personResult is PersonResult.Result)
        val createdPerson = (personResult as PersonResult.Result).vtjPersonDTO
        assertEquals(testGuardian.socialSecurityNumber, createdPerson.socialSecurityNumber)
        assertEquals(1, createdPerson.children.size)
        assertEquals(testChild.socialSecurityNumber, createdPerson.children.first().socialSecurityNumber)

        val guardianChildren = db.read { getGuardianChildIds(it.handle, createdPerson.id) }
        assertEquals(1, guardianChildren.size)
        assertEquals(createdPerson.children.first().id, guardianChildren.first())
    }

    @Test
    fun `upsert person with children updates guardian relationships`() {
        val testGuardian = generatePerson(ssn = "130535-531K", children = mutableListOf(generatePerson("020319A123K")))

        db.transaction { service.upsertVtjGuardianAndChildren(it, PersonResult.Result(testGuardian)) }

        val testChild2 = generatePerson("241220A321N")
        val personResult = db.transaction {
            service.upsertVtjGuardianAndChildren(
                it,
                PersonResult.Result(testGuardian.copy(children = mutableListOf(testChild2)))
            )
        }

        assertTrue(personResult is PersonResult.Result)
        val createdPerson = (personResult as PersonResult.Result).vtjPersonDTO
        assertEquals(testGuardian.socialSecurityNumber, createdPerson.socialSecurityNumber)
        assertEquals(1, createdPerson.children.size)
        assertEquals(testChild2.socialSecurityNumber, createdPerson.children.first().socialSecurityNumber)

        val guardianChildren = db.read { getGuardianChildIds(it.handle, createdPerson.id) }
        assertEquals(1, guardianChildren.size)
        assertEquals(createdPerson.children.first().id, guardianChildren.first())
    }

    private fun generatePerson(ssn: String, children: MutableList<VtjPersonDTO> = mutableListOf()): VtjPersonDTO {
        return VtjPersonDTO(
            id = UUID.randomUUID(),
            socialSecurityNumber = ssn,
            dateOfBirth = LocalDate.now().minusYears(22),
            source = PersonDataSource.VTJ,
            firstName = "dmf",
            lastName = "pfrn",
            children = children,
            restrictedDetailsEndDate = null,
            restrictedDetailsEnabled = false,
            streetAddress = "Katu 1 A1",
            city = "Espoo",
            postalCode = "02230",
            streetAddressSe = "Vägen 1 A1",
            citySe = "Esbo",
            nativeLanguage = NativeLanguage(languageName = "", code = "fi") // the language name is ignored
        )
    }
}
