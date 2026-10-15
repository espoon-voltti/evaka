// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.PureJdbiTest
import evaka.core.identity.Ssn
import evaka.core.pis.getPersonById
import evaka.core.shared.PersonId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.vtjclient.dto.VtjPerson
import evaka.core.vtjclient.mapper.VtjHenkiloMapper
import evaka.core.vtjclient.service.persondetails.SoapVtjClient
import evaka.core.vtjclient.service.persondetails.VtjClient
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PersonServiceIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {

    private val mockVtjSoapTransport: VtjSoapTransport = MockVtjSoapTransport()
    private lateinit var personService: PersonService

    val user = AuthenticatedUser.SystemInternalUser
    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 3, 2), LocalTime.of(12, 0))

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
            restrictedDetailsEnabled = false,
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
            restrictedDetailsEnabled = false,
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
            restrictedDetailsEnabled = false,
        )

    @BeforeEach
    fun setup() {
        db.transaction {
            it.insert(testPersonWithoutVtjChildren, DevPersonType.ADULT)
            it.insert(testPersonWithVtjChildren, DevPersonType.ADULT)
            it.insert(testPersonDependantChild, DevPersonType.CHILD)
        }
        MockVtjSoapTransport.resetQueryCounts()
        personService = PersonService(SoapVtjClient(mockVtjSoapTransport, VtjHenkiloMapper()))
    }

    @Test
    fun `getUpToDatePersonFromVtj fetches person vtj info only once`() {
        MockVtjSoapTransport.addPERUSSANOMA3RequestExpectation(testPersonWithoutVtjChildren)
        db.transaction {
            personService.getUpToDatePersonFromVtj(
                it,
                user,
                HelsinkiDateTime.now(),
                testPersonWithoutVtjChildren.id,
            )
        }
        assertEquals(
            1,
            MockVtjSoapTransport.getPERUSSANOMA3RequestCount(testPersonWithoutVtjChildren),
        )
    }

    @Test
    fun `getPersonWithDependants fetches person and children from vtj only once`() {
        MockVtjSoapTransport.addHUOLTAJAHUOLLETTAVARequestExpectation(
            testPersonWithVtjChildren,
            listOf(testPersonDependantChild),
        )
        MockVtjSoapTransport.addPERUSSANOMA3RequestExpectation(testPersonWithVtjChildren)
        MockVtjSoapTransport.addPERUSSANOMA3RequestExpectation(testPersonDependantChild)
        db.transaction {
            personService.getPersonWithChildren(
                it,
                user,
                HelsinkiDateTime.now(),
                testPersonWithVtjChildren.id,
            )
        }
        assertEquals(0, MockVtjSoapTransport.getPERUSSANOMA3RequestCount(testPersonWithVtjChildren))
        assertEquals(1, MockVtjSoapTransport.getPERUSSANOMA3RequestCount(testPersonDependantChild))
        assertEquals(
            1,
            MockVtjSoapTransport.getHUOLTAJAHUOLLETTAVARequestCount(testPersonWithVtjChildren),
        )
    }

    @Test
    fun `addSsn writes the requested SSN and the VTJ details`() {
        val person = DevPerson(ssn = null, firstName = "Tuntematon")
        val vtjRecord = DevPerson(ssn = "070644-937X", firstName = "Johannes", lastName = "Karhula")
        db.transaction { it.insert(person, DevPersonType.ADULT) }
        MockVtjSoapTransport.addPERUSSANOMA3RequestExpectation(vtjRecord)

        db.transaction {
            personService.addSsn(it, user, now, person.id, Ssn.parse(vtjRecord.ssn!!)!!)
        }

        val updated = db.read { it.getPersonById(person.id) }!!
        assertEquals(vtjRecord.ssn, updated.ssn?.value)
        assertEquals("Johannes", updated.firstName)
        assertEquals(now, updated.updatedFromVtj)
    }

    @Test
    fun `addSsn fails when VTJ returns a different SSN than the requested one`() {
        val person = DevPerson(ssn = null)
        db.transaction { it.insert(person, DevPersonType.ADULT) }
        val requestedSsn = Ssn.parse("070644-937X")!!
        val vtjClientReturningAnotherSsn =
            object : VtjClient {
                override fun fetchPerson(query: VtjClient.DetailsQuery) =
                    VtjPerson(
                        firstNames = "Johannes",
                        lastName = "Karhula",
                        socialSecurityNumber = "010199-8137",
                        restrictedDetails = null,
                    )

                override fun fetchWithGuardians(query: VtjClient.DetailsQuery) = error("unused")

                override fun fetchWithDependants(query: VtjClient.DetailsQuery) = error("unused")
            }

        assertThrows<BadRequest> {
            db.transaction {
                PersonService(vtjClientReturningAnotherSsn)
                    .addSsn(it, user, now, person.id, requestedSsn)
            }
        }

        assertNull(db.read { it.getPersonById(person.id) }!!.ssn)
    }
}
