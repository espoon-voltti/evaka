// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.getGuardianChildIds
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.PersonDataSource
import fi.espoo.evaka.vtjclient.dto.VtjPersonDTO
import fi.espoo.evaka.vtjclient.service.persondetails.PersonStorageService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class PersonStorageServiceIntegrationTest : PureJdbiTest() {
    lateinit var service: PersonStorageService

    @BeforeEach
    private fun beforeEach() {
        service = PersonStorageService()
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
        }
    }

    @Test
    fun `upsert new person creates new person`() {
        val testGuardian = generatePerson(ssn = "130535-531K")
        val createdPerson = db.transaction { service.upsertVtjPerson(it, testGuardian) }
        assertEquals(testGuardian.socialSecurityNumber, (createdPerson.identity as ExternalIdentifier.SSN).ssn)
    }

    @Test
    fun `upsert existing person with no children updates person`() {
        val testGuardian = generatePerson(ssn = "130535-531K")
        val modifiedPerson = db.transaction {
            service.upsertVtjPerson(it, testGuardian)
            service.upsertVtjPerson(it, testGuardian.copy(streetAddress = "Modified"))
        }
        assertEquals("Modified", modifiedPerson.streetAddress)
    }

    @Test
    fun `create new person with children creates guardian relationships`() {
        val testChild = generatePerson("020319A123K")
        val testGuardian = generatePerson(ssn = "130535-531K", children = mutableListOf(testChild))

        val createdPerson =
            db.transaction { service.upsertVtjChildren(it, testGuardian) }
        assertEquals(testGuardian.socialSecurityNumber, createdPerson.socialSecurityNumber)
        assertEquals(1, createdPerson.children.size)
        assertEquals(testChild.socialSecurityNumber, createdPerson.children.first().socialSecurityNumber)

        val guardianChildren = db.read { it.getGuardianChildIds(createdPerson.id) }
        assertEquals(1, guardianChildren.size)
        assertEquals(createdPerson.children.first().id, guardianChildren.first())
    }

    @Test
    fun `upsert person with children updates guardian relationships`() {
        val testGuardian = generatePerson(ssn = "130535-531K", children = mutableListOf(generatePerson("020319A123K")))

        db.transaction { service.upsertVtjChildren(it, testGuardian) }

        val testChild2 = generatePerson("241220A321N")
        val createdPerson = db.transaction {
            service.upsertVtjChildren(
                it,
                testGuardian.copy(children = mutableListOf(testChild2))
            )
        }

        assertEquals(testGuardian.socialSecurityNumber, createdPerson.socialSecurityNumber)
        assertEquals(1, createdPerson.children.size)
        assertEquals(testChild2.socialSecurityNumber, createdPerson.children.first().socialSecurityNumber)

        val guardianChildren = db.read { it.getGuardianChildIds(createdPerson.id) }
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
