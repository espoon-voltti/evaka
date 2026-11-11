// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.identity.Ssn
import evaka.core.identity.getDobFromSsn
import evaka.core.person.Person
import evaka.core.person.applyDeathEffects
import evaka.core.person.getPersonById
import evaka.core.person.getPersonBySSN
import evaka.core.person.getPersonIdsBySsns
import evaka.core.person.isMinor
import evaka.core.person.updatePersonSsnAddingDisabled
import evaka.core.shared.ChildId
import evaka.core.shared.EvakaUserId
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.async.AsyncJobType
import evaka.core.shared.async.removeUnclaimedJobs
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.NotFound
import fi.espoo.voltti.logging.loggers.info
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
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
        val person =
            sync(
                    dbc,
                    requestingUserId,
                    clock,
                    listOf(SyncTarget(ssn, guardians = false, dependants = false)),
                )
                .getValue(ssn)
        return GetOrCreateResult(person, created = true)
    }

    fun ensureDependantsSynced(
        dbc: Database.Connection,
        requestingUserId: EvakaUserId,
        clock: EvakaClock,
        personId: PersonId,
    ) {
        val person = dbc.read { it.getVtjSyncPerson(personId, clock.today()) } ?: return
        if (person.ssn == null || person.vtjDependantsQueried != null || !person.dependantSync)
            return
        val _ =
            sync(
                dbc,
                requestingUserId,
                clock,
                listOf(SyncTarget(person.ssn, guardians = false, dependants = true)),
            )
    }

    /** Syncs the dependants of a guardian who is about to become a head of family */
    fun syncDependantsForFamilySetup(
        dbc: Database.Connection,
        requestingUserId: EvakaUserId,
        clock: EvakaClock,
        personId: PersonId,
    ) {
        val person = dbc.read { it.getVtjSyncPerson(personId, clock.today()) } ?: return
        if (person.ssn == null || person.vtjDependantsQueried != null) return
        val _ =
            sync(
                dbc,
                requestingUserId,
                clock,
                listOf(SyncTarget(person.ssn, guardians = false, dependants = true)),
            )
    }

    fun ensureGuardiansSynced(
        dbc: Database.Connection,
        requestingUserId: EvakaUserId,
        clock: EvakaClock,
        childId: ChildId,
    ) {
        val child = dbc.read { it.getVtjSyncPerson(childId, clock.today()) } ?: return
        if (child.ssn == null || !isMinor(child.dateOfBirth, clock.today())) return
        if (child.vtjGuardiansQueried != null) return
        val _ =
            sync(
                dbc,
                requestingUserId,
                clock,
                listOf(SyncTarget(child.ssn, guardians = true, dependants = false)),
            )
    }

    fun refreshPersons(
        dbc: Database.Connection,
        requestingUserId: EvakaUserId,
        clock: EvakaClock,
        personIds: Collection<PersonId>,
    ) {
        val today = clock.today()
        val persons = dbc.read { it.getVtjSyncPersons(personIds, today) }
        val _ = sync(dbc, requestingUserId, clock, refreshTargets(persons, today))
    }

    /**
     * Unlike refreshPersons, this syncs the dependants also of a person who is not a dependant-sync
     * person, and then marks them synced.
     */
    fun refreshPersonsGreedy(
        dbc: Database.Connection,
        requestingUserId: EvakaUserId,
        clock: EvakaClock,
        personIds: Collection<PersonId>,
    ) {
        val today = clock.today()
        val targets =
            dbc.read { it.getVtjSyncPersons(personIds, today) }
                .mapNotNull { person ->
                    person.ssn?.let {
                        SyncTarget(
                            it,
                            guardians = isMinor(person.dateOfBirth, today),
                            dependants = true,
                        )
                    }
                }
        val _ = sync(dbc, requestingUserId, clock, targets)
    }

    fun addSsn(
        dbc: Database.Connection,
        requestingUserId: EvakaUserId,
        clock: EvakaClock,
        personId: PersonId,
        ssn: Ssn,
    ): Person {
        dbc.read { tx ->
            val person = tx.getPersonById(personId) ?: throw NotFound("Person $personId not found")
            if (person.ssn != null) throw BadRequest("User already has ssn")
            if (tx.getPersonBySSN(ssn.value) != null) {
                throw Conflict("User with same ssn already exists")
            }
        }
        val vtjPerson =
            vtjClient.fetchPersons(requestingUserId, listOf(ssn))[ssn]
                ?: throw NotFound("VTJ person not found")
        if (vtjPerson.ssn != ssn) {
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

    fun previewPerson(requestingUserId: EvakaUserId, ssn: Ssn): VtjPersonPreview {
        val person =
            vtjClient.fetchPersons(requestingUserId, listOf(ssn))[ssn]
                ?: throw NotFound("VTJ person not found")
        return VtjPersonPreview(
            socialSecurityNumber = person.ssn.value,
            firstName = person.firstNames,
            lastName = person.lastName,
            dateOfBirth = getDobFromSsn(person.ssn.value),
            streetAddress = person.residentialAddress?.streetAddress ?: "",
            restrictedDetailsEnabled = person.restrictedDetails?.enabled ?: false,
        )
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
                setOf(
                    AsyncJobType(AsyncJob.SyncMissingVtjRelations::class),
                    AsyncJobType(AsyncJob.SyncMissingVtjRelationsOfPerson::class),
                )
            )
            val counts = tx.countPersonsToSync(now)
            val ids = tx.getPersonsToSync(now, limit)
            if (ids.isNotEmpty()) {
                asyncJobRunner.plan(
                    tx,
                    listOf(AsyncJob.SyncMissingVtjRelations(ids)),
                    retryCount = 3,
                    runAt = now,
                )
            }
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

    /**
     * Refreshes the persons in one batch and returns the ids of the refreshed persons. A person
     * that VTJ does not return as requested, or every person if fetching or saving the batch fails,
     * gets a [AsyncJob.SyncMissingVtjRelationsOfPerson] of their own, so that one person cannot
     * stop the others.
     */
    fun syncMissingRelations(
        dbc: Database.Connection,
        clock: EvakaClock,
        personIds: List<PersonId>,
    ): List<PersonId> {
        val today = clock.today()
        val persons = dbc.read { it.getVtjSyncPersons(personIds, today) }
        val (savedIds, retryIds) =
            try {
                val batch =
                    fetch(
                        dbc,
                        AuthenticatedUser.SystemInternalUser.evakaUserId,
                        refreshTargets(persons, today),
                    )
                val savedIds =
                    dbc.transaction { tx -> save(tx, clock.now(), batch) }.values.map { it.id }
                if (batch.failures.isNotEmpty()) {
                    logger.warn {
                        "VTJ sync failed for ${batch.failures.size} persons: ${batch.failures.values.groupingBy { it }.eachCount()}"
                    }
                }
                savedIds to persons.filter { it.ssn in batch.failures }.map { it.id }
            } catch (e: Exception) {
                logger.error(e) { "VTJ sync of ${personIds.size} persons failed" }
                emptyList<PersonId>() to personIds
            }
        if (retryIds.isNotEmpty()) {
            logger.warn { "Planning a VTJ sync job of their own for ${retryIds.size} persons" }
            dbc.transaction { tx ->
                asyncJobRunner.plan(
                    tx,
                    retryIds.map { AsyncJob.SyncMissingVtjRelationsOfPerson(it) },
                    retryCount = 3,
                    runAt = clock.now(),
                )
            }
        }
        return savedIds
    }

    private fun refreshTargets(persons: List<VtjSyncPerson>, today: LocalDate): List<SyncTarget> =
        persons.mapNotNull { person ->
            if (person.ssn == null) return@mapNotNull null
            val minor = isMinor(person.dateOfBirth, today)
            logger.info {
                "Refreshing person ${person.id} from VTJ (minor: $minor, dependant-sync: ${person.dependantSync})"
            }
            SyncTarget(person.ssn, guardians = minor, dependants = person.dependantSync)
        }

    /** Throws if VTJ does not return a person of [targets] as requested. */
    private fun sync(
        dbc: Database.Connection,
        requestingUserId: EvakaUserId,
        clock: EvakaClock,
        targets: List<SyncTarget>,
    ): Map<Ssn, Person> {
        val batch = fetch(dbc, requestingUserId, targets)
        when (batch.failures.values.firstOrNull()) {
            SyncFailure.NOT_FOUND,
            SyncFailure.RELATIVE_NOT_FOUND -> throw NotFound("VTJ person not found")
            SyncFailure.DIFFERENT_SSN ->
                throw BadRequest("VTJ returned a different SSN than the requested one")
            null -> {}
        }
        return dbc.transaction { tx -> save(tx, clock.now(), batch) }
    }

    private fun fetch(
        dbc: Database.Connection,
        requestingUserId: EvakaUserId,
        targets: List<SyncTarget>,
    ): SyncBatch {
        val basic =
            vtjClient.fetchPersons(
                requestingUserId,
                targets.filter { !it.guardians && !it.dependants }.map { it.ssn },
            )
        val withGuardians =
            vtjClient.fetchWithGuardians(
                requestingUserId,
                targets.filter { it.guardians }.map { it.ssn },
            )
        val withDependants =
            vtjClient.fetchWithDependants(
                requestingUserId,
                targets.filter { it.dependants }.map { it.ssn },
            )
        val fetched =
            basic +
                withGuardians.mapValues { it.value.person } +
                withDependants.mapValues { it.value.person }
        val fetchFailures =
            targets
                .mapNotNull { target ->
                    val person = fetched[target.ssn]
                    when {
                        person == null -> target.ssn to SyncFailure.NOT_FOUND
                        person.ssn != target.ssn -> target.ssn to SyncFailure.DIFFERENT_SSN
                        else -> null
                    }
                }
                .toMap()
        val fetchedGuardians =
            withGuardians.filterKeys { it !in fetchFailures }.mapValues { it.value.guardians }
        val fetchedDependants =
            withDependants.filterKeys { it !in fetchFailures }.mapValues { it.value.dependants }
        val (existingRelatives, relativesNotInEvaka) =
            dbc.read {
                it.getPersonIdsBySsns(
                    (fetchedGuardians.values.flatten() + fetchedDependants.values.flatten())
                        .distinct()
                )
            }
        val missingRelatives = relativesNotInEvaka.filterNot { it in fetched }
        val newRelatives = vtjClient.fetchPersons(requestingUserId, missingRelatives)
        val relativesNotFound = missingRelatives.filterNot { it in newRelatives }.toSet()
        val failures =
            fetchFailures +
                listOf(fetchedGuardians, fetchedDependants)
                    .flatMap { relations ->
                        relations.filterValues { it.any(relativesNotFound::contains) }.keys
                    }
                    .associateWith { SyncFailure.RELATIVE_NOT_FOUND }
        return SyncBatch(
            targets = targets,
            persons = fetched.filterKeys { it !in failures },
            guardians = fetchedGuardians.filterKeys { it !in failures },
            dependants = fetchedDependants.filterKeys { it !in failures },
            existingRelatives = existingRelatives,
            newRelatives = newRelatives,
            failures = failures,
        )
    }

    private fun save(
        tx: Database.Transaction,
        now: HelsinkiDateTime,
        batch: SyncBatch,
    ): Map<Ssn, Person> {
        val saved = batch.persons.mapValues { applyFromVtj(tx, now, it.value) }
        val relativeIds =
            batch.existingRelatives +
                batch.newRelatives.mapValues { applyFromVtj(tx, now, it.value).id }
        fun idsOf(ssns: List<Ssn>) = ssns.mapNotNull { saved[it]?.id ?: relativeIds[it] }.toSet()
        batch.guardians.forEach { (ssn, guardians) ->
            tx.applyGuardians(saved.getValue(ssn).id, idsOf(guardians), now)
        }
        batch.dependants.forEach { (ssn, dependants) ->
            tx.applyDependants(saved.getValue(ssn).id, idsOf(dependants), now)
        }
        batch.targets
            .filter { !it.guardians && !it.dependants }
            .mapNotNull { saved[it.ssn] }
            .forEach { tx.clearDependantsSynced(it.id) }
        return saved
    }

    private fun applyFromVtj(
        tx: Database.Transaction,
        now: HelsinkiDateTime,
        person: VtjPerson,
    ): Person {
        val result = tx.applyPerson(person, now)
        if (result.newDateOfDeath != null) {
            tx.applyDeathEffects(asyncJobRunner, now, result.id, result.newDateOfDeath)
        }
        return result.person
    }
}

/** Which VTJ data a sync writes for the person: without relations, the person only. */
private data class SyncTarget(val ssn: Ssn, val guardians: Boolean, val dependants: Boolean)

private enum class SyncFailure {
    NOT_FOUND,
    DIFFERENT_SSN,
    RELATIVE_NOT_FOUND,
}

/**
 * [persons], [guardians] and [dependants] have only the targets that have no [failures].
 * [newRelatives] are the relatives that are not in eVaka, keyed by the SSN that VTJ gave for the
 * relation.
 */
private class SyncBatch(
    val targets: List<SyncTarget>,
    val persons: Map<Ssn, VtjPerson>,
    val guardians: Map<Ssn, List<Ssn>>,
    val dependants: Map<Ssn, List<Ssn>>,
    val existingRelatives: Map<Ssn, PersonId>,
    val newRelatives: Map<Ssn, VtjPerson>,
    val failures: Map<Ssn, SyncFailure>,
)

data class GetOrCreateResult(val person: Person, val created: Boolean)

data class VtjPersonPreview(
    val socialSecurityNumber: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val streetAddress: String,
    val restrictedDetailsEnabled: Boolean,
)
