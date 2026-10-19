// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.PureJdbiTest
import evaka.core.identity.Ssn
import evaka.core.identity.getDobFromSsn
import evaka.core.pis.getParentships
import evaka.core.pis.getPersonById
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.dev.DevParentship
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.NotFound
import evaka.core.user.updateLastStrongLogin
import evaka.core.vtjclient.dto.VtjPerson
import evaka.core.vtjclient.mapper.VtjHenkiloMapper
import evaka.core.vtjclient.service.persondetails.SoapVtjClient
import evaka.core.vtjclient.service.persondetails.VtjClient
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.RequestType.HUOLLETTAVA_HUOLTAJAT
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.RequestType.HUOLTAJA_HUOLLETTAVA
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.RequestType.PERUSSANOMA3
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock

/** Fixture family: X is a child of A and B. */
class VtjSyncServiceIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    private lateinit var vtjSyncService: VtjSyncService
    private val user = AuthenticatedUser.SystemInternalUser
    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2026, 3, 2), LocalTime.of(12, 0)))

    private val a = person("150385-9013", "Aino")
    private val b = person("200687-903A", "Bertta")
    private val x = person("050120A910J", "Xavier")

    @BeforeEach
    fun beforeEach() {
        asyncJobRunner = AsyncJobRunner(AsyncJob::class, listOf(AsyncJob.main), jdbi, noopTracer)
        vtjSyncService =
            VtjSyncService(
                SoapVtjClient(MockVtjSoapTransport(), VtjHenkiloMapper()),
                asyncJobRunner,
            )
        MockVtjSoapTransport.resetQueryCounts()
        addToVtj(a, dependants = listOf(x))
        addToVtj(b, dependants = listOf(x))
        addToVtj(x, guardians = listOf(a, b))
    }

    @Test
    fun `getOrCreatePerson fails when VTJ returns a different SSN than the requested one`() {
        assertThrows<BadRequest> {
            VtjSyncService(vtjClientReturningAnotherSsn, mock())
                .getOrCreatePerson(db, user.evakaUserId, clock, a.ssn())
        }

        assertPersons()
    }

    @Test
    fun `a failure inside a dependants sync leaves the marker empty, and a second call completes it`() {
        db.transaction { tx ->
            tx.insert(a, DevPersonType.ADULT)
            tx.updateLastStrongLogin(clock.now(), a.id)
        }
        MockVtjSoapTransport.removeRequestExpectation(x, PERUSSANOMA3)

        assertThrows<NotFound> {
            vtjSyncService.ensureDependantsSynced(db, user.evakaUserId, clock, a.id)
        }
        assertNull(getPerson(a).vtjDependantsQueried)
        assertPersons(a)
        assertGuardians()

        MockVtjSoapTransport.addPERUSSANOMA3RequestExpectation(x)
        vtjSyncService.ensureDependantsSynced(db, user.evakaUserId, clock, a.id)

        assertNotNull(getPerson(a).vtjDependantsQueried)
        assertGuardians(a to x)
    }

    @Test
    fun `addSsn writes the requested SSN and the VTJ details of an adult`() {
        val person = DevPerson(ssn = null, dateOfBirth = b.dateOfBirth, firstName = "Tuntematon")
        db.transaction { it.insert(person, DevPersonType.ADULT) }

        vtjSyncService.addSsn(db, user.evakaUserId, clock, person.id, b.ssn())

        MockVtjSoapTransport.assertRequests(PERUSSANOMA3 to b)
        val updated = getPerson(b)
        assertEquals(person.id, updated.id)
        assertEquals("Bertta", updated.firstName)
        assertEquals(clock.now(), updated.updatedFromVtj)
        assertGuardians()
    }

    @Test
    fun `addSsn on a minor syncs the guardians`() {
        val person = DevPerson(ssn = null, dateOfBirth = x.dateOfBirth)
        db.transaction { it.insert(person, DevPersonType.CHILD) }

        vtjSyncService.addSsn(db, user.evakaUserId, clock, person.id, x.ssn())

        MockVtjSoapTransport.assertRequests(
            PERUSSANOMA3 to x,
            HUOLLETTAVA_HUOLTAJAT to x,
            PERUSSANOMA3 to a,
            PERUSSANOMA3 to b,
        )
        assertEquals(person.id, getPerson(x).id)
        assertPersons(a, b, x)
        assertGuardians(a to x, b to x)
    }

    @Test
    fun `addSsn on a head of family syncs the dependants`() {
        val person = DevPerson(ssn = null, dateOfBirth = a.dateOfBirth)
        // A child with no SSN yet, so the dependants sync creates X as another person
        val childWithNoSsn = DevPerson(ssn = null, dateOfBirth = x.dateOfBirth)
        db.transaction { tx ->
            tx.insert(person, DevPersonType.ADULT)
            tx.insert(childWithNoSsn, DevPersonType.CHILD)
            tx.insert(
                DevParentship(
                    childId = childWithNoSsn.id,
                    headOfChildId = person.id,
                    startDate = clock.today(),
                    endDate = x.dateOfBirth.plusYears(18).minusDays(1),
                )
            )
        }

        vtjSyncService.addSsn(db, user.evakaUserId, clock, person.id, a.ssn())

        MockVtjSoapTransport.assertRequests(
            PERUSSANOMA3 to a,
            HUOLTAJA_HUOLLETTAVA to a,
            PERUSSANOMA3 to x,
        )
        assertEquals(person.id, getPerson(a).id)
        assertGuardians(a to x)
    }

    @Test
    fun `addSsn applies the death effects when VTJ reports a date of death`() {
        val dateOfDeath = LocalDate.of(2026, 2, 1)
        val person = DevPerson(ssn = null)
        val child = DevPerson(ssn = null, dateOfBirth = LocalDate.of(2020, 1, 1))
        val vtjRecord = DevPerson(ssn = "070644-937X", dateOfDeath = dateOfDeath)
        db.transaction { tx ->
            tx.insert(person, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                DevParentship(
                    childId = child.id,
                    headOfChildId = person.id,
                    startDate = LocalDate.of(2020, 1, 1),
                    endDate = LocalDate.of(2037, 12, 31),
                )
            )
        }
        addToVtj(vtjRecord)

        vtjSyncService.addSsn(db, user.evakaUserId, clock, person.id, vtjRecord.ssn())

        db.read { tx ->
            assertEquals(
                listOf(dateOfDeath),
                tx.getParentships(headOfChildId = person.id, childId = null).map { it.endDate },
            )
            assertEquals(
                2,
                tx.createQuery {
                        sql(
                            "SELECT count(*) FROM async_job WHERE type = 'GenerateFinanceDecisions'"
                        )
                    }
                    .exactlyOne<Int>(),
            )
        }
    }

    @Test
    fun `addSsn fails when VTJ returns a different SSN than the requested one`() {
        val person = DevPerson(ssn = null)
        db.transaction { it.insert(person, DevPersonType.ADULT) }

        assertThrows<BadRequest> {
            VtjSyncService(vtjClientReturningAnotherSsn, mock())
                .addSsn(db, user.evakaUserId, clock, person.id, Ssn.parse("070644-937X")!!)
        }

        assertNull(db.read { it.getPersonById(person.id) }!!.ssn)
    }

    @AfterEach
    fun afterEach() {
        asyncJobRunner.close()
    }

    private fun person(ssn: String, firstName: String) =
        DevPerson(
            ssn = ssn,
            dateOfBirth = getDobFromSsn(ssn),
            firstName = firstName,
            lastName = "Testinen",
            streetAddress = "Kotikatu 1",
            postalCode = "02100",
            postOffice = "Espoo",
        )

    private fun DevPerson.ssn() = Ssn.parse(ssn!!)!!

    private val vtjClientReturningAnotherSsn =
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

    private fun addToVtj(
        person: DevPerson,
        dependants: List<DevPerson> = emptyList(),
        guardians: List<DevPerson> = emptyList(),
    ) {
        MockVtjSoapTransport.addPERUSSANOMA3RequestExpectation(person)
        MockVtjSoapTransport.addHUOLTAJAHUOLLETTAVARequestExpectation(person, dependants)
        MockVtjSoapTransport.addHUOLLETTAVAHUOLTAJATRequestExpectation(person, guardians)
    }

    private fun getPerson(person: DevPerson) = db.read { tx ->
        tx.createQuery {
                sql("SELECT id FROM person WHERE social_security_number = ${bind(person.ssn)}")
            }
            .exactlyOne<PersonId>()
            .let { tx.getPersonById(it)!! }
    }

    private fun assertPersons(vararg persons: DevPerson) {
        assertEquals(
            persons.map { it.ssn!! }.toSet(),
            db.read { tx ->
                tx.createQuery { sql("SELECT social_security_number FROM person") }.toSet<String>()
            },
        )
    }

    private fun assertGuardians(vararg guardianToChild: Pair<DevPerson, DevPerson>) {
        assertEquals(
            guardianToChild.map { (guardian, child) -> guardian.ssn!! to child.ssn!! }.toSet(),
            db.read { tx ->
                tx.createQuery {
                        sql(
                            """
SELECT g.social_security_number AS guardian, c.social_security_number AS child
FROM guardian
JOIN person g ON g.id = guardian.guardian_id
JOIN person c ON c.id = guardian.child_id
"""
                        )
                    }
                    .toSet { column<String>("guardian") to column<String>("child") }
            },
        )
    }
}
