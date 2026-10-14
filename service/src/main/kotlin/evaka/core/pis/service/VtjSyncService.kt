// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.identity.Ssn
import evaka.core.pis.applyDependants
import evaka.core.pis.applyGuardians
import evaka.core.pis.applyPerson
import evaka.core.pis.clearDependantsSynced
import evaka.core.pis.getPersonById
import evaka.core.pis.getPersonBySSN
import evaka.core.pis.isDependantSyncPerson
import evaka.core.pis.isMinor
import evaka.core.shared.ChildId
import evaka.core.shared.PersonId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.vtjclient.dto.VtjPerson
import evaka.core.vtjclient.dto.VtjRelative
import evaka.core.vtjclient.service.persondetails.VtjClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class VtjSyncService(private val vtjClient: VtjClient) {
    fun getOrCreateChildWithGuardians(
        dbc: Database.Connection,
        user: AuthenticatedUser,
        clock: EvakaClock,
        ssn: Ssn,
    ): ChildId {
        val child = dbc.read { it.getPersonBySSN(ssn.value) }
        if (child != null) {
            ensureGuardiansSynced(dbc, user, clock, child.id)
            return child.id
        }
        syncGuardians(dbc, user, clock, ssn)
        return dbc.read { it.getPersonBySSN(ssn.value) }?.id
            ?: error("Child was not created from VTJ")
    }

    fun ensureDependantsSynced(
        dbc: Database.Connection,
        user: AuthenticatedUser,
        clock: EvakaClock,
        personId: PersonId,
    ) {
        val person = dbc.read { it.getPersonById(personId) } ?: return
        if (person.ssn == null || person.vtjDependantsQueried != null) return
        if (!dbc.read { it.isDependantSyncPerson(personId, clock.today()) }) return
        syncDependants(dbc, user, clock, person.ssn)
    }

    /** Syncs the dependants of a guardian who is about to become a head of family */
    fun syncDependantsForFamilySetup(
        dbc: Database.Connection,
        user: AuthenticatedUser,
        clock: EvakaClock,
        personId: PersonId,
    ) {
        val person = dbc.read { it.getPersonById(personId) } ?: return
        if (person.ssn == null || person.vtjDependantsQueried != null) return
        syncDependants(dbc, user, clock, person.ssn)
    }

    fun ensureGuardiansSynced(
        dbc: Database.Connection,
        user: AuthenticatedUser,
        clock: EvakaClock,
        childId: ChildId,
    ) {
        val child = dbc.read { it.getPersonById(childId) } ?: return
        if (child.ssn == null || !isMinor(child.dateOfBirth, clock.today())) return
        if (child.vtjGuardiansQueried != null) return
        syncGuardians(dbc, user, clock, child.ssn)
    }

    fun refreshPerson(
        dbc: Database.Connection,
        user: AuthenticatedUser,
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
        if (minor) syncGuardians(dbc, user, clock, person.ssn)
        if (dependantSync) syncDependants(dbc, user, clock, person.ssn)
        if (!minor && !dependantSync) {
            val vtjPerson =
                vtjClient.fetchPerson(VtjClient.DetailsQuery(user.evakaUserId, person.ssn))
            dbc.transaction { tx ->
                val id = applyFromVtj(tx, clock.now(), vtjPerson)
                tx.clearDependantsSynced(id)
            }
        }
    }

    fun applyFromVtj(tx: Database.Transaction, now: HelsinkiDateTime, person: VtjPerson): PersonId =
        tx.applyPerson(person, now).id

    private fun syncGuardians(
        dbc: Database.Connection,
        user: AuthenticatedUser,
        clock: EvakaClock,
        ssn: Ssn,
    ) {
        val snapshot = vtjClient.fetchWithGuardians(VtjClient.DetailsQuery(user.evakaUserId, ssn))
        val newGuardians = fetchNewRelatives(dbc, user, snapshot.relatives)
        dbc.transaction { tx ->
            val now = clock.now()
            val childId = applyFromVtj(tx, now, snapshot.person)
            newGuardians.forEach {
                val _ = applyFromVtj(tx, now, it)
            }
            tx.applyGuardians(childId, tx.getRelativeIds(snapshot.relatives), now)
        }
    }

    private fun syncDependants(
        dbc: Database.Connection,
        user: AuthenticatedUser,
        clock: EvakaClock,
        ssn: Ssn,
    ) {
        val snapshot = vtjClient.fetchWithDependants(VtjClient.DetailsQuery(user.evakaUserId, ssn))
        val newDependants = fetchNewRelatives(dbc, user, snapshot.relatives)
        dbc.transaction { tx ->
            val now = clock.now()
            val guardianId = applyFromVtj(tx, now, snapshot.person)
            newDependants.forEach {
                val _ = applyFromVtj(tx, now, it)
            }
            tx.applyDependants(guardianId, tx.getRelativeIds(snapshot.relatives), now)
        }
    }

    private fun fetchNewRelatives(
        dbc: Database.Connection,
        user: AuthenticatedUser,
        relatives: List<VtjRelative>,
    ): List<VtjPerson> =
        dbc.read { tx -> relatives.filter { tx.getPersonBySSN(it.ssn.value) == null } }
            .map { vtjClient.fetchPerson(VtjClient.DetailsQuery(user.evakaUserId, it.ssn)) }

    private fun Database.Read.getRelativeIds(relatives: List<VtjRelative>): Set<PersonId> =
        relatives
            .map { getPersonBySSN(it.ssn.value)?.id ?: error("VTJ relative is not in the DB") }
            .toSet()
}
