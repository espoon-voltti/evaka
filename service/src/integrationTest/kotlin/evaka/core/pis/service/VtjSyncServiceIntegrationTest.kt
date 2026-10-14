// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.PureJdbiTest
import evaka.core.identity.Ssn
import evaka.core.identity.getDobFromSsn
import evaka.core.pis.getPersonById
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevParentship
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.NotFound
import evaka.core.user.updateLastStrongLogin
import evaka.core.vtjclient.mapper.VtjHenkiloMapper
import evaka.core.vtjclient.service.persondetails.SoapVtjClient
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

/**
 * Fixture family: A is a citizen. X is a child of A and B. B is also a guardian of Z, whose other
 * guardian is C. M is a minor guardian of Y, and P is the guardian of M.
 */
class VtjSyncServiceIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    private lateinit var vtjSyncService: VtjSyncService
    private val user = AuthenticatedUser.SystemInternalUser
    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2026, 3, 2), LocalTime.of(12, 0)))

    private val a = person("150385-9013", "Aino")
    private val b = person("200687-903A", "Bertta")
    private val c = person("101090-905X", "Cecilia")
    private val x = person("050120A910J", "Xavier")
    private val z = person("080622A9123", "Zeta")
    private val p = person("010170-903M", "Pertti")
    private val m = person("010609A901V", "Minna")
    private val y = person("011025A9028", "Ykä")

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
        addToVtj(b, dependants = listOf(x, z))
        addToVtj(c, dependants = listOf(z))
        addToVtj(x, guardians = listOf(a, b))
        addToVtj(z, guardians = listOf(b, c))
        addToVtj(p, dependants = listOf(m))
        addToVtj(m, dependants = listOf(y), guardians = listOf(p))
        addToVtj(y, guardians = listOf(m))
    }

    @Test
    fun `getOrCreateChildWithGuardians creates a new child and its guardians from one guardians query`() {
        val childId = vtjSyncService.getOrCreateChildWithGuardians(db, user, clock, x.ssn())

        MockVtjSoapTransport.assertRequests(
            HUOLLETTAVA_HUOLTAJAT to x,
            PERUSSANOMA3 to a,
            PERUSSANOMA3 to b,
        )
        assertEquals(getPerson(x).id, childId)
        assertPersons(a, b, x)
        assertGuardians(a to x, b to x)
        assertNotNull(getPerson(x).vtjGuardiansQueried)
    }

    @Test
    fun `getOrCreateChildWithGuardians makes no request for a child whose guardians are synced`() {
        db.transaction { tx ->
            tx.insert(x, DevPersonType.CHILD)
            tx.setVtjGuardiansQueried(x)
        }

        val childId = vtjSyncService.getOrCreateChildWithGuardians(db, user, clock, x.ssn())

        MockVtjSoapTransport.assertRequests()
        assertEquals(x.id, childId)
    }

    @Test
    fun `a new guardian who is a minor is created as a leaf`() {
        vtjSyncService.getOrCreateChildWithGuardians(db, user, clock, y.ssn())

        MockVtjSoapTransport.assertRequests(HUOLLETTAVA_HUOLTAJAT to y, PERUSSANOMA3 to m)
        assertPersons(m, y)
        assertGuardians(m to y)
        assertNull(getPerson(m).vtjGuardiansQueried)
    }

    @Test
    fun `refreshPerson on a leaf adult creates nobody and clears the dependants marker`() {
        db.transaction { tx ->
            tx.insert(b, DevPersonType.RAW_ROW)
            tx.setVtjDependantsQueried(b)
        }

        vtjSyncService.refreshPerson(db, user, clock, b.id)

        MockVtjSoapTransport.assertRequests(PERUSSANOMA3 to b)
        assertPersons(b)
        assertGuardians()
        assertNull(getPerson(b).vtjDependantsQueried)
    }

    @Test
    fun `refreshPerson on a dependant-sync adult creates a new dependant as a leaf`() {
        db.transaction { tx ->
            tx.insert(a, DevPersonType.ADULT)
            tx.updateLastStrongLogin(clock.now(), a.id)
        }

        vtjSyncService.refreshPerson(db, user, clock, a.id)

        MockVtjSoapTransport.assertRequests(HUOLTAJA_HUOLLETTAVA to a, PERUSSANOMA3 to x)
        assertPersons(a, x)
        assertGuardians(a to x)
        assertNotNull(getPerson(a).vtjDependantsQueried)
        assertNull(getPerson(x).vtjGuardiansQueried)
    }

    @Test
    fun `refreshPerson on a minor queries only the guardians`() {
        db.transaction { tx ->
            tx.insert(a, DevPersonType.RAW_ROW)
            tx.insert(b, DevPersonType.RAW_ROW)
            tx.insert(x, DevPersonType.CHILD)
        }

        vtjSyncService.refreshPerson(db, user, clock, x.id)

        MockVtjSoapTransport.assertRequests(HUOLLETTAVA_HUOLTAJAT to x)
        assertPersons(a, b, x)
        assertGuardians(a to x, b to x)
    }

    @Test
    fun `refreshPerson on a minor who is a dependant-sync person queries both relations`() {
        db.transaction { tx ->
            tx.insert(p, DevPersonType.RAW_ROW)
            tx.insert(m, DevPersonType.RAW_ROW)
            tx.insert(y, DevPersonType.CHILD)
            tx.insert(
                DevParentship(
                    childId = y.id,
                    headOfChildId = m.id,
                    startDate = y.dateOfBirth,
                    endDate = y.dateOfBirth.plusYears(18).minusDays(1),
                )
            )
        }

        vtjSyncService.refreshPerson(db, user, clock, m.id)

        MockVtjSoapTransport.assertRequests(HUOLLETTAVA_HUOLTAJAT to m, HUOLTAJA_HUOLLETTAVA to m)
        assertPersons(p, m, y)
        assertGuardians(p to m, m to y)
    }

    @Test
    fun `ensureDependantsSynced makes no request for a person who is not dependant-sync`() {
        db.transaction { it.insert(b, DevPersonType.RAW_ROW) }

        vtjSyncService.ensureDependantsSynced(db, user, clock, b.id)

        MockVtjSoapTransport.assertRequests()
        assertNull(getPerson(b).vtjDependantsQueried)
    }

    @Test
    fun `ensureDependantsSynced makes no request when the dependants are synced`() {
        db.transaction { tx ->
            tx.insert(a, DevPersonType.ADULT)
            tx.updateLastStrongLogin(clock.now(), a.id)
            tx.setVtjDependantsQueried(a)
        }

        vtjSyncService.ensureDependantsSynced(db, user, clock, a.id)

        MockVtjSoapTransport.assertRequests()
    }

    @Test
    fun `ensureDependantsSynced syncs a dependant-sync person whose dependants are not synced`() {
        db.transaction { tx ->
            tx.insert(a, DevPersonType.ADULT)
            tx.updateLastStrongLogin(clock.now(), a.id)
        }

        vtjSyncService.ensureDependantsSynced(db, user, clock, a.id)

        MockVtjSoapTransport.assertRequests(HUOLTAJA_HUOLLETTAVA to a, PERUSSANOMA3 to x)
        assertNotNull(getPerson(a).vtjDependantsQueried)
    }

    @Test
    fun `syncDependantsForFamilySetup syncs a person who is not dependant-sync`() {
        db.transaction { it.insert(b, DevPersonType.RAW_ROW) }

        vtjSyncService.syncDependantsForFamilySetup(db, user, clock, b.id)

        MockVtjSoapTransport.assertRequests(
            HUOLTAJA_HUOLLETTAVA to b,
            PERUSSANOMA3 to x,
            PERUSSANOMA3 to z,
        )
        assertPersons(b, x, z)
        assertGuardians(b to x, b to z)
        assertNotNull(getPerson(b).vtjDependantsQueried)
    }

    @Test
    fun `ensureGuardiansSynced makes no request when the guardians are synced`() {
        db.transaction { tx ->
            tx.insert(x, DevPersonType.CHILD)
            tx.setVtjGuardiansQueried(x)
        }

        vtjSyncService.ensureGuardiansSynced(db, user, clock, x.id)

        MockVtjSoapTransport.assertRequests()
    }

    @Test
    fun `ensureGuardiansSynced syncs a minor whose guardians are not synced`() {
        db.transaction { tx ->
            tx.insert(a, DevPersonType.RAW_ROW)
            tx.insert(x, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = a.id, childId = x.id))
        }

        vtjSyncService.ensureGuardiansSynced(db, user, clock, x.id)

        MockVtjSoapTransport.assertRequests(HUOLLETTAVA_HUOLTAJAT to x, PERUSSANOMA3 to b)
        assertGuardians(a to x, b to x)
        assertNotNull(getPerson(x).vtjGuardiansQueried)
    }

    @Test
    fun `a failure inside a dependants sync leaves the marker empty, and a second call completes it`() {
        db.transaction { tx ->
            tx.insert(a, DevPersonType.ADULT)
            tx.updateLastStrongLogin(clock.now(), a.id)
        }
        MockVtjSoapTransport.removeRequestExpectation(x, PERUSSANOMA3)

        assertThrows<NotFound> { vtjSyncService.ensureDependantsSynced(db, user, clock, a.id) }
        assertNull(getPerson(a).vtjDependantsQueried)
        assertPersons(a)
        assertGuardians()

        MockVtjSoapTransport.addPERUSSANOMA3RequestExpectation(x)
        vtjSyncService.ensureDependantsSynced(db, user, clock, a.id)

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

    private fun DevPerson.ssn() = Ssn.parse(ssn!!)!!

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

    private fun Database.Transaction.setVtjDependantsQueried(person: DevPerson) = execute {
        sql(
            "UPDATE person SET vtj_dependants_queried = ${bind(clock.now())} WHERE id = ${bind(person.id)}"
        )
    }

    private fun Database.Transaction.setVtjGuardiansQueried(person: DevPerson) = execute {
        sql(
            "UPDATE person SET vtj_guardians_queried = ${bind(clock.now())} WHERE id = ${bind(person.id)}"
        )
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
