// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.PureJdbiTest
import evaka.core.identity.getDobFromSsn
import evaka.core.pis.getPersonById
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.NotFound
import evaka.core.user.updateLastStrongLogin
import evaka.core.vtjclient.mapper.VtjHenkiloMapper
import evaka.core.vtjclient.service.persondetails.SoapVtjClient
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
