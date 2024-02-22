// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.pis.getDependantGuardians
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.personIsHeadOfFamily
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import java.time.LocalDate
import java.time.Period
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class FridgeFamilyService(
    private val personService: PersonService,
    private val parentshipService: ParentshipService
) {
    fun doVTJRefresh(db: Database.Connection, msg: AsyncJob.VTJRefresh, clock: EvakaClock) {
        updateGuardianAndFamilyFromVtj(
            db,
            AuthenticatedUser.SystemInternalUser,
            clock,
            msg.personId
        )
    }

    fun updateGuardianOrChildFromVtj(
        db: Database.Connection,
        user: AuthenticatedUser,
        clock: EvakaClock,
        personId: PersonId
    ) {
        val person = db.read { tx -> tx.getPersonById(personId) } ?: return
        val age = Period.between(person.dateOfBirth, clock.today()).years
        if (age >= 18) {
            updateGuardianAndFamilyFromVtj(db, user, clock, personId)
        } else {
            val currentGuardians =
                db.read { tx -> tx.getDependantGuardians(personId) }.map { it.id }.toSet()
            val updatedGuardians =
                db.transaction { tx ->
                        personService.getGuardians(tx, user, personId, forceRefresh = true)
                    }
                    .map { it.id }
                    .toSet()
            (currentGuardians + updatedGuardians).forEach { guardianId ->
                updateGuardianAndFamilyFromVtj(db, user, clock, guardianId)
            }
        }
    }

    private fun updateGuardianAndFamilyFromVtj(
        db: Database.Connection,
        user: AuthenticatedUser,
        clock: EvakaClock,
        personId: PersonId
    ) {
        logger.info("Refreshing $personId from VTJ")
        val targetPerson =
            db.transaction {
                personService.getPersonWithChildren(
                    it,
                    user = user,
                    id = personId,
                    forceRefresh = true
                )
            }
        if (targetPerson != null) {
            logger.info("Person to refresh has ${targetPerson.children.size} children")

            val partner =
                db.read { getPartnerId(it, clock, personId) }
                    ?.also { logger.info("Person has fridge partner $it") }
                    ?.let { partnerId ->
                        db.transaction {
                            personService.getPersonWithChildren(
                                it,
                                user = user,
                                id = partnerId,
                                forceRefresh = true
                            )
                        }
                    }
                    ?.takeIf { livesInSameAddress(it.address, targetPerson.address) }

            val head =
                if (
                    partner != null &&
                        db.read { it.personIsHeadOfFamily(partner.id, clock.today()) }
                )
                    partner
                else targetPerson

            if (partner != null) {
                logger.info(
                    "Partner lives in the same address and has ${partner.children.size} children"
                )
                if (head == partner) {
                    logger.info(
                        "Partner has a fridge family, so adding the new children to that family"
                    )
                }
            }

            val children = targetPerson.children + (partner?.children ?: emptyList())

            val currentFridgeChildren = db.read { getCurrentFridgeChildren(it, clock, head.id) }
            val newChildrenInSameAddress =
                children
                    .asSequence()
                    .distinct()
                    .filter { child -> !child.socialSecurityNumber.isNullOrBlank() }
                    .filter { child -> child.dateOfBirth.isAfter(clock.today().minusYears(18)) }
                    .filter { livesInSameAddress(it.address, head.address) }
                    .filter { !currentFridgeChildren.contains(it.id) }
                    .toList()

            newChildrenInSameAddress.forEach { child ->
                try {
                    val startDate =
                        if (
                            childShouldBeAddedToFamilyStartingFromBirthday(
                                clock.today(),
                                child.dateOfBirth
                            )
                        ) {
                            child.dateOfBirth
                        } else {
                            clock.today()
                        }
                    db.transaction { tx ->
                        parentshipService.createParentship(
                            tx,
                            clock,
                            childId = child.id,
                            headOfChildId = head.id,
                            startDate = startDate,
                            endDate = child.dateOfBirth.plusYears(18).minusDays(1)
                        )
                    }
                    logger.info("Child ${child.id} added to head of child ${head.id}")
                } catch (e: Exception) {
                    logger.warn("Problem while adding a child to fridge family:", e)
                }
            }
        }
    }

    fun getPartnerId(tx: Database.Read, clock: EvakaClock, personId: PersonId): PersonId? {
        // language=sql
        val sql =
            """
            SELECT p2.person_id AS partner_id
            FROM fridge_partner p1
            LEFT OUTER JOIN fridge_partner p2 ON p1.partnership_id = p2.partnership_id AND p1.person_id != p2.person_id
            WHERE p1.person_id = :personId AND daterange(p1.start_date, p1.end_date, '[]') @> :today AND p1.conflict = false AND p2.conflict = false
            """
                .trimIndent()

        @Suppress("DEPRECATION")
        return tx.createQuery(sql)
            .bind("today", clock.today())
            .bind("personId", personId)
            .exactlyOneOrNull<PersonId>()
    }

    private fun getCurrentFridgeChildren(
        tx: Database.Read,
        clock: EvakaClock,
        personId: PersonId
    ): Set<ChildId> {
        // language=sql
        val sql =
            """
            SELECT child_id FROM fridge_child 
            WHERE head_of_child = :personId AND daterange(start_date, end_date, '[]') @> :today AND conflict = false
            """
                .trimIndent()
        @Suppress("DEPRECATION")
        return tx.createQuery(sql)
            .bind("today", clock.today())
            .bind("personId", personId)
            .toSet<ChildId>()
    }

    private fun livesInSameAddress(
        address1: PersonAddressDTO,
        address2: PersonAddressDTO
    ): Boolean {
        val sameResidencyCode = address1.residenceCode == address2.residenceCode
        val sameStreetAddress = address1.streetAddress == address2.streetAddress
        return sameResidencyCode || sameStreetAddress
    }

    // 3 months is arbitrary
    private fun childShouldBeAddedToFamilyStartingFromBirthday(
        today: LocalDate,
        birthday: LocalDate
    ) = today <= birthday.plusMonths(3)
}
