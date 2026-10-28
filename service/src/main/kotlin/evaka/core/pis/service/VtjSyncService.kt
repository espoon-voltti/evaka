// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.identity.Ssn
import evaka.core.pis.applyDeathEffects
import evaka.core.pis.applyDependants
import evaka.core.pis.applyGuardians
import evaka.core.pis.applyPerson
import evaka.core.pis.clearDependantsSynced
import evaka.core.pis.clearStaleDependantsSynced
import evaka.core.pis.countPersonsToSync
import evaka.core.pis.getPersonById
import evaka.core.pis.getPersonBySSN
import evaka.core.pis.getPersonIdsBySsns
import evaka.core.pis.getPersonsToSync
import evaka.core.pis.isDependantSyncPerson
import evaka.core.pis.isMinor
import evaka.core.pis.setSsnFromVtj
import evaka.core.pis.updatePersonSsnAddingDisabled
import evaka.core.shared.ChildId
import evaka.core.shared.EvakaUserId
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.async.AsyncJobType
import evaka.core.shared.async.removeUnclaimedJobs
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.NotFound
import evaka.core.vtjclient.dto.VtjPerson
import evaka.core.vtjclient.service.persondetails.VtjClient
import fi.espoo.voltti.logging.loggers.info
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class VtjSyncService(
    private val vtjClient: VtjClient,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
    /** A new person is created as a leaf: no relations are synced. */
    fun getOrCreatePerson(
        dbc: Database.Connection,
        requestingUserId: EvakaUserId,
        clock: EvakaClock,
        ssn: Ssn,
    ): GetOrCreateResult {
        val existing = dbc.read { it.getPersonBySSN(ssn.value) }
        if (existing != null) return GetOrCreateResult(existing, created = false)
        val person = syncBasic(dbc, requestingUserId, clock, ssn, clearDependantsSynced = false)
        return GetOrCreateResult(person, created = true)
    }

    fun ensureDependantsSynced(
        dbc: Database.Connection,
        requestingUserId: EvakaUserId,
        clock: EvakaClock,
        personId: PersonId,
    ) {
        val person = dbc.read { it.getPersonById(personId) } ?: return
        if (person.ssn == null || person.vtjDependantsQueried != null) return
        if (!dbc.read { it.isDependantSyncPerson(personId, clock.today()) }) return
        syncDependants(dbc, requestingUserId, clock, person.ssn)
    }

    /** Syncs the dependants of a guardian who is about to become a head of family */
    fun syncDependantsForFamilySetup(
        dbc: Database.Connection,
        requestingUserId: EvakaUserId,
        clock: EvakaClock,
        personId: PersonId,
    ) {
        val person = dbc.read { it.getPersonById(personId) } ?: return
        if (person.ssn == null || person.vtjDependantsQueried != null) return
        syncDependants(dbc, requestingUserId, clock, person.ssn)
    }

    fun ensureGuardiansSynced(
        dbc: Database.Connection,
        requestingUserId: EvakaUserId,
        clock: EvakaClock,
        childId: ChildId,
    ) {
        val child = dbc.read { it.getPersonById(childId) } ?: return
        if (child.ssn == null || !isMinor(child.dateOfBirth, clock.today())) return
        if (child.vtjGuardiansQueried != null) return
        syncGuardians(dbc, requestingUserId, clock, child.ssn)
    }

    fun refreshPerson(
        dbc: Database.Connection,
        requestingUserId: EvakaUserId,
        clock: EvakaClock,
        personId: PersonId,
    ) {
        val person = dbc.read { it.getPersonById(personId) } ?: return
        if (person.ssn == null) return
        val minor = isMinor(person.dateOfBirth, clock.today())
        val dependantSync = dbc.read { it.isDependantSyncPerson(personId, clock.today()) }
        logger.info {
            "Refreshing person $personId from VTJ (minor: $minor, dependant-sync: $dependantSync)"
        }
        if (minor) syncGuardians(dbc, requestingUserId, clock, person.ssn)
        if (dependantSync) syncDependants(dbc, requestingUserId, clock, person.ssn)
        if (!minor && !dependantSync) {
            val _ =
                syncBasic(dbc, requestingUserId, clock, person.ssn, clearDependantsSynced = true)
        }
    }

    /**
     * Unlike refreshPerson, this syncs the dependants also when the person is not a dependant-sync
     * person, and then marks them synced.
     */
    fun refreshPersonAndDependants(
        dbc: Database.Connection,
        requestingUserId: EvakaUserId,
        clock: EvakaClock,
        personId: PersonId,
    ) {
        val person = dbc.read { it.getPersonById(personId) } ?: return
        if (person.ssn == null) return
        if (isMinor(person.dateOfBirth, clock.today()))
            syncGuardians(dbc, requestingUserId, clock, person.ssn)
        syncDependants(dbc, requestingUserId, clock, person.ssn)
    }

    fun addSsn(
        dbc: Database.Connection,
        requestingUserId: EvakaUserId,
        clock: EvakaClock,
        personId: PersonId,
        ssn: Ssn,
    ): PersonDTO {
        dbc.read { tx ->
            val person = tx.getPersonById(personId) ?: throw NotFound("Person $personId not found")
            if (person.ssn != null) throw BadRequest("User already has ssn")
            if (tx.getPersonBySSN(ssn.value) != null) {
                throw Conflict("User with same ssn already exists")
            }
        }
        val vtjPerson = vtjClient.fetchPerson(VtjClient.DetailsQuery(requestingUserId, ssn))
        if (vtjPerson.socialSecurityNumber != ssn.value) {
            throw BadRequest("VTJ returned a different SSN than the one being added")
        }
        val person = dbc.transaction { tx ->
            val now = clock.now()
            tx.updatePersonSsnAddingDisabled(personId, false)
            val result = tx.setSsnFromVtj(personId, vtjPerson, now)
            if (result.newDateOfDeath != null) {
                tx.applyDeathEffects(asyncJobRunner, now, result.id, result.newDateOfDeath)
            }
            result.person
        }
        ensureGuardiansSynced(dbc, requestingUserId, clock, personId)
        ensureDependantsSynced(dbc, requestingUserId, clock, personId)
        return person
    }

    /**
     * Plans a VTJ sync for at most [limit] persons who changed so that they need one, for example a
     * new citizen user, a new head of family or partner, or a child who entered services. Also
     * clears the dependants marker of each person who no longer needs synced dependants.
     */
    fun planMissingSyncs(dbc: Database.Connection, clock: EvakaClock, limit: Int) {
        val now = clock.now()
        dbc.transaction { tx ->
            val cleared =
                tx.clearStaleDependantsSynced(
                    olderThan = now.minusDays(1),
                    today = now.toLocalDate(),
                )
            tx.removeUnclaimedJobs(
                setOf(AsyncJobType(AsyncJob.SyncMissingVtjRelationsOfPerson::class))
            )
            val counts = tx.countPersonsToSync(now)
            val ids = tx.getPersonsToSync(now, limit)
            asyncJobRunner.plan(
                tx,
                ids.map { AsyncJob.SyncMissingVtjRelationsOfPerson(it) },
                retryCount = 3,
                runAt = now,
            )
            logger.info(
                mapOf(
                    "planned" to ids.size,
                    "newlyDependantSync" to counts.newlyDependantSync,
                    "guardianSyncChildren" to counts.guardianSyncChildren,
                    "dependantSyncBacklog" to counts.dependantSyncBacklog,
                    "clearedDependantsMarkers" to cleared,
                )
            ) {
                "Planned VTJ syncs for ${ids.size} persons (newly dependant-sync persons ${counts.newlyDependantSync}, guardian-sync children ${counts.guardianSyncChildren}, dependant-sync backlog ${counts.dependantSyncBacklog}), cleared $cleared stale dependants markers"
            }
        }
    }

    fun applyFromVtj(
        tx: Database.Transaction,
        now: HelsinkiDateTime,
        person: VtjPerson,
    ): PersonDTO {
        val result = tx.applyPerson(person, now)
        if (result.newDateOfDeath != null) {
            tx.applyDeathEffects(asyncJobRunner, now, result.id, result.newDateOfDeath)
        }
        return result.person
    }

    private fun syncBasic(
        dbc: Database.Connection,
        requestingUserId: EvakaUserId,
        clock: EvakaClock,
        ssn: Ssn,
        clearDependantsSynced: Boolean,
    ): PersonDTO {
        val vtjPerson = vtjClient.fetchPerson(VtjClient.DetailsQuery(requestingUserId, ssn))
        if (vtjPerson.socialSecurityNumber != ssn.value) {
            throw BadRequest("VTJ returned a different SSN than the requested one")
        }
        return dbc.transaction { tx ->
            val person = applyFromVtj(tx, clock.now(), vtjPerson)
            if (clearDependantsSynced) tx.clearDependantsSynced(person.id)
            person
        }
    }

    private fun syncGuardians(
        dbc: Database.Connection,
        requestingUserId: EvakaUserId,
        clock: EvakaClock,
        ssn: Ssn,
    ) {
        val snapshot = vtjClient.fetchWithGuardians(VtjClient.DetailsQuery(requestingUserId, ssn))
        val (existingGuardianIds, newGuardians) =
            fetchMissingPersons(dbc, requestingUserId, snapshot.guardians.map { it.ssn })
        dbc.transaction { tx ->
            val now = clock.now()
            val child = applyFromVtj(tx, now, snapshot.person)
            val newGuardianIds = newGuardians.map { applyFromVtj(tx, now, it).id }
            tx.applyGuardians(child.id, (existingGuardianIds + newGuardianIds).toSet(), now)
        }
    }

    private fun syncDependants(
        dbc: Database.Connection,
        requestingUserId: EvakaUserId,
        clock: EvakaClock,
        ssn: Ssn,
    ) {
        val snapshot = vtjClient.fetchWithDependants(VtjClient.DetailsQuery(requestingUserId, ssn))
        val (existingDependantIds, newDependants) =
            fetchMissingPersons(dbc, requestingUserId, snapshot.dependants.map { it.ssn })
        dbc.transaction { tx ->
            val now = clock.now()
            val guardian = applyFromVtj(tx, now, snapshot.person)
            val newDependantIds = newDependants.map { applyFromVtj(tx, now, it).id }
            tx.applyDependants(guardian.id, (existingDependantIds + newDependantIds).toSet(), now)
        }
    }

    private fun fetchMissingPersons(
        dbc: Database.Connection,
        requestingUserId: EvakaUserId,
        ssns: List<Ssn>,
    ): Pair<List<PersonId>, List<VtjPerson>> {
        val (existingIds, missingSSNs) =
            dbc.read { tx ->
                val (found, notFound) = tx.getPersonIdsBySsns(ssns)
                found.values.toList() to notFound
            }
        val newPersons = missingSSNs.map {
            vtjClient.fetchPerson(VtjClient.DetailsQuery(requestingUserId, it))
        }
        return existingIds to newPersons
    }
}

data class GetOrCreateResult(val person: PersonDTO, val created: Boolean)
