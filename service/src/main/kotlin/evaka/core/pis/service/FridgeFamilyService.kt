// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.pis.Creator
import evaka.core.pis.getDependantGuardians
import evaka.core.pis.getFosterParents
import evaka.core.pis.getGuardianDependants
import evaka.core.pis.getPersonById
import evaka.core.pis.isDependantSyncPerson
import evaka.core.pis.isMinor
import evaka.core.pis.personIsHeadOfFamily
import evaka.core.shared.ChildId
import evaka.core.shared.EvakaUserId
import evaka.core.shared.PersonId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class FridgeFamilyService(
    private val personService: PersonService,
    private val vtjSyncService: VtjSyncService,
    private val parentshipService: ParentshipService,
) {
    fun refreshFamily(
        db: Database.Connection,
        requestingUserId: EvakaUserId,
        clock: EvakaClock,
        personId: PersonId,
    ) {
        val today = clock.today()
        val person = db.read { it.getPersonById(personId) } ?: return
        val guardianIds =
            if (isMinor(person.dateOfBirth, today)) {
                vtjSyncService.refreshPerson(db, requestingUserId, clock, personId)
                val childGuardianIds = db.read { it.getDependantGuardians(personId) }.map { it.id }
                childGuardianIds.forEach {
                    vtjSyncService.refreshPersonAndDependants(db, requestingUserId, clock, it)
                }
                childGuardianIds +
                    listOf(personId).filter {
                        db.read { tx -> tx.isDependantSyncPerson(it, today) }
                    }
            } else {
                vtjSyncService.refreshPersonAndDependants(db, requestingUserId, clock, personId)
                listOf(personId)
            }
        guardianIds
            .mapNotNull { id -> db.read { getPartnerId(it, clock, id) } }
            .filter { it !in guardianIds }
            .distinct()
            .forEach { vtjSyncService.refreshPersonAndDependants(db, requestingUserId, clock, it) }
        guardianIds.forEach { addChildrenToFridgeFamily(db, clock, it) }
    }

    fun addChildrenToFridgeFamilies(
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
        (listOf(personId) + guardianIds)
            .filter { id -> db.read { it.isDependantSyncPerson(id, today) } }
            .forEach { addChildrenToFridgeFamily(db, clock, it) }
    }

    fun addChildrenToFridgeFamily(
        db: Database.Connection,
        clock: EvakaClock,
        guardianId: PersonId,
    ) {
        val today = clock.today()
        val guardian = db.read { it.getPersonById(guardianId) } ?: return
        if (guardian.dateOfDeath != null) return

        val partnerAtSameAddress =
            db.read { getPartnerId(it, clock, guardianId) }
                ?.also { logger.info { "Person $guardianId has fridge partner $it" } }
                ?.let { partnerId -> db.read { it.getPersonById(partnerId) } }
                ?.takeIf { personService.personsLiveInTheSameAddress(it, guardian) }

        val head =
            partnerAtSameAddress?.takeIf { partner ->
                db.read { it.personIsHeadOfFamily(partner.id, today) }
            } ?: guardian
        if (partnerAtSameAddress != null) {
            logger.info {
                "Partner ${partnerAtSameAddress.id} lives at the same address, head is ${head.id}"
            }
        }

        val childrenToAdd = db.read { tx ->
            val currentFridgeChildren = getCurrentFridgeChildren(tx, clock, head.id)
            listOfNotNull(guardian, partnerAtSameAddress)
                .flatMap { tx.getGuardianDependants(it.id) }
                .asSequence()
                .distinctBy { it.id }
                .filter { child -> child.ssn != null }
                .filter { child -> isMinor(child.dateOfBirth, today) }
                .filter { personService.personsLiveInTheSameAddress(it, head) }
                .filter { !currentFridgeChildren.contains(it.id) }
                .filter { child ->
                    tx.getFosterParents(child.id).none { it.validDuring.includes(today) }
                }
                .toList()
        }

        childrenToAdd.forEach { child ->
            try {
                val startDate =
                    if (childShouldBeAddedToFamilyStartingFromBirthday(today, child.dateOfBirth)) {
                        child.dateOfBirth
                    } else {
                        today
                    }
                db.transaction { tx ->
                    parentshipService.createParentship(
                        tx,
                        clock,
                        childId = child.id,
                        headOfChildId = head.id,
                        startDate = startDate,
                        endDate = child.dateOfBirth.plusYears(18).minusDays(1),
                        Creator.DVV,
                    )
                }
                logger.info { "Child ${child.id} added to head of child ${head.id}" }
            } catch (e: Exception) {
                logger.warn(e) { "Problem while adding a child to fridge family:" }
            }
        }
    }

    fun getPartnerId(tx: Database.Read, clock: EvakaClock, personId: PersonId): PersonId? {
        return tx.createQuery {
                sql(
                    """
SELECT p2.person_id AS partner_id
FROM fridge_partner p1
LEFT OUTER JOIN fridge_partner p2 ON p1.partnership_id = p2.partnership_id AND p1.person_id != p2.person_id
WHERE p1.person_id = ${bind(personId)} AND daterange(p1.start_date, p1.end_date, '[]') @> ${bind(clock.today())} AND p1.conflict = false AND p2.conflict = false
"""
                )
            }
            .exactlyOneOrNull<PersonId>()
    }

    private fun getCurrentFridgeChildren(
        tx: Database.Read,
        clock: EvakaClock,
        personId: PersonId,
    ): Set<ChildId> {
        return tx.createQuery {
                sql(
                    """
SELECT child_id FROM fridge_child
WHERE head_of_child = ${bind(personId)} AND daterange(start_date, end_date, '[]') @> ${bind(clock.today())} AND conflict = false
"""
                )
            }
            .toSet<ChildId>()
    }

    // 3 months is arbitrary
    private fun childShouldBeAddedToFamilyStartingFromBirthday(
        today: LocalDate,
        birthday: LocalDate,
    ) = today <= birthday.plusMonths(3)
}
