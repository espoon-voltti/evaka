// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.vtjclient.mapper.VtjHenkiloMapper
import fi.espoo.evaka.vtjclient.service.persondetails.VTJPersonDetailsService
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PersonServiceIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {

    private val mockVtjClientService: IVtjClientService = MockVtjClientService()
    private lateinit var personService: PersonService

    val user = AuthenticatedUser.SystemInternalUser

    private val testPersonWithoutVtjChildren =
        DevPerson(
            id = PersonId(UUID.randomUUID()),
            dateOfBirth = LocalDate.parse("1980-01-01"),
            dateOfDeath = null,
            firstName = "Harri",
            lastName = "Huoltaja",
            ssn = "010579-9999",
            streetAddress = "Katuosoite",
            postalCode = "02230",
            postOffice = "Espoo",
            restrictedDetailsEnabled = false
        )

    private val testPersonWithVtjChildren =
        DevPerson(
            id = PersonId(UUID.randomUUID()),
            dateOfBirth = LocalDate.parse("1981-02-22"),
            dateOfDeath = null,
            firstName = "Mikael",
            lastName = "Högfors",
            ssn = "220281-9456",
            streetAddress = "Kamreerintie 4",
            postalCode = "02100",
            postOffice = "Espoo",
            restrictedDetailsEnabled = false
        )

    private val testPersonDependantChild =
        DevPerson(
            id = PersonId(UUID.randomUUID()),
            dateOfBirth = LocalDate.parse("2013-10-07"),
            dateOfDeath = null,
            firstName = "Antero",
            lastName = "Högfors",
            ssn = "071013A960W",
            streetAddress = "Kamreerintie 4",
            postalCode = "02100",
            postOffice = "Espoo",
            restrictedDetailsEnabled = false
        )

    @BeforeEach
    fun setup() {
        db.transaction {
            it.insert(testPersonWithoutVtjChildren, DevPersonType.ADULT)
            it.insert(testPersonWithVtjChildren, DevPersonType.ADULT)
            it.insert(testPersonDependantChild, DevPersonType.CHILD)
        }
        MockVtjClientService.resetQueryCounts()
        personService =
            PersonService(VTJPersonDetailsService(mockVtjClientService, VtjHenkiloMapper()))
    }

    @Test
    fun `getUpToDatePersonFromVtj fetches person vtj info only once`() {
        MockVtjClientService.addPERUSSANOMA3RequestExpectation(testPersonWithoutVtjChildren)
        db.transaction {
            personService.getUpToDatePersonFromVtj(it, user, testPersonWithoutVtjChildren.id)
        }
        assertEquals(
            1,
            MockVtjClientService.getPERUSSANOMA3RequestCount(testPersonWithoutVtjChildren)
        )
    }

    @Test
    fun `getPersonWithDependants fetches person and children from vtj only once`() {
        MockVtjClientService.addHUOLTAJAHUOLLETTAVARequestExpectation(
            testPersonWithVtjChildren,
            listOf(testPersonDependantChild)
        )
        MockVtjClientService.addPERUSSANOMA3RequestExpectation(testPersonWithVtjChildren)
        MockVtjClientService.addPERUSSANOMA3RequestExpectation(testPersonDependantChild)
        db.transaction {
            personService.getPersonWithChildren(it, user, testPersonWithVtjChildren.id)
        }
        assertEquals(0, MockVtjClientService.getPERUSSANOMA3RequestCount(testPersonWithVtjChildren))
        assertEquals(1, MockVtjClientService.getPERUSSANOMA3RequestCount(testPersonDependantChild))
        assertEquals(
            1,
            MockVtjClientService.getHUOLTAJAHUOLLETTAVARequestCount(testPersonWithVtjChildren)
        )
    }
}
