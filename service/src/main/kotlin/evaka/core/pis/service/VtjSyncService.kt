// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.identity.Ssn
import evaka.core.pis.ApplyResult
import evaka.core.pis.applyDeathEffects
import evaka.core.pis.applyDependants
import evaka.core.pis.applyGuardians
import evaka.core.pis.applyPerson
import evaka.core.pis.clearDependantsSynced
import evaka.core.pis.getPersonById
import evaka.core.pis.getPersonIdsBySsns
import evaka.core.pis.isDependantSyncPerson
import evaka.core.pis.isMinor
import evaka.core.pis.setSsnFromVtj
import evaka.core.shared.ChildId
import evaka.core.shared.EvakaUserId
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.vtjclient.dto.VtjPerson
import evaka.core.vtjclient.service.persondetails.VtjClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class VtjSyncService(
    private val vtjClient: VtjClient,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
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
            val vtjPerson =
                vtjClient.fetchPerson(VtjClient.DetailsQuery(requestingUserId, person.ssn))
            dbc.transaction { tx ->
                val id = applyFromVtj(tx, clock.now(), vtjPerson)
                tx.clearDependantsSynced(id)
            }
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

    fun applyFromVtj(tx: Database.Transaction, now: HelsinkiDateTime, person: VtjPerson): PersonId =
        applyDeathEffectsOf(tx, now, tx.applyPerson(person, now))

    fun applySsnFromVtj(
        tx: Database.Transaction,
        now: HelsinkiDateTime,
        personId: PersonId,
        person: VtjPerson,
    ) {
        val _ = applyDeathEffectsOf(tx, now, tx.setSsnFromVtj(personId, person, now))
    }

    private fun applyDeathEffectsOf(
        tx: Database.Transaction,
        now: HelsinkiDateTime,
        result: ApplyResult,
    ): PersonId {
        if (result.newDateOfDeath != null) {
            tx.applyDeathEffects(asyncJobRunner, now, result.id, result.newDateOfDeath)
        }
        return result.id
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
            val childId = applyFromVtj(tx, now, snapshot.person)
            val newGuardianIds = newGuardians.map { applyFromVtj(tx, now, it) }
            tx.applyGuardians(childId, (existingGuardianIds + newGuardianIds).toSet(), now)
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
            val guardianId = applyFromVtj(tx, now, snapshot.person)
            val newDependantIds = newDependants.map { applyFromVtj(tx, now, it) }
            tx.applyDependants(guardianId, (existingDependantIds + newDependantIds).toSet(), now)
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
