// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.dvv.custodyCounts
import evaka.core.dvv.startDate
import evaka.core.family.FridgeFamilyService
import evaka.core.identity.Ssn
import evaka.core.person.getDependantGuardians
import evaka.core.person.getPersonById
import evaka.core.person.getPersonBySSN
import evaka.core.person.isMinor
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.async.PermanentAsyncJobFailure
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import fi.espoo.voltti.logging.loggers.info
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class UpdateFromVtjAsyncJobProcessor(
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val vtjSyncService: VtjSyncService,
    private val fridgeFamilyService: FridgeFamilyService,
) {
    init {
        asyncJobRunner.registerHandler(::updateFromVtj)
        asyncJobRunner.registerHandler(::syncMissingVtjRelations)
        asyncJobRunner.registerHandler { db, clock, msg: AsyncJob.SyncMissingVtjRelationsOfPerson ->
            refreshPersonAndFamilies(db, clock, msg.personId)
        }
    }

    fun updateFromVtj(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.UpdateFromVtj,
    ) {
        val ssn =
            Ssn.parse(msg.ssn)
                ?: throw PermanentAsyncJobFailure("Invalid SSN in UpdateFromVtj payload")
        val person = db.read { it.getPersonBySSN(ssn.value) }
        if (person == null) {
            logger.info { "Person of an UpdateFromVtj job is not in eVaka, nothing to refresh" }
            return
        }
        val modifications = msg.modifications
        if (modifications == null) {
            refreshPersonAndFamilies(db, clock, person.id)
            return
        }

        val today = clock.today()
        val stored = db.read { it.getStoredVtjPerson(person.id, today) }
        val predicted = stored?.let { applyDvvChanges(it, modifications, today) }
        ComparingVtjClient.withoutComparison { refreshPersonAndFamilies(db, clock, person.id) }
        val refreshed = db.read { it.getStoredVtjPerson(person.id, today) }
        if (predicted == null || refreshed == null) return

        val groups = modifications.flatMap { it.tietoryhmat }
        val differences = compareStoredVtjPersons(predicted.person, refreshed)
        val result =
            if (differences.isEmpty()) ComparisonResult.EQUAL else ComparisonResult.DIFFERENT
        logger.info(
            mapOf(
                "path" to "MODIFICATIONS",
                "changes" to
                    groups.map { "${it.tietoryhma}:${it.muutosattribuutti}" }.distinct().sorted(),
                "result" to result.name,
                "differences" to differences.map { mapOf("field" to it.field) + it.details },
                "unknown" to predicted.unknown,
                "newRelatives" to predicted.newRelatives.size,
                "futureGroups" to
                    groups
                        .filter { it.startDate()?.isAfter(today) == true }
                        .groupingBy { it.tietoryhma }
                        .eachCount(),
                "custody" to custodyCounts(groups),
                "personId" to person.id.toString(),
                "targetId" to ssn.value.take(6),
            )
        ) {
            "VTJ comparison of DVV modifications: ${result.name}"
        }
    }

    fun syncMissingVtjRelations(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SyncMissingVtjRelations,
    ) {
        vtjSyncService.syncMissingRelations(db, clock, msg.personIds).forEach {
            addChildrenToFridgeFamilies(db, clock, it)
        }
    }

    private fun refreshPersonAndFamilies(
        db: Database.Connection,
        clock: EvakaClock,
        personId: PersonId,
    ) {
        vtjSyncService.refreshPersons(
            db,
            AuthenticatedUser.SystemInternalUser.evakaUserId,
            clock,
            listOf(personId),
        )
        addChildrenToFridgeFamilies(db, clock, personId)
    }

    private fun addChildrenToFridgeFamilies(
        db: Database.Connection,
        clock: EvakaClock,
        personId: PersonId,
    ) {
        val today = clock.today()
        val person = db.read { it.getPersonById(personId) } ?: return
        val guardianIds =
            if (isMinor(person.dateOfBirth, today)) {
                db.read { it.getDependantGuardians(personId) }.map { it.id }
            } else {
                emptyList()
            }
        db.read { it.getVtjSyncPersons(listOf(personId) + guardianIds, today) }
            .filter { it.dependantSync }
            .forEach { fridgeFamilyService.addChildrenToFridgeFamily(db, clock, it.id) }
    }
}
