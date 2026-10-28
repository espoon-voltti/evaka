// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.PureJdbiTest
import evaka.core.application.ApplicationStatus
import evaka.core.application.ApplicationType
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.identity.getDobFromSsn
import evaka.core.pis.ModifySource
import evaka.core.pis.PersonsToSyncCount
import evaka.core.pis.UpdateFromVtjAsyncJobProcessor
import evaka.core.pis.countPersonsToSync
import evaka.core.pis.getParentships
import evaka.core.pis.getPersonById
import evaka.core.pis.updatePartnershipDuration
import evaka.core.shared.PartnershipId
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.vtjclient.mapper.VtjHenkiloMapper
import evaka.core.vtjclient.service.persondetails.SoapVtjClient
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.RequestType.HUOLLETTAVA_HUOLTAJAT
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.RequestType.HUOLTAJA_HUOLLETTAVA
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock

/**
 * The fridge rows and the citizen user rows get their timestamps from the DB clock, so the mock
 * clock follows real time, and a row that is older than the window of the nightly sync job is
 * inserted with explicit timestamps.
 */
class SyncMissingVtjRelationsIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    private lateinit var vtjSyncService: VtjSyncService
    private val clock = MockEvakaClock(HelsinkiDateTime.now())
    private val today = clock.today()
    private val longAgo = clock.now().minusDays(30)

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val a = person("150385-9013")
    private val b = person("200687-903A")
    private val x = person("050120A910J")
    private val z = person("080622A9123")

    @BeforeEach
    fun beforeEach() {
        asyncJobRunner = AsyncJobRunner(AsyncJob::class, listOf(AsyncJob.main), jdbi, noopTracer)
        val vtjClient = SoapVtjClient(MockVtjSoapTransport(), VtjHenkiloMapper())
        vtjSyncService = VtjSyncService(vtjClient, asyncJobRunner)
        UpdateFromVtjAsyncJobProcessor(
            asyncJobRunner,
            vtjSyncService,
            FridgeFamilyService(
                PersonService(vtjClient, vtjSyncService),
                vtjSyncService,
                ParentshipService(asyncJobRunner),
            ),
        )
        MockVtjSoapTransport.resetQueryCounts()
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(a, DevPersonType.RAW_ROW)
            tx.insert(b, DevPersonType.RAW_ROW)
            tx.insert(x, DevPersonType.CHILD)
            tx.insert(z, DevPersonType.CHILD)
        }
    }

    @AfterEach
    fun afterEach() {
        asyncJobRunner.close()
    }

    @Test
    fun `plans a sync for a new citizen user`() {
        db.transaction { it.insertCitizenUser(a, createdAt = clock.now()) }

        planMissingSyncs()

        assertEquals(setOf(a.id), plannedSyncs())
    }

    @Test
    fun `plans a sync for the head of a new fridge child row`() {
        db.transaction { it.insertParentship(a, x, startDate = today, updated = clock.now()) }

        planMissingSyncs()

        assertEquals(setOf(a.id), plannedSyncs())
    }

    @Test
    fun `plans a sync for the partners of a new partnership`() {
        db.transaction { it.insertPartnership(a, b, startDate = today, updated = clock.now()) }

        planMissingSyncs()

        assertEquals(setOf(a.id, b.id), plannedSyncs())
    }

    @Test
    fun `plans a sync for the partners of an old partnership whose start date has come`() {
        db.transaction {
            it.insertPartnership(a, b, startDate = today.minusDays(1), updated = longAgo)
        }

        planMissingSyncs()

        assertEquals(setOf(a.id, b.id), plannedSyncs())
    }

    @Test
    fun `plans a sync for the partners of an old partnership whose dates were edited`() {
        val partnership = db.transaction {
            it.insertPartnership(
                a,
                b,
                startDate = today.minusYears(1),
                endDate = today.minusDays(1),
                updated = longAgo,
            )
        }
        planMissingSyncs()
        assertEquals(emptySet(), plannedSyncs())

        db.transaction {
            it.updatePartnershipDuration(
                partnership,
                startDate = today.minusYears(1),
                endDate = null,
                ModifySource.USER,
                clock.now(),
                modifiedBy = null,
            )
        }
        planMissingSyncs()

        assertEquals(setOf(a.id, b.id), plannedSyncs())
    }

    @Test
    fun `plans a sync for the master of a merge who gets the fridge rows of the duplicate`() {
        val duplicate = DevPerson(ssn = null)
        db.transaction { tx ->
            tx.insert(duplicate, DevPersonType.ADULT)
            tx.insertParentship(duplicate, x, startDate = today.minusYears(1), updated = longAgo)
        }
        planMissingSyncs()
        assertEquals(emptySet(), plannedSyncs())

        db.transaction { tx ->
            MergeService(mock(), mock()).mergePeople(tx, clock, master = a.id, duplicate.id)
        }
        planMissingSyncs()

        assertEquals(setOf(a.id), plannedSyncs())
    }

    @Test
    fun `does not plan a sync for a dependant-sync person whose reason is older than the window`() {
        db.transaction { tx ->
            tx.insertCitizenUser(a, createdAt = longAgo)
            tx.insertParentship(b, x, startDate = today.minusYears(1), updated = longAgo)
        }

        planMissingSyncs()

        assertEquals(emptySet(), plannedSyncs())
        assertEquals(
            PersonsToSyncCount(
                newlyDependantSync = 0,
                guardianSyncChildren = 0,
                dependantSyncBacklog = 2,
            ),
            countPersonsToSync(),
        )
    }

    @Test
    fun `does not plan a sync for a person whose dependants are synced`() {
        db.transaction { tx ->
            tx.insertCitizenUser(a, createdAt = clock.now())
            tx.setVtjDependantsQueried(a, clock.now())
        }

        planMissingSyncs()

        assertEquals(emptySet(), plannedSyncs())
    }

    @Test
    fun `does not plan a sync for a minor outside services or a child whose guardians are synced`() {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = x.id,
                    unitId = daycare.id,
                    startDate = today,
                    endDate = today.plusMonths(6),
                )
            )
            tx.setVtjGuardiansQueried(x, clock.now())
        }

        planMissingSyncs()

        assertEquals(emptySet(), plannedSyncs())
    }

    @Test
    fun `plans a sync for a child whose placement ends today, not for one whose placement ended yesterday`() {
        db.transaction { tx ->
            tx.insertPlacement(x, endDate = today)
            tx.insertPlacement(z, endDate = today.minusDays(1))
        }

        planMissingSyncs()

        assertEquals(setOf(x.id), plannedSyncs())
    }

    @ParameterizedTest
    @EnumSource(
        value = ApplicationStatus::class,
        names = ["ACTIVE", "REJECTED", "CANCELLED"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `plans a sync for a child with an application that is not in a final state`(
        status: ApplicationStatus
    ) {
        db.transaction { it.insertApplication(x, status) }

        planMissingSyncs()

        assertEquals(setOf(x.id), plannedSyncs())
    }

    @ParameterizedTest
    @EnumSource(value = ApplicationStatus::class, names = ["ACTIVE", "REJECTED", "CANCELLED"])
    fun `does not plan a sync for a child with an application in a final state`(
        status: ApplicationStatus
    ) {
        db.transaction { it.insertApplication(x, status) }

        planMissingSyncs()

        assertEquals(emptySet(), plannedSyncs())
    }

    @Test
    fun `plans a sync for a placed child until the day before the 18th birthday`() {
        val turns18Tomorrow =
            DevPerson(ssn = "101090-905X", dateOfBirth = today.minusYears(18).plusDays(1))
        val turned18Today = DevPerson(ssn = "010617A123U", dateOfBirth = today.minusYears(18))
        db.transaction { tx ->
            tx.insert(turns18Tomorrow, DevPersonType.CHILD)
            tx.insert(turned18Today, DevPersonType.CHILD)
            tx.insertPlacement(turns18Tomorrow, endDate = today)
            tx.insertPlacement(turned18Today, endDate = today)
        }

        planMissingSyncs()

        assertEquals(setOf(turns18Tomorrow.id), plannedSyncs())
    }

    @Test
    fun `does not plan a sync for a placed child with no SSN`() {
        val noSsn = DevPerson(ssn = null, dateOfBirth = today.minusYears(2))
        db.transaction { tx ->
            tx.insert(noSsn, DevPersonType.CHILD)
            tx.insertPlacement(noSsn, endDate = today)
        }

        planMissingSyncs()

        assertEquals(emptySet(), plannedSyncs())
    }

    @Test
    fun `counts the persons to sync and plans at most the limit, newly dependant-sync persons first`() {
        db.transaction { tx ->
            tx.insertCitizenUser(a, createdAt = clock.now())
            tx.insertCitizenUser(b, createdAt = longAgo)
            tx.insertApplication(x, ApplicationStatus.SENT)
            tx.insertApplication(z, ApplicationStatus.SENT)
        }

        assertEquals(
            PersonsToSyncCount(
                newlyDependantSync = 1,
                guardianSyncChildren = 2,
                dependantSyncBacklog = 2,
            ),
            countPersonsToSync(),
        )

        vtjSyncService.planMissingSyncs(db, clock, limit = 2)

        // The DB orders UUIDs like their text form
        assertEquals(setOf(a.id, minOf(x.id, z.id, compareBy { it.toString() })), plannedSyncs())
    }

    @Test
    fun `replaces the syncs that are still waiting from the previous run`() {
        db.transaction { it.insertCitizenUser(a, createdAt = clock.now()) }

        planMissingSyncs()
        planMissingSyncs()

        assertEquals(listOf(a.id), plannedSyncList())
    }

    @Test
    fun `clears an old dependants marker of a person who is not dependant-sync, and keeps a newer one`() {
        db.transaction { tx ->
            tx.setVtjDependantsQueried(a, clock.now().minusDays(2))
            tx.setVtjDependantsQueried(b, clock.now().minusHours(12))
        }

        planMissingSyncs()

        assertNull(getPerson(a).vtjDependantsQueried)
        assertNotNull(getPerson(b).vtjDependantsQueried)
    }

    @Test
    fun `keeps an old dependants marker of a dependant-sync person`() {
        db.transaction { tx ->
            tx.insertCitizenUser(a, createdAt = longAgo)
            tx.setVtjDependantsQueried(a, longAgo)
        }

        planMissingSyncs()

        assertNotNull(getPerson(a).vtjDependantsQueried)
    }

    @Test
    fun `a planned sync syncs the dependants of the person`() {
        MockVtjSoapTransport.addPERUSSANOMA3RequestExpectation(a)
        MockVtjSoapTransport.addHUOLTAJAHUOLLETTAVARequestExpectation(a, listOf(x))
        db.transaction { it.insertCitizenUser(a, createdAt = clock.now()) }

        planMissingSyncs()
        asyncJobRunner.runPendingJobsSync(clock)

        MockVtjSoapTransport.assertRequests(HUOLTAJA_HUOLLETTAVA to a)
        assertNotNull(getPerson(a).vtjDependantsQueried)
        assertEquals(emptySet(), plannedSyncs())
    }

    @Test
    fun `a planned sync adds the synced dependants at the same address to the family of the person`() {
        MockVtjSoapTransport.addHUOLTAJAHUOLLETTAVARequestExpectation(a, listOf(x))
        db.transaction { it.insertCitizenUser(a, createdAt = clock.now()) }

        planMissingSyncs()
        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(listOf(x.id), fridgeChildIds(a))
    }

    @Test
    fun `a planned sync of a child adds the child to the family of a guardian at the same address`() {
        MockVtjSoapTransport.addHUOLLETTAVAHUOLTAJATRequestExpectation(x, listOf(a))
        db.transaction { tx ->
            // A's dependants were synced before, so only the child gets a sync
            tx.insertCitizenUser(a, createdAt = longAgo)
            tx.setVtjDependantsQueried(a, longAgo)
            tx.insertPlacement(x, endDate = today.plusMonths(6))
        }

        planMissingSyncs()
        assertEquals(setOf(x.id), plannedSyncs())
        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(listOf(x.id), fridgeChildIds(a))
    }

    @Test
    fun `a failing sync is tried a few times, leaves the flag empty, and is planned again the next night`() {
        MockVtjSoapTransport.removeRequestExpectation(x, HUOLLETTAVA_HUOLTAJAT)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = x.id,
                    unitId = daycare.id,
                    startDate = today,
                    endDate = today.plusMonths(6),
                )
            )
        }

        planMissingSyncs()
        // The tries are 5 minutes apart
        repeat(5) {
            asyncJobRunner.runPendingJobsSync(MockEvakaClock(clock.now().plusMinutes(5L * it)))
        }

        MockVtjSoapTransport.assertRequests(
            HUOLLETTAVA_HUOLTAJAT to x,
            HUOLLETTAVA_HUOLTAJAT to x,
            HUOLLETTAVA_HUOLTAJAT to x,
        )
        assertNull(getPerson(x).vtjGuardiansQueried)
        assertEquals(emptySet(), plannedSyncs())

        vtjSyncService.planMissingSyncs(db, MockEvakaClock(clock.now().plusDays(1)), limit = 100)

        assertEquals(setOf(x.id), plannedSyncs())
    }

    // Everyone lives at the same address, so a synced child can be added to the family of a
    // guardian
    private fun person(ssn: String) =
        DevPerson(
            ssn = ssn,
            dateOfBirth = getDobFromSsn(ssn),
            streetAddress = "Kotikatu 1",
            postalCode = "02100",
            postOffice = "Espoo",
        )

    private fun planMissingSyncs() = vtjSyncService.planMissingSyncs(db, clock, limit = 100)

    private fun countPersonsToSync() = db.read { it.countPersonsToSync(clock.now()) }

    private fun plannedSyncList(): List<PersonId> = db.read { tx ->
        tx.createQuery {
                sql(
                    """
SELECT (payload ->> 'personId')::uuid
FROM async_job
WHERE type = 'SyncMissingVtjRelationsOfPerson' AND completed_at IS NULL AND retry_count > 0
"""
                )
            }
            .toList<PersonId>()
    }

    private fun plannedSyncs(): Set<PersonId> = plannedSyncList().toSet()

    private fun getPerson(person: DevPerson) = db.read { it.getPersonById(person.id)!! }

    private fun fridgeChildIds(head: DevPerson) =
        db.read { it.getParentships(headOfChildId = head.id, childId = null) }.map { it.childId }

    private fun Database.Transaction.insertCitizenUser(
        person: DevPerson,
        createdAt: HelsinkiDateTime,
    ) = execute {
        sql(
            "INSERT INTO citizen_user (id, created_at) VALUES (${bind(person.id)}, ${bind(createdAt)})"
        )
    }

    private fun Database.Transaction.insertParentship(
        head: DevPerson,
        child: DevPerson,
        startDate: LocalDate,
        updated: HelsinkiDateTime,
    ) = execute {
        sql(
            """
INSERT INTO fridge_child (head_of_child, child_id, start_date, end_date, created_at, updated)
VALUES (${bind(head.id)}, ${bind(child.id)}, ${bind(startDate)}, ${bind(child.dateOfBirth.plusYears(18).minusDays(1))}, ${bind(updated)}, ${bind(updated)})
"""
        )
    }

    private fun Database.Transaction.insertPartnership(
        first: DevPerson,
        second: DevPerson,
        startDate: LocalDate,
        endDate: LocalDate? = null,
        updated: HelsinkiDateTime,
    ): PartnershipId {
        val id = PartnershipId(UUID.randomUUID())
        listOf(Triple(first, 1, 2), Triple(second, 2, 1)).forEach { (partner, indx, otherIndx) ->
            execute {
                sql(
                    """
INSERT INTO fridge_partner (partnership_id, indx, other_indx, person_id, start_date, end_date, created_at, updated)
VALUES (${bind(id)}, ${bind(indx)}, ${bind(otherIndx)}, ${bind(partner.id)}, ${bind(startDate)}, ${bind(endDate)}, ${bind(updated)}, ${bind(updated)})
"""
                )
            }
        }
        return id
    }

    private fun Database.Transaction.insertPlacement(child: DevPerson, endDate: LocalDate) =
        insert(
            DevPlacement(
                childId = child.id,
                unitId = daycare.id,
                startDate = today.minusYears(1),
                endDate = endDate,
            )
        )

    private fun Database.Transaction.insertApplication(
        child: DevPerson,
        status: ApplicationStatus,
    ) {
        insertTestApplication(
            type = ApplicationType.DAYCARE,
            status = status,
            guardianId = b.id,
            childId = child.id,
            confidential = true,
            document =
                DaycareFormV0(
                    type = ApplicationType.DAYCARE,
                    child = Child(dateOfBirth = null),
                    guardian = Adult(),
                    apply = Apply(preferredUnits = listOf(daycare.id)),
                ),
        )
    }

    private fun Database.Transaction.setVtjDependantsQueried(
        person: DevPerson,
        at: HelsinkiDateTime,
    ) = execute {
        sql("UPDATE person SET vtj_dependants_queried = ${bind(at)} WHERE id = ${bind(person.id)}")
    }

    private fun Database.Transaction.setVtjGuardiansQueried(
        person: DevPerson,
        at: HelsinkiDateTime,
    ) = execute {
        sql("UPDATE person SET vtj_guardians_queried = ${bind(at)} WHERE id = ${bind(person.id)}")
    }
}
