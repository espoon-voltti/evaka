// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.PureJdbiTest
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.vtjclient.mapper.VtjHenkiloMapper
import evaka.core.vtjclient.service.persondetails.SoapVtjClient
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import kotlin.test.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PersonServiceIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {

    private val mockVtjSoapTransport: VtjSoapTransport = MockVtjSoapTransport()
    private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    private lateinit var personService: PersonService

    val user = AuthenticatedUser.SystemInternalUser
    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 3, 2), LocalTime.of(12, 0))

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
            it.insert(testPersonWithVtjChildren, DevPersonType.ADULT)
            it.insert(testPersonDependantChild, DevPersonType.CHILD)
        }
        MockVtjSoapTransport.resetQueryCounts()
        val vtjClient = SoapVtjClient(mockVtjSoapTransport, VtjHenkiloMapper())
        asyncJobRunner = AsyncJobRunner(AsyncJob::class, listOf(AsyncJob.main), jdbi, noopTracer)
        personService = PersonService(vtjClient, VtjSyncService(vtjClient, asyncJobRunner))
    }

    @AfterEach
    fun afterEach() {
        asyncJobRunner.close()
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
}
